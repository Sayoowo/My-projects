Hybrid Cryptographic Security System

A Java-based secure messaging system implementing hybrid encryption (AES-256 + RSA-2048) with SHA-256 keyed MAC authentication.

## Project Structure
```
CS3750/
├── KeyGen/
│   └── RSAConfidentiality.java    # Key generation
├── Sender/
│   └── senderProgram.java         # Encryption & MAC
└── Receiver/
    └── receiverProgram.java       # Decryption & Verification
```

## Features

- **Hybrid Encryption**: AES-256 for messages, RSA-2048 for key distribution
- **Authentication**: SHA-256 keyed MAC with Kxy || M || Kxy structure
- **Security**: Provides confidentiality, integrity, and authentication
- **Scalability**: Chunk-based processing for large files

## Usage

### 1. Generate Keys
```bash
cd KeyGen
javac RSAConfidentiality.java
java RSAConfidentiality
```

### 2. Encrypt Message
```bash
cd Sender
javac senderProgram.java
java senderProgram
```

### 3. Decrypt and Verify
```bash
cd Receiver
javac receiverProgram.java
java receiverProgram
```

## Technologies

- Java Cryptography Architecture (JCA)
- AES-256-ECB encryption
- RSA-2048 with PKCS#1 padding
- SHA-256 hashing

## Course Information

**Course:** CS 3750 - Computer and Network Security  
**Institution:** Metropolitan State University of Denver  
**Semester:** Fall 2024

## Author

Sayo Owolabi
