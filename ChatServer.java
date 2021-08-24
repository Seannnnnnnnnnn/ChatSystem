package com.ChatRoomApplication;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class ChatServer {
    static final int standardPort = 4444;
    static final boolean alive = true;
    static private final List<ClientConnection> connectionList = new ArrayList<>();
    static private final List<ChatRoom> roomList = new ArrayList<>();


    public static void main(String[] args) {
        handleNewConnections();
    }


    static synchronized void addRoom(ChatRoom roomThread) {
        roomList.add(roomThread);
    }


    static synchronized void closeClientConnection(ClientConnection connection) {
        broadcast(String.format("[Server] : %s has left the chat", connection.guestName), null);
        connectionList.remove(connection);
    }


    static synchronized void addClientConnection(ClientConnection connection) {
        connectionList.add(connection);
        broadcast(String.format("[Server] : %s has entered the Main Hall\n", connection.guestName), null);
    }


    static void broadcast(String message, ChatRoom room) {
        /*
        broadcasts message to room. If room is null, broadcasts to all presently connected clients
        */
        System.out.println(message);
        for (ClientConnection connection: connectionList) {
            connection.sendMessage(message+"\n");
        }
    }


    static void handleNewConnections() {
        /*
        handles all new connections to the server as per the described Protocol.
        */
        ServerSocket serverSocket;
        ChatRoom mainHall = new ChatRoom("MainHall", null);
        addRoom(mainHall);
        System.out.println("[Server] created Main Hall");

        try {
            serverSocket = new ServerSocket(standardPort);
            System.out.format("[Server] hosting on port : %d\n", standardPort);
            while (alive) {
                Socket client = serverSocket.accept();
                ClientConnection clientConnection = new ClientConnection(client);
                Thread clientConnectionThread = new Thread(clientConnection);
                clientConnectionThread.start();

                addClientConnection(clientConnection);

            }
        } catch (IOException e) {
            e.getStackTrace();
        }
    }


    static class ChatRoom extends Thread {
        /*
        Implements the different rooms. Within chatRoom subclass, we contain list of all users within room.
        */
        private final String roomName;
        private final ClientConnection admin;
        private final List<ClientConnection> roomMembers = new ArrayList<>();


        public ChatRoom(String roomName, ClientConnection admin) {
            this.roomName = roomName;
            this.admin = admin;
            roomMembers.add(admin);
        }
    }


    static class ClientConnection extends Thread {
        /*
        Implements the separate client connections
        */
        private Socket socket;
        private BufferedReader reader;
        private PrintWriter writer;
        private String guestName;
        private ChatRoom currentRoom;
        private boolean connectionAlive = true;


        public ClientConnection(Socket socket) throws IOException {
            this.socket = socket;
            this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.writer = new PrintWriter(socket.getOutputStream());
            this.guestName = generateName();
            this.currentRoom = roomList.get(0);
        }


        private static String generateName() {
            /*
            Generates the default name for client connections when they join server
            */
            return String.format("guest%d", connectionList.size() + 1);
        }


        public void sendMessage(String message) {
            writer.print(message);
            writer.flush();
        }


        public void close() {
            try {
                closeClientConnection(this);
                reader.close();
                writer.close();
                socket.close();
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
        }


        @Override
        public void run() {
            while (connectionAlive) {
                try {
                    String input = reader.readLine();
                    if (input != null) {
                        broadcast(String.format("[%s] : %s", guestName, input), currentRoom);
                    } else {
                        connectionAlive = false;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            close();
        }
    }
}
