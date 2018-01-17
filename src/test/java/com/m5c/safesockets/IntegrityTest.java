package com.m5c.safesockets;

import java.io.IOException;
import org.junit.Assert;
import org.junit.Test;

/**
 * Test whether transmitted messages arrive correctly at other end.
 *
 * @author m5c
 */
public class IntegrityTest extends AbstractTest
{

    /**
     * A simple reception test. We see if what we get actually is what we sent.
     */
    @Test(timeout=500)
    public void receptionTest() throws IOException, InterruptedException
    {
        SafeSocketPair pair = setupMasterSlaveConnection();

        // Sending blocks until message was Acked. So we can assert it was set, as soon as we passed the sending line.
        String testMessage = "Toto";
        pair.getMaster().sendMessage(testMessage);
        
        // wait until message has arrived
        while(receivedMessages.size() < 1)
        {
            Thread.sleep(100);
            System.out.println("- Waiting for message to be processed by receiving thread -");
        }

        
        //Thread.sleep(1000); // < Must not fail when removed!
        Assert.assertEquals(testMessage, receivedMessages.get(0));

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
