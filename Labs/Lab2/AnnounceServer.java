
import java.io.*;
import java.net.*;

public class AnnounceServer extends Thread{

    private DatagramSocket socket = null;
    private String multicast_address = null;
    private int multicast_port = -1;

    
    public AnnounceServer(String multicast_address, int multicast_port, int local_port) throws IOException{
        super("AnnounceServer");
        socket = new DatagramSocket(local_port);
        this.multicast_address = multicast_address;
        this.multicast_port = multicast_port;
    }   

    @Override
    public void run() {

        String msg = "Hello! I'm online!";
        byte[] buf = new byte[256];

        buf = msg.getBytes();
       

        while(true){
            try { 
                InetAddress group = InetAddress.getByName(this.multicast_address);
                DatagramPacket packet = new DatagramPacket(buf, buf.length, group, multicast_port);
                socket.send(packet);

                sleep((long) 1000);
            } 
            catch (InterruptedException e) { }
            catch (IOException e) { e.printStackTrace();}
        }
        
    }
}
