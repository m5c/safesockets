package com.m5c.safesockets;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

/**
 *
 * @author m5c
 */
public final class SafeSocket extends MessageHandler implements Terminatable
{

    private final int INITIAL_HEART_BEAT_ID = 0;

    // Needed to distinguish messages with identical content (have same hash otherwise)
    private int messageSalt = 0;

    // Tells whether the SafeSocket is in Client or Server mode (set in ctor)
    private final boolean serverMode;

    // The inner socket used for communication
    private final Socket socket;

    // Amount of time between the sending of two consecutive heartbeats.
    private final int period;

    // Amount of time for the reply to arrive (should be less then period)
    private final int timeout;

    // Each heartBeat or send message requires an ack within the timeout interval - this list is needed to defuse the threads killing the connection on reception of the ack 
    // Note: on client side the heart beats themselves are interpreted as signal to defuse the associated connectionKiller
    private final Map<String, Terminator> connectionKillers = Collections.synchronizedMap(new LinkedHashMap<String, Terminator>());

    // Each message sent has an attached CountDownLatch, so we can block in the sending method until the Ack (or timeout for the Ack) has arrived.
    private final Map<String, CountDownLatch> ackBlockers = Collections.synchronizedMap(new LinkedHashMap<String, CountDownLatch>());

    private boolean socketAlive = false;
    private PrintWriter printWriter;

    // Collections for the observers (incoming messages and connection breakdown)
    private final Collection<MessageObserver> messageObservers;
    private final Collection<BreakdownObserver> breakdownObservers;

    // Filter that intercepts any outward message and can manipulate the content.
    private final Filter outFilter;

    /**
     * Constructor for server side SafeSocket. Calling this constructor blocks
     * until a client connection is established.
     *
     * @param port: The server port
     * @param period: Time between two HeartBeats
     * @param timeout: Max time from sending of heartbeat to reception of the
     * corresponding ACK before connection is considered dead.
     * @param messageObservers: A Collection of Observers to be notified on
     * message reception.
     * @throws IOException
     */
    public SafeSocket(ServerSocket serverSocket, int period, int timeout, Collection<MessageObserver> messageObservers, Collection<BreakdownObserver> breakdownObservers) throws IOException
    {
        this(serverSocket, period, timeout, messageObservers, breakdownObservers, new DefaultFilter(), new DefaultFilter());
    }

    /**
     * Constructor for server side SafeSocket with custom I/O filters. Calling
     * this constructor blocks until a client connection is established. XXX
     *
     * @param serverSocket
     * @param period
     * @param timeout
     * @param messageObservers
     * @param breakdownObservers
     * @param inFilter
     * @param outFilter
     * @throws IOException
     */
    public SafeSocket(ServerSocket serverSocket, int period, int timeout, Collection<MessageObserver> messageObservers, Collection<BreakdownObserver> breakdownObservers, Filter inFilter, Filter outFilter) throws IOException
    {
        serverMode = true;
        this.period = period;
        this.timeout = timeout;
        this.messageObservers = messageObservers;
        this.breakdownObservers = breakdownObservers;
        this.outFilter = outFilter;

        // Wait for client to connect
        socket = serverSocket.accept();

        // Now that the connection has been established, launch asynchronous heartbeat and wait for messages (actual and Acks) to come in
        activateReader(inFilter);

        // Prepare for Read/Write, Set up hearbeat (sender), 
        initializeHearbeats();
    }

    /**
     * Constructor for setting up client side SafeSocket.
     *
     * @param serverIp
     * @param port
     * @param period
     * @param timeout
     * @param messageObservers
     * @param breakdownObservers
     * @throws IOException
     */
    public SafeSocket(String serverIp, int port, int period, int timeout, Collection<MessageObserver> messageObservers, Collection<BreakdownObserver> breakdownObservers) throws IOException
    {
        this(serverIp, port, period, timeout, messageObservers, breakdownObservers, new DefaultFilter(), new DefaultFilter());
    }

