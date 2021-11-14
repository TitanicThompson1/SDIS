package src.data;

import src.utils.*;

/**
 * Class that is a key for the hashmap of the stored, restore and backup chunks, which contains information about the chunks
 */
public class ChunkIdentifier {
    private String fileID;
    private int chunkNo;

    /**
     * Default constructor
     * @param fileID the ID of the file that contains the piece
     * @param chunkNo the number of the chunk
     */
    public ChunkIdentifier(byte[] fileID, int chunkNo){
        this.fileID = MyUtils.encodeHexString(fileID);
        this.chunkNo = chunkNo;
    }

    /**
     * Alternative default constructor
     * @param fileID the ID of the file that contains the piece
     * @param chunkNo the number of the chunk
     */
    public ChunkIdentifier(String fileID, int chunkNo){
        this.fileID = fileID;
        this.chunkNo = chunkNo;
    }

    /**
     * Constructor to parse information. Parses the data from the string, that contains the chunk information
     * @param toParse String containing the chunk information
     */
    public ChunkIdentifier(String toParse){
        String[] parts = toParse.split("_");

        this.fileID = parts[0];
        this.chunkNo = Integer.parseInt(parts[1]);
    }

    // Getters
    
    public int getChunkNo() {
        return chunkNo;
    }

    public String getFileID() {
        return fileID;
    }

    /**
     * Convert the chunk information to a string
     * @return the formatted string with the chunk information
     */

    @Override
    public String toString(){
        return this.fileID + "_" + String.valueOf(this.chunkNo);
    }

    @Override
    public boolean equals(Object o){
        if (this == o) return true;
        if (o == null || this.getClass() != o.getClass()) {
            return false;
        } 

        ChunkIdentifier cID = (ChunkIdentifier) o;

        if(this.fileID.equals(cID.getFileID()) && this.chunkNo == cID.getChunkNo()) return true;

        return false;
    }

    @Override    
    public int hashCode() {        
        return (this.fileID + String.valueOf(this.chunkNo)).hashCode();
    }
}
