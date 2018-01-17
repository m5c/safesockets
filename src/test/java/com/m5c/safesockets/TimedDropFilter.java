package com.m5c.safesockets;

/**
 * Custom filter that does not perform any interception until predefined point
 * of failure, then entirely omits all network traffic.
 *
 * @author m5c
 */
class TimedDropFilter implements Filter
{

    private final long dropOutTimestamp;

    public TimedDropFilter(long timeout)
    {
        dropOutTimestamp = System.currentTimeMillis() + timeout;
    }

    @Override
    public String filter(String input)
    {
        long TTL = dropOutTimestamp - System.currentTimeMillis();

        // if before scheduled network crash -> pass
        if (TTL > 0)
            return input;
        else
            return InternalMessages.MESSAGE_DISCARDED;
    }

}
