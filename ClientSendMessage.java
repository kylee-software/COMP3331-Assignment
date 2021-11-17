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

    ClientSendMessage(Client client, Socket clientSocket) throws IOException {
        this.client = client;
        this.clientSocket = clientSocket;
        // define ObjectInputStream instance which would be used to receive response from the server
        this.outputStream = new ObjectOutputStream(clientSocket.getOutputStream());

        // define a BufferedReader to get command from command line i.e., standard command from keyboard
        this.reader = new BufferedReader(new InputStreamReader(System.in));
    }

    @Override
    public void run() {
        super.run();

        while (true) {
            try {
                isLoggedIn = client.isLoggedIn();
                user = client.getUser();

                if (!isLoggedIn) {
                    Thread.sleep(1000);
                    if (user == null) {
                        System.out.println("============= Welcome to the Greatest Messaging System! =============");
                        System.out.println("Action: login or register?");

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
                                System.out.println("Username:");
                                String username = reader.readLine();
                                // get new password
                                System.out.println("Password: ");
                                String password = reader.readLine();

                                sendMessage("register", username + " " + password);
                            }
                            default -> {
                                System.out.println("Error: Invalid Command!");
                            }
                        }
                    } else {
                        // ask user to re-enter their password if username is valid but wrong password
                        String password = reader.readLine();
                        sendMessage("login", user + " " + password);
                    }
                } else {
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

    private void sendMessage(String type, String message) throws Exception {
        Message toSend = new Message(null, type);
        toSend.setMessage(message);
        outputStream.writeObject(toSend);
        outputStream.flush();
    }

    private void logout() throws Exception {
        client.setLoginStatus(false);
        client.setUser(null);
        user = null;
    }
}
