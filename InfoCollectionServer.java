import javax.net.ssl.* ;
import java.io.*;

public class InfoCollectionServer {
    public static void main (String[] args ) {
       
       // Checks if port number is provided 
        if (args.length != 1){
            System.out.println("Usage: java InfoCollectionServer <port>");
            return;
        }
        int port = Integer.parseInt(args[0]);
        System.out.println("Starting on port " + port + "....");
        

        try{
            //create ssl server socket factory
            SSLServerSocketFactory sslFactory = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
            
            //create ssl server on port 
            SSLServerSocket serverSocket = 
            (SSLServerSocket) sslFactory.createServerSocket(port);
            System.out.println("Server is listening to port"+ port);
            
            int userID = 1; // counter for unqiue user filenames 
                // keep accepting clients forever 
                while(true){

                System.out.println("\nWaiting for client connection...");
                    

                    //Accepts incoming clients
                    SSLSocket clientSocket = (SSLSocket) serverSocket.accept();
                    System.out.println ("client connected! Starting a new thread");
                     // create a new thread for this client
                   Thread clientThread = new  Thread(new ClientHandler(clientSocket, userID));
    clientThread.start();

                    userID++;

                 }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

    // ClientHandler class - runs in separate thread for each client
class ClientHandler implements Runnable {
    private SSLSocket socket;
    private int userID;
    
    public ClientHandler(SSLSocket socket, int userID) {
        this.socket = socket;
        this.userID = userID;
    }
    
    public void run() {
        try {
            SSLSession session = socket.getSession();
            
            System.out.println("\n=== SSL Session Information ===");
            System.out.println("Peer host is " + session.getPeerHost());
            System.out.println("Cipher suite is " + session.getCipherSuite());
            System.out.println("Protocol is " + session.getProtocol());
            System.out.println("Session ID is " + bytesToHex(session.getId()));
            System.out.println("The creation time of this session is " + 
                new java.util.Date(session.getCreationTime()));
            System.out.println("The last accessed time of this session is " + 
                new java.util.Date(session.getLastAccessedTime()));
            System.out.println("===============================\n");
            
            BufferedReader in = new BufferedReader(
                new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            
            boolean addMore = true;
            int currentUserID = userID;
            
            while (addMore) {
                String fileName = currentUserID + ".txt";
                PrintWriter fileWriter = new PrintWriter(new FileWriter(fileName));
                
                System.out.println("Collecting information for user " + currentUserID);
                
                out.println("User Name: ");
                String userName = in.readLine();
                fileWriter.println("User Name: " + userName);
                System.out.println("  Received: " + userName);
                
                out.println("Full Name: ");
                String fullName = in.readLine();
                fileWriter.println("Full Name: " + fullName);
                System.out.println("  Received: " + fullName);
                
                out.println("Address: ");
                String address = in.readLine();
                fileWriter.println("Address: " + address);
                System.out.println("  Received: " + address);
                
                out.println("Phone number: ");
                String phone = in.readLine();
                fileWriter.println("Phone number: " + phone);
                System.out.println("  Received: " + phone);
                
                out.println("Email address: ");
                String email = in.readLine();
                fileWriter.println("Email address: " + email);
                System.out.println("  Received: " + email);
                
                fileWriter.close();
                System.out.println("Saved to " + fileName);
                
                out.println("Add more users? (yes or any for no)");
                String response = in.readLine();
                System.out.println("  Received: " + response);
                
                if (!response.equalsIgnoreCase("yes")) {
                    addMore = false;
                }
                
                currentUserID++;
            }
            
            socket.close();
            System.out.println("Connection closed.\n");
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }
}
