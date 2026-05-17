import java.io.File;
import java.io.FileInputStream;
import java.security.MessageDigest;

public class HashCalc {
    public static String getMD5(File file) throws Exception { return getHash(file, "MD5"); }
    public static String getSHA256(File file) throws Exception { return getHash(file, "SHA-256"); }

    private static String getHash(File file, String algorithm) throws Exception {
        MessageDigest digest = MessageDigest.getInstance(algorithm);
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] byteArray = new byte[1024 * 8];
            int bytesCount;
            while ((bytesCount = fis.read(byteArray)) != -1) {
                digest.update(byteArray, 0, bytesCount);
            }
        }
        StringBuilder sb = new StringBuilder();
        for (byte b : digest.digest()) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}