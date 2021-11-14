package src.communication;

import src.utils.*;

import java.io.*;
import java.net.*;
import java.nio.*;
import java.util.*;

/**
 * This class is a wrapper for the DatagramPacket
 */
public class Message implements Serializable {
    private String version;
    private String type;
    private int senderID;
    private byte[] fileID;
    private int chunkNo = -1;
    private int desiredRepDeg = -1;
    private byte[] body = null;
    private int localPort = -1;

    /**
     * Default constructor
     * @param version protocol version
     * @param type message type
     * @param senderID peer ID
     * @param fileID the encrypted file identifier
     * @param chunkNo number of the chunk
     * @param desiredRepDeg desired replication degree
     */
    public Message(String version, String type, int senderID, byte[] fileID, int chunkNo, int desiredRepDeg){
        this.version = version;
        this.type = type;
        this.senderID = senderID;
        this.fileID = fileID;
        this.chunkNo = chunkNo;
        this.desiredRepDeg = desiredRepDeg;
    }

    /**
     * Simplefied constructor
     * @param version protocol version
     * @param type message type
     * @param senderID peer ID
     * @param fileID the encrypted file identifier
     */
    public Message(String version, String type, int senderID, byte[] fileID){
        this.version = version;
        this.type = type;
        this.senderID = senderID;
        this.fileID = fileID;
    }

    /**
     * Constructor for DatagramPacket. Parses the data from packet
     * @param packet DatagramPacket containing all the message.
     */
    public Message(DatagramPacket packet){
        
        byte[] msg = packet.getData();

        // startPosition[0]  is that position where it is going to start copying
        int[] startPosition = {0};

        this.version = new String(extractField(msg, startPosition));
        this.type = new String(extractField(msg, startPosition));
        this.senderID = extractInt(msg, startPosition);
        this.fileID = extractFileID(msg, startPosition);
 
        // If the message type is Delete, there are no more arguments
        if(this.type.equals("DELETE") || this.type.equals("ISALIVE") || this.type.equals("DEAD"))
           return;

        this.chunkNo = extractInt(msg, startPosition);

        // If the message type is Stored, Getchunk or Removed, there are no more arguments
        if(this.type.equals("STORED") || this.type.equals("GETCHUNK") || this.type.equals("REMOVED"))
            return;

        // If the message type is Putchunk, next its the desired replication degree
        if(this.type.equals("PUTCHUNK")){
            this.desiredRepDeg = extractInt(msg, startPosition);
        }

        // If the message type is Getchunk for the TCP connection, next its the local port for the connection
        if(this.type.equals("GETCHUNKTCP")){
            this.localPort = extractInt(msg, startPosition);
            return;
        }

        // Cleaning the CRLF
        startPosition[0] += 4;

        this.body = new byte[packet.getLength() - startPosition[0]];

        System.arraycopy(msg, startPosition[0], this.body, 0, this.body.length);

    }

    /**
     * Extracts the field from message starting at startPosition[0]. Fields are separated by whitespaces
     * @param message the all message
     * @param startPosition array with the start position
     * @return a byte array with the extracted field
     */
    private byte[] extractField(byte[] message, int[] startPosition){
        
        
        String whiteSpace = " ";
        
        byte ws = whiteSpace.getBytes()[0];

        int i = startPosition[0], j = i;
        while(message[j] != ws){
            j++;
        }

        byte[] temp = new byte[j - i];
        
        System.arraycopy(message, i, temp, 0, j - i);
        
        j++;
        startPosition[0] = j;
         
        return temp;
    }

    /**
     * Extracts an int from message starting at startPosition[0].
     * @param message the all message
     * @param startPosition array with the start position
     * @return the extracted integer
     */
    private int extractInt(byte[] message, int[] startPosition){

        int i = startPosition[0];

        byte[] temp = new byte[4];
        
        System.arraycopy(message, i, temp, 0, 4);
        
        i += 5;
        startPosition[0] = i;
         
        return ByteBuffer.wrap(temp).getInt();
    }

    /**
     * Extracts the message body starting at startPosition[0].
     * @param message the all message
     * @param startPosition array with the start position
     * @return the extracted body, in a byte array
     */
    private byte[] extractBody(byte[] message, int[] startPosition){

    
        int i = startPosition[0], j = i;
        while(message[j] != 0){
            j++;
        }

        byte[] temp = new byte[j - i];
        
        System.arraycopy(message, i, temp, 0, j - i);
        
        j++;
        startPosition[0] = j;
         
        return temp;
    }

