/*
 * Java multi-threading server with TCP
 * There are two points of this example code:
 * - socket programming with TCP e.g., how to define a server socket, how to exchange data between client and server
 * - multi-threading
 *
 * Author: Wei Song
 * Date: 2021-09-28
 * */
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.locks.ReentrantLock;

public class Server {

    // Server information
    private static ServerSocket serverSocket;
    private static Integer serverPort;
    public long blockDuration;
    public long timeout;
    private static HashMap<String, User> data = new HashMap<>();
    public ReentrantLock synLock = new ReentrantLock();
    private static ArrayList<ClientThread> clients = new ArrayList<>();

    public Server(long blockDuration, long timeout) {
        this.blockDuration = blockDuration;
        this.timeout = timeout;
    }

    // turn data into Hashmaps and return it
    private static void generateData() {
        File file = new File("credentials.txt");
        BufferedReader fileReader = null;
        // Getting user login information from the credential file
        try {
            fileReader = new BufferedReader(new FileReader(file));
            String line = null;

            while ((line = fileReader.readLine()) != null) {
                String[] userInfo = line.split(" ");
                String username = userInfo[0];
                String password = userInfo[1];

                data.put(username, new User(username, password));
            }
        } catch (Exception e) {
            System.out.println("Credential File Does Not Exist!");
            e.printStackTrace();
        }
    }

    // Save all the changes to a user's information
    public void addUser(String username, String password) {
        BufferedWriter fileWriter = null;

        try {
            fileWriter = new BufferedWriter(new FileWriter("credentials.txt", true));
            fileWriter.write(username + " " + password);
            fileWriter.newLine();
            fileWriter.close();

            data.put(username, new User(username, password));
        } catch (Exception e) {
            System.out.println("Credential File Does Not Exist!");
            e.printStackTrace();
        }
    }

    public User getUser(String username) {
        return data.get(username);
    }

    public void updateUser(User user) {
        data.put(user.getUsername(), user);
    }

    public ClientThread getClientServer(String target) {
        for (ClientThread client : clients) {
            if (client.getUser().getUsername().equals(target)) {
                return client;
            }
        }
        return null;
    }

    public void broadcast(String type, Message message) throws Exception {
        String sender = message.getSender();

        System.out.println("Broadcasting a message to all online users.");

        for (ClientThread client : clients) {
            User user = client.getUser();
            if (user != null && (!user.getUsername().equals(sender))) {
                if (type.equals("presence")) {
                    message.setMessage("SERVER");
                    client.receiveBroadcast(message);
                } else {
                    if (!user.isUserBlacklisted(sender)) {
                        client.receiveBroadcast(message);
                    }
                }
            }
        }
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 3) {
            System.out.println("===== Error usage: java TCPServer SERVER_PORT =====");
            return;
        }

        // acquire port number from command line parameter
        serverPort = Integer.parseInt(args[0]);
        long blockDuration = Integer.parseInt(args[1]);
        long timeout = Integer.parseInt(args[2]);

        Server server = new Server(blockDuration, timeout);

        // define server socket with the input port number, by default the host would be localhost i.e., 127.0.0.1
        serverSocket = new ServerSocket(serverPort);

        // get login credential data from txt file and parse it into hashmaps;
        generateData();

        // make serverSocket listen connection request from clients
        System.out.println("===== Server is running =====");
        System.out.println("===== Waiting for connection request from clients...=====");

        while (true) {
            // when new connection request reaches the server, then server socket establishes connection
            Socket clientSocket = serverSocket.accept();
            // for each user there would be one thread, all the request/response for that user would be processed in
            // that thread
            // different users will be working in different thread which is multi-threading (i.e., concurrent)
            ClientThread clientThread = new ClientThread(server, clientSocket);
            clients.add(clientThread);
            clientThread.start();
        }
    }
}