    /**
     * Constructor for setting up client side SafeSocket with custom I/O
     * filters.
     *
     * @param serverIp
     * @param port
     * @param period
     * @param timeout
     * @param messageObservers
     * @param breakdownObservers
     * @param inFilter
     * @param outFilter
     * @throws IOException
     */
    public SafeSocket(String serverIp, int port, int period, int timeout, Collection<MessageObserver> messageObservers, Collection<BreakdownObserver> breakdownObservers, Filter inFilter, Filter outFilter) throws IOException
    {
        serverMode = false;
        this.period = period;
        this.timeout = timeout;
        this.messageObservers = messageObservers;
        this.breakdownObservers = breakdownObservers;
        this.outFilter = outFilter;

        // Connect to server
        socket = new Socket();
        SocketAddress address = new InetSocketAddress(serverIp, port);
        socket.connect(address, timeout);

        // Now that the connection has been established, asynchronously wait for messages and hearbeats to come in
        activateReader(inFilter);

        initializeHearbeats();
    }

    public boolean isMaster()
    {
        return serverMode;
    }

    private void initializeHearbeats() throws IOException
    {
        socketAlive = true;

        // Retrieve printwriter (needed to send messages through the socket later).
        printWriter = new PrintWriter(socket.getOutputStream(), true);

        // Server actively sends heartbeat and waits for ACKs
        if (serverMode) {
            Thread heartBeatSender = new Thread(new HeartBeatThread());
            heartBeatSender.start();
        }

        // Initialize heartbeat receiver
        else {
            Terminator initialTerminator = new Terminator("T-HBA: " + INITIAL_HEART_BEAT_ID, (int) (period * 1.5), this);
            String killerId = InternalMessages.HEART_BEAT + INITIAL_HEART_BEAT_ID;
            connectionKillers.put(killerId, initialTerminator);
            initialTerminator.start();
        }
    }

    private void activateReader(Filter inFilter) throws IOException
    {
        Thread activateThread = new SocketReaderThread(socket, this, inFilter);
        activateThread.start();
    }

    @Override
    protected void handleInternalMessage(String message)
    {
        // In case of an expected message (anything but delimiter), find attatched connection killer and deactivate it
        if (message.startsWith(InternalMessages.MESSAGE_ACK) || message.startsWith(InternalMessages.HEART_BEAT_ACK)) {
            if (!connectionKillers.containsKey(message))
                throw new RuntimeException("Unable to resolve terminator for internal message: " + message);
            connectionKillers.remove(message).deactivate();

            // unblock sender method
            if (message.startsWith(InternalMessages.MESSAGE_ACK)) {
                CountDownLatch latch = ackBlockers.remove(message);
                latch.countDown();
            }
        }
        // in case of a heartbeat: send back matching ack and reset heartbeat receiver
        else if (message.startsWith(InternalMessages.HEART_BEAT)) {
            String heartBeatId = message.replace(InternalMessages.HEART_BEAT, "");
            sendAckLessMessage(InternalMessages.HEART_BEAT_ACK + heartBeatId);
            resetHeartBeatReceiver(message);
        }
    }

    /**
     * Tells whether the underlying socket connection is alive or dead. (Note:
     * It is better to register a breakdown observer if you want to receive
     * information about a breakdown synchronously).
     *
     * @return whether connection is alive or not
     */
    public boolean isSocketAlive()
    {
        return socketAlive;
    }

