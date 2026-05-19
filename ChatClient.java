import java.io.*;
import java.net.*;
import java.util.Scanner;

public class ChatClient {
    private static final String HOST = "127.0.0.1";
    private static final int PORT = 6000;
    private volatile boolean isRunning = true;

    public static void main(String[] args) {
        new ChatClient().startClientPipeline();
    }

    public void startClientPipeline() {
        System.out.println("[CLIENT] Attempting node configuration with gateway " + HOST + ":" + PORT);
        
        try (Socket socket = new Socket(HOST, PORT);
             BufferedReader inboundNetworkStream = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter outboundNetworkStream = new PrintWriter(socket.getOutputStream(), true);
             Scanner localConsoleInput = new Scanner(System.in)) {

            System.out.println("[CLIENT] Established TCP pipe handshake with infrastructure server.");

            // Loop until server approves a completely unique handle/identity
            while (isRunning) {
                String validationFrame = inboundNetworkStream.readLine();
                if (validationFrame == null) {
                    System.out.println("[CRITICAL] Server dropped unexpected packet teardown frame.");
                    return;
                }

                if (validationFrame.startsWith("SUBMIT_USERNAME_REQUEST")) {
                    System.out.print("Choose your handle/username: ");
                    String desiredName = localConsoleInput.nextLine();
                    outboundNetworkStream.println(desiredName);
                } else if (validationFrame.startsWith("USERNAME_REJECTED")) {
                    System.out.println("[DENIED] That username is blank or already active. Try again.");
                } else if (validationFrame.startsWith("USERNAME_ACCEPTED")) {
                    String activeName = validationFrame.substring(18);
                    System.out.println("[SUCCESS] Identity set! Welcome, " + activeName + ".");
                    System.out.println("-> Type messages normally. Send '/quit' cleanly to disconnect.");
                    break;
                }
            }

            // Spawn the specialized Async Network Monitor Background Thread
            Thread messageReceiverThread = new Thread(new ServerListener(inboundNetworkStream));
            messageReceiverThread.start();

            // The Main Thread processes your outgoing commands directly from the keyboard console
            while (isRunning) {
                if (localConsoleInput.hasNextLine()) {
                    String userInput = localConsoleInput.nextLine();
                    outboundNetworkStream.println(userInput);
                    
                    if (userInput.trim().equalsIgnoreCase("/quit")) {
                        isRunning = false;
                    }
                }
            }

        } catch (IOException e) {
            System.err.println("[NETWORK DOWN] Offline. Could not connect to remote service terminal: " + e.getMessage());
        } finally {
            System.out.println("[SHUTDOWN] Network sessions terminated. Session closed safely.");
            System.exit(0);
        }
    }

    // Handles inbound data streams concurrently without locking up your terminal window
    private class ServerListener implements Runnable {
        private final BufferedReader netReader;

        public ServerListener(BufferedReader reader) {
            this.netReader = reader;
        }

        @Override
        public void run() {
            try {
                String networkPayload;
                while (isRunning && (networkPayload = netReader.readLine()) != null) {
                    System.out.println(networkPayload);
                }
            } catch (IOException e) {
                if (isRunning) {
                    System.out.println("[ALERT] Streaming channel broken down by the remote server host.");
                }
            } finally {
                isRunning = false;
            }
        }
    }
}

