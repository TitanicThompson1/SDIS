import java.io.ObjectOutputStream;



import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

public class SSLClient {

    SSLSocket s = null;  
    SSLSocketFactory ssf = (SSLSocketFactory) SSLSocketFactory.getDefault();
    public static void main(String[] args){
    
        
        System.setProperty("javax.net.ssl.trustStoreType", "JKS");
        System.setProperty("javax.net.ssl.trustStore", "truststore");
        System.setProperty("javax.net.ssl.trustStorePassword", "123456");
        System.setProperty("javax.net.ssl.keyStore", "client.keys");
        System.setProperty("javax.net.ssl.keyStorePassword", "123456");


        try {
            SSLSocket sslSocket = (SSLSocket) SSLSocketFactory.getDefault().createSocket(args[0], Integer.parseInt(args[1]));
            String[] cyphers = new String[2];
            cyphers[0] = "TLS_RSA_WITH_AES_128_CBC_SHA";
            cyphers[1] = "TLS_DHE_RSA_WITH_AES_128_CBC_SHA";

            sslSocket.setEnabledCipherSuites(cyphers);

            ObjectOutputStream out = new ObjectOutputStream(sslSocket.getOutputStream());
            //ObjectInputStream in = new ObjectInputStream(sslSocket.getInputStream());

            out.writeObject(new String("Hello"));

        } catch (Exception e) {
            //TODO: handle exception
        }
    }
}