    /**
     * Sends a message through the SafeSocket and tells you whether it has been
     * certainly transmitted to the other side or not. Note, this method blocks,
     * until an ACK for the transmitted message has arrived, or a timeout has
     * occurred. This is an intentional design feature of SafeSockets and should
     * not be sidestepped by calling this method in an extra tread. It is an
     * essential part of communication to rely on previous messages being
     * transmitted before continuing the "conversation".
     *
     * @param message
     * @return Whether the message has arrived FOR SURE on the other side. Note:
     * If returned false, the message may still have been transmitted (and only
     * the ACK did not make it). In case the connection has already been
     * detached this method directly returns false, with no extra error
     * handling. Reason is the sender has the possibility to subscribe as
     * BreakdownObserver. If he does so he does no need to actively send
     * messages on a dead connection.
     */
    public boolean sendMessage(String message)
    {
        // Bounce if socket is not alive any more (sender has already been 
        // notified, about breakdown. We directly return false)
        if (!isSocketAlive())
            return false;

        // Check if the message contains substrings reserved for internal usage
        saneMessageCheck(message);

        // We use an ACk message with the salted messages Hash as identifier
        String wrappedMessage = message + "\n" + InternalMessages.MESSAGE_DELIMITER + getSalt();
        String messageId = InternalMessages.MESSAGE_ACK + Md5Hasher.getMessageHash(wrappedMessage);

        // Create a latch and stock it in ackBlockers hashmap, so it is access-
        // ible throughout the class. (unblocked by timeout or ack receiver)        
        CountDownLatch awaitAck = new CountDownLatch(1);
        ackBlockers.put(messageId, awaitAck);

        // Create a killer that unblocks the latch in case of a timeout
        Terminator timeoutKiller = new Terminator("T-MA: " + message + " / " + messageId, timeout, this);
        connectionKillers.put(messageId, timeoutKiller);

        // Actually send the message, then launch the killer and block this thread
        sendAckLessMessage(wrappedMessage);
        timeoutKiller.start();
        try {
            awaitAck.await();
        }
        catch (InterruptedException ex) {
            throw new RuntimeException();
        }

        /*
         * If the socket is still marked as alive, the ack was received. If not,
         * the socket was already closed by a connection killer and we cannot
         * rely on the message having arrived on the other side.
         */
        return socketAlive;
    }

    /**
     * This method sends the actual message. ACKs must be attended and handled
     * externally to this method. Also Actual user-messages should must be
     * passed to this method in one piece, not in chunks (this is required for
     * thread safety.)
     *
     * @param message
     */
    private void sendAckLessMessage(String message)
    {
        // Before sending, pipe message through filter. Send the output unless it has been flagged as discarded.
        message = outFilter.filter(message);
        if (!message.matches(InternalMessages.MESSAGE_DISCARDED)) {
            printWriter.println(message);
            printWriter.flush();
        }
    }

    /**
     * Passes incoming message to all registered message observers. Note: The
     * Observers MUST be notified asynchronously. Otherwise we risk blocking
     * observers (e.g. if they send messages themselves). This can easily lead
     * to connection breakdowns, since the SocketReaderThread is also blocked
     * then and cannot handle incoming heartbeats or messages anymore (so no
     * ACKs will be sent out, too)
     */
    protected void notifyAllMessageObservers(final String message)
    {
        for (MessageObserver messageObserver : messageObservers) {
            Thread observerNotifierThread = new ObserverNotifierThread(messageObserver, this, message);
            observerNotifierThread.start();
        }
    }

    /**
     * Notifies all attached breakdown observers, the connection has finally
     * broken down.
     */
    private synchronized void notifyAllBreakDownObservers(boolean intended)
    {
        for (BreakdownObserver breakdownObserver : breakdownObservers) {
            breakdownObserver.notifyBreakdownObserver(this, intended);
        }
    }

    /**
     * private inner Thread that periodically sends heartBeats (ACK requests) to
     * the remote client.
     */
    private class HeartBeatThread implements Runnable
    {

        @Override
        public void run()
        {
            int heartBeartCounter = 0;
            while (socketAlive) {
                try {
                    // Register and launch new Terminator
                    String killerId = InternalMessages.HEART_BEAT_ACK + heartBeartCounter;
                    Terminator killer = new Terminator("T-HBA: " + heartBeartCounter, timeout, SafeSocket.this);
                    connectionKillers.put(killerId, killer);
                    killer.start();

                    // Request ACK for Heartbeat to defuse Connection Killer
                    sendAckLessMessage(InternalMessages.HEART_BEAT + heartBeartCounter);
                    Thread.sleep(period);

                    heartBeartCounter += 1;
                }
                catch (InterruptedException ex) {
                    throw new RuntimeException();
                }
            }
        }

    }

