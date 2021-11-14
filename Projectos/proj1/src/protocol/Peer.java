package src.protocol;

import src.data.*;
import src.communication.*;
import src.utils.*;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Class that contains the operations to be executed by the peers to provide the backup service
 */
public class Peer implements PeerInterface, Runnable{

    private int ID;
    private String protocol;

    private ConcurrentHashMap<String, FileInfo> backedupFiles = new ConcurrentHashMap<>();                                  // Maps the FileID to a FileInfo. Saves all the backedup files
    private ConcurrentHashMap<ChunkIdentifier, ChunkInfo> storedChunks = new ConcurrentHashMap<>();                         // Saves all the chunks already stored
    private ConcurrentHashMap<ChunkIdentifier, Counter> backedupChunks = new ConcurrentHashMap<>();                         // Maps the ChunkIdentifier to the actual replication degree. Saves all the chunks already backdup
    private ConcurrentHashMap<ChunkIdentifier, HashSet<Integer>> toStoreChunks = new ConcurrentHashMap<>();                 // Maps the ChunkIdentifier to a HashSet containg the peers that stored the chunckIdentifier. Saves the actual rep deg of the chunk being stored
    private ConcurrentHashMap<ChunkIdentifier, Boolean> restoreChunks = new ConcurrentHashMap<>();                          // Maps the ChunkIdentifier to a boolean that identifies whether or not the chunk has been restored. Saves all the chunks already restored 
    private ConcurrentHashMap<ChunkIdentifier, Boolean> reclaimChunks = new ConcurrentHashMap<>();                          // Maps the ReclaimInfo to a boolean that identifies whether or not the chunk has been reclaimed. Saves all the chunks to be reclaimed
    private ConcurrentHashMap<String, HashSet<Integer>> restoreFileChunks = new ConcurrentHashMap<>();                      // Maps the FileID to an array of integers that keeps track on the chunks id already restored. Saves all the chunks to be restored
    private ConcurrentHashMap<String, String> deletedFiles = new ConcurrentHashMap<>();                                     // Maps the FileID to a string (dummy). Saves all deleted files


    private String storedMapFilename;               //Filename where the metadata of the stored chunks are saved
    private String backedupMapFilename;             //Filename where the metadata of the backedup chunks are saved
    private String backedupFileMapFilename;         //Filename where the metadata of the backedup files are saved
    private String deletedFileMapFilename;          //Filename where the metadata of the deleted files are saved

    private Channel mcChannel;                  // Multicast Control Channel
    private Channel mdbChannel;                 // Multicast Data Backedup Channel
    private Channel mdrChannel;                 // Multicast Data Restore Channel

    private int maxAvailableSize = 100000;      // In Kbytes
    private int currentSize;                    // In Kbytes

    private TCP tcp;                // Class for establishing and dealing with tcp communication, used in the enhancements version

    /**
     * Default constructor
     * @param protocol version of the protocol
     * @param ID the peer ID
     */
    public Peer(String protocol, int ID){
        this.protocol = protocol;
        this.ID = ID;

        // Files names where information for the backup service is saved
        this.storedMapFilename = MyConstants.path + "Peer" + String.valueOf(this.ID) + "S.map";
        this.backedupMapFilename = MyConstants.path + "Peer" + String.valueOf(this.ID) + "B.map";
        this.backedupFileMapFilename = MyConstants.path + "Peer" + String.valueOf(this.ID) + "BF.map";
        this.deletedFileMapFilename = MyConstants.path + "Peer" + String.valueOf(this.ID) + "D.map";
        

        // Imports information stored in non-volatile memory, for easy access
        try {
            this.storedChunks = MyFileHandler.importStoredFromFile(this.storedMapFilename);
            this.backedupChunks = MyFileHandler.importBckupFromFile(this.backedupMapFilename);
            this.backedupFiles = MyFileHandler.importbckdFilesFromFile(this.backedupFileMapFilename);
            this.deletedFiles = MyFileHandler.importdelFilesFromFile(this.deletedFileMapFilename);

            
        } catch (Exception e) {
            e.printStackTrace();

            // Starts with empty maps
            this.storedChunks = new ConcurrentHashMap<>();
            this.backedupChunks = new ConcurrentHashMap<>();
            this.backedupFiles = new ConcurrentHashMap<>();
            this.deletedFiles = new ConcurrentHashMap<>();
        }

        // Get the occupation of the current peer
        this.currentSize = getPeerOcupation();

        // If the version given is for the enhancements
        if (this.protocol.equals("2.0")) {
            // Initiaze TCP connection
            this.tcp = new TCP(this);

            // Start thread
            Thread tcpThread = new Thread(this.tcp);
            tcpThread.start();
        }
    }

