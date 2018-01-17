package com.m5c.safesockets;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

/**
 *
 * @author m5c
 */
public class SocketReaderThread extends Thread
{

    // The socket whichs output is operated on
    private final Socket socket;

    // The instance handling the extracted messages (lately the safeSocket)
    private final MessageHandler messageHandler;

    // The filter to be used for all incoming messages
    private final Filter inputFilter;

    SocketReaderThread(Socket socket, MessageHandler messageHandler, Filter inputFilter)
    {
        super();
        this.socket = socket;
        this.messageHandler = messageHandler;
        this.inputFilter = inputFilter;
    }

    @Override
    public void run()
    {
        try { //ToDo : try-with resource instead!
            while (true) {
                BufferedReader bufferedReader = null;
                try {
                    bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                    // Read in message coming over TCP Socket (until socket was closed ). Note: The connection will only be closed by the server, never by the client (except for breakdowns.)
                    while (!socket.isClosed()) {
                        StringBuilder messageBuilder = new StringBuilder("");

                        // Keep on reading until message complete (or connection brutally closed)
                        boolean messageComplete = false;
                        while (!messageComplete) {
                            messageComplete = handleInputLine(bufferedReader, messageBuilder);
                        }
                    }
                }
                catch (IOException ex) {
                    System.out.println("IOEX on read.");
                    throw new UnfriendlyConnectionBreakdownException();
                }
                finally {
                    try {
                        if (bufferedReader != null)
                            bufferedReader.close();
                    }
                    catch (IOException ex) {
                        System.out.println("IOEX on close.");
                        throw new UnfriendlyConnectionBreakdownException();

                    }
                }
            }
        }
        catch (UnfriendlyConnectionBreakdownException e) {
            System.out.println("TRY CATCH READER FAIL!");
            messageHandler.assymentricDisconnect(false); // Must be cause...
        }
    }

    /**
     * Interprets a single input line and returns whether the line just read
     * completed a payload message message (lines sent to this method can be
     * both, internal or payload).
     */
    private boolean handleInputLine(BufferedReader bufferedReader, StringBuilder messageBuilder) throws UnfriendlyConnectionBreakdownException, IOException
    {
        // Get the line that just arrived as sting. If not possible something nasty is happening -> just kill conection
        String inputLine = bufferedReader.readLine();
        if (inputLine == null)
            throw new UnfriendlyConnectionBreakdownException();

        // Hook point for custom filters. Message is replaced by the filter's output. Messages filled with MESSAGE_DISCARDED key will not be treated.
        inputLine = inputFilter.filter(inputLine);

        // case one: it is an internal message -> string builder gets not extended, but we handle the message internally anyways
        if (InternalMessages.isReserved(inputLine))
            return handleReservedInputLine(inputLine, messageBuilder);

        // case two: the message is actual payload -> no need for internal interpretations, we just extend the stringbuilder stitiching the currently incoming message together
        // If no builder initialized yet -> do it
        else
            handlePayloadInputLine(inputLine, messageBuilder);
        return false;
    }

    /**
    * Handles a single line that matches an internal message
    */
    private boolean handleReservedInputLine(String inputLine, StringBuilder messageBuilder)
    {
        // In case the signal to abandon connection was received -> properly shut down everything
        if (inputLine.startsWith(InternalMessages.DISCONNECT))
            //remote host requested disconnect
            messageHandler.assymentricDisconnect(true);

        // If it is an internal message other than Disconnect or MessageDelimiter-> handle as an internal message (ACK, HeartBeat, etc...)
        else if (InternalMessages.isReserved(inputLine) && !inputLine.startsWith(InternalMessages.MESSAGE_DELIMITER))
            messageHandler.handleInternalMessage(inputLine);         // Btw: Ack lines cannot arrive within actual messages, since the sending method is synchronized (as is the flush method)

        // Internal message that a payload message has been fully received -> notfiy observers and send reception ACK
        else if (inputLine.startsWith(InternalMessages.MESSAGE_DELIMITER)) {
            int salt = Integer.parseInt(inputLine.replaceFirst(InternalMessages.MESSAGE_DELIMITER, ""));
            messageHandler.handleUserMessage(messageBuilder.toString(), salt);
            return true;
        }
        return false;
    }

    /**
     * Handles a single line of a payload message
     * 
     * @param inputLine
     * @param messageBuilder
     * @return 
     */
    private void handlePayloadInputLine(String inputLine, StringBuilder messageBuilder)
    {
        // first entry goes without newline
        if (messageBuilder.toString().isEmpty())
            messageBuilder.append(inputLine);
        // all others add newline before content
        else
            messageBuilder.append("\n").append(inputLine);
    }

}
