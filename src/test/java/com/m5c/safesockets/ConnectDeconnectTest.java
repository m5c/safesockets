package com.m5c.safesockets;

import java.io.IOException;
import org.junit.Test;

/**
 * This JUNIT-test checks if connections can be established and dissolved properly.
 *
 * @author m5c
 */
public class ConnectDeconnectTest extends AbstractTest
{

    /**
     * Testing whether we get notified with the right intended flag if
     * connection is actively closed by server.
     *
     * @throws IOException
     * @throws InterruptedException
     */
    @Test(timeout = 15000)
    public void serversideCloseTest() throws IOException, InterruptedException
    {
        for (int i = 0; i < 100; i++) {

            resetBreakdownFlag();

            // Create new Safesocket connection with master and slave
            SafeSocketPair pair = setupMasterSlaveConnection();

            // Shut down by master
            shutDownConnection(true, pair);
        }

    }

    /**
     * Testing whether we get notified with the right intended flag if
     * connection is actively closed by client.
     *
     * @throws IOException
     * @throws InterruptedException
     */
    @Test(timeout = 15000)
    public void clientSideCloseTest() throws IOException, InterruptedException
    {
        for (int i = 0; i < 100; i++) {
            
            resetBreakdownFlag();

            // Create new Safesocket connection with master and slave
            SafeSocketPair pair = setupMasterSlaveConnection();

            // Shut down by slave
            shutDownConnection(false, pair);
        }
    }

}
