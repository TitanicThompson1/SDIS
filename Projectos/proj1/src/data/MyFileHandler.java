package src.data;

import java.io.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import src.utils.MyConstants;

/**
 * This class handles the file
 */
public class MyFileHandler {
    private File file;
    private boolean finished;
    private FileInputStream fileInputStream;
    private FileOutputStream OutputStream;
    private static FileOutputStream fileOutputStream;
    private int chunkNo;
    
    /**
     * Default constructor
     * @param filePath the file path
     * @param output whether it is a file to read or write
     */
    public MyFileHandler(String filePath, boolean output) throws FileNotFoundException, SecurityException, IOException {

        this.file = new File(filePath);
        
        if(output){
            this.OutputStream = new FileOutputStream(file);
        }else{
            this.fileInputStream = new FileInputStream(file);
        }
        
        this.finished = false;

        this.chunkNo = 0;        
    }

    /**
     * Write Map information to a file
     * @param map the Map with the data
     */
    public void saveMapToFile(Map<?,?> map) throws IOException{
        
        this.OutputStream = new FileOutputStream(this.file);

        // For each Map key
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            
            this.OutputStream.write((entry.getKey().toString() + ":" + entry.getValue().toString() + "\n").getBytes());
        }
    }

    /**
     * Creates a log file with the given name
     * @param filename the file name
     */
    public static void setLogFile(String filename){

        String filePath = MyConstants.path + filename;

        File file = new File(filePath);
        
        try {
            file.createNewFile();

            MyFileHandler.fileOutputStream = new FileOutputStream(file);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Write a log with the given message
     * @param msg the message to write
     */
    public static void writeLog(String msg) {

        try { 

            MyFileHandler.fileOutputStream.write(msg.getBytes());

            MyFileHandler.fileOutputStream.flush();
        } 
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Imports the information about stored chunks to a Map
     * @param filename the file name with the information of the stored chunks
     * @return a Map with the information of the stored chunks
     */
    public static ConcurrentHashMap<ChunkIdentifier, ChunkInfo> importStoredFromFile(String filename) throws IOException{
        
        ConcurrentHashMap<ChunkIdentifier, ChunkInfo> res = new ConcurrentHashMap<>();

         // Create file object
        File file = new File(filename);

        if(!file.exists()) return res;

        // Create BufferedReader object from the File
        BufferedReader br = new BufferedReader(new FileReader(file));

        String line;

        // Read file line by line
        while ((line = br.readLine()) != null) {
  
            // Split the line by :
            String[] parts = line.split(":");

            // First part is name, second is number
            String chunkIdent = parts[0].trim();
            String chunkInfo = parts[1].trim();

            // Put name, number in HashMap if they are not empty
            if (!chunkIdent.equals("") && !chunkInfo.equals(""))
                res.put(new ChunkIdentifier(chunkIdent), new ChunkInfo(chunkInfo));
        }

        br.close();
        return res;  
    }

    /**
     * Import the information about backedup chunks to a Map
     * @param filename the file name with the information of the backedup chunks
     * @return a Map with the information of the backedup chunks
     */
    public static ConcurrentHashMap<ChunkIdentifier, Counter> importBckupFromFile(String filename) throws IOException{
        
        ConcurrentHashMap<ChunkIdentifier, Counter> res = new ConcurrentHashMap<>();

         // Create file object
        File file = new File(filename);

        if(!file.exists()) return res;

        // Create BufferedReader object from the File
        BufferedReader br = new BufferedReader(new FileReader(file));

        String line;

        // Read file line by line
        while ((line = br.readLine()) != null) {
  
            // Split the line by :
            String[] parts = line.split(":");

            String chunkIdent = parts[0].trim();
            String repDeg = parts[1].trim();

            // Put name, number in HashMap if they are not empty
            if (!chunkIdent.equals("") && !repDeg.equals(""))
                res.put(new ChunkIdentifier(chunkIdent), new Counter(repDeg));
        }
        br.close();
        return res;
    }

    /**
     * Import the information about backedup files to a Map
     * @param filename the file name with the information of the backedup files
     * @return a Map with the information of the backedup files
     */
    public static ConcurrentHashMap<String, FileInfo> importbckdFilesFromFile(String filename) throws IOException{
        
        ConcurrentHashMap<String, FileInfo> res = new ConcurrentHashMap<>();

        // Create file object
        File file = new File(filename);

        if(!file.exists()) return res;

        // Create BufferedReader object from the File
        BufferedReader br = new BufferedReader(new FileReader(file));

        String line;

        // Read file line by line
        while ((line = br.readLine()) != null) {
  
            // Split the line by :
            String[] parts = line.split(":");

            // First part is name, second is number
            String fileID = parts[0].trim();
            String fileInfo = parts[1].trim();

            // Put name, number in HashMap if they are not empty
            if (!fileID.equals("") && !fileInfo.equals(""))
                res.put(fileID, new FileInfo(fileInfo));
        }

        br.close();
        return res;
        
    }

    /**
     * Import the information about deleted files to a Map
     * @param filename the file name with the information of the backedup files
     * @return a Map with the information of the deleted files
     */
    public static ConcurrentHashMap<String, String> importdelFilesFromFile(String filename) throws IOException{
        
        ConcurrentHashMap<String, String> res = new ConcurrentHashMap<>();

        // Create file object
        File file = new File(filename);

        if(!file.exists()) return res;

        // Create BufferedReader object from the File
        BufferedReader br = new BufferedReader(new FileReader(file));

        String line;

        // Read file line by line
        while ((line = br.readLine()) != null) {
  
            // Split the line by :
            String[] parts = line.split(":");

            // First part is name, second is number
            String fileID = parts[0].trim();

            // Put name, number in HashMap if they are not empty
            if (!fileID.equals("") )
                res.put(fileID, "true");
        }

        br.close();
        return res;
        
    }

    /**
     * Get a chunk from the file
     * @return the content of the chunk
     */
    public byte[] getChunk(){
        if(this.finished) return null;

        chunkNo++;
        
        byte[] chunk = new byte[64000];

        try {
            int nBytesRead = this.fileInputStream.read(chunk);

            // In case the chunk does not occupy the maximum size
            if(nBytesRead != 64000){

                this.finished = true;
                byte[] chunkRes = new byte[nBytesRead];
                System.arraycopy(chunk, 0, chunkRes, 0, nBytesRead);
                
                return chunkRes;
            }
            return chunk;

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Export the content received to a file
     * @param filename the file name
     * @param chunk the content of the chunk
     */
    public static void exportChunk(String filename, byte[] chunk){

        String filePath = MyConstants.path + filename;

        File chunkFile = new File(filePath);
        
        try { 
            chunkFile.createNewFile();
            OutputStream os = new FileOutputStream(chunkFile); 
  
            // Start writing the bytes in it 
            os.write(chunk); 
  
            // Close the file 
            os.close(); 
        } 
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Import the content of the file
     * @param filename the file name
     * @return a byte with the content of the chunk
     */
    public static byte[] importChunk(String filename){

        String filePath = MyConstants.path + filename;
        File chunkFile = new File(filePath);
        byte[] body = null;

        try{    
            FileInputStream is = new FileInputStream(chunkFile);    
     
            // Start reading the bytes in it 
            body = is.readAllBytes();
           
            // Close the file
            is.close();
            
          }
        catch(IOException e) {
            e.printStackTrace();
        }
        return body;
    }

    /**
     * Get the chunk number
     * @return an integer representing the chunk number
     */
    public int getChunkNo() {
        return chunkNo;
    }

    /**
     * Close the file
     */
    public void close() throws IOException{
        this.fileInputStream.close();
    }

    /**
     * Verify if the file is at the end
     * @return True in case the file is at the end. False otherwise
     */
    public boolean isFinished(){
        return this.finished;
    }

    /**
     * Reset the reading of the file, initializing it again
     */
    public void reset(){
        try {
            this.fileInputStream = new FileInputStream(file);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
       
        this.finished = false;
    }
    
    /**
     * Delete the given file 
     * @param filename the file name
     */
    public static void deleteFile(String filename){
        String filepath = MyConstants.path + filename;
        try { 

            File file = new File(filepath);
            if(file.delete()){
                MyFileHandler.writeLog("Deleted file " + filename + "\n");
            }else{
                MyFileHandler.writeLog("Couldn't dele file " + filename + "\n");
            }
        } 
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Restore the file through the chunks that have backedup it
     * @param filename the file name
     * @param fileID the file ID
     * @param totalChunks the number of chunks that have backedup the file
     */
    public static void restoreFile(String filename, String fileID, int totalChunks) {
        
        String filePath = MyConstants.path + filename;
        
        File file = new File(filePath);
        FileOutputStream restoredFile;
        try {
            file.createNewFile();
            restoredFile = new FileOutputStream(file);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        int chunkNo = 0;
        while (true) {
            ChunkIdentifier chunkIdentifier = new ChunkIdentifier(fileID, chunkNo);
            String chunkPath = chunkIdentifier.toString() + ".ckn";
            byte[] content;

            // Import content of the chunk
            content = importChunk(chunkPath);

            try {
                //Write the content of the chunk to the file
                restoredFile.write(content);
    
            } catch (Exception e) {
                e.printStackTrace();
                break;
            }
            
            // Delete the chunk from non-volatil memory
            MyFileHandler.deleteFile(chunkPath);

            chunkNo++;
            
            if (chunkNo == totalChunks) {
                writeLog("Restored File.\n");
                break;
            }
        }

        try {
            restoredFile.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

