package com.m5c.safesockets;

/**
 *
 * @author m5c
 */
public interface BreakdownObserver
{

    /**
     * Interface for classes who what to register as observers on connection
     * breakDown events. The SafeSocket will call this method when the
     * connection is closed down.
     *
     * @param safeSocket, as a reference to the caller (facilitates distinction
     * when maintaining multiple connections)
     * @param intended, telling whether the the breakdown occurred due to an
     * active disconnect request or a network issue.
     */
    public void notifyBreakdownObserver(SafeSocket safeSocket, boolean intended);

}
