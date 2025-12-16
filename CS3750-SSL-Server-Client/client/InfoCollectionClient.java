import java.io.* ;
import javax.net.ssl.*;


public class InfoCollectionClient {
    public static void main (String[] args ){
        // checks if hostname and ports are provided
        if (args.length !=2){
        System.out.println("Usuage: Java InfoCollectionClient <hostname> <port>");
        return ;
    }
    String host = args [0];
    int port = Integer.parseInt(args[1]);
       System.out.println("Connecting to " + host + " on port " + port + "...");

        try {
            // create ssl socket factory 
            SSLSocketFactory sslFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
            // create ssl socket and connect to server 
           SSLSocket socket = (SSLSocket) sslFactory.createSocket(host, port);
            System.out.println("Connected to server! \n");
            SSLSession session = socket.getSession();
            
            System.out.println("=== SSL Session Information ===");
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
            BufferedReader userInput = new BufferedReader(
                new InputStreamReader(System.in));
            
            String question;
            while ((question = in.readLine()) != null) {
                System.out.print(question);
                String answer = userInput.readLine();
                out.println(answer);
                
                if (question.contains("Add more users") && 
                    !answer.equalsIgnoreCase("yes")) {
                    break;
                }
            }
            
            socket.close();
            System.out.println("\nConnection closed.");
            
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error: " + e.getMessage());
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


