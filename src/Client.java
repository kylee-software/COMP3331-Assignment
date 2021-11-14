/*
 * Java socket programming client example with TCP
 * socket programming at the client side, which provides example of how to define client socket, how to send message to
 * the server and get response from the server with DataInputStream and DataOutputStream
 *
 * Author: Wei Song
 * Date: 2021-09-28
 * */

import java.net.*;
import java.io.*;

public class Client {
    // server host and port number, which would be acquired from command line parameter
    private static String serverHost;
    private static Integer serverPort;
    private static Socket clientSocket;
    private static DataInputStream dataInputStream;
    private static DataOutputStream dataOutputStream;
    private static BufferedReader reader;
    private static boolean isLoggedIn;
    private static String user;

    /* ┌────────────────────────────────────────────────────────────────┐ */
    /* │                        Authentication                          │ */
    /* └────────────────────────────────────────────────────────────────┘ */

    private static void login(String loginStatus, String username) throws IOException {
        switch (loginStatus) {
            case "USERNAME" -> System.out.println("Username is invalid! Please try again or register a new account!");
            case "BLOCKED" -> System.out.println(
                    "Your account is blocked die to multiple failed login attempts! Please try again " +
                    "later!");
            case "ONLINE" -> System.out.println(
                    "This account is already logged in somewhere else. Please try another account!");
            case "SUCCESS" -> {
                isLoggedIn = true;
                user = username;
                System.out.println("You have logged in successfully!");
            }
            case "PASSWORD" -> {
                String response = "PASSWORD";
                while (response.equals("PASSWORD")) {
                    System.out.println("Wrong password! Please re-enter:");
                    String input = reader.readLine();
                    dataOutputStream.writeUTF(input);
                    dataOutputStream.flush();

                    response = (String) dataInputStream.readUTF();

                    if (response.equals("BLOCKED")) {
                        System.out.println(
                                "Your account is blocked die to multiple failed login attempts! Please try again " +
                                "later!");
                    } else {
                        isLoggedIn = true;
                        user = username;
                        System.out.println("You have logged in successfully!");
                    }
                    break;
                }
            }
        }
    }

    private static void register(String loginStatus, String username) throws IOException {
        if (loginStatus.equals("USERNAME")) {
            System.out.println("Account with this username is already existed! Please try a different username");
        } else {
            isLoggedIn = true;
            user = username;
            System.out.println("You have successfully created an account! Welcome to the SQUAD " + username + "!");
        }
    }

    private static void logout() throws IOException {
        isLoggedIn = false;
        dataOutputStream.writeUTF("logout" + " " + user);
        dataOutputStream.flush();
        user = null;
    }

    /* ┌────────────────────────────────────────────────────────────────┐ */
    /* │                           Message                              │ */
    /* └────────────────────────────────────────────────────────────────┘ */

    /* ┌────────────────────────────────────────────────────────────────┐ */
    /* │                           Notifications                        │ */
    /* └────────────────────────────────────────────────────────────────┘ */

    /* ┌────────────────────────────────────────────────────────────────┐ */
    /* │                         Find Users Online                      │ */
    /* └────────────────────────────────────────────────────────────────┘ */

    /* ┌────────────────────────────────────────────────────────────────┐ */
    /* │                       Find Online History                      │ */
    /* └────────────────────────────────────────────────────────────────┘ */

    /* ┌────────────────────────────────────────────────────────────────┐ */
    /* │                            Blacklist                           │ */
    /* └────────────────────────────────────────────────────────────────┘ */

    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            System.out.println("===== Error usage: java TCPClient SERVER_IP SERVER_PORT =====");
            return;
        }

        serverHost = args[0];
        serverPort = Integer.parseInt(args[1]);

        // define socket for client
        clientSocket = new Socket(serverHost, serverPort);

        // define DataInputStream instance which would be used to receive response from the server
        // define DataOutputStream instance which would be used to send message to the server
        dataInputStream = new DataInputStream(clientSocket.getInputStream());
        dataOutputStream = new DataOutputStream(clientSocket.getOutputStream());

        // define a BufferedReader to get input from command line i.e., standard input from keyboard
        reader = new BufferedReader(new InputStreamReader(System.in));

        while (true) {
            if (!isLoggedIn) {
                System.out.println("======== Welcome to Awesomeness! ========");
                System.out.println("Action: login or register?");
                // read input from command line
                String message = reader.readLine();

                switch (message) {
                    case "login" -> {
                        System.out.println("============= Login =============");
                        // get username
                        System.out.println("Username:");
                        String username = reader.readLine();
                        // get password
                        System.out.println("Password:");
                        String password = reader.readLine();
                        // write message into dataOutputStream and send/flush to the server
                        dataOutputStream.writeUTF("login" + " " + username + " " + password);
                        dataOutputStream.flush();

                        String response = (String) dataInputStream.readUTF();
                        login(response, username);
                    }
                    case "register" -> {
                        System.out.println("============= Register =============");

                        // get user username
                        System.out.println("Username:");
                        String username = reader.readLine();
                        // get new password
                        System.out.println("Password: ");
                        String password = reader.readLine();

                        dataOutputStream.writeUTF("register" + " " + username + " " + password);
                        dataOutputStream.flush();

                        String response = (String) dataInputStream.readUTF();
                        register(response, username);
                    }
                    case "exit" -> {
                        clientSocket.close();
                        dataOutputStream.close();
                        dataInputStream.close();
                    }
                }
            } else {
                System.out.println("What can I do for you?");
                String[] input = reader.readLine().split(" ");

                switch (input[0]) {
                    case "message" -> {
                        break;
                    }
                    case "broadcast" -> {
                        break;
                    }
                    case "whoelse" -> {
                        break;
                    }
                    case "whoelsesince" -> {
                        break;
                    }
                    case "block" -> {
                        break;
                    }
                    case "unblock" -> {
                        break;
                    }
                    case "logout" -> {
                        logout();
                    }
                    case "exit" -> {
                        logout();
                        clientSocket.close();
                        dataOutputStream.close();
                        dataInputStream.close();
                        break;
                    }
                }
            }
        }
    }
}