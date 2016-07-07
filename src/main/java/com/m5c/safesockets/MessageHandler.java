package com.m5c.safesockets;

/**
 * Package scoped interface to grant the SocketReaderThread restricted access to
 * the SafeSockets methods needed for message handling.
 *
 * @author m5c
 */
public abstract class MessageHandler
{

    protected abstract void handleInternalMessage(String message);

    protected abstract void handleUserMessage(String message);

    protected abstract void assymentricDisconnect(boolean intended);

}
