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
        connectionHandler();
    }


    static synchronized void addRoom(ChatRoom roomThread) {
        roomList.add(roomThread);
    }


    static synchronized void addClientConnection(ClientConnection connection) {
        broadcast(String.format("[Server] : %s has entered the Main Hall", connection.socket.getPort()), roomList.get(0));
        connectionList.add(connection);
    }


    static void broadcast(String message, ChatRoom room) {
        // broadcasts message to room. If room is null, broadcasts to all presently connected clients
        if (room == null) {
            for (ClientConnection connection: connectionList) {
                connection.sendMessage(message);
            }
        } else {
            for (ClientConnection connection: room.roomMembers) {
                connection.sendMessage(message);
            }
        }
    }


    static void connectionHandler() {
        // handles incoming client connections and moves them to MainHall chatRoom.
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


        public ChatRoom(String roomName, ClientConnection connection) {
            this.roomName = roomName;
            this.admin = connection;
            roomMembers.add(connection);
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
        private boolean connectionAlive = true;


        public ClientConnection(Socket socket) throws IOException {
            this.socket = socket;
            this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.writer = new PrintWriter(socket.getOutputStream());
        }

        public void sendMessage(String message) {
            writer.print(message);
            writer.flush();
        }
    }
}
