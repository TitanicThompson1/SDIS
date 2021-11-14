package src.communication;

import src.protocol.*;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Class that is a value for the hashmap of the stored chunks, which contains information about the chunks
 */
public class Channel implements Runnable{
    private MulticastSocket socket = null;
    private int mCastPort;
    private String mCastAddr;
    private InetAddress inetMCAddress;
    private Peer peer;

    /**
     * Default constructor
     * @param mCastPort the ID of the file that contains the piece
     * @param mCastAddr the number of the chunk
     * @param peer the desired replication degree to backup the chunk
     */
    public Channel (int mCastPort, String mCastAddr, Peer peer) throws IOException {
        this.mCastPort = mCastPort;
        this.mCastAddr = mCastAddr;
        this.socket = new MulticastSocket(mCastPort);
        this.peer= peer;
    }

    @Override
    public void run() {

        try {

            //Join Multicast Group
            this.inetMCAddress = InetAddress.getByName(this.mCastAddr);
            this.socket.joinGroup(inetMCAddress);
            
            ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newCachedThreadPool();

            //Receives a message and creates a thread to handle the message received
            while(true)
            {
                // Receives a message
                byte[] buf = new byte[65000];
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                this.socket.receive(packet);

                // Creates the thread to deal with message
                Task task = new Task(this.peer, packet);
                
                executor.execute(task);
            }
        } catch (IOException e) {
            e.printStackTrace();           
        }
            
    }

    /**
     * Send the message through the respective channel
     * @param msg a specific Message according to the protocol
     */
    public void sendMessage(Message msg) {

        DatagramPacket packet = msg.toPacket(this.inetMCAddress, this.mCastPort);
        try{
            this.socket.send(packet);
        }catch (IOException ioE){
            ioE.printStackTrace();
        }
    }
    
}
