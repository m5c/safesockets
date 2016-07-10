package com.m5c.safesockets;

/**
 * Helper class to notify all registered MessageObservers Asynchronously. This
 * is required to prevent blocking the principal loop in the SocketReaderThread.
 *
 * @author m5c
 */
public class ObserverNotifierThread extends Thread
{

    private final MessageObserver observer;
    private final SafeSocket safeSocket;
    private final String message;

    public ObserverNotifierThread(MessageObserver observer, SafeSocket safeSocket, String message)
    {
        this.observer = observer;
        this.safeSocket = safeSocket;
        this.message = message;
    }

    @Override
    public void run()
    {
        observer.notifyMessageObserver(safeSocket, message);
    }

}
