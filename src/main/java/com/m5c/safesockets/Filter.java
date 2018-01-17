package com.m5c.safesockets;

/**
 * Implement this interface and pass them to the SafeSockets constructors to
 * hook in your own filter implementations. This allows you to manipulate
 * SafeSocket's behaviour for in-/out-bound messages. All traffic will be piped
 * through the filters passed as ctr arguments. For incoming messages: prior
 * to any content inspection For outgoing messages: posterior to any further
 * modification To discard a message, do not set it to
 * InternalMessages.MESSAGE_DISCARDED
 *
 * @author m5c
 */
public interface Filter
{

    public String filter(String input);

}
