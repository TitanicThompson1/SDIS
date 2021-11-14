package src.protocol;

import src.data.*;
import src.communication.*;
import src.utils.*;

import java.net.DatagramPacket;
import java.util.concurrent.ThreadLocalRandom;

/**
 * This class receives a DatagramPacket and processes it
 */
public class Task implements Runnable {

    private Peer peer;          // peer that received the messsage
    private Message message;    // the message
    private DatagramPacket datagramPacket;

    /**
     * Default constructor
     * @param peer peer that received the message
     * @param datagramPacket the packet containing the message
     */
    public Task(Peer peer, DatagramPacket datagramPacket) {
        this.peer = peer;
        this.message = new Message(datagramPacket);
        this.datagramPacket = datagramPacket;
    }

    /**
     * Processes the pessage, accordingly to type
     */
    @Override
    public void run() {

        String msgType = this.message.getType();

        // if message was sent by the peer itself
        if(this.message.getSenderID() == this.peer.getID()){
            return;
        }
            
        

        MyFileHandler.writeLog("Received message: " + this.message + "\n");

        // Parsing message by type
        if (msgType.equals("PUTCHUNK")) {
            processPutchunk();
        } else if (msgType.equals("STORED")) {
            processStored();
        } else if (msgType.equals("GETCHUNK")) {
            processGetchunk();
        } else if (msgType.equals("GETCHUNKTCP")) {
                processGetchunkTCP();
        } else if (msgType.equals("CHUNK")) {
            processChunk();
        } else if (msgType.equals("CHUNKTCP")) {
            processChunkTCP();
        } else if (msgType.equals("DELETE")) {
            processDelete();
        } else if (msgType.equals("REMOVED")) {
            processRemove();
        }else if (msgType.equals("ISALIVE")){
            processIsAlive();
        }else if(msgType.equals("DEAD")){
            processDead();
        }
    }

    /* BACKUP PROTOCOL */


    /**
     * Processes the Putchunk message
     */
    private void processPutchunk() {

        ChunkIdentifier chunkIdentifier = new ChunkIdentifier(this.message.getFileID(), this.message.getChunkNo());

        // Verify if the process came from the reclaim protocol
        this.peer.verifyIfReclaim(chunkIdentifier);

        // If the Peer has backedup the file
        if(this.peer.hasBackedupFile(this.message.getFileID()))
            return;


        try {        
            this.peer.setToStore(chunkIdentifier, this.message.getDesiredRepDeg(), this.message.getBody().length);
        } catch (Exception e) {
           e.printStackTrace();
           return;
        }

        // Calculating wait time
        int waitTime = ThreadLocalRandom.current().nextInt(0, 401);

        try {
            Thread.sleep(waitTime);
        } catch (InterruptedException ie) {
            ie.printStackTrace();
        }

        if(!this.peer.getProtocol().equals("1.0") && this.peer.isFullyStored(chunkIdentifier, this.message.getDesiredRepDeg())){
            MyFileHandler.writeLog("File already with the desired rep deg. No need to store it\n");
            // Doesn't need to store in the map now
            this.peer.removeFromToStore(chunkIdentifier);
            return;
        }

        // If it was not possible to save the chunk, stops the protocol
        if(!this.peer.storeChunk(chunkIdentifier, this.message.getBody(), this.message.getDesiredRepDeg())) {
            this.peer.removeFromToStore(chunkIdentifier);
            return;
        }

        // Doesn't need to store in the map now
        this.peer.removeFromToStore(chunkIdentifier);

        // Sending stored message
        Message storedMessage = new Message(this.peer.getProtocol(), "STORED", this.peer.getID(), this.message.getFileID(), this.message.getChunkNo(), -1);

        MyFileHandler.writeLog("Sending message " + storedMessage + "\n");

        this.peer.sendMessage(MyConstants.MCCHANNEL, storedMessage);
    }

    /**
     * Processes the Stored message
     */
    private void processStored() {
        
        this.peer.updateRepDeg(new ChunkIdentifier(this.message.getFileID(), this.message.getChunkNo()), this.message.getSenderID());
    }

    /* RESTORE PROTOCOL */

