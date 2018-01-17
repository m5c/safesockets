package com.m5c.safesockets;

import java.io.IOException;
import org.junit.Assert;
import org.junit.Test;

/**
 * Sets up SafeSocket connection with infeasible short ACK-timeout. What happens
 * is that the connection dies directly after setup. Goal of this Test class is
 * to verify the connection dies "correctly" on both ends. That is to say: Is
 * recognized as dead. Returns false on every sent message instead of blocking
 * on send().
 *
 * @author m5c
 */
public class CorrectBehaviourOnBreakdownTest extends AbstractTest
{

    public CorrectBehaviourOnBreakdownTest()
    {
        // Sets up Safesockets with a max-response time of 10ms for each heartbeat / message / etc. 
        //super(10);
    }

    /**
     * Verifies is a broken connection is detected as broken (by async
     * notifications and by the SafeSocket ends themselves)
     */
    @Test(timeout = 2000)
    public void breakDownDetectedTest() throws IOException, InterruptedException
    {
        for (int i = 0; i < 20; i++) {
            SafeSocketPair pair = createBrokenConnection();

            // Check if we have been notified about breakdown
            Assert.assertNotNull(mostRecentIntendedFlag);
            Assert.assertFalse(mostRecentIntendedFlag);

            // Now check if its still alive (must be detected as broken from both sides)
            Assert.assertFalse(pair.getMaster().isSocketAlive());
            Assert.assertFalse(pair.getSlave().isSocketAlive());
        }
    }

    /**
     * Tries to send something on a SafeSocket that is assumed dead. Must not
     * block. Actual test is whether timeout after @Test annotation is reached.
     */
    @Test(timeout = 2000)
    public void noSendingOnBrokenTest() throws IOException, InterruptedException
    {
        // Run this test multiple times, network tends to be moody.
        for (int i = 0; i < 20; i++) {
            SafeSocketPair pair = createBrokenConnection();

            // Try to send something from both ends and verify
            // * that sending does not block -> if it does, the test will automatically fails due @Test(timeout=...)
            // * that it was not acked by other side
            Assert.assertFalse(pair.getMaster().sendMessage("Pi. Pa. Po."));
            Assert.assertFalse(pair.getSlave().sendMessage("Po. Pa. Pi."));
        }

    }

    /**
     * Tests behaviour when broken connection is attempted to be shut down:
     * Expected: Nothing happens. Safesockets are closeable. They will not throw an exception when double-closed.
     */
    @Test(timeout = 1000)
    public void noShutdownOnBrokenTest() throws IOException, InterruptedException
    {
        SafeSocketPair pair = createBrokenConnection();

        // See what happens if we try to shutdown anyways...
        pair.getMaster().close();
        pair.getSlave().close();
        
        // Try to send somethign over closed connection
        boolean transmitted = pair.getMaster().sendMessage("Baaaaaaam!!!");
        Assert.assertFalse(transmitted);
        transmitted = pair.getSlave().sendMessage("Neeeeeiiiiiinn!!!");
        Assert.assertFalse(transmitted);
    }

    /**
     * Creates a connection that works for some time but then emulates network
     * error where no traffic is transmitted any more. This behaviour usually
     * arises when an Ethernet cable is suddenly unplugged. POsockets do not
     * recognize this dropout, but SafeSockets claim to do, thanks to the
     * heartbeat. This test verifies if both ends really recognize the
     * connection as broken at latest at T=incident+ACK_Timeout.
     *
     * @param incidentTimer
     * @return
     */
    @Test(timeout = 10000)
    public void unreliableNetworkEmulationTest() throws IOException, InterruptedException
    { 
        resetBreakdownFlag();
        
        long timeout = 4000; // make connection crash after 4 secs. ACK timeout is at 3 seconds so we should have received notification on breakdown at latest after 7(+somemilli) seconds
        SafeSocketPair connection = setupMasterSlaveConnection(timeout, new TimedDropFilter(timeout));
               
        System.out.println("Checking again in 2sec...");
        // ACK timeout (!! not drop timeout !! ) is at 3 seconds so we should have received notification on breakdown at latest after 4+3 = 7(+somemilli) seconds
        Thread.sleep(2000);
        
        // 7 seconds not yet over -> No breakdown notifications expected, yet.
        System.out.println("mRIF = "+mostRecentIntendedFlag);
        Assert.assertNull(mostRecentIntendedFlag);
        
        Thread.sleep(5000);

        // Two breakdown notifications by now.
        Assert.assertNotNull(mostRecentIntendedFlag);
        Assert.assertFalse(mostRecentIntendedFlag);
        
        System.out.println(connection.toString());
        
    }
    
    /**
     * Creates a brocken connection by infeasible ACK-constraints
     *
     * @return
     * @throws IOException
     * @throws InterruptedException
     */
    private SafeSocketPair createBrokenConnection() throws IOException, InterruptedException
    {
        // Note first param is the max tolerated delay for each heartbeat / message until ack reception. Second param is the artifical delay on every IO operation on client side, so this number effectively doubles in its affect on the RTT.
        SafeSocketPair pair = setupMasterSlaveConnectionCustamAckAndDelay(5, 4); // custom connection with ack timeout of 5 ms. Will fail within shortly, even over a loopback connection.

        // Wait half a second. Afterwards the connection will be already broken due irrealistic internal timeouts.
        Thread.sleep(50);

        return pair;
    }

}
