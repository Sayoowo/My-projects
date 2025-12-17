import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.spec.RSAPrivateKeySpec;
import java.util.Arrays;
import java.util.Scanner;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class receiverProgram {
    
    public static void main(String[] args) {
        System.out.println("=== Receiver Program ===\n");
        
        // Step 3 (from requirements): Prompt for output filename
        Scanner scanner = new Scanner(System.in);
        System.out.print("Input the name of the message file: ");
        String outputFilename = scanner.nextLine();
        
        // Step 4: RSA Decrypt kxy.rsacipher (C1) to get Kxy
        System.out.println("\nStep 1: Decrypting Kxy with RSA using Ky-...");
        
        byte[] Kxy = null;
        try {
            // Read Y's private key (Ky-)
            PrivateKey privKeyY = readPrivKeyFromFile("YPrivate.key");
            
            // Read ciphertext C1 from kxy.rsacipher
            byte[] C1 = Files.readAllBytes(Paths.get("kxy.rsacipher"));
            System.out.println("Read ciphertext C1 from kxy.rsacipher: " + C1.length + " bytes");
            
            // Decrypt C1 with RSA to get Kxy
            Cipher rsaCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            rsaCipher.init(Cipher.DECRYPT_MODE, privKeyY);
            Kxy = rsaCipher.doFinal(C1);
            
            System.out.println("Kxy decrypted successfully: " + Kxy.length + " bytes");
            
            // SAVE Kxy to message.kmk
            try (FileOutputStream fos = new FileOutputStream("message.kmk")) {
                fos.write(Kxy);
                System.out.println("Kxy saved to message.kmk");
            }
            
            // DISPLAY Kxy in Hexadecimal
            System.out.println("\nDecrypted Kxy in Hexadecimal:");
            for (int i = 0; i < Kxy.length; i++) {
                System.out.format("%02X ", Kxy[i]);
            }
            System.out.println("\n");
            
        } catch (Exception e) {
            System.out.println("Error during RSA decryption: " + e.getMessage());
            e.printStackTrace();
            scanner.close();
            return;
        }
        
        // Step 5: AES Decrypt message.aescipher (C2) block by block to get M
        System.out.println("Step 2: Decrypting message M with AES...");
        
        byte[] decryptedMessage = null;
        try {
            // Set up AES cipher for decryption
            Cipher aesCipher = Cipher.getInstance("AES/ECB/NoPadding");
            SecretKeySpec aesKey = new SecretKeySpec(Kxy, "AES");
            aesCipher.init(Cipher.DECRYPT_MODE, aesKey);
            
            // Open message.aescipher (C2) for reading block by block
            try (FileInputStream fis = new FileInputStream("message.aescipher");
                 FileOutputStream fosOutput = new FileOutputStream(outputFilename);
                 FileOutputStream fosKmk = new FileOutputStream("message.kmk", true)) {
                
                System.out.println("Reading ciphertext C2 from message.aescipher...");
                
                byte[] buffer = new byte[1024]; // Multiple of 16 bytes
                int bytesRead;
                int totalDecrypted = 0;
                
                // Use ByteArrayOutputStream to collect all decrypted data
                ByteArrayOutputStream allDecrypted = new ByteArrayOutputStream();
                
                while ((bytesRead = fis.read(buffer)) != -1) {
                    byte[] blockToDecrypt;
                    byte[] decryptedBlock;
                    
                    // Check if we need to handle last block specially
                    if (bytesRead == buffer.length && bytesRead % 16 == 0) {
                        // Full buffer and multiple of 16 - decrypt directly
                        decryptedBlock = aesCipher.update(buffer, 0, bytesRead);
                    } else {
                        // Partial read or not multiple of 16
                        // Create array with exact size for this block
                        int blockSize = bytesRead;
                        if (bytesRead % 16 != 0) {
                            // This shouldn't happen with proper encryption, but handle it
                            blockSize = ((bytesRead / 16) + 1) * 16;
                        }
                        blockToDecrypt = new byte[blockSize];
                        System.arraycopy(buffer, 0, blockToDecrypt, 0, bytesRead);
                        
                        decryptedBlock = aesCipher.update(blockToDecrypt);
                    }
                    
                    if (decryptedBlock != null) {
                        // Write to output file
                        fosOutput.write(decryptedBlock);
                        
                        // Collect for later processing
                        allDecrypted.write(decryptedBlock);
                        
                        totalDecrypted += decryptedBlock.length;
                    }
                }
                
                // Finalize decryption
                byte[] finalBlock = aesCipher.doFinal();
                if (finalBlock != null && finalBlock.length > 0) {
                    fosOutput.write(finalBlock);
                    allDecrypted.write(finalBlock);
                    totalDecrypted += finalBlock.length;
                }
                
                // Get all decrypted data
                byte[] allDecryptedBytes = allDecrypted.toByteArray();
                
                // Remove padding (trailing zeros)
                int actualLength = allDecryptedBytes.length;
                for (int i = allDecryptedBytes.length - 1; i >= 0; i--) {
                    if (allDecryptedBytes[i] != 0) {
                        actualLength = i + 1;
                        break;
                    }
                }
                
                // Trim to actual message length
                decryptedMessage = new byte[actualLength];
                System.arraycopy(allDecryptedBytes, 0, decryptedMessage, 0, actualLength);
                
                System.out.println("Message M decrypted successfully: " + decryptedMessage.length + " bytes");
                System.out.println("Decrypted message M saved to " + outputFilename);
                
                // APPEND M to message.kmk
                fosKmk.write(decryptedMessage);
                System.out.println("Message M appended to message.kmk");
                
            }
            
            // APPEND Kxy again to message.kmk (now we have Kxy || M || Kxy)
            try (FileOutputStream fos = new FileOutputStream("message.kmk", true)) {
                fos.write(Kxy);
                System.out.println("Kxy appended to message.kmk");
                System.out.println("Final message.kmk structure: Kxy || M || Kxy");
            }
            
        } catch (Exception e) {
            System.out.println("Error during AES decryption: " + e.getMessage());
            e.printStackTrace();
            scanner.close();
            return;
        }
        
        // Step 6: Calculate SHA256(Kxy || M || Kxy) by reading message.kmk piece by piece
        System.out.println("\nStep 3: Calculating SHA256(Kxy || M || Kxy)...");
        
        byte[] calculatedHash = null;
        try {
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            
            // Read message.kmk piece by piece (1024 bytes at a time)
            try (FileInputStream fis = new FileInputStream("message.kmk")) {
                byte[] buffer = new byte[1024]; // Small multiple of 1024 bytes
                int bytesRead;
                int totalRead = 0;
                
                System.out.println("Reading message.kmk piece by piece...");
                
                while ((bytesRead = fis.read(buffer)) != -1) {
                    sha256.update(buffer, 0, bytesRead);
                    totalRead += bytesRead;
                    System.out.println("  Read " + bytesRead + " bytes (total: " + totalRead + " bytes)");
                }
                
                System.out.println("Total bytes read from message.kmk: " + totalRead + " bytes");
            }
            
            // Get final hash
            calculatedHash = sha256.digest();
            
            System.out.println("SHA256 hash calculated successfully: " + calculatedHash.length + " bytes");
            
        } catch (Exception e) {
            System.out.println("Error calculating SHA256: " + e.getMessage());
            e.printStackTrace();
            scanner.close();
            return;
        }
        
        // Step 7: Compare with message.khmac and display results
        System.out.println("\nStep 4: Verifying hash MAC...");
        
        try {
            // Read the hash from message.khmac
            byte[] receivedHash = Files.readAllBytes(Paths.get("message.khmac"));
            System.out.println("Read keyed hash MAC from message.khmac: " + receivedHash.length + " bytes");
            
            // Display CALCULATED hash in hexadecimal
            System.out.println("\nCalculated SHA256(Kxy || M || Kxy) in Hexadecimal:");
            for (int i = 0; i < calculatedHash.length; i++) {
                System.out.format("%02X ", calculatedHash[i]);
                if ((i + 1) % 16 == 0) {
                    System.out.println();
                }
            }
            System.out.println();
            
            // Display RECEIVED hash in hexadecimal
            System.out.println("Received SHA256(Kxy || M || Kxy) from message.khmac in Hexadecimal:");
            for (int i = 0; i < receivedHash.length; i++) {
                System.out.format("%02X ", receivedHash[i]);
                if ((i + 1) % 16 == 0) {
                    System.out.println();
                }
            }
            System.out.println();
            
            // COMPARE the hashes
            System.out.println("=== Message Authentication Checking ===");
            if (Arrays.equals(calculatedHash, receivedHash)) {
                System.out.println("✓ PASS: Hash verification passed!");
                System.out.println("✓ The message has NOT been tampered with.");
                System.out.println("✓ Message integrity and authenticity confirmed.");
            } else {
                System.out.println("✗ FAIL: Hash verification FAILED!");
                System.out.println("✗ The message may have been tampered with.");
                System.out.println("✗ Message integrity compromised.");
                
                // Show which byte differs
                for (int i = 0; i < Math.min(calculatedHash.length, receivedHash.length); i++) {
                    if (calculatedHash[i] != receivedHash[i]) {
                        System.out.println("✗ First difference at byte " + i + 
                            ": calculated=" + String.format("%02X", calculatedHash[i]) +
                            ", received=" + String.format("%02X", receivedHash[i]));
                        break;
                    }
                }
            }
            
        } catch (IOException e) {
            System.out.println("Error reading message.khmac: " + e.getMessage());
            e.printStackTrace();
            scanner.close();
            return;
        }
        
        scanner.close();
        System.out.println("\n=== Receiver program complete! ===");
    }
    
    // Method to read RSA private key from file
    public static PrivateKey readPrivKeyFromFile(String keyFileName) throws Exception {
        FileInputStream in = new FileInputStream(keyFileName);
        ObjectInputStream oin = new ObjectInputStream(new BufferedInputStream(in));
        
        try {
            BigInteger m = (BigInteger) oin.readObject();
            BigInteger e = (BigInteger) oin.readObject();
            
            System.out.println("Read from " + keyFileName + ": modulus and exponent loaded");
            
            RSAPrivateKeySpec keySpec = new RSAPrivateKeySpec(m, e);
            KeyFactory factory = KeyFactory.getInstance("RSA");
            PrivateKey key = factory.generatePrivate(keySpec);
            
            return key;
        } finally {
            oin.close();
        }
    }
}