    /**
     * Processes the Getchunk message
     */
    private void processGetchunk() {

        byte[] chunkContent;

        // Gets the content of the requested chunk
        chunkContent = this.peer.getChunk(new ChunkIdentifier(this.message.getFileID(), this.message.getChunkNo()));

        // If the peer doesn't has the chunk
        if(chunkContent == null) return;

        // Waits a random time between 0-400ms before sending the chunk
        int waitTime = ThreadLocalRandom.current().nextInt(0, 401);

        try {
            Thread.sleep(waitTime);
        } catch (InterruptedException ie) {
            ie.printStackTrace();
        }

        // Verifies if the requested chunck was already sent
        if (this.peer.chunkRestored(new ChunkIdentifier(this.message.getFileID(), this.message.getChunkNo()))) {
            return;
        }

        // Sends the content of the requested
        Message chunkMessage = new Message(this.peer.getProtocol(), "CHUNK", this.peer.getID(), this.message.getFileID(), this.message.getChunkNo(), -1);

        chunkMessage.setBody(chunkContent);

        MyFileHandler.writeLog("Sending message" + chunkMessage + "\n");

        this.peer.sendMessage(MyConstants.MDRCHANNEL, chunkMessage);
    }

    /**
     * Processes the Getchunk message for the TCP connection
     */
    private void processGetchunkTCP() {

        byte[] chunkContent;

        // Gets the content of the requested chunk
        chunkContent = this.peer.getChunk(new ChunkIdentifier(this.message.getFileID(), this.message.getChunkNo()));

        // If the peer doesn't has the chunk
        if(chunkContent == null) return;

        // Waits a random time between 0-400ms before sending the chunk
        int waitTime = ThreadLocalRandom.current().nextInt(0, 401);

        try {
            Thread.sleep(waitTime);
        } catch (InterruptedException ie) {
            ie.printStackTrace();
        }

        // Verifies if the requested chunck was already sent
        if (this.peer.chunkRestored(new ChunkIdentifier(this.message.getFileID(), this.message.getChunkNo()))) {
            return;
        }

        Message chunkMessage = new Message(this.peer.getProtocol(), "CHUNKTCP", this.peer.getID(), this.message.getFileID(), this.message.getChunkNo(), -1);

        // Notify peers the chunk has been restored
        MyFileHandler.writeLog("Sending message" + chunkMessage + "\n");
        this.peer.sendMessage(MyConstants.MDRCHANNEL, chunkMessage);

        // Send message with the chunk content using TCP
        chunkMessage.setBody(chunkContent);
        TCP.sendChunk(chunkMessage, this.datagramPacket.getAddress(), this.message.getLocalPort());
    }

    /**
     * Processes the Chunk message
     */
    private void processChunk() {
        this.peer.restoreChunk(new ChunkIdentifier(this.message.getFileID(), this.message.getChunkNo()), this.message.getBody());
    }

    /**
     * Processes the Chunk message for the TCP connection
     */
    private void processChunkTCP() {
        this.peer.restoreChunk(new ChunkIdentifier(this.message.getFileID(), this.message.getChunkNo()), null);
    }
    
    /* DELETE PROTOCOL */

    /**
     * Processes the Delete message
     */
    private void processDelete() {
        this.peer.deleteChunks(this.message.getFileID());
    }

    /**
     * Processes isAlive message 
     */
    private void processIsAlive() {

        // Checks if file was not deleted
        if(this.peer.isFileAlive(MyUtils.encodeHexString(this.message.getFileID()))) return;

        // Sends message to inform file was deleted two times to increase probability that the peer receives it
        Message deadMessage = new Message(this.peer.getProtocol(), "DEAD", this.peer.getID(), this.message.getFileID());

        this.peer.sendMessage(MyConstants.MCCHANNEL, deadMessage);

    }

    /**
     * Processes dead message 
     */
    private void processDead() {
        this.peer.deleteChunks(this.message.getFileID());
    }



    /* RECLAIM PROTOCOL */

    /**
     * Processes the Remove message
     */
    private void processRemove() {

        // The keys for the HashMaps of Peers
        ChunkIdentifier chunkIdentifier = new ChunkIdentifier(this.message.getFileID(), this.message.getChunkNo());

        // Checks if it stores the chunk received, decreasing the actual replication degree.
        this.peer.updateChunckInfo(chunkIdentifier, this.message.getSenderID());

        this.peer.updateBackupChunks(chunkIdentifier);

        if(!this.peer.checkIfNeedToBackup(chunkIdentifier)) return;

        try {

            this.peer.putIntoReclaimChunkMap(chunkIdentifier);

            int waitTime = ThreadLocalRandom.current().nextInt(0, 401);
  
            Thread.sleep(waitTime);

        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        // Verifies if it's needed to increase the actual replication degree
        if (!this.peer.checkIfAlreadyReclaimed(chunkIdentifier)) {
            this.peer.sendChunk(this.message.getFileID(), this.message.getChunkNo());
        }
        
    }
}