package com.ChatRoomApplication;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class ChatServer {
    static final int standardPort = 4444;

    public static void main(String[] args) {
        System.out.format("listening on port: %d\n", standardPort);
        connectionHandler();
    }


    static void connectionHandler() {
        // handles incoming client connections
    }


    static class chatRoom extends Thread {
        /*
        Implements the different rooms. Within chatRoom subclass, we contain list of all users within room.
        */
        private final String roomName;
        private final clientConnection admin;
        private final List<clientConnection> roomMembers = new ArrayList<>();


        public chatRoom(String roomName, clientConnection connection) {
            this.roomName = roomName;
            this.admin = connection;
            roomMembers.add(connection);
        }
    }

    static class clientConnection extends Thread {
        /*
        Implements the separate client connections
        */
        private Socket socket;
        private BufferedReader reader;
        private PrintWriter writer;
        private String guestName;
        private boolean connectionAlive = true;


        public clientConnection(Socket socket) throws IOException {
            this.socket = socket;
            this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.writer = new PrintWriter(socket.getOutputStream());
        }
    }
}
