package com.m5c.safesockets;


import com.m5c.safesockets.Filter;

/**
 *
 * @author m5c
 */
public class LatencyFilter implements Filter
{

    private final long delay;

    public LatencyFilter(long delay)
    {
        this.delay = delay;
    }

    @Override
    public String filter(String input)
    {
        try {
            Thread.sleep(delay);
            return input;
        }
        catch (InterruptedException ex) {
            throw new RuntimeException("Latency filter failed to emulate network delay.");
        }
    }

}
