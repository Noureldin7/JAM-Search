package eng.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;



public class Hash {
    static String byte2Hex(byte inputByte){
        int digit = inputByte & 0xff;
        return Integer.toHexString(digit);
    }
    static String byteArray2Hex(byte[] inputBytes){
        String hexString = "";
        for (byte inputByte : inputBytes) {
            hexString += byte2Hex(inputByte);
        }
        return hexString;
    }
    public static String encrypt(String input,String algo) throws Exception{
        MessageDigest md = MessageDigest.getInstance(algo);
        byte[] hashBytes = md.digest(input.getBytes(StandardCharsets.UTF_8));
        return byteArray2Hex(hashBytes);
    }
    public static void main(String[] args) throws Exception{
        System.out.println(new Scrap("https://www.google.com").getUrlHash());
        // System.out.println(encrypt("abcdefghijklmnopqrstuvwxyz", "SHA-1"));
    }
}