    /**
     * Deactivates the current ConnectionKiller (if there is one). Then starts a
     * new one. Called by the client on reception of a HeartBeat message.
     */
    private void resetHeartBeatReceiver(String message)
    {
        //defuse running connection killer
        connectionKillers.get(message).deactivate();

        // Launch a new one, next id is old id+1
        int idNumber = Integer.parseInt(message.replace(InternalMessages.HEART_BEAT, "")) + 1;
        String terminatorId = InternalMessages.HEART_BEAT + idNumber;
        Terminator nextTerminator = new Terminator("T-HBR: " + idNumber, (int) (period * 1.5), this);
        connectionKillers.put(terminatorId, nextTerminator);
        nextTerminator.start();
    }

    /**
     * In contrast to the next method this public method is not called due to a
     * broken connection, but as an intentional user-taken decision to close
     * down a working connection. Tells whether an actual close was issued (in
     * case the connection was already dead, the call is ignored) NOTE:
     * closeable interface currently not implemented for java 1.6 compatibility.
     */
    public void close()
    {
        if (isSocketAlive()) {
            // Thus the other side has to be notified about this decision as well
            sendAckLessMessage(InternalMessages.DISCONNECT);

            // before we close down the local socket.
            assymentricDisconnect(true);
        }
    }

    /**
     * Actively closing the connection prevents replying to further incoming
     * heartbeats / ACKs / sending of further messages. Method is synchronized
     * because in rare cases two timeouts could otherwise trigger two
     * simultaneous calls. (E.g. if concurrent hearbeat and message timeout) It
     * suffices if observers are then notified only once, for a broken
     * connection can never be revived.
     */
    @Override
    protected synchronized void assymentricDisconnect(boolean intended)
    {
        if (socketAlive) {

            try {
                socket.close();
                socketAlive = false;

                //unblock all potentially blocked threads
                for (CountDownLatch latch : ackBlockers.values()) {
                    latch.countDown();
                }
            }
            catch (IOException ex) {
                throw new RuntimeException("Unable to close connection.");
            }
            notifyAllBreakDownObservers(intended); // < sends false here - how can that be?
        }
    }

    /**
     * Called if a deadline for an awaited Ack or Heartbeat has not been met. In
     * that case we consider the connection broken on close it down.
     */
    @Override
    public void onTerminate(String cause)
    {
        System.out.println("Termination request, due to ACK timeout: "+cause+(isSocketAlive()?"[UNDEFUSED]":"[DEFUSED]"));
        assymentricDisconnect(false);
    }

    /**
     * Any message containing a line stating with a keyword reserved for
     * SafeSocket internal communication is considered malicious and will result
     * in Runtime exception plus immediate connection close.
     */
    private void saneMessageCheck(String message)
    {
        if (message == null || message.trim().equals(""))
            throw new RuntimeException("Sending of null / whitespace messages not allowed.");
        for (String line : message.split("\n")) {
            if (InternalMessages.isReserved(line)) {
                close();
                throw new RuntimeException("Shutting down connection due to "
                        + "malicious message colliding patterns reserved for"
                        + " internal safeSocket communication: "
                        + message);
            }
        }
    }

    @Override
    protected void handleUserMessage(String message, int salt)
    {
        // Notify all local registered observers
        notifyAllMessageObservers(message);

        // Create hash of wrapped message and send reception ACK back to sender
        String wrappedMessage = message + "\n" + InternalMessages.MESSAGE_DELIMITER + salt;
        String checksum = Md5Hasher.getMessageHash(wrappedMessage);
        sendAckLessMessage(InternalMessages.MESSAGE_ACK + checksum);
    }

    /**
     * Tells you whether the remote socket is attached to the same physical
     * network interface as the local ending. Useful if you want to detect dummy
     * loopback connections.
     *
     * @return
     */
    public boolean isLoopbackConnection()
    {
        try {
            return socket.getInetAddress().equals(InetAddress.getByName(null));
        }
        catch (UnknownHostException ex) {
            throw new RuntimeException("Unable to locate network interface details");
        }
    }

    /**
     * A new salt is required for every USER-message, to create unique hashes
     * even on identical message content.
     */
    private synchronized int getSalt()
    {
        messageSalt += 1;
        return messageSalt;
    }

}
