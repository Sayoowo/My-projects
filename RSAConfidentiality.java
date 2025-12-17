import java.io.*;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.*;
import java.security.spec.*;
import java.util.Scanner;

public class RSAConfidentiality {
    
    public static void main(String[] args) throws Exception {
        SecureRandom random = new SecureRandom();
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(1024, random);
        
        // Step 1: Create RSA key pair for X
        KeyPair pairX = generator.generateKeyPair();
        Key pubKeyX = pairX.getPublic();
        Key privKeyX = pairX.getPrivate();
        
        // Step 2: Create RSA key pair for Y
        KeyPair pairY = generator.generateKeyPair();
        Key pubKeyY = pairY.getPublic();
        Key privKeyY = pairY.getPrivate();
        
        // Step 3: Get key parameters and save to files
        KeyFactory factory = KeyFactory.getInstance("RSA");
        
        // X's public key
        RSAPublicKeySpec pubKSpecX = factory.getKeySpec(pubKeyX, RSAPublicKeySpec.class);
        saveToFile("XPublic.key", pubKSpecX.getModulus(), pubKSpecX.getPublicExponent());
        
        // X's private key
        RSAPrivateKeySpec privKSpecX = factory.getKeySpec(privKeyX, RSAPrivateKeySpec.class);
        saveToFile("XPrivate.key", privKSpecX.getModulus(), privKSpecX.getPrivateExponent());
        
        // Y's public key
        RSAPublicKeySpec pubKSpecY = factory.getKeySpec(pubKeyY, RSAPublicKeySpec.class);
        saveToFile("YPublic.key", pubKSpecY.getModulus(), pubKSpecY.getPublicExponent());
        
        // Y's private key
        RSAPrivateKeySpec privKSpecY = factory.getKeySpec(privKeyY, RSAPrivateKeySpec.class);
        saveToFile("YPrivate.key", privKSpecY.getModulus(), privKSpecY.getPrivateExponent());
        
        // Step 4: Get 16-character symmetric key from user
        Scanner scanner = new Scanner(System.in);
        String symmetricKey = null;
        
        while (symmetricKey == null || symmetricKey.length() != 16) {
            System.out.print("Enter a 16-character string for the symmetric key: ");
            symmetricKey = scanner.nextLine();
            
            if (symmetricKey.length() != 16) {
                System.out.println("Error: Key must be exactly 16 characters! You entered " 
                    + symmetricKey.length() + " characters.");
            }
        }
        
        // Save symmetric key to file (UTF-8 encoding = 128 bits for 16 characters)
        Files.write(Paths.get("symmetric.key"), symmetricKey.getBytes("UTF-8"));
        System.out.println("Symmetric key saved to symmetric.key (128-bit AES key)");
        
        scanner.close();
        
        System.out.println("\nKey generation complete!");
        System.out.println("Created files: XPublic.key, XPrivate.key, YPublic.key, YPrivate.key, symmetric.key");
    }
    
    // Save RSA key parameters to file
    public static void saveToFile(String fileName, BigInteger mod, BigInteger exp) 
            throws IOException {
        
        System.out.println("Write to " + fileName + ": modulus = " + 
            mod.toString() + ", exponent = " + exp.toString() + "\n");
        
        ObjectOutputStream oout = new ObjectOutputStream(
            new BufferedOutputStream(new FileOutputStream(fileName)));
        
        try {
            oout.writeObject(mod);
            oout.writeObject(exp);
        } catch (Exception e) {
            throw new IOException("Unexpected error", e);
        } finally {
            oout.close();
        }
    }
}