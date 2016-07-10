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
                        StringBuilder incomingMessage = null;
                        String inputLine;

                        //keep on reading until message complete (or connection brutally closed)
                        boolean messageComplete = false;
                        while (!messageComplete) {
                            inputLine = bufferedReader.readLine();
                            if (inputLine == null)
                                throw new UnfriendlyConnectionBreakdownException();
                            
                            // Ack lines cannot arrive within actual messages, since the sending method is synchronized (as is the flush method)
                            // If an internal message other than DELIMITER
                            if (InternalMessages.isReserved(inputLine) && !inputLine.startsWith(InternalMessages.MESSAGE_DELIMITER))
                                messageHandler.handleInternalMessage(inputLine);
                            // End of a message reached, notfiy observersa and send Ack for reception
                            else if (inputLine.startsWith(InternalMessages.MESSAGE_DELIMITER)) {
                                messageComplete = true;
                                int salt = Integer.parseInt(inputLine.replaceFirst(InternalMessages.MESSAGE_DELIMITER, ""));
                                messageHandler.handleUserMessage(incomingMessage.toString(), salt);
                            }
                            else if (inputLine.startsWith(InternalMessages.DISCONNECT))
                                //remote host requested disconnect
                                messageHandler.assymentricDisconnect(true);

                            // The line just read is part of an ordinary message
                            // Either initialize or append line just read
                            else if (incomingMessage == null)
                                incomingMessage = new StringBuilder(inputLine);
                            else
                                incomingMessage.append("\n").append(inputLine);
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
