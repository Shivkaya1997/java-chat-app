import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class ChatServer {
    private static final int PORT = 6000;
    // Thread-safe map to store Username -> Output Stream pairing
    private static final Map<String, PrintWriter> activeClients = new ConcurrentHashMap<>();
    // Thread pool limits maximum resource footprints on the host system
    private static final ExecutorService threadPool = Executors.newFixedThreadPool(50);

    public static void main(String[] args) {
        System.out.println("[SERVER] Booting chat server on port " + PORT + "...");
        
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("[SERVER] Network port bound successfully. Awaiting clients...");
            
            while (!serverSocket.isClosed()) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    System.out.println("[CONNECT] Raw handshake from: " + clientSocket.getRemoteSocketAddress());
                    
                    // Delegate client lifecycle management to our thread pool
                    threadPool.execute(new ClientConnectionHandler(clientSocket));
                } catch (IOException e) {
                    System.err.println("[ERROR] Failed connection handshake: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("[CRITICAL] Could not start server on port " + PORT + ": " + e.getMessage());
        } finally {
            threadPool.shutdown();
        }
    }

    private static class ClientConnectionHandler implements Runnable {
        private final Socket socket;
        private String username;
        private BufferedReader reader;
        private PrintWriter writer;

        public ClientConnectionHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                // Initialize dedicated streams for this socket connection
                this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                this.writer = new PrintWriter(socket.getOutputStream(), true);

                // Phase 1: Unique Username Registration Protocol
                while (true) {
                    writer.println("SUBMIT_USERNAME_REQUEST");
                    username = reader.readLine();
                    
                    if (username == null) return; // Client dropped connection early
                    username = username.trim();

                    if (!username.isEmpty() && !activeClients.containsKey(username)) {
                        activeClients.put(username, writer);
                        writer.println("USERNAME_ACCEPTED " + username);
                        broadcast("[SYSTEM] " + username + " has joined the conversation.");
                        System.out.println("[REGISTRATION] Registered client: " + username);
                        break;
                    } else {
                        writer.println("USERNAME_REJECTED");
                    }
                }

                // Phase 2: Live Chat Broadcast Engine
                String incomingPayload;
                while ((incomingPayload = reader.readLine()) != null) {
                    if (incomingPayload.trim().equalsIgnoreCase("/quit")) {
                        break;
                    }
                    // Prevent empty line broadcasts
                    if (!incomingPayload.trim().isEmpty()) {
                        broadcast(username + ": " + incomingPayload);
                    }
                }
            } catch (IOException e) {
                System.out.println("[NETWORK INFO] " + (username != null ? username : "Unregistered user") + " connection reset.");
            } finally {
                cleanUpResources();
            }
        }

        // Thread-safe delivery mechanism to write out to all connections simultaneously
        private void broadcast(String standardMessage) {
            for (PrintWriter clientWriter : activeClients.values()) {
                clientWriter.println(standardMessage);
            }
        }

        // Guarantees zero memory/thread leaks when a socket breaks down
        private void cleanUpResources() {
            if (username != null && activeClients.containsKey(username)) {
                activeClients.remove(username);
                broadcast("[SYSTEM] " + username + " has departed the chat.");
                System.out.println("[DISCONNECT] Cleared profile for: " + username);
            }
            try {
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
            } catch (IOException e) {
                System.err.println("[ERROR] Resource teardown exception: " + e.getMessage());
            }
        }
    }
}

