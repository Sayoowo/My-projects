import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.util.Scanner;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class senderProgram {
    private static final int BUFFER_SIZE = 8192;
    
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        
        // Read Kxy (128-bit AES key) from symmetric.key file
        byte[] Kxy = null;
        try {
            Kxy = Files.readAllBytes(Paths.get("symmetric.key"));
            System.out.println("Symmetric key Kxy loaded from symmetric.key (" + Kxy.length + " bytes)");
        } catch (IOException e) {
            System.out.println("Error reading symmetric.key file: " + e.getMessage());
            scanner.close();
            return;
        }
        
        // Step 1: Write Kxy to message.kmk
        try (FileOutputStream fos = new FileOutputStream("message.kmk")) {
            fos.write(Kxy);
            System.out.println("Kxy written to message.kmk");
        } catch (IOException e) {
            System.out.println("Error writing Kxy: " + e.getMessage());
            scanner.close();
            return;
        }
        
        // Step 2: Prompt for and read message M
        System.out.print("Input the name of the message file: ");
        String filename = scanner.nextLine();
        
        // Step 3: Read M and append to message.kmk (piece by piece)
        try (FileInputStream fis = new FileInputStream(filename);
             FileOutputStream fos = new FileOutputStream("message.kmk", true)) {
            
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;
            int totalBytes = 0;
            
            while ((bytesRead = fis.read(buffer)) != -1) {
                System.out.println("Read " + bytesRead + " bytes");
                fos.write(buffer, 0, bytesRead);
                totalBytes += bytesRead;
            }
            
            System.out.println("File read successfully! Total size: " + totalBytes + " bytes");
            System.out.println("Message M appended to message.kmk");
            
        } catch (IOException e) {
            System.out.println("Error reading file: " + e.getMessage());
            scanner.close();
            return;
        }
        
        // Step 4: Append Kxy again to message.kmk
        try (FileOutputStream fos = new FileOutputStream("message.kmk", true)) {
            fos.write(Kxy);
            System.out.println("Kxy appended to message.kmk");
        } catch (IOException e) {
            System.out.println("Error appending final Kxy: " + e.getMessage());
            scanner.close();
            return;
        }
        
        System.out.println("\nComplete! File structure: Kxy + M + Kxy saved to message.kmk");
        
        // Step 5: Calculate SHA256 keyed hash MAC of (Kxy || M || Kxy)
        System.out.println("\nCalculating SHA256(Kxy || M || Kxy)...");
        byte[] hashMAC = null;
        try {
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            
            // Read message.kmk piece by piece and update the hash
            try (FileInputStream fis = new FileInputStream("message.kmk")) {
                byte[] buffer = new byte[1024];
                int bytesRead;
                
                while ((bytesRead = fis.read(buffer)) != -1) {
                    sha256.update(buffer, 0, bytesRead);
                }
            }
            
            hashMAC = sha256.digest();
            System.out.println("SHA256 hash calculated successfully (" + hashMAC.length + " bytes)");
            
        } catch (NoSuchAlgorithmException e) {
            System.out.println("Error: SHA-256 algorithm not found");
            scanner.close();
            return;
        } catch (IOException e) {
            System.out.println("Error reading message.kmk for hashing: " + e.getMessage());
            scanner.close();
            return;
        }
        
        // Ask user about inverting first byte (for testing)
        System.out.print("\nDo you want to invert the 1st byte in SHA256(Kxy||M||Kxy)? (Y or N): ");
        String response = scanner.nextLine().trim().toUpperCase();
        
        if (response.equals("Y")) {
            hashMAC[0] = (byte) ~hashMAC[0];
            System.out.println("First byte inverted for testing purposes");
        } else {
            System.out.println("No modifications made");
        }
        
        // Save the hash MAC to message.khmac
        try (FileOutputStream fos = new FileOutputStream("message.khmac")) {
            fos.write(hashMAC);
            System.out.println("\nKeyed hash MAC saved to message.khmac");
        } catch (IOException e) {
            System.out.println("Error writing message.khmac: " + e.getMessage());
            scanner.close();
            return;
        }
        
        // Display the hash MAC in hexadecimal
        System.out.println("\nSHA256(Kxy || M || Kxy) in Hexadecimal:");
        for (int i = 0; i < hashMAC.length; i++) {
            System.out.format("%02X ", hashMAC[i]);
            if ((i + 1) % 16 == 0) {
                System.out.println();
            }
        }
        System.out.println();
        
        // Step 6: AES Encryption of M
        System.out.println("\nEncrypting message M with AES...");
        
        try {
            // Set up AES cipher
            Cipher aesCipher = Cipher.getInstance("AES/ECB/NoPadding");
            SecretKeySpec aesKey = new SecretKeySpec(Kxy, "AES");
            aesCipher.init(Cipher.ENCRYPT_MODE, aesKey);
            
            // Read M from the original file and encrypt piece by piece
            try (FileInputStream fis = new FileInputStream(filename);
                 FileOutputStream fos = new FileOutputStream("message.aescipher")) {
                
                byte[] buffer = new byte[1024]; // Multiple of 16 bytes (1024 = 16 * 64)
                int bytesRead;
                int totalEncrypted = 0;
                
                while ((bytesRead = fis.read(buffer)) != -1) {
                    byte[] toEncrypt;
                    byte[] encrypted;
                    
                    // Check if we read a full buffer or partial
                    if (bytesRead == buffer.length && bytesRead % 16 == 0) {
                        // Full buffer and multiple of 16 - encrypt directly
                        encrypted = aesCipher.update(buffer);
                    } else {
                        // Partial read or not multiple of 16
                        // Create array with exact size needed
                        int encryptSize = bytesRead;
                        if (bytesRead % 16 != 0) {
                            // Round up to next multiple of 16
                            encryptSize = ((bytesRead / 16) + 1) * 16;
                        }
                        toEncrypt = new byte[encryptSize];
                        System.arraycopy(buffer, 0, toEncrypt, 0, bytesRead);
                        // Remaining bytes are automatically 0 (padding)
                        
                        encrypted = aesCipher.update(toEncrypt);
                    }
                    
                    if (encrypted != null) {
                        fos.write(encrypted);
                        totalEncrypted += encrypted.length;
                    }
                }
                
                // Finalize encryption (in case there's remaining data)
                byte[] finalBlock = aesCipher.doFinal();
                if (finalBlock != null && finalBlock.length > 0) {
                    fos.write(finalBlock);
                    totalEncrypted += finalBlock.length;
                }
                
                System.out.println("AES encryption complete!");
                System.out.println("Encrypted " + totalEncrypted + " bytes saved to message.aescipher");
            }
            
        } catch (Exception e) {
            System.out.println("Error during AES encryption: " + e.getMessage());
            e.printStackTrace();
            scanner.close();
            return;
        }
        
        // Step 7: RSA Encryption of Kxy using Ky+
        System.out.println("\nEncrypting Kxy with RSA using Ky+...");
        
        try {
            // Read Y's public key from YPublic.key
            PublicKey pubKeyY = readPubKeyFromFile("YPublic.key");
            
            // Set up RSA cipher
            Cipher rsaCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            rsaCipher.init(Cipher.ENCRYPT_MODE, pubKeyY);
            
            // Encrypt Kxy (16 bytes)
            byte[] kxyRsaCipher = rsaCipher.doFinal(Kxy);
            
            System.out.println("RSA encryption complete!");
            System.out.println("Kxy encrypted: " + Kxy.length + " bytes -> " + kxyRsaCipher.length + " bytes");
            
            // Save to kxy.rsacipher
            try (FileOutputStream fos = new FileOutputStream("kxy.rsacipher")) {
                fos.write(kxyRsaCipher);
                System.out.println("RSA ciphertext saved to kxy.rsacipher");
            }
            
        } catch (Exception e) {
            System.out.println("Error during RSA encryption: " + e.getMessage());
            e.printStackTrace();
            scanner.close();
            return;
        }
        
        scanner.close();
        System.out.println("\nSender program complete!");
    }
    
    // Method to read RSA public key from file
    public static PublicKey readPubKeyFromFile(String keyFileName) throws Exception {
        java.io.InputStream in = new FileInputStream(keyFileName);
        java.io.ObjectInputStream oin = new java.io.ObjectInputStream(new java.io.BufferedInputStream(in));
        
        try {
            java.math.BigInteger m = (java.math.BigInteger) oin.readObject();
            java.math.BigInteger e = (java.math.BigInteger) oin.readObject();
            
            System.out.println("Read from " + keyFileName + ": modulus and exponent loaded");
            
            java.security.spec.RSAPublicKeySpec keySpec = new java.security.spec.RSAPublicKeySpec(m, e);
            java.security.KeyFactory factory = java.security.KeyFactory.getInstance("RSA");
            PublicKey key = factory.generatePublic(keySpec);
            
            return key;
        } finally {
            oin.close();
        }
    }
}