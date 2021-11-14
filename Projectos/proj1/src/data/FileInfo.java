package src.data;

/**
 * Class that is a value for the hashmap of the backup files, which contains information about the files
 */
public class FileInfo {
    private String filename;
    private int desiredRepDeg = -1;
    private int chunksNo = -1;

    /**
     * Default constructor
     * @param filename name of the file
     * @param desiredRepDeg the desired replication degree to backup the file
     */
    public FileInfo(String filename, int desiredRepDeg){
        this.filename = filename;
        this.desiredRepDeg = desiredRepDeg;
    }

    /**
     * Constructor to parse information. Parses the data from the string, that contains the file information for the backup
     * @param toParse String containing the file information for the backup
     */
    public FileInfo (String toParse) { 
        String[] parts = toParse.split("_");

        this.filename = parts[0];
        this.desiredRepDeg = Integer.parseInt(parts[1]);
        this.chunksNo= Integer.parseInt(parts[2]);
    }

    // Getters
    public String getFilename() {
        return this.filename;
    }

    public int getDesiredRepDeg() {
        return this.desiredRepDeg;
    }

    public int getChunksNo() {
        return this.chunksNo;
    }

    // Setters
    public void setChunkNo(int chunkNo) {
        this.chunksNo = chunkNo;
    }

    /**
     * Convert the saved file information to a string
     * @return the formatted string with the file information
     */
    @Override
    public String toString() {
        return this.filename + "_" + this.desiredRepDeg + "_" + this.chunksNo;
    }

}
