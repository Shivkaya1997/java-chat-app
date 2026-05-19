Multi-Client Java Chat ApplicationA lightweight, multi-threaded console chat application built using native Java TCP sockets. 
This project demonstrates core networking concepts, asynchronous data streams, and concurrent thread management without relying on external third-party frameworks.


🚢 FeaturesMulti-Client Support: Handles dozens of concurrent client connections simultaneously using Java threads.
Real-time Broadcasting: Instantly forwards incoming text payloads to all other active chat participants.
Thread-Safe Registry: Uses a CopyOnWriteArrayList to safely track active client handles without concurrency crashes.
Non-Blocking Client UI: Uses a dedicated background listener thread to display incoming messages while you type.
Clean Resource Teardown: Automatically handles unexpected disconnects and safely releases network socket streams.


🏗 Architecture OverviewThe system uses a centralized server model split into three distinct files:ChatServer.
java: Runs the main loop on port 8080, listens for handshakes, and spins up a thread for each new user.
ClientHandler.java: Implements Runnable. Runs in the background on the server to manage the read/write lifecycle of a single user.
ChatClient.java: The end-user terminal interface. It connects to the server and handles user input/output streams simultaneously.

🚀 Getting StartedPrerequisitesJava Development Kit (JDK): version 8 or higher installed.
Terminal/Command Prompt: To compile and execute the classes.1. CompilationOpen your terminal inside the project directory containing your .java files and compile them all using the Java compiler:bashjavac ChatServer.java ClientHandler.java ChatClient.java
Use code with caution.2. Running the ServerStart the centralized chat engine first. It will bind to port 8080 and wait for connections:bashjava ChatServer
Use code with caution.Expected Output:textChat Server initialized on port 8080
Use code with caution.3. Running the ClientsOpen a new, separate terminal window for each client you want to connect (e.g., User A and User B):bashjava ChatClient
Use code with caution.Expected Output:textConnected to Chat Server at 127.0.0.1:8080
> 
Use code with caution.💬 How to UseType any message into a client window and press Enter to send it.The message will instantly appear on all other active client terminals.To exit the chat safely and close the connection, simply type exit and press Enter.
