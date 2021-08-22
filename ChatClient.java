package com.ChatRoomApplication;

import java.io.IOException;
import java.net.Socket;

public class ChatClient {
    private static final int serverPort = 4444;
    private static String proxy = "localhost";
    private static String name;

    public static void main(String[] args) {
        establishConnection();
    }

    public static void establishConnection() {
        try {
            Socket socket = new Socket(proxy, serverPort);
        } catch (IOException e) {
            e.getStackTrace();
        }
    }
}
