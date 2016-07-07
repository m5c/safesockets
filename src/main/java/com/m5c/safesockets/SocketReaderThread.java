package com.m5c.safesockets;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

/**
 *
 * @author m5c
 */
public class SocketReaderThread extends Thread
{

    // The socket whichs output is operated on

    private final Socket socket;

    // The instance handling the extracted messages (lately the safeSocket)
    private final MessageHandler messageHandler;

    SocketReaderThread(Socket socket, MessageHandler messageHandler)
    {
        super();
        this.socket = socket;
        this.messageHandler = messageHandler;
    }

    @Override
    public void run()
    {
        try {
            while (true) {
                BufferedReader bufferedReader = null;
                try {
                    bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                    // Read in message coming over TCP Socket (until socket was closed ). Note: The connection will only be closed by the server, never by the client (except for breakdowns.)
                    while (!socket.isClosed()) {
                        StringBuilder incomingMessage = new StringBuilder("");
                        String inputLine;

                        //keep on reading until message complete (or connection brutally closed)
                        boolean messageComplete = false;
                        while (!messageComplete) {
                            inputLine = bufferedReader.readLine();
                            if (inputLine == null)
                                throw new UnfriendlyConnectionBreakdownException();

                            // Ack lines may arrive within actual messeges, since they are sent by concurrent threads (whereas the sending of actual messages in blocking).
                            if (InternalMessages.isReserved(inputLine) && !inputLine.startsWith(InternalMessages.MESSAGE_DELIMITER))
                                messageHandler.handleInternalMessage(inputLine);
                            // End of a message reached, notfiy observersa and send Ack for reception
                            else if (inputLine.startsWith(InternalMessages.MESSAGE_DELIMITER)) {
                                messageComplete = true;
                                messageHandler.handleUserMessage(incomingMessage.toString());
                            }
                            else if(inputLine.startsWith(InternalMessages.DISCONNECT))
                            {
                                //remote host requested disconnect
                                messageHandler.assymentricDisconnect(true);
                            }
                            // The line just read is part of an ordinary message
                            else
                                incomingMessage.append(inputLine);
                        }
                    }
                }
                catch (IOException ex) {
                    throw new UnfriendlyConnectionBreakdownException();
                }
                finally {
                    try {
                        if (bufferedReader != null)
                            bufferedReader.close();
                    }
                    catch (IOException ex) {
                        throw new UnfriendlyConnectionBreakdownException();

                    }
                }
            }
        }
        catch (UnfriendlyConnectionBreakdownException e) {
            //ToDo: use disconnect here...
            //socketAlive = false;
            //notifyAllBreakDownObservers();
            messageHandler.assymentricDisconnect(false);
        }
    }

}