    /**
     * Extracts the filedID from message starting at startPosition[0].
     * @param message the all message
     * @param startPosition array with the start position
     * @return the extracted fileID, in a byte array
     */
    private byte[] extractFileID(byte[] message, int[] startPosition){

        int i = startPosition[0];

        byte[] temp = new byte[32];
        
        System.arraycopy(message, i, temp, 0, 32);
        
        i += 33;
        startPosition[0] = i;
         
        return temp;
    }


    /**
     * Transforms the object into a DatagramPacket
     * @param receiverAddrss the receiver IP address
     * @param receiverPort the receiver port
     * @return the DatagramPacket with all the information of the message
     */
    public DatagramPacket toPacket(InetAddress receiverAddrss, int receiverPort){

        // Calculating the total size of the message
        int msgLength = this.version.length() + 1 + this.type.length() + 1 + MyConstants.SIZEINT + 1 + this.fileID.length + 1;

        if (this.chunkNo != -1) {
            msgLength += MyConstants.SIZEINT + 1;
        }

        if(this.desiredRepDeg != -1) {
            msgLength += MyConstants.SIZEINT + 1;
        }  

        if(this.localPort != -1) {
            msgLength += MyConstants.SIZEINT + 1;
        }  
        
        // Adding the two CRLF
        msgLength += 4;

        if(this.body != null){
            msgLength += this.body.length;
        }

        // Joining everything with a ByteBuffer
        ByteBuffer buffer = ByteBuffer.allocate(msgLength);

        String whitespace = " ";

        // Putting version
        buffer.put(this.version.getBytes());
        buffer.put(whitespace.getBytes());

        // Putting message type
        buffer.put(this.type.getBytes());
        buffer.put(whitespace.getBytes());

        // Putting sender ID
        buffer.putInt(this.senderID);
        buffer.put(whitespace.getBytes());

        // Putting file ID
        buffer.put(this.fileID);
        buffer.put(whitespace.getBytes());

        // Checks if has chunkNo. If yes, then adds it.
        if (this.chunkNo != -1) {
           buffer.putInt(this.chunkNo);
           buffer.put(whitespace.getBytes());
        }

        // Checks if has desired replication degree. If yes, then adds it.
        if(this.desiredRepDeg != -1) {
            buffer.putInt(this.desiredRepDeg);
            buffer.put(whitespace.getBytes());
        }   
        
        // Checks if has local port. If yes, then adds it.
        if(this.localPort != -1) {
            buffer.putInt(this.localPort);
            buffer.put(whitespace.getBytes());
        }   

        //CRLF
        byte[] delimiter = {0x15, 0x12};

        buffer.put(delimiter);  
        buffer.put(delimiter);

        // Checks if has body. If yes, then adds it.
        if(this.body != null){ 
            buffer.put(this.body);
        }

        byte[] toSend = buffer.array();

        DatagramPacket packet = new DatagramPacket(toSend, toSend.length, receiverAddrss, receiverPort);

        return packet;
    }

    // Getters
    public String getVersion() {
        return version;
    }

    public String getType() {
        return type;
    }

    public int getSenderID() {
        return senderID;
    }

    public int getChunkNo() {
        return chunkNo;
    }

    public int getDesiredRepDeg() {
        return desiredRepDeg;
    }

    public byte[] getBody(){
        return this.body;
    }

    public byte[] getFileID(){
        return this.fileID;
    }

    public int getLocalPort(){
        return this.localPort;
    }

    // Setters
    public void setVersion(String version) {
        this.version = version;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setSenderID(int senderID) {
        this.senderID = senderID;
    }

    public void setChunckNo(int chunckNo) {
        this.chunkNo = chunckNo;
    }

    public void setDesiredRepDeg(int desiredRepDeg) {
        this.desiredRepDeg = desiredRepDeg;
    }

    public void setBody(byte[] body){
        this.body = body;
    }

    public void setfileID(byte[] fileID){
        this.fileID = fileID;
    }

    public void setLocalPort(int localPort){
        this.localPort = localPort;
    }

    /**
     * Converts the message to a string
     * @return the formatted string with the message
     */
    @Override
    public String toString(){
        

        StringJoiner stringJoiner = new StringJoiner(" ");
        
        stringJoiner.add(this.version);
        stringJoiner.add(this.type);
        stringJoiner.add(String.valueOf(senderID));
        stringJoiner.add(MyUtils.encodeHexString(this.fileID));

        if (this.chunkNo != -1) {
            stringJoiner.add(String.valueOf(this.chunkNo));
        }

        if(this.desiredRepDeg != -1) {
            stringJoiner.add(String.valueOf(this.desiredRepDeg));
        }

        if(this.localPort != -1) {
            stringJoiner.add(String.valueOf(this.localPort));
        }
        
        return stringJoiner.toString();
    }   
}
