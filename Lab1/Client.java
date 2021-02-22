import java.io.*;
import java.net.*;
import java.util.*;

public class Client {
    public static void main(String[] args) throws IOException{
        
        if(args.length < 4 || args.length > 5){
            System.out.println("Usage: java Client <host> <port> <oper> <opnd>*");
            return;
        }

        // get a datagram socket
        DatagramSocket socket = new DatagramSocket();
        byte[] buf = new byte[256];
        if(args.length == 4)
            buf = String.join(" ", args[2], args[3]).getBytes();
        else
            buf = String.join(" ", args[2], args[3], args[4]).getBytes();

        InetAddress address = InetAddress.getByName(args[0]);
        DatagramPacket packet = new DatagramPacket(buf, buf.length, address, Integer.parseInt(args[1]));
        socket.send(packet);

        socket.receive(packet);

        String res = new String(packet.getData(), 0, packet.getLength());
       
        System.out.print("Client: ");
        for(int i = 2; i < args.length; i++){
            System.out.print(args[i] + " ");
        }
        System.out.println(": " + res);

        socket.close();
    }
}
