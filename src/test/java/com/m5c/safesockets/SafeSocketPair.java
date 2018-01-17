package com.m5c.safesockets;

/**
 * Wrapper for a master-slave safesocket connection. Master is the entity who
 * originated the serversocket, slave is the one who connected to that socket.
 *
 * @author m5c
 */
public class SafeSocketPair
{
    private final SafeSocket master;
    private final SafeSocket slave;

    public SafeSocketPair(SafeSocket master, SafeSocket slave)
    {
        this.master = master;
        this.slave = slave;
    }

    public SafeSocket getMaster()
    {
        return master;
    }

    public SafeSocket getSlave()
    {
        return slave;
    }
    
    
}
