/*
 * Java socket programming client example with TCP
 * socket programming at the client side, which provides example of how to define client socket, how to send message to
 * the server and get response from the server with ObjectInputStream and ObjectOutputStream
 *
 * Author: Wei Song
 * Date: 2021-09-28
 * */

import java.net.*;

public class Client {
    private boolean isLoggedIn = false;
    private String user = null;
    private int portCount;

    public void setLoginStatus(boolean isLoggedIn) {
        this.isLoggedIn = isLoggedIn;
    }

    public boolean isLoggedIn() {
        return this.isLoggedIn;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getUser() {
        return this.user;
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.out.println("===== Error usage: java TCPClient SERVER_IP SERVER_PORT =====");
            return;
        }

        // server host and port number, which would be acquired from command line parameter
        String serverHost = args[0];
        int serverPort = Integer.parseInt(args[1]);

        Client client = new Client();

        // Start the P2P server
        ServerSocket p2pSocket = new ServerSocket(0);
        P2P p2p = new P2P(p2pSocket);

        try {
            // define socket for client
            Socket clientSocket = new Socket(serverHost, serverPort);

            // function for sending messages to the server
            ClientSendMessage clientSendMessageThread = new ClientSendMessage(client, clientSocket, p2p,
                                                                              p2pSocket.getLocalPort());
            // function for receiving messages from the server
            ClientReceiveMessage clientReceiveMessageThread = new ClientReceiveMessage(client, clientSocket, p2p, p2pSocket.getLocalPort());

            clientSendMessageThread.start();
            clientReceiveMessageThread.start();
        } catch (ConnectException connectException) {
            System.out.println("The server has not yet been initialized.");
        }
    }
}