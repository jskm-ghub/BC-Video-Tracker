import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
//Encryptor.encrpytFile("/Users/lsnicotra/Desktop/BC-Video-Tracker/src/secure/credentials.json","/Users/lsnicotra/Desktop/BC-Video-Tracker/src/secure/EncryptedCredentials.enc","BCRAVENS12345678");
public class Encryptor {

     private static final String ALGO = "AES";

     public static void encryptFile(String inputPath, String outputPath, String secret) {
          try{
               byte[] keyBytes = secret.getBytes();
               SecretKey key = new SecretKeySpec(keyBytes, ALGO);     
               Cipher cipher = Cipher.getInstance(ALGO);
     
               cipher.init(Cipher.ENCRYPT_MODE, key);  
     
               byte[] inputBytes = Files.readAllBytes(Paths.get(inputPath));
               byte[] encryptedBytes = cipher.doFinal(inputBytes); 
     
               Files.write(Paths.get(outputPath), Base64.getEncoder().encode(encryptedBytes));
          }catch(Exception e){
               e.printStackTrace();
          }
     }
}
