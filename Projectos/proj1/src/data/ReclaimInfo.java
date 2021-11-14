package src.data;

/**
 * Class that is the key for the reclaim chunk hashmap
 */
public class ReclaimInfo {
    private ChunkIdentifier chunkIdentifier;
    private int senderID;

    /**
     * Default constructor
     * @param chunkIdentifier the identifier of the chunk
     * @param senderID the peer ID
     */
    public ReclaimInfo(ChunkIdentifier chunkIdentifier, int senderID) {
        this.chunkIdentifier = chunkIdentifier;
        this.senderID = senderID;
    }

    // Getters and Setters
    public int getSenderID() {
        return this.senderID;
    }

    public ChunkIdentifier getChunkIdentifier() {
        return this.chunkIdentifier;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || this.getClass() != o.getClass()) return false;

        ReclaimInfo reclaimInfo = (ReclaimInfo) o;
        
        if(this.chunkIdentifier.equals(reclaimInfo.getChunkIdentifier()) && this.senderID == reclaimInfo.getSenderID()) return true;

        return false;
    }

}
