package com.m5c.safesockets;

/**
 *
 * @author m5c 
 */
public class InternalMessages
{

    // Key words reserved for SafeSocket inter-communication (no actual message may start with one of these)
    // The internal messages actualy sent use these constants and append an HASH (Messages) od an ID (Heartbeats)
    protected static final String MESSAGE_DELIMITER = "SAFE_SOCKET_MESSAGE_DELIMITER_"; //
    protected static final String MESSAGE_ACK = "SAFE_SOCKET_ACK_MESSAGE_";
    protected static final String HEART_BEAT = "SAFE_SOCKET_HEART_BEAT_";
    protected static final String HEART_BEAT_ACK = "SAFE_SOCKET_ACK_HEART_BEAT_";
    protected static final String DISCONNECT = "SAFE_SOCKET_DISCONNECT";                //
    protected static final String MESSAGE_DISCARDED = "MESSAGE_DISCARDED_BY_FILTER";

    /**
     * Checks whether the given String matches the pattern of SafeSocket
     * metaMetamessages (HeartBeats, Acks, Delimiters)
     *
     * @param message
     * @return whether a string begins with one of the reserves messages
     */
    public static boolean isReserved(String message)
    {
        return (message.startsWith(InternalMessages.MESSAGE_ACK) || message.startsWith(InternalMessages.HEART_BEAT_ACK) || message.startsWith(InternalMessages.HEART_BEAT) || message.startsWith(InternalMessages.MESSAGE_DELIMITER) || message.startsWith(InternalMessages.MESSAGE_DISCARDED) || message.startsWith(DISCONNECT));
    }

}
