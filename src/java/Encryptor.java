import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.swing.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;

public class Encryptor
{
     public static void main(String[] args)
     {
          String user = JOptionPane.showInputDialog("Input DB Username");
          String pass = JOptionPane.showInputDialog("Input DB Password");
          Encryptor.encryptCredentials(user, pass);
     }

     private static final String ALGO = "AES";
     private static final String SECRET = "BCRAVENS12345678"; // This needs to match the SECRET attribute in DBManager
     private static final String CRED_DELIMITER = "<:>"; // This needs to match the CRED_DELIMITER in DBManager
     private static final String FILE_PATH = "src/lib/EncryptedCredentials.txt";

     public static void encryptCredentials(String dbUser, String dbPassword)
     {
          try
          {
               byte[] keyBytes = SECRET.getBytes();
               SecretKey key = new SecretKeySpec(keyBytes, ALGO);     
               Cipher cipher = Cipher.getInstance(ALGO);
     
               cipher.init(Cipher.ENCRYPT_MODE, key);  

               String input = dbUser + CRED_DELIMITER + dbPassword;
               byte[] encryptedBytes = cipher.doFinal(input.getBytes());
     
               Files.write(Paths.get(FILE_PATH), Base64.getEncoder().encode(encryptedBytes));
               System.out.println("File encrypted successfully.");
          }catch(Exception e)
          {
               e.printStackTrace();
          }
     }
}
