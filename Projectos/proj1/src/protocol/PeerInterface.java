package src.protocol;

import java.io.*;
import java.rmi.*;

/**
 * PeerInterface - The RMI interface of the peer
 */
public interface PeerInterface extends Remote{

    /**
     * Executes the backup subprotocol
     * @param filePath the path to the file
     * @param desiredRepDeg the desired replication degree
     * @throws Exception
     */
    void backup(String filePath, int desiredRepDeg) throws Exception;

    /**
     * Executes the restore subprotocol
     * @param filePath the path to the file
     * @throws Exception
     */
    void restore(String filePath) throws Exception;

    /**
     * Executes the delete subprotocol
     * @param filePath the path to the file
     * @throws Exception
     */
    void delete(String filePath) throws Exception;

    /**
     * Executes the reclaim subprotocol
     * @param filePath the path to the file
     * @throws Exception
     */
    void reclaim(int availableSize) throws Exception;

    /**
     * Retrieves the peer's state
     * @return string with the state of the peer
     * @throws Exception
     */
    String state() throws Exception;
}

            