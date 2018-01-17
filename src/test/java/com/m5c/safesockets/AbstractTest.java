package com.m5c.safesockets;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import org.junit.AfterClass;
import org.junit.BeforeClass;

/**
 * Base class for JUNIT tests. Provides base functionality such as connection
 * and listener setup that is required throughout all test classes.
 *
 * @author m5c
 */
public abstract class AbstractTest implements MessageObserver, BreakdownObserver
{

    final static int PORT = 2610;
    int heartBeatRate = 100;
    int timeout = 3000;
    static ServerSocket serverSocket;
    final Collection<MessageObserver> messageObservers = new LinkedList<MessageObserver>();
    final Collection<BreakdownObserver> breakdownObservers = new LinkedList<BreakdownObserver>();
    List<String> receivedMessages = new LinkedList<String>();
    SafeSocket master;
    Boolean mostRecentIntendedFlag = null;

    // default ctr
    public AbstractTest()
    {
        messageObservers.add(this);
        breakdownObservers.add(this);
    }

    // Overloaded constructors fur custom Hearbeat and Timeout setups (usefull for testing of breakdown behaviour)
    public AbstractTest(int heartBeatRate, int timeout)
    {
        messageObservers.add(this);
        breakdownObservers.add(this);
        this.heartBeatRate = heartBeatRate;
        this.timeout = timeout;
    }

    /*
    public AbstractTest(int timeout)
    {
        messageObservers.add(this);
        breakdownObservers.add(this);
        this.timeout = timeout;
    }*/

    @Override
    public void notifyMessageObserver(SafeSocket safeSocket, String message)
    {
        receivedMessages.add(message);
    }

    @Override
    public void notifyBreakdownObserver(SafeSocket safeSocket, boolean intended)
    {
        mostRecentIntendedFlag = new Boolean(intended);

        if (!intended)
            System.out.println("WARNING: Connection broke down during testing. (This may have been part of a test.)");
    }

    void resetReceivedMessageList()
    {
        receivedMessages.clear();
    }

    void resetBreakdownFlag()
    {
        mostRecentIntendedFlag = null;
    }

    class MasterConnectionStarter extends Thread
    {

        private final Filter inputFilter;
        private final Filter outputFilter;

        public MasterConnectionStarter(Filter inputFilter, Filter outputFilter)
        {
            this.inputFilter = inputFilter;
            this.outputFilter = outputFilter;
        }

        @Override
        public void run()
        {
            try {
                System.out.println("Setting up new master connection.");
                master = new SafeSocket(serverSocket, heartBeatRate, timeout, messageObservers, breakdownObservers, inputFilter, outputFilter);
            }
            catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }

    }

    /**
     * Provides a ready-for-communication pair of safesockets that will emulate
     * an ethernet connectivity incident where all packets scheduled for
     * communication are dropped / are ignored after a predefined timeout.
     *
     * @param connectionDropTime (will be ignored if negative)
     * @return
     * @throws InterruptedException
     * @throws IOException
     */
    SafeSocketPair setupMasterSlaveConnection(long connectionDropoutTime, Filter clientSideFilter) throws InterruptedException, IOException
    {
        // reset master connection
        master = null;

        // reset tested value
        mostRecentIntendedFlag = null;

        Filter inputFilter;
        Filter outputFilter;
        if (connectionDropoutTime < 0) {
            // use default filter
            inputFilter = new DefaultFilter();//new LoggerFilter("");
            outputFilter = new DefaultFilter();//new LoggerFilter("");
        }
        else {
            // create filter that passes anything before incident timeout and discard anything afterwards.
            inputFilter = new TimedDropFilter(connectionDropoutTime);
            outputFilter = new TimedDropFilter(connectionDropoutTime);
        }

        // Master connection startup blocks until client has connected, so we start it in extra thread.
        (new MasterConnectionStarter(inputFilter, outputFilter)).start();

        // Connect to master side.
        SafeSocket slave = new SafeSocket("localhost", PORT, heartBeatRate, timeout, messageObservers, breakdownObservers, clientSideFilter, clientSideFilter);

        // Note: usually server side setup would block until socket is accepted. This is not possible here, because we create a loopback connection (would be adeadlock if both wait until other ready.)
        // As master side setup was issued from an extra thread, the master reference retured from this method risks being null if we do not deliberately wait for it to be set.
        while (master == null) {
            Thread.sleep(10);
        }

        // All cool working connection is set up, it is safe to return it now.
        return new SafeSocketPair(master, slave);
    }

    /**
     * Provides a ready-for-communication pair of safesockets without timeout
     * for broken ethernet incident.
     *
     * @return
     * @throws IOException
     * @throws InterruptedException
     */
    SafeSocketPair setupMasterSlaveConnection() throws InterruptedException, IOException
    {
        // passing a negative dropout time deactivates the emulation of scheduled network incidents.
        return setupMasterSlaveConnection(-1, new DefaultFilter());
    }

    /**
     * Same as previous method but with additional parameters for custom
     * SafeSocket timeout constraint. Useful for setting up connections that
     * immediately fail internally due to infeasible ACK constraints. The
     * infeasiblity is emulated by a fake RTT. We hook in a custom IO Filter
     * that postpones the treatment of each incoming / outgoing message by a
     * predefined amount in milliseconds.
     *
     * @param customHeeartBeatRate
     * @param customTimeout
     * @return
     * @throws InterruptedException
     * @throws IOException
     */
    SafeSocketPair setupMasterSlaveConnectionCustamAckAndDelay(int customAckTimeout, long customFilterDelay) throws InterruptedException, IOException
    {
        int defaultTimeout = timeout;
        timeout = customAckTimeout;

        // passing a negative dropout time deactivates the emulation of scheduled network incidents.
        SafeSocketPair connection = setupMasterSlaveConnection(-1, new LatencyFilter(customFilterDelay));

        // global default params must be restored, for further tests. (note tests must not access this class multithreaded, but this should not arise with JUNIT.)
        timeout = defaultTimeout;

        return connection;
    }

    void shutDownConnection(boolean byMaster, SafeSocketPair pair) throws InterruptedException
    {
        // properly shut down connection ->  ToDo: should block until disconnect truely done... how to check?
        if (byMaster)
            pair.getMaster().close();
        else
            pair.getSlave().close();

        while (pair.getSlave().isSocketAlive() || pair.getMaster().isSocketAlive()) {
            Thread.sleep(100);
            System.out.println("- Waiting for socket to be detected as closed from both sides -");
        }
        org.junit.Assert.assertTrue(mostRecentIntendedFlag);
    }

    //only once for all tests
    @BeforeClass
    public static void setUpClass() throws IOException
    {
        // Serversocket will be reused throughout tests. Server does not close down beofre ultimate end of all communication (typically server shutdown)
        serverSocket = new ServerSocket(PORT);
    }

    @AfterClass
    public static void tearDownClass() throws IOException
    {
        serverSocket.close();
    }

}
