package brickst.robocust.connection;

import java.net.*;

/**
 * The ServerSocketProcessor interface is given to a ServerSocketListener to
 * provide it a method to invoke when a new connection (Socket) is accepted.
 * <br>
 * NOTE: The interface is called from the ServerSocketListener's only thread, so
 * new connections will not be accepted while the Socket is being "processed".
 * Thus the implementation should arrange for processing by another thread, then
 * return.
 * isshould return as soon as possible after arranging for the asynchronous
 */
public interface ServerSocketProcessor
{
    public abstract void handleNewConnection(Socket s);
}
