package com.ChatRoomApplication;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.net.Socket;

public class ChatClient {
    private static final int serverPort = 4444;
    private static final String location = "localhost";
    private static final boolean connectionAlive = true;
    private static String id = "";

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


    public static String marshallJSON(String keyboardInput) {
        /* Marshals user input to appropriate JSON message, then stringifies to be communicated with Server */
        JSONObject jsonRepresentation = new JSONObject();
        if (keyboardInput.charAt(0) != '#' ) {
            jsonRepresentation.put("type", "message");
            jsonRepresentation.put("content", keyboardInput);
        }
        else {
            String[] split = keyboardInput.split("\\s+");
            String type = split[0];
            String remainder = keyboardInput.replaceFirst(type+" ", "");
            System.out.format("This is your message type: %s, and the remainder of your command: %s\n", type, remainder);
            System.out.println("Sorry, haven't implemented beyond this :)\n");
        }
        return jsonRepresentation+"\n";
    }


    public static JSONObject unmarshallJSON(String serverMessage) throws ParseException{
        /* Unmarshalls messages received from server */
        JSONParser unmarshaller = new JSONParser();
        return (JSONObject) unmarshaller.parse(serverMessage);
    }


    static void handleJSON(String serverMessage) {
        /* Method for managing all S2C requests. First unmarshalls serverMessage then performs action accordingly */
        try {
            JSONObject unmarshalled = unmarshallJSON(serverMessage);
            String messageType = unmarshalled.get("type").toString();

            if (messageType.equals("message")) {
                System.out.println(unmarshalled.get("content"));
            }

            else if (messageType.equals("newidentity")) {
                assert id.equals(unmarshalled.get("former"));         // assert that this message is intended for us
                id = unmarshalled.get("identity").toString();
                System.out.format("Changed identity to : %s\n", id);
            }

            else if (messageType.equals("roomcontents")) {
                System.out.println("printing room contents: \n");
                String roomName = unmarshalled.get("roomid").toString();
                String roomMembers = unmarshalled.get("roomcontents").toString();
                System.out.format("Current members of %s are : %s\n", roomName, roomMembers);
            }

        } catch (ParseException e) {
            System.out.println("Could not unmarshall message from server\n");
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
                    String jsonMarshall = marshallJSON(clientInput);
                    System.out.format("As JSON %s", jsonMarshall);
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
                    handleJSON(input);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}