package src.utils;

/**
 * MyUtils - Contains some helper functions
 */

public class MyUtils {

    /**
     * Transforms a byte array to an hexadecimal string
     * @param byteArray the byte array to transform
     * @return the formatted string
     */
    public static String encodeHexString(byte[] byteArray) {
        StringBuffer hexStringBuffer = new StringBuffer();
        for (int i = 0; i < byteArray.length; i++) {
            hexStringBuffer.append(byteToHex(byteArray[i]));
        }
        return hexStringBuffer.toString();
    }

    /**
     * Transforms a byte to an hexadecimal String
     * @param num byte to transform
     * @return the formatted string
     */
    private static String byteToHex(byte num) {
        char[] hexDigits = new char[2];
        hexDigits[0] = Character.forDigit((num >> 4) & 0xF, 16);
        hexDigits[1] = Character.forDigit((num & 0xF), 16);
        return new String(hexDigits);
    }

    /**
     * Transforms a hexadecimal string to a byte array
     * @param str string to transform
     * @return byte array with string
     */
    public static byte[] hexStringToByteArray(String str){
        byte[] val = new byte[str.length() / 2];
        for (int i = 0; i < val.length; i++) {
            int index = i * 2;
            int j = Integer.parseInt(str.substring(index, index + 2), 16);
            val[i] = (byte) j;
        }
        return val;
    }

    
    /**
     * Gets the filename from the fileID and chunkNo
     * @param fileID byte array with fileID
     * @param chunkNo the chunk ID number
     * @return string filename
     */
    public static String getFilename(byte[] fileID, int chunkNo){
        return MyUtils.encodeHexString(fileID) + "_" + String.valueOf(chunkNo) + ".ckn";
    }

    /**
     * Gets the filename from the fileID and chunkNo
     * @param fileID string with fileID
     * @param chunkNo the chunk ID number
     * @return string filename
     */
    public static String getFilename(String fileID, int chunkNo){
        return fileID + "_" + String.valueOf(chunkNo) + ".ckn";
    }
}
