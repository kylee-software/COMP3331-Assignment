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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;

public class Server {

    // Server information
    private static ServerSocket serverSocket;
    private static Integer serverPort;
    public final long BLOCK_DURATION;
    public final long TIMEOUT;
    public final LocalDateTime START_TIME;

    private static HashMap<String, User> data = new HashMap<>();
    private static ArrayList<ClientThread> clients = new ArrayList<>();

    public Server(long blockDuration, long timeout) {
        BLOCK_DURATION = blockDuration;
        TIMEOUT = timeout;
        START_TIME = LocalDateTime.now();
    }

    /* ┌────────────────────────────────────────────────────────────────┐ */
    /* │                       Users Related Functions                  │ */
    /* └────────────────────────────────────────────────────────────────┘ */
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

    /* ┌────────────────────────────────────────────────────────────────┐ */
    /* │                            Broadcasts                          │ */
    /* └────────────────────────────────────────────────────────────────┘ */

    /**
     * broadcast messages to all online users excluding the sender and users that blocked the sender
     *
     * @param type   "presence" or "message"
     *               presence: to notify users whenever a user logged in and logged out
     *               message: sender being one of the users trying to send a broadcast message to all online users
     * @param packet the packet that contains the information about the message
     * @throws Exception throw exception when an error occurs
     */
    public void broadcast(String type, Packet packet) throws IOException {
        String sender = packet.getSender();
        User senderInfo = getUser(sender);
        boolean blockedBroadcast = false;

        for (ClientThread client : clients) {
            User user = client.getUser();
            if (user != null && (!user.getUsername().equals(sender))) {
                if (type.equals("presence")) {
                    if (!senderInfo.isUserBlacklisted(user.getUsername())) {
                        System.out.println(sender + " " + user.getUsername());
                        packet.setSender("SERVER");
                        client.receiveBroadcast(packet);
                    }
                } else {
                    if (!user.isUserBlacklisted(sender)) {
                        client.receiveBroadcast(packet);
                    } else {
                        // when another user blocked the sender
                        blockedBroadcast = true;
                    }
                }
            }
        }

        if (blockedBroadcast) {
            // inform the sender about the status of the message
            ClientThread senderServer = getClientServer(sender);
            Packet confirmationMsg = new Packet("server", "broadcast");
            confirmationMsg.setMessage("the message is successfully sent to most users except for some.");
            senderServer.receiveBroadcast(confirmationMsg);
        }
    }

    /* ┌────────────────────────────────────────────────────────────────┐ */
    /* │                         List of Online Users                   │ */
    /* └────────────────────────────────────────────────────────────────┘ */

    /**
     * list of all online users upon a request of a user
     *
     * @param requester the user who is requesting the command "whoelse"
     * @return a packet contains the list of all online user excluding the requester and all users who blocked
     * the requester
     */
    public Packet listAllOnlineUsers(String requester) {
        Packet packet = new Packet("SERVER", "whoelse");
        String messageBody = "";
        int onlineCount = 0;

        for (ClientThread client : clients) {
            User user = client.getUser();
            if (user != null && (!user.getUsername().equals(requester))) {
                String username = user.getUsername();
                if (!user.isUserBlacklisted(requester) && user.getLoginStatus().equals("ONLINE")) {
                    messageBody += ("\n" + "    " + username);
                    onlineCount += 1;
                }
            }
        }

        packet.setMessage(String.valueOf(onlineCount) + " other user(s) are currently online." + messageBody);
        return packet;
    }

    /* ┌────────────────────────────────────────────────────────────────┐ */
    /* │                         Online History                         │ */
    /* └────────────────────────────────────────────────────────────────┘ */

    /**
     * get a list of users that were online since the requested date time
     *
     * @param requester the requester who is requesting the command "whoelsesince"
     * @param dateTime  the date time that is being considered
     * @return a packet contains a list of users that were online since the requested date time excluding the
     * requester and all the users who blocked the requester
     */
    public Packet getUsersSince(String requester, LocalDateTime dateTime) {
        Packet packet = new Packet("SERVER", "whoelsesince");
        String messageBody = "";
        int onlineCount = 0;

        for (User user : data.values()) {
            boolean isBlocked = user.isUserBlacklisted(requester);
            String username = user.getUsername();
            if (!isBlocked && (!username.equals(requester))) {
                if (user.getLastLogin().compareTo(dateTime) >= 0) {
                    messageBody += ("\n" + "    " + username);
                    onlineCount += 1;
                }
            }
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        packet.setMessage(
                String.valueOf(onlineCount) + " other user(s) are online since " + dateTime.format(formatter) + "." +
                messageBody);
        return packet;
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
