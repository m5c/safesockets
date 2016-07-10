package com.m5c.safesockets;

/**
 * Once launched waits a precise amount of time (timeout) before sending a
 * onTerminate message to the referenced Terminatable object. Can be defused by
 * calling the deactivate() method - in that case onTerminate will not be called
 * on meeting the timeout deadline.
 *
 * @author m5c
 */
public class Terminator extends Thread
{

    private final int timeout;
    private final Terminatable terminatable;
    private boolean expectedSignalArrived = false;
    private final String description;

    public Terminator(String description, int timeout, Terminatable terminatable)
    {
        super();
        this.timeout = timeout;
        this.terminatable = terminatable;
        this.description = description;
    }

    public void deactivate()
    {
        expectedSignalArrived = true;
    }

    @Override
    public void run()
    {
        try {
            Thread.sleep(timeout);
            if (!expectedSignalArrived)
                terminatable.onTerminate(description);
        }
        catch (InterruptedException e) {
            throw new RuntimeException();
        }
    }

}
