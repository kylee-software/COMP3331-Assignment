import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Arrays;

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
    final String ANSI_BOLD = "\u001B[1m";

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
                        System.out.println(ANSI_BOLD + "\n============= Welcome to the Greatest Messaging System! " +
                                           "=============" + ANSI_RESET);
                        System.out.print("login or register: ");

                        String choice = reader.readLine();

                        switch (choice) {
                            case "login" -> {
                                System.out.println(
                                        "------------- " + ANSI_BOLD + "Login" + ANSI_RESET + " -------------");
                                // ask to log in if user hasn't logged in yet
                                System.out.print("Username: ");
                                String username = reader.readLine();

                                System.out.print("Password: ");
                                String password = reader.readLine();

                                sendMessage("login", username + " " + password);
                            }
                            case "register" -> {
                                System.out.println("------------ " + ANSI_BOLD + "Register" + ANSI_RESET + " " +
                                                   "------------");
                                // get user username
                                System.out.print("Username: ");
                                String username = reader.readLine();
                                // get new password
                                System.out.print("Password: ");
                                String password = reader.readLine();

                                sendMessage("register", username + " " + password);
                            }
                            default -> {
                                // prevent user from using other commands before logged in
                                System.out.println(ANSI_RED + ANSI_BOLD + "Error" + ANSI_RESET + ": Invalid Command!");
                                System.out.println(ANSI_BOLD +
                                                   "----------------------------------------------------------------------" +
                                                   ANSI_RESET);
                            }
                        }
                    } else {
                        // ask user to re-enter their password if username is valid but wrong password
                        String password = reader.readLine();
                        sendMessage("login", user + " " + password);
                    }
                } else {
                    // if user hasn't entered any input, go back to top of the loop
                    // this is for when server send multiple responses back to the client that would chanage the login
                    // status
                    if (reader.ready()) {
                        // allow users to use excluded features after they logged in
                        String[] command = reader.readLine().split(" ");

                        switch (command[0]) {
                            case "message" -> {
                                try {
                                    String messageBody = String.join(" ", Arrays.copyOfRange(command, 1,
                                                                                             command.length));
                                    sendMessage("message", messageBody);
                                } catch (Exception e) {
                                    System.out.println(invalidCommandMsg(command[0]));
                                }
                            }
                            case "broadcast" -> {
                                try {
                                    String messageBody =
                                            String.join(" ", Arrays.copyOfRange(command, 1, command.length));
                                    sendMessage("broadcast", messageBody);
                                } catch (Exception e) {
                                    System.out.println(invalidCommandMsg(command[0]));
                                }
                            }
                            case "whoelse" -> {
                                sendMessage("whoelse", "N/A");
                            }
                            case "whoelsesince" -> {
                                try {
                                    sendMessage("whoelsesince", command[1]);
                                } catch (Exception e) {
                                    System.out.println(invalidCommandMsg(command[0]));
                                }
                            }
                            case "block" -> {
                                try {
                                    sendMessage("block", command[1]);
                                } catch (Exception e) {
                                    System.out.println(invalidCommandMsg(command[0]));
                                }
                            }
                            case "unblock" -> {
                                try {
                                    sendMessage("unblock", command[1]);
                                } catch (Exception e) {
                                    System.out.println(invalidCommandMsg(command[0]));
                                }
                            }
                            case "logout" -> {
                                logout();
                                sendMessage("logout", "N/A");
                            }
                            case "startprivate" -> {
                                sendMessage(command[0], String.join(" ", Arrays.copyOfRange(command, 1,
                                                                                            command.length)));
                            }
                            case "exit" -> {
                                logout();
                                // send a message to server to close input stream
                                sendMessage("exit", "N/A");
                                clientSocket.close();
                                outputStream.close();
                            }
                            default -> {
                                // prevent user from using invalid commands before logged in
                                System.out.println(ANSI_RED + ANSI_BOLD + "Error" + ANSI_RESET + ": Invalid Command!");
                                System.out.println(ANSI_BOLD +
                                                   "----------------------------------------------------------------------" +
                                                   ANSI_RESET);
                            }
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
     *
     * @param type    represents the command user entered
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
     *
     * @throws Exception throw exception when error occur
     */
    private void logout() throws Exception {
        client.setLoginStatus(false);
        client.setUser(null);
        user = null;
    }

    /**
     * format the error message when user entered uncompleted command
     *
     * @param type the type of command
     * @return the error message
     */
    private String invalidCommandMsg(String type) {
        String toReturn = ANSI_RED + "Error" + ANSI_RESET + ": [correct format] " + type;

        switch (type) {
            case "message" -> {
                toReturn += " <user> <message>";
            }
            case "broadcast" -> {
                toReturn += " <message>";
            }
            case "whoelsesince" -> {
                toReturn += " <time in seconds>";
            }
            case "block", "unblock" -> {
                toReturn += " <user>";
            }
        }
        return toReturn;
    }
}
