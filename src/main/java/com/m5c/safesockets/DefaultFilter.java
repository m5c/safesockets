package com.m5c.safesockets;

/**
 * The behaviour of this default filter is to pass through any message as-is.
 *
 * @author m5c
 */
class DefaultFilter implements Filter
{

    @Override
    public String filter(String input)
    {
        return input;
    }

}
