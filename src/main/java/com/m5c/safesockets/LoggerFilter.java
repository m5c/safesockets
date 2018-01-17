package com.m5c.safesockets;

/**
 * Alike the default filter, this one does not modify any intercepted messages.
 * However it prints the plain message content of each line to console. Useful
 * for debugging.
 *
 * @author m5c
 */
public class LoggerFilter implements Filter
{

    private final String logPrefix;

    public LoggerFilter(String logPrefix)
    {
        this.logPrefix = logPrefix;
    }

    @Override
    public String filter(String input)
    {
        System.out.println(System.currentTimeMillis() + " - " + logPrefix + input);
        return input;
    }

}
