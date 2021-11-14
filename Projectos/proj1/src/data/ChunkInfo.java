package src.data;

import java.util.HashSet;

/**
 * Class that is a value for the hashmap of the stored chunks, which contains information about the chunks
 */
public class ChunkInfo {
    private String fileID;
    private int chunkNo;
    private int desiredRepDeg;
    private HashSet<Integer> backupPeers = new HashSet<>();
    private int bodyLenght;

    /**
     * Default constructor
     * @param fileID the ID of the file that contains the piece
     * @param chunkNo the number of the chunk
     * @param desiredRepDeg the desired replication degree to backup the chunk
     * @param bodyLenght the size of the chunk body
     */
    public ChunkInfo(String fileID, int chunkNo, int desiredRepDeg, int bodyLenght) {
        this.fileID = fileID;
        this.chunkNo = chunkNo;
        this.desiredRepDeg = desiredRepDeg;
        this.bodyLenght = bodyLenght;
    }

    /**
     * Constructor with the actual replication degree
     * @param fileID the ID of the file that contains the piece
     * @param chunkNo the number of the chunk
     * @param desiredRepDeg the desired replication degree to backup the chunk
     * @param actualRepDeg the actual replication degree to backup the chunk
     * @param bodyLenght the size of the chunk body
     */
    public ChunkInfo(String fileID, int chunkNo, int desiredRepDeg, int actualRepDeg, int bodyLenght) {
        this.fileID = fileID;
        this.chunkNo = chunkNo;
        this.desiredRepDeg = desiredRepDeg;
        this.bodyLenght = bodyLenght;
    }

    /**
     * Constructor to parse information. Parses the data from the string, that contains the chunk information
     * @param toParse String containing the chunk information
     */
    public ChunkInfo(String toParse){
        String[] parts = toParse.split("_");

        this.fileID = parts[0];
        this.chunkNo = Integer.parseInt(parts[1]);
        this.desiredRepDeg = Integer.parseInt(parts[2]);
        this.bodyLenght = Integer.parseInt(parts[3]);

        String[] peers = parts[4].split("-");

        for (String peer : peers) {
            this.backupPeers.add(Integer.parseInt(peer));
        }
        
    }
    
    // Getters
    public int getBodyLength(){
        return this.bodyLenght;
    }

    public String getFileID(){
        return this.fileID;
    }
    
    public int getChunkNo() {
        return this.chunkNo;
    }

    public int getDesiredRepDeg() {
        return this.desiredRepDeg;
    }

    public HashSet<Integer> getBackupPeers(){
        return this.backupPeers;
    }

    public int getActualRepDeg() {
        int actualRepDeg = this.backupPeers.size() + 1;
        return actualRepDeg;
    }
    
    // Setters
    public void setDesiredRepDeg(int desiredRepDeg) {
        this.desiredRepDeg = desiredRepDeg;
    }

    public void setBackedUpPeers(HashSet<Integer> peers){
        this.backupPeers = peers;
    }

    /**
     * Add the indicated peer to the backupPeers ArrayList, that contains the peers that store the chunk
     * @param peerId the ID of a peer
     * @return True if the indicated peer was not in the backupPeers ArrayList, and adds it. False otherwise
     */
    public boolean addPeer(int peerId) {
        return this.backupPeers.add(peerId);
    }

    /**
     * Remove the indicated peer from the backupPeers ArrayList, that contains the peers that store the chunk
     * @param peerId the ID of a peer
     */
    public void removePeer(int peerId) {
        this.backupPeers.remove(peerId);
    }
    
    /**
     * Convert the chunk information to a string
     * @return the formatted string with the chunk information
     */
    @Override
    public String toString(){
        
        String res =  this.fileID + "_" + this.chunkNo + "_" + this.desiredRepDeg + "_" + this.bodyLenght + "_";

        int counter = 0;
        for (int peer : this.backupPeers) {
            if (counter == this.backupPeers.size() - 1)
                res += peer;
            else
                res += peer + "-";

            counter++;
        }
        
        return res;
    }
}
