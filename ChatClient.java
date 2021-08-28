package com.ChatRoomApplication;

import java.io.*;
import java.net.Socket;

public class ChatClient {
    private static final int serverPort = 4444;
    private static final String location = "localhost";
    private static final boolean connectionAlive = true;
    private static String id;

    public static void main(String[] args) {
        establishConnection();
    }

    public static void establishConnection() {
        try {
            Socket socket = new Socket(location, serverPort);

            Thread listener = new Listener(socket);
            Thread writer = new Writer(socket);
            listener.start();
            writer.start();

        } catch (IOException e) {
            e.getStackTrace();
        }
    }


    private static class Writer extends Thread {
        /*
        Thread for handling the writing and flushing of client messages
        */

        PrintWriter writer;
        BufferedReader keyboardReader = new BufferedReader(new InputStreamReader(System.in));

        public Writer(Socket socket) {
            try {
                this.writer = new PrintWriter(socket.getOutputStream());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            while (connectionAlive) {
                try {
                    String clientInput = keyboardReader.readLine();
                    writer.println(clientInput);
                    writer.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    private static class Listener extends Thread {
        /*
        Thread for handling the reading of messages from server
        */
        BufferedReader reader;

        public Listener(Socket socket) {
            try {
                this.reader =  new BufferedReader(new InputStreamReader(socket.getInputStream()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            while (connectionAlive) {
                try {
                    String input = reader.readLine();
                    System.out.println(input);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }



}
