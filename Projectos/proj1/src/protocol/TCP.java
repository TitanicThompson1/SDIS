package src.protocol;

import java.io.*;
import java.net.*;

import src.communication.*;
import src.data.*;

/**
 * This class handles the tcp communication, used in the enhancements version
 */
public class TCP implements Runnable{

    private ServerSocket serverSocket = null;
    private int localPort;
    private Peer peer;

    /**
     * Default constructor
     * @param peer instance 
     */
    public TCP(Peer peer) {

        try {
            serverSocket = new ServerSocket(0);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
        this.localPort = serverSocket.getLocalPort();
        this.peer = peer;
    }

    @Override
    public void run() {
        while (true) {
            Socket socket = acceptNewConnection();

            MyFileHandler.writeLog("Accepted TCP connection\n");

            handleConnection(socket);

            // Close the socket
            try {
                socket.close();
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
        }
    }

    // Getters
    public int getLocalPort() {
        return localPort;
    }

    /**
     * Listens for a connection to be made to this socket and accepts it
     * @return a Socket
     */
    public Socket acceptNewConnection() {
        try {
            return this.serverSocket.accept();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Read the receiving data from socket that contains the chunk to be restored, and restores it
     * @param socket 
     */
    public void handleConnection(Socket socket) {

        try {
            // Creates a buffering character-input stream that reads text from a character-input stream, buffering characters so as to provide for the efficient reading of characters, arrays, and lines.
            // Get a stream for receiving data from socket
            ObjectInputStream obj = new ObjectInputStream(socket.getInputStream());

            // CHUNK Message for the restore process
            Message message = (Message) obj.readObject();
            
            this.peer.restoreChunk(new ChunkIdentifier(message.getFileID(), message.getChunkNo()), message.getBody());

            // Close the buffering character-input
            obj.close();

        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
    }

    /**
     * Closes the TCP connection
     */
    public void closeConnection() {
        try {
            this.serverSocket.close();
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
    }

    /**
     * Send the message through the TCP connection
     * @param message message with the content of a chunk
     * @param address the address to the TCP connection
     * @param localPort the local port to the TCP connection
     */
    public static void sendChunk(Message message, InetAddress address, int localPort) {
        Socket socket;

        try {
            // Create a TCP socket for sending data
            socket = new Socket(address, localPort);

            // Create a new PrintWriter, with automatic line flushing, that gives Prints formatted representation
            // Get a stream for sending data via socket
            ObjectOutputStream obj = new ObjectOutputStream(socket.getOutputStream());

            obj.writeObject(message);

            obj.close();
            socket.close();                
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
    }
}
