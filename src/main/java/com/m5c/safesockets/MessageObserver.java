package com.m5c.safesockets;

/**
 *
 * @author m5c
 */
public interface MessageObserver
{
    public void notifyMessageObserver(SafeSocket safeSocket, String message);
}
