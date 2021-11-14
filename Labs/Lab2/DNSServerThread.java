

import java.io.*;
import java.net.*;
import java.util.*;

public class DNSServerThread extends Thread {

    protected DatagramSocket socket = null;
    protected Hashtable<String, String> dnsTable = null; 

    public DNSServerThread(int port) throws IOException{
        this("DNSServerThread", port);
    }

    public DNSServerThread(String name, int port) throws IOException {
        super(name);
        socket = new DatagramSocket(port);
        dnsTable = new Hashtable<String, String>();
    }

    @Override
    public void run() {
        
        while(true){
            try {
                
                DatagramPacket packet = receive_request();
                String[] request = parse_request(packet);
                log(request);
                
                //respond
                respond_to_request(request, packet);
                
            } catch (IOException e) {
                e.printStackTrace();
                break;
            }
        }
        socket.close();
    }


    private DatagramPacket receive_request() throws IOException {
        byte[] buf = new byte[256];

        // receive request
        DatagramPacket packet = new DatagramPacket(buf, buf.length);
        socket.receive(packet);
        return packet;
    }

    private String[] parse_request(DatagramPacket packet){

        String request = new String(packet.getData(), 0, packet.getLength());
        return request.split("\\s+");
    }

    private void respond_to_request(String[] request, DatagramPacket packet) throws IOException{
        if(request[0].equals("REGISTER")){
            int response = register(request);
            respond(packet, response);
            
        }else if(request[0].equals("LOOKUP")){
            String response = lookup(request[1]);
            respond(packet, request[1], response);
        }
        
    }

    private int register(String[] request) {
        
        if(hasHostname(request)){
            return -1;
        }else {
            dnsTable.put(request[1], request[2]);
            return dnsTable.size();
        }
    }

    private boolean hasHostname(String[] request) {
        return dnsTable.containsKey(request[1]);
    }

    private void respond(DatagramPacket packet, int response) throws IOException {
        byte[] buf = new byte[256];

        buf = String.valueOf(response).getBytes();

        socket.send(new DatagramPacket(buf, buf.length, packet.getAddress(), packet.getPort()));
    }
    

    private String lookup(String dnsName) {

        String ip = dnsTable.get(dnsName);
        if (ip == null) return "NOT_FOUND"; 
        else return ip;
    }

    private void respond(DatagramPacket packet, String dnsName, String ip) throws IOException {
        byte[] buf = new byte[256];

        buf = String.join(" ", dnsName, ip).getBytes();

        socket.send(new DatagramPacket(buf, buf.length, packet.getAddress(), packet.getPort()));
    }

    
    private void log(String[] request) {
        System.out.print("Server: ");
        for(String ele : request){
            System.out.print(ele + " ");
        }
        System.out.print("\n");
    }

}
