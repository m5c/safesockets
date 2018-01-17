package com.m5c.safesockets;

import java.io.IOException;
import org.junit.Assert;
import org.junit.Test;

/**
 * This test verify if messages arrive at the other end in the right order.
 * Short messages have a tendency to arrive faster than long messages - this
 * must be excluded. Note: This is not a check for ordered transmission despite
 * concurrent sending.
 *
 * @author m5c
 */
public class MessageOrderingTest extends AbstractTest
{

    private final String shortMessage = "blib blab blubb.";
    private final String longMessage;

    public MessageOrderingTest()
    {

        // Also we create two test messages. A very short and a very long one.
        StringBuilder longMessageBuilder = new StringBuilder("LongMessage");
        for (int i = 0; i < 1000; i++) {
            longMessageBuilder.append("_LongMessage");
        }
        longMessage = longMessageBuilder.toString();
    }

    /**
     * ToDo: Run this test in a loop. Does not always fail / succeed. Appears to
     * depend on side conditions. We burst out 200 interleaved messages and
     * check if they arrived in the right order at the other end. Note: Flooding
     * the channel can delay acks. Connection will then be assumed as dead. This
     * is normal because replies do come within predefined maximum response
     * time. -> Increasing Safesocket- internal-TIMEOUT to 500ms for this JUNIT
     * test.
     */
    @Test(timeout = 10000) // Note: Burst takes about 7 seconds.
    public void floodReceptionTest() throws IOException, InterruptedException
    {
        resetBreakdownFlag();
        resetReceivedMessageList();

        // Obtain both ends of SafeSocket connection.
        SafeSocketPair pair = setupMasterSlaveConnection();

        // Set up chronometer for logging
        long floodStartTimestamp = System.currentTimeMillis();
        // Do the following 100 times
        int amount = 100;
        for (int i = 0; i < amount; i++) {

            /* We do not want to block the test in case the connection breakes 
             down meanwhile. As the test gets asynchronously notified about 
             breakdowns it suffices to check a corresponding flag set by the 
             abstract super class.*/
            Assert.assertNull(mostRecentIntendedFlag); // If this fails, then the connection broke down during testing -> check timeout param in abstract class!

            // First send long message, then send short message. Sending blocks until reception, so messages should arrive at other end in same order.
            pair.getMaster().sendMessage(longMessage);
            pair.getMaster().sendMessage(shortMessage);
        }
        long floodDuration = System.currentTimeMillis() - floodStartTimestamp;
        System.out.println("Burst out " + amount + " long-short pairs in " + floodDuration + "ms.");

        // wait until all messages have arrived
        while (receivedMessages.size() < 2 * amount) {
            Thread.sleep(100);
            System.out.println("- Waiting for messages to be processed by receiving thread -");
        }

        // check if long messages are all on even possitions and short on odd
        for (int i = 0; i < 2 * amount; i++) {

            //System.out.println("- Verifying order in flooded communication ["+ i + "/"+ 2*amount+ "] -");
            // if even -> must be long message
            if (i % 2 == 0)
                Assert.assertEquals(receivedMessages.get(i), longMessage);
            else
                Assert.assertEquals(shortMessage, receivedMessages.get(i));
        }

        // properly shut down connection
        pair.getSlave().close();

        while (pair.getMaster().isSocketAlive() || pair.getSlave().isSocketAlive()) {
            Thread.sleep(100);
            System.out.println("- Waiting for socket to be detected as closed from both sides -");
        }
        Assert.assertTrue(mostRecentIntendedFlag);
        Assert.assertFalse(pair.getMaster().isSocketAlive());
    }

    /**
     * We send a very long message, followed by a very short. Then we check if
     * the arrive in the correct order. Difference to the previous test is that
     * we stall every time a pair of messages was sent. So it is merely a
     * repeated execution of a simpler test, not a flooding test.
     *
     * @throws IOException
     * @throws InterruptedException
     */
    @Test(timeout = 10000)
    public void orderedReceptionTest() throws IOException, InterruptedException
    {
        resetBreakdownFlag();

        // Obtain both ends of SafeSocket connection.
        SafeSocketPair pair = setupMasterSlaveConnection();

        // Do the following 100 times
        int amount = 100;
        for (int i = 0; i < amount; i++) {

            /* We do not want to block the test in case the connection breakes 
             down meanwhile. As the test gets asynchronously notified about 
             breakdowns it suffices to check a corresponding flag set by the 
             abstract super class.*/
            Assert.assertNull(mostRecentIntendedFlag); // If this fails, then the connection broke down during testing -> check timeout param in abstract class!

            // First send long message, then send short message. Sending blocks until reception, so messages should arrive at other end in same order.
            resetReceivedMessageList();
            pair.getMaster().sendMessage(longMessage);
            pair.getMaster().sendMessage(shortMessage);

            // wait until both messages have arrived
            while (receivedMessages.size() < 2) {
                Thread.sleep(20);
                System.out.println("- Waiting for messages to be processed by receiving thread [" + i + "/" + amount + "]-");
            }

            Assert.assertEquals(receivedMessages.get(0), longMessage);
            Assert.assertEquals(shortMessage, receivedMessages.get(1));
        }

        // properly shut down connection
        pair.getSlave().close();

        while (pair.getMaster().isSocketAlive() || pair.getSlave().isSocketAlive()) {
            Thread.sleep(100);
            System.out.println("- Waiting for socket to be detected as closed from both sides -");
        }
        Assert.assertTrue(mostRecentIntendedFlag);
        Assert.assertFalse(pair.getMaster().isSocketAlive());
    }

}
