

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;
import java.util.Hashtable;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;

public class SSLServer {

    private Hashtable<String, String> dnsTable = new Hashtable<>();



    public static void main(String[] args){

        
        System.setProperty("javax.net.ssl.trustStoreType", "JKS");
        System.setProperty("javax.net.ssl.trustStore", "truststore");
        System.setProperty("javax.net.ssl.trustStorePassword", "123456");
        System.setProperty("javax.net.ssl.keyStore", "server.keys");
        System.setProperty("javax.net.ssl.keyStorePassword", "123456");
        
        try {
            SSLServerSocket sslServerSocket = (SSLServerSocket) SSLServerSocketFactory
            .getDefault().createServerSocket(Integer.parseInt(args[0]));
            

            
            String[] cyphers = new String[2];
            cyphers[0] = "TLS_RSA_WITH_AES_128_CBC_SHA";
            cyphers[1] = "TLS_DHE_RSA_WITH_AES_128_CBC_SHA";
            
            sslServerSocket.setEnabledCipherSuites(cyphers);

            while(true){
                           
                SSLSocket sock = (SSLSocket) sslServerSocket.accept();

                ObjectInputStream stream = new ObjectInputStream(sock.getInputStream());

                String msg = (String) stream.readObject();

                System.out.println(msg);
            }
        } catch (Exception e) {
            System.err.println("Server exception: " + e.toString());
            e.printStackTrace();
        }
    }

   
    public int register(String ip, String port)  {
        if(hasHostname(ip)){
            return -1;
        }else {
            dnsTable.put(ip, port);
            return dnsTable.size();
        }
    }

    private boolean hasHostname(String ip) {
        return dnsTable.containsKey(ip);
    }

    public String lookup(String dnsName) {
        String ip = dnsTable.get(dnsName);
        if (ip == null) return "NOT_FOUND"; 
        else return ip;
    }

}