    // Setters
    public void setMCChannel(Channel mcChannel){
        this.mcChannel = mcChannel;
    }

    public void setMDBChannel(Channel mdbChannel){
        this.mdbChannel = mdbChannel;
    }

    public void setMDRChannel(Channel mdrChannel){
        this.mdrChannel = mdrChannel;
    }
    
    // Getters
    public String getProtocol() {
        return this.protocol;
    }

    private int getPeerOcupation() {
        int size = 0;
        Collection<ChunkInfo> allValues = this.storedChunks.values();

        for (ChunkInfo value : allValues) {
            size += value.getBodyLength() / 1000;
        }

        return size;
    }
    
    public int getID(){
        return this.ID;
    }

    @Override
    public void run() {

        MyFileHandler writeStored = null;
        MyFileHandler writeBackUp = null;
        MyFileHandler writeBackUpFiles = null;
        MyFileHandler writeDelFiles = null;
        try {
            writeStored = new MyFileHandler(this.storedMapFilename, true);
            writeBackUp = new MyFileHandler(this.backedupMapFilename, true);
            writeBackUpFiles = new MyFileHandler(this.backedupFileMapFilename, true);
            writeDelFiles = new MyFileHandler(this.deletedFileMapFilename, true);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }


        while(true){
            try {
                Thread.sleep(1000);    
            
                writeStored.saveMapToFile(this.storedChunks);
                writeBackUp.saveMapToFile(this.backedupChunks);
                writeBackUpFiles.saveMapToFile(this.backedupFiles);
                writeDelFiles.saveMapToFile(this.deletedFiles);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /* BACKUP PROTOCOL */

    @Override
    public void backup(String filename, int desiredRepDeg) throws Exception {

        String filePath = MyConstants.path + filename;

        // Calculate the file ID
        byte[] fileID = calculateFileID(filePath);
    
        
        // Convert fileID to string 
        String fileIDStr = MyUtils.encodeHexString(fileID);
        
        FileInfo fileInfo = new FileInfo(filename, desiredRepDeg);

        // Checks if the the file is already backedup
        if(this.backedupFiles.put(fileIDStr, fileInfo) != null){
            throw new Exception("File already backed up!");
        }

        MyFileHandler.writeLog("Sending the files...\n");

        // Starts sending the chunks
        int nChunksSent = sendChunks(fileID, filePath, desiredRepDeg);

        // Saves in the file information the number of chunks sent
        fileInfo.setChunkNo(nChunksSent);
    }
    
    /**
     * Send the chunks
     * @param fileID the file ID
     * @param filePath the file path
     * @param desiredRepDeg the desired replication degree
     * @return the number of chunks sent
     */
    private int sendChunks(byte[] fileID, String filePath, int desiredRepDeg) throws Exception {
        boolean notSent = true;
        MyFileHandler fileHandler;
        int counter = 0;
        int nChunk = 0;

        fileHandler = new MyFileHandler(filePath, false);

        ArrayList<ChunkIdentifier> chunksSent = new ArrayList<>();
        int t = 1000;   // 1 second
        
        while(notSent && counter < 5){

            nChunk = 0;

            while(!fileHandler.isFinished()){
               
                sendChunkFromFile(fileID, desiredRepDeg, fileHandler, nChunk);
                
                chunksSent.add(new ChunkIdentifier(fileID, nChunk));
                
                nChunk += 1;
            }

            MyFileHandler.writeLog("Sent all PUTCHUNK messages\n");
            
            // Waiting for replies
            Thread.sleep(t);    
            
            // Updating next wait time
            t *= 2;
            
            notSent = !checkComplete(chunksSent, desiredRepDeg);
            
            if(notSent)
                MyFileHandler.writeLog("Desired Replication Degree not reached\n");
            
            counter++;

            // To read from file beginning again
            fileHandler.reset();
        }

        if(counter == 5) {
            throw new RuntimeException("Counter exceeded maximum tries");
        }

        fileHandler.close(); 
        
        return nChunk;
    }

    /**
     * Send the chunks
     * @param fileID the path ID
     * @param desiredRepDeg the desired replication degree
     * @param fileHandler the class that handles the file
     * @param nChunk the chunk number
     */
    private void sendChunkFromFile(byte[] fileID, int desiredRepDeg, MyFileHandler fileHandler, int nChunk) {
        byte[] chunk = fileHandler.getChunk();
   
        Message putchunkMsg = new Message(this.protocol, "PUTCHUNK", this.ID, fileID, nChunk, desiredRepDeg);

        putchunkMsg.setBody(chunk);
        
        ChunkIdentifier chunkIds = new ChunkIdentifier(fileID, nChunk);
        
        // If the chunk is repeated
        if(this.backedupChunks.get(chunkIds) != null){
            this.backedupChunks.remove(chunkIds);
        }

        this.backedupChunks.put(chunkIds, new Counter(0));
        
        sendMessage(MyConstants.MDBCHANNEL, putchunkMsg);
        
        MyFileHandler.writeLog("Sent message " + putchunkMsg + " in MDB channel\n");
    }

    /**
     * Checks if the chunks sent were stored by at least the desired number of peers given by the desired replication degree
     * @param chunks the chunks sent
     * @param desiredRepDeg the desired replication degree for each chunk
     * @return True if the chunks sent have the desired replication degree. False otherwise
     */
    private boolean checkComplete(ArrayList<ChunkIdentifier> chunks, int desiredRepDeg) {

        int actualRegDeg = -1;
        
        for(int i = 0; i < chunks.size(); i++) {
            actualRegDeg = this.backedupChunks.get(chunks.get(i)).getCurrentValue();

            if(actualRegDeg < desiredRepDeg){

                MyFileHandler.writeLog("Chunk: " + chunks.get(i) + " doesn't have the needed RepDeg - DesiredRepDeg: " + desiredRepDeg + " ActualRepDeg: " + actualRegDeg + "\n");
                return false;
            }
        }
        return true;
    }

    /**
     * Update the replication degree of the received chunk
     * @param key the received chunk
     * @param senderID the desired replication degree for the chunk
     */
    public void updateRepDeg(ChunkIdentifier key, int senderID){

        // Verifies if the peer is deciding to store the chunk
        if(this.toStoreChunks.containsKey(key)){
            this.toStoreChunks.get(key).add(senderID);
        }

        // If the peer is the initiatior peer
        if(this.backedupChunks.containsKey(key)) {
            this.backedupChunks.get(key).increment();

            int aux = this.backedupChunks.get(key).getCurrentValue();

            MyFileHandler.writeLog("Peer Initiator updated the replication degree of " + key + " to " + aux + "\n");
        } 

        // If the Peer stores chunks
        if(this.storedChunks.containsKey(key)) {

            if(this.storedChunks.get(key).addPeer(senderID)) {
                MyFileHandler.writeLog("Peers that backs up: " + this.storedChunks.get(key).getBackupPeers() + "\n");
                MyFileHandler.writeLog("Incremented the replication degree of " + key + " to " + this.storedChunks.get(key).getActualRepDeg() + "\n");
            } 
        }
    }

    /**
     * Store the body of the received chunk
     * @param key the received chunk
     * @param body the of the received chunk
     * @param desiredRepDeg the desired replication degree for the chunk
     * @return True if the peer has space to store the chunk. False otherwise
     */
    public boolean storeChunk(ChunkIdentifier key, byte[] body, int desiredRepDeg) {

        // Verify if the Peer already has stored the chunk
        if(hasChunkStored(key)) return true;

        // Verify if there is space to store a chunk
        int updSize = (body.length / 1000) + this.currentSize;
        if (updSize > this.maxAvailableSize) {
            MyFileHandler.writeLog("Exceded Size!\n");
            return false;
        }

        //Saves the chunk
        this.currentSize = this.currentSize + body.length / 1000;
        MyFileHandler.exportChunk(key.toString() + ".ckn", body);

        ChunkInfo cInfo;
        cInfo = new ChunkInfo(key.getFileID(), key.getChunkNo(), desiredRepDeg, body.length);

        cInfo.setBackedUpPeers(this.toStoreChunks.get(key));

        this.storedChunks.put(key, cInfo);

        return true;
    }

    /**
     * Verifies if the peer already has stored the chunk
     * @param key the received chunk
     * @return True if the peer has stored the chunk. False otherwise.
     */
    public boolean hasChunkStored(ChunkIdentifier key){
        //
        if(this.storedChunks.containsKey(key)){
            return true;
        }
        return false;
    }

    /**
     * Verifies if the peer has the file backedup
     * @param fileID file identifier
     * @return true, if the file was backedup. False otherwise.
     */
    public boolean hasBackedupFile(byte[] fileID){
        return this.backedupFiles.containsKey(MyUtils.encodeHexString(fileID));
    }

    /**
     * Puts the chunkIdentifier in the map toStore
     * @param chunkIdentifier
     * @throws Exception if it had already the ID in the map
     */
    public void setToStore(ChunkIdentifier chunkIdentifier, int desiredRepDeg, int bodyLenght) throws Exception{
        

        if(this.storedChunks.containsKey(chunkIdentifier)){
            this.toStoreChunks.put(chunkIdentifier, this.storedChunks.get(chunkIdentifier).getBackupPeers());
        }
        // If already has the ID
        else if(this.toStoreChunks.put(chunkIdentifier, new HashSet<>()) != null){
            throw new Exception("Already had chunkIdentifier, but was not suposted to\n");
        }
    }

    /**
     * Verifiy if the chunk already has a high enough replication degree
     * @param chunkIdentifier the chunk ID
     * @param desiredRepDeg the desired replication degree
     * @return true, if is high enough; false otherwise
     */
    public boolean isFullyStored(ChunkIdentifier chunkIdentifier, int desiredRepDeg){
        if(this.protocol.equals("1.0")) return false;

        int actualRegDeg = this.toStoreChunks.get(chunkIdentifier).size();

        return actualRegDeg >= desiredRepDeg;
    }

    /**
     * Removes from map toStoreChunks the entry with  a certain key
     * @param key key of entry to be remove
     */
    public void removeFromToStore(ChunkIdentifier key) {
        this.toStoreChunks.remove(key);
    }


    /* RESTORE PROTOCOL */

    @Override
    public void restore(String filename) throws Exception {

        String filePath = MyConstants.path + filename;

        // Calculate the file ID
        byte[] fileID = calculateFileID(filePath);

        // Verify if the file asked has been backup
        if (!hasBackedupFile(fileID))
            throw new Exception("File doesn't exists.");

        // Adding HashSet to map to keep track of received chunks
        HashSet<Integer> idList = new HashSet<>(); 
        this.restoreFileChunks.put(MyUtils.encodeHexString(fileID), idList);

        // Starts asking for the chunks that have backedup the file
        askChunks(fileID);
    }

    /**
     * Request for the chunks that have backedup the file
     * @param fileID the file ID
     */
    private void askChunks(byte[] fileID) throws Exception {
        int nChunk = 0;

        while(true) {
                
            // When there are no more chunks
            if(!this.backedupChunks.containsKey(new ChunkIdentifier(fileID, nChunk))) {
                break;
            }

            Message getchunkMsg;

            // With the enhacements
            if (this.protocol.equals("2.0")) {
                getchunkMsg = new Message(this.protocol, "GETCHUNKTCP", this.ID, fileID, nChunk, -1);
                getchunkMsg.setLocalPort(tcp.getLocalPort());
            } 
            // Without
            else
                getchunkMsg = new Message(this.protocol, "GETCHUNK", this.ID, fileID, nChunk, -1);

            sendMessage(MyConstants.MCCHANNEL, getchunkMsg);
            MyFileHandler.writeLog("Sent message " + getchunkMsg + " in MC channel\n");

            nChunk++;
        }
    }

    /**
     * Get the content of the received chunk in case the peer has stored it 
     * @param key the received chunk
     * @return the content of the received chunk
     */
    public byte[] getChunk(ChunkIdentifier key) {
        
        if(!this.storedChunks.containsKey(key)) {
            return null;
        }

        byte[] chunk = MyFileHandler.importChunk(key.toString() + ".ckn");
        
        // Adding chunk to the HashMap to keep track of the chunks to restore
        this.restoreChunks.put(key, false);

        return chunk;
    }

    /**
     * Update the chunk information if it is in the process of restoring
     * @param key the received chunk
     * @param body the of the received chunk
     */
    public void restoreChunk(ChunkIdentifier key, byte[] body) {

        // Peers that can restore the asked chunk
        if(this.restoreChunks.containsKey(key)) {
            this.restoreChunks.remove(key);
            this.restoreChunks.put(key, true);
        }

        // The peer initiator keeps track on the chunks already restored and writes them to the file
        if (this.backedupChunks.containsKey(key) && body != null) {
            
            // Repeated chunk after receiveing all chunks
            if(!this.restoreFileChunks.containsKey(key.getFileID()))
                return;

            String fileID = key.getFileID();
            int chunkNo = key.getChunkNo();
            FileInfo fileInfo = this.backedupFiles.get(fileID);

            this.restoreFileChunks.get(fileID).add(chunkNo);

            MyFileHandler.exportChunk(key.toString() + ".ckn", body);
            
            // If all the chunks that have backedup the file were restored
            if (this.restoreFileChunks.containsKey(fileID) && this.restoreFileChunks.get(fileID).size() == fileInfo.getChunksNo()) {
                this.restoreFileChunks.remove(fileID);
                MyFileHandler.writeLog("All chunks restored.\n");
                MyFileHandler.restoreFile("restored_" + fileInfo.getFilename(), fileID, fileInfo.getChunksNo());
                // if (this.protocol.equals("2.0"))
                //     this.tcp.closeConnection();
            }
        }
    }

    /**
     * Verify if the received chunk has been restored
     * @param key the received chunk
     * @return True if the received chunk has not been restore. False otherwise
     */
    public boolean chunkRestored(ChunkIdentifier key) {
        
        // If the received chunk has not been restored
        if(this.restoreChunks.get(key) == false) {
            this.restoreChunks.remove(key);
            this.restoreChunks.put(key, true);
            return false;
        }

        // If it has already been restored
        else 
            return true;
    }

    /* DELETE PROTOCOL */

    @Override
    public void delete(String filename) throws Exception {

        String filePath = MyConstants.path + filename;

        // Calculate the file ID
        byte[] fileID = calculateFileID(filePath);

        // Verify if the file asked has been backup
        if(!hasBackedupFile(fileID)){
            throw new Exception("File is not backedup!");
        }
         
        removeFromMaps(fileID);
        
        sendDelMsgs(fileID);

        // Saves that file was deleted
        putDelFileInMap(fileID);
    }

    /**
     * Puts in the deletedFile map the fileID
     * @param fileID file identifier
     */
    public void putDelFileInMap(byte[] fileID){
        this.deletedFiles.put(MyUtils.encodeHexString(fileID), "true");
    }
 
    /**
     * Remove chunks and file from the backedupChunks Map and backedupFiles Map, respectively
     * @param fileID the file ID
     */
    private void removeFromMaps(byte[] fileID){

        String strFileID = MyUtils.encodeHexString(fileID);

        // Remove from backedup files
        this.backedupFiles.remove(strFileID);

        // Remove from Backedup Chunk map
        for (Map.Entry<ChunkIdentifier, Counter> set : this.backedupChunks.entrySet()) {
            if(set.getKey().getFileID().equals(strFileID)){
                this.backedupChunks.remove(set.getKey());
            }
        }
    }
    
    /**
     * Notify it has deleted the file 
     * @param fileID the file ID
     */
    private void sendDelMsgs(byte[] fileID){

        Message delMessage = new Message(this.protocol, "DELETE", this.ID, fileID);

        for(int i = 0; i < 2; i++){
            sendMessage(MyConstants.MCCHANNEL, delMessage);
            MyFileHandler.writeLog("Sent message: " + delMessage);
        }
    }

    /**
     * Delete the chunks that have backedup the file, in case the peer has stored them
     * @param fileID the file ID
     */
    public void deleteChunks(byte[] fileID){

        Collection<ChunkInfo> allValues = this.storedChunks.values();

        String fileIDStr = MyUtils.encodeHexString(fileID);

        for(ChunkInfo entry : allValues){
            if(entry.getFileID().equals(fileIDStr)){
                allValues.remove(entry);
                MyFileHandler.deleteFile(entry.getFileID() + "_" + String.valueOf(entry.getChunkNo()) + ".ckn");
                MyFileHandler.writeLog("Removed chunk: " + entry + "\n");
            }
        }
    }

    /**
     * Sends isAlive message for all stored files to verify if any of them was deleted
     */
    public void sendIsAliveMessages(){
        if(this.protocol.equals("1.0")) return;

        
        // Get all stored files
        HashSet<String> storedFiles = new HashSet<>();
        for(Map.Entry<ChunkIdentifier, ChunkInfo> entry : this.storedChunks.entrySet()){
            storedFiles.add(entry.getKey().getFileID());
        }

        // Sending isAlive message gor all stored files
        for(String fileID: storedFiles){
            Message delMessage = new Message(this.protocol, "ISALIVE", this.ID, MyUtils.hexStringToByteArray(fileID));
            sendMessage(MyConstants.MCCHANNEL, delMessage);
            

        }
    }

    /**
     * Verifies if the file with fileID was deleted
     * @param fileID file identifier
     * @return true if file was deleted. False otherwise
     */
    public boolean isFileAlive(String fileID){
        return !this.deletedFiles.containsKey(fileID);
    }

    /* RECLAIM PROTOCOL */

    @Override
    public void reclaim(int availableSize) throws Exception {

        MyFileHandler.writeLog("Initiating Reclaim!\n -----------------------\n");
        
        // Update the maximum size that the peer can occupy
        this.maxAvailableSize = availableSize;
        
        // Calculate the size needed to delete
        int toRemove = this.currentSize - availableSize;
        
        // If the new max is greater than the current size
        if (toRemove <= 0)
            return;

        Collection<ChunkInfo> allValues = this.storedChunks.values();

        // Deleting first the chunks where the actual replication degree is greater then the desired one  OBSELETE BECAUSE OF ENHANCEMENT
        for (ChunkInfo value : allValues) {
            int desiredRepDeg = value.getDesiredRepDeg();
            int actualRepDeg = value.getActualRepDeg();

            if(actualRepDeg > desiredRepDeg){
                
                // Remove the chunk from the map and delete the file associated
                removeChunk(allValues, value);
                
                toRemove -= value.getBodyLength() / 1000;
                // Verify if the required size has already been deleted
                if (toRemove <= 0)
                    return;
            }
        }

        // Deleting the remaing needed chunks
        for (ChunkInfo value : allValues) {

            // Remove the chunk from the map and delete the file associated
            removeChunk(allValues, value);
            
            toRemove -= value.getBodyLength() / 1000;
            
            // Verify if the required size has already been deleted
            if (toRemove <= 0)
                break;
        }
    }

    /**
     * Delete the chunk in case the peer has stored it 
     * @param values the file ID
     * @param value the file ID
     */
    private void removeChunk(Collection<ChunkInfo> values, ChunkInfo value) {
        byte[] fileID = MyUtils.hexStringToByteArray(value.getFileID());

        MyFileHandler.writeLog("Remove chunk: " + value + "\n");
        MyFileHandler.deleteFile(value.getFileID() + "_" + String.valueOf(value.getChunkNo()) + ".ckn");
        
        values.remove(value);

        Message reclaimMessage = new Message(this.protocol, "REMOVED", this.ID, fileID, value.getChunkNo(), -1);
        
        sendMessage(MyConstants.MCCHANNEL, reclaimMessage);
        MyFileHandler.writeLog("Sent message: " + reclaimMessage + "\n");


    }

    /**
     * Send the received chunk
     * @param body the chunk body
     * @param fileID the file ID
     * @param chunkNo the chunk number
     * @param desiredRepDeg the desired replication degree for the chunk 
     * @throws InterruptedException
     */
    public void sendChunk(byte[] fileID, int chunkNo) {
        boolean notSent = true;
        int counter = 0;
        
        // Chunk filename
        String filename = MyUtils.getFilename(fileID, chunkNo);

        // Getting information about the chunk
        ChunkIdentifier chunkIdentifier = new ChunkIdentifier(fileID, chunkNo);
        ChunkInfo chunkInfo =  this.storedChunks.get(chunkIdentifier);
        
        // Getting the content of the chunk
        byte[] body = MyFileHandler.importChunk(filename);

        int t = 1000;   // 1 second
        
        // Putting together a PUTCHUNK message
        Message putchunkMsg = new Message(this.protocol, "PUTCHUNK", this.ID, fileID, chunkNo, chunkInfo.getDesiredRepDeg());
        putchunkMsg.setBody(body);

        this.toStoreChunks.put(chunkIdentifier, chunkInfo.getBackupPeers());
        
        while(notSent && counter < 5){
            
            sendMessage(MyConstants.MDBCHANNEL, putchunkMsg);

            MyFileHandler.writeLog("Sent PUTCHUNK MESSAGE\n");


            // Waiting for replies
            try {
                Thread.sleep(t);
            } catch (InterruptedException e) {
                e.printStackTrace();
                return;
            }
            
            // Updating next wait time
            t *= 2;
            
            int actualRepDeg = this.toStoreChunks.get(chunkIdentifier).size() + 1;
            int desiredRepDeg = chunkInfo.getDesiredRepDeg();
            
            // If actual rep deg is greater or equal then the desired on
            if(actualRepDeg >= desiredRepDeg){
                notSent = false;
                this.backedupChunks.remove(chunkIdentifier);
                this.storedChunks.get(chunkIdentifier).setBackedUpPeers(this.toStoreChunks.get(chunkIdentifier));
            }

            if(notSent){
                MyFileHandler.writeLog("Desired Replication Degree not reached\n");
            }

            counter++;
        }

        if(counter == 5) {
            throw new RuntimeException("Counter exceeded maximum tries");
        }

        // Signaling to other peers that it stores the chunk
        Message storedMessage = new Message(this.protocol, "STORED", this.ID, fileID, chunkNo, -1);
        sendMessage(MyConstants.MCCHANNEL, storedMessage);

        MyFileHandler.writeLog("Sending message " + storedMessage + "\n");
    }

    /**
     * Updates the actual replication degree of chunk
     * @param key the received chunk
     * @param senderID the peer ID which deleted the received chunk
     */
    public void updateChunckInfo(ChunkIdentifier key, int senderID) {

        if(this.storedChunks.containsKey(key)) {
            ChunkInfo chunckInfo = this.storedChunks.get(key);

            //chunckInfo.decrementActualRepDeg();
            chunckInfo.removePeer(senderID);

            MyFileHandler.writeLog("Updated the replication degree of " + key + " to " +  chunckInfo.getActualRepDeg() + "\n");
        }
    }

    /**
     * Checks if the chunk has an actual rep deg greater or equal than the desired one
     * @param key chunk ID
     * @return True if actual rep deg < desired rep deg. False Otherwise
     */
    public boolean checkIfNeedToBackup(ChunkIdentifier key){
        
        ChunkInfo chunckInfo = this.storedChunks.get(key);
        if(chunckInfo == null) return false;

        
        if (chunckInfo.getActualRepDeg() < chunckInfo.getDesiredRepDeg())
            return true;
        else 
            return false;
    }

    /**
     * Put the chunk to be reclaimed in case it does not have it
     * @param key the received chunk
     */
    public void putIntoReclaimChunkMap(ChunkIdentifier key) throws Exception {
        if(!this.reclaimChunks.containsKey(key)) {
            reclaimChunks.put(key, false);

        }else{
            throw new Exception("Hashmap reclaimChunks already has key");
        }
    }

    /**
     * Verify if the chunk has already been reclaimed
     * @param key the received chunk
     * @return True if the chunk has already been reclaimed. False otherwise
     */
    public boolean checkIfAlreadyReclaimed(ChunkIdentifier key) {

        boolean sent = this.reclaimChunks.get(key);
        this.reclaimChunks.remove(key);

        return sent;
    }

    /**
     * Verify if the received chunk came from the reclaim protocol. In case it did, updates the information about the chunk not having to be claimed again
     * @param key the received chunk
     */
    public void verifyIfReclaim(ChunkIdentifier key) {

        if(!this.reclaimChunks.containsKey(key))
            return;

        boolean sent = this.reclaimChunks.get(key);

        if (!sent) {
            this.reclaimChunks.remove(key);
            this.reclaimChunks.put(key, true);
        }
    }

    public void updateBackupChunks(ChunkIdentifier key) {
        if (this.backedupChunks.containsKey(key)) {
            this.backedupChunks.get(key).decrement();
        }   
    }
    
    /* STATE PROTOCOL */

    @Override
    public String state() throws Exception {
        
        StringJoiner result = new StringJoiner("\n");

        result.add("Backedup Files:");
        for (Map.Entry<String, FileInfo> set : this.backedupFiles.entrySet()) {
  
            result.add("Filename: " + set.getValue().getFilename());
            result.add("FileID: " + set.getKey());
            result.add("Desired Replication Degree: " + String.valueOf(set.getValue().getDesiredRepDeg()));
            
            result.add("Chunks: ");
            for(Map.Entry<ChunkIdentifier, Counter> chunkSet : this.backedupChunks.entrySet()){
                if(chunkSet.getKey().getFileID().equals(set.getKey())) continue;

                result.add("   Chunk ID: " + chunkSet.getKey().getChunkNo());
                result.add("   Actual Replication Degree: " + chunkSet.getValue());
            }

            
        }


        result.add("Stored chunks:");
        for (Map.Entry<ChunkIdentifier, ChunkInfo> set : this.storedChunks.entrySet()) {
  
            // Printing all elements of the stored chunks Map
            result.add("   FileID: " + set.getKey().getFileID());
            result.add("   ID: " + set.getKey().getChunkNo());
            result.add("   Size: " + set.getValue().getBodyLength());
            result.add("   Desired Replication Degree: " + set.getValue().getDesiredRepDeg());
            result.add("   Actual Replication Degree: " + set.getValue().getActualRepDeg());
            result.add("---------------------------------------------");
        }
        // Separator
        result.add("||---------------------------------------------||");
        return result.toString();
    }

    /* COMMON */

    /**
     * Send the message through the respective channel
     * @param channel identifies the communication channel
     * @param msg a specific Message according to the protocol
     */
    public void sendMessage(int channel, Message msg) {

        switch (channel) {
            case MyConstants.MCCHANNEL:             // Multicast Control Channel
                this.mcChannel.sendMessage(msg);
                break;
            case MyConstants.MDBCHANNEL:            // Multicast Data Backedup Channel
                this.mdbChannel.sendMessage(msg);
                break;
            case MyConstants.MDRCHANNEL:            // Multicast Data Restore Channel
                this.mdrChannel.sendMessage(msg);
                break;
            default:
                MyFileHandler.writeLog("Channel not regonized\n");
                break;
        }
        
    }

    /**
     * Calculate the file ID using the SHA256 cryptographic hash function, combining the last modified time of the file and its path
     * @param path the path of the file
     * @return the file ID
     */
    private byte[] calculateFileID(String path) throws Exception{
        
        File file = new File(path);

        if(!file.exists())
            throw new Exception("File does not exist!");

        Path pathObj = Paths.get(path);

        String lastModefied = Files.getLastModifiedTime(pathObj).toString();

        String toHash = path + lastModefied;

        MessageDigest digest = MessageDigest.getInstance("SHA-256");

        byte[] encodedhash = digest.digest(toHash.getBytes(StandardCharsets.UTF_8));
        
        MyFileHandler.writeLog("Calculated File ID " + MyUtils.encodeHexString(encodedhash) + " for " + path + "\n");

        return encodedhash;
    }
}
