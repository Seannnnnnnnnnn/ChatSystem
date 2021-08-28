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


    static void newIdentity(ClientConnection connection) {
        /* the server generates a unique id for the client which is guest followed by the smallest integer
        greater than 0 that is currently not in use by any other connected client, he server tells the client its
        id using an newIdentity message */

    }

    static synchronized void addRoom(ChatRoom roomThread) {
        roomList.add(roomThread);
    }


    static synchronized void closeClientConnection(ClientConnection connection) {
        broadcastToAll(String.format("[Server] : %s has left the chat", connection.guestName));
        connectionList.remove(connection);
    }


    static synchronized void addClientConnection(ClientConnection connection) {
        connectionList.add(connection);
        broadcastToAll(String.format("[Server] : %s has entered the Main Hall", connection.guestName));
    }


    static void broadcastToAll(String message) {
        /*
        broadcasts message to all live connections
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
                mainHall.joinRoom(clientConnection);
            }
        } catch (IOException e) {
            e.getStackTrace();
        }
    }


    static class ChatRoom {
        /*
        Implements the different rooms. Within chatRoom subclass, we contain list of all users within room.
        */
        private final String roomName;
        private final ClientConnection admin;
        static private final List<ClientConnection> roomMembers = new ArrayList<>();


        public ChatRoom(String roomName, ClientConnection admin) {
            this.roomName = roomName;
            this.admin = admin;
            roomMembers.add(admin);
        }


        public void joinRoom(ClientConnection connection) {
            broadcastToAll(String.format("[Server] : %s has joined %s", connection.guestName, roomName));
            roomMembers.add(connection);
        }


        private void broadcastToRoom(String message) {
            /*
            Broadcasts message to ClientConnection's in roomMembers
            */
            for (ClientConnection connection: roomMembers) {
                if (connection != null) {
                    connection.sendMessage(String.format("[%s] : %s\n", roomName, message));
                }
            }
        }
    }


    static class ClientConnection extends Thread {
        /*
        Implements the separate client connections
        */
        private final Socket socket;
        private final BufferedReader reader;
        private final PrintWriter writer;
        private final String guestName;
        private final ChatRoom currentRoom;
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
                        String finalised = String.format("[%s] : %s", guestName, input);

                        currentRoom.broadcastToRoom(finalised);
                        System.out.println(finalised);   // I print to server here to ease debuggin
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