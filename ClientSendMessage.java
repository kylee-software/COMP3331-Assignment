import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ClientSendMessage extends Thread {
    private Client client;
    private Socket clientSocket;
    private final ObjectOutputStream outputStream;
    private final BufferedReader reader;
    private boolean isLoggedIn;
    private String user;

    // coloring text
    final String ANSI_RESET = "\u001B[0m";
    final String ANSI_RED = "\u001B[31m";

    ClientSendMessage(Client client, Socket clientSocket) throws IOException {
        this.client = client;
        this.clientSocket = clientSocket;
        // define ObjectInputStream instance which would be used to receive response from the server
        this.outputStream = new ObjectOutputStream(clientSocket.getOutputStream());

        // define a BufferedReader to get command from command line i.e., standard command from keyboard
        this.reader = new BufferedReader(new InputStreamReader(System.in));
    }

    /**
     * Main function that deals with handling the responses from the console to the server
     */
    @Override
    public void run() {
        super.run();

        while (true) {
            try {
                Thread.sleep(100);
                isLoggedIn = client.isLoggedIn();
                user = client.getUser();

                if (!isLoggedIn) {
                    if (user == null) {
                        System.out.println("============= Welcome to the Greatest Messaging System! =============");
                        System.out.println(ANSI_RED + "Action!" + ANSI_RESET + " login or register: ");

                        String choice = reader.readLine();

                        switch (choice) {
                            case "login" -> {
                                System.out.println("============= Login =============");
                                // ask to log in if user hasn't logged in yet
                                System.out.println("Username:");
                                String username = reader.readLine();

                                System.out.println("Password:");
                                String password = reader.readLine();

                                sendMessage("login", username + " " + password);
                            }
                            case "register" -> {
                                System.out.println("============= Register =============");

                                // get user username
                                System.out.println("Username: ");
                                String username = reader.readLine();
                                // get new password
                                System.out.println("Password: ");
                                String password = reader.readLine();

                                sendMessage("register", username + " " + password);
                            }
                            default -> {
                                // prevent user from using other commands before logged in
                                System.out.println(ANSI_RED + "Error" + ANSI_RESET + ": Invalid Command!");
                            }
                        }
                    } else {
                        // ask user to re-enter their password if username is valid but wrong password
                        String password = reader.readLine();
                        sendMessage("login", user + " " + password);
                    }
                } else {
                    // allow users to use excluded features after they logged in
                    String[] command = reader.readLine().split(" ");

                    switch (command[0]) {
                        case "message" -> {
                            sendMessage("message", command[1] + " " + command[2]);
                        }
                        case "broadcast" -> {
                            sendMessage("broadcast", command[1]);
                        }
                        case "whoelse" -> {
                            sendMessage("whoelse","N/A");
                        }
                        case "whoelsesince" -> {
                            sendMessage("whoelsesince", command[1]);
                        }
                        case "block" -> {
                            sendMessage("block", command[1]);
                        }
                        case "unblock" -> {
                            sendMessage("unblock", command[1]);
                        }
                        case "logout" -> {
                            logout();
                            sendMessage("logout", "N/A");
                        }
                        case "exit" -> {
                            logout();
                            // send a message to server to close input stream
                            sendMessage("exit", "N/A");
                            clientSocket.close();
                            outputStream.close();
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                break;
            }
        }
    }

    /**
     * send a message to the server in order to handle the inputs from the console
     * @param type represents the command user entered
     * @param message message body of the packet
     * @throws Exception throw exception when error occur
     */
    private void sendMessage(String type, String message) throws Exception {
        Packet toSend = new Packet(null, type);
        toSend.setMessage(message);
        outputStream.writeObject(toSend);
        outputStream.flush();
    }

    /**
     * reset login status when a user logout
     * @throws Exception throw exception when error occur
     */
    private void logout() throws Exception {
        client.setLoginStatus(false);
        client.setUser(null);
        user = null;
    }
}
