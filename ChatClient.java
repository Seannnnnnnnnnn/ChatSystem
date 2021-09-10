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
    private static String currentRoom = "MainHall";
    private static String newRoomName = null;// when client makes #createroom request, they receive a roomList response
                                             // with the new room if successful. This variable is overridden at the
                                             // #createroom request, to verify if the room was successfully created.
                                             // This is all managed within the roomList response handler method.

    public static void main(String[] args) { establishConnection(); }


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
        if (keyboardInput.charAt(0) != '#') {
            jsonRepresentation.put("type", "message");
            jsonRepresentation.put("content", keyboardInput);
        }
        else {
            String[] split = keyboardInput.split("\\s+");
            String type = split[0];
            String remainder = keyboardInput.replaceFirst(type, "").trim();

            if (type.equals("#quit")) {
                jsonRepresentation.put("type","quit");
            }

            else if (type.equals("#newidentity")) {
                jsonRepresentation.put("type","newidentity");
                jsonRepresentation.put("identity", remainder);
            }

            else if (type.equals("#join")) {
                jsonRepresentation.put("type", "roomchange");
                jsonRepresentation.put("roomid", remainder);
            }

            else if (type.equals("#who")) {
                jsonRepresentation.put("type", "who");
                jsonRepresentation.put("roomid", remainder);
            }

            else if (type.equals("#createroom")) {
                newRoomName = remainder; // we set the new room name here. Then verified as successful in handling roomList
                jsonRepresentation.put("type", "createroom");
                jsonRepresentation.put("roomid", remainder);
            }

            else if (type.equals("#list")) {
                jsonRepresentation.put("type", "list");
            }

            else if (type.equals("#delete")) {
                jsonRepresentation.put("type", "delete");
                jsonRepresentation.put("roomid", remainder);
            }

            else {
                System.out.format("It appears you have tried to enter a server request.\nUnfortunately, %s is not a " +
                                  "recognised request format. This will be transmitted as a standard message instead.\n"
                                  ,type);
                jsonRepresentation.put("type", "message");
                jsonRepresentation.put("content", keyboardInput);
            }
        }
        return jsonRepresentation+"\n";
    }


    static void newIdentity(JSONObject unmarshalledResponse){
        if (id.equals(unmarshalledResponse.get("former")) && !id.equals(unmarshalledResponse.get("identity"))) {
            id = unmarshalledResponse.get("identity").toString();
            System.out.format("Changed identity to : %s\n", id);
        } else if (id.equals(unmarshalledResponse.get("former")) && id.equals(unmarshalledResponse.get("identity"))) {
            System.out.println("identity invalid or already in use\n");
        } else {
            System.out.format("%s changed their identity to %s\n", unmarshalledResponse.get("former"),
                              unmarshalledResponse.get("identity"));
        }
    }


    static void roomChange(JSONObject unmarshalledResponse) {
        if (!id.equals(unmarshalledResponse.get("identity"))) {
            System.out.format("%s changed room to %s\n", unmarshalledResponse.get("identity"),
                              unmarshalledResponse.get("roomid"));
        }
        else {
            System.out.format("moved to %s\n", unmarshalledResponse.get("roomid"));
        }
    }


    static void roomList(JSONObject unmarshalledResponse) {
        String currentRooms = unmarshalledResponse.get("rooms").toString();
        if (newRoomName != null && currentRooms.contains(newRoomName)) {  // if newRoomName is not null, then roomList message was received as part of the #creatroom protocol
            System.out.format("Room %s created\n", newRoomName);
            newRoomName = null;

        } else if (newRoomName != null && !currentRooms.contains(newRoomName)){
            System.out.format("Room %s is invalid, or already in use\n", newRoomName);
            newRoomName = null;

        } else {
            System.out.println("current rooms: ");
            System.out.println(unmarshalledResponse.get("rooms"));
            System.out.println(getRoomsFromResponse(unmarshalledResponse));
        }
    }


    static String getRoomsFromResponse(JSONObject unmarshalledArray) {
        /* Method to stringify the room contents from the server response */
        return "";
    }


    static void roomContents(JSONObject unmarshalledResponse) {
        String roomid = unmarshalledResponse.get("roomid").toString();
        String identities = unmarshalledResponse.get("identities").toString();
        System.out.format("[%s] : %s\n", roomid, identities);
    }


    private static JSONObject unmarshallJSON(String serverMessage) throws ParseException{
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
                newIdentity(unmarshalled);
            }

            else if (messageType.equals("roomchange")) {
                roomChange(unmarshalled);
            }

            else if (messageType.equals("roomcontents")) {
                roomContents(unmarshalled);
            }

            else if (messageType.equals("roomlist")) {
                roomList(unmarshalled);
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
        /* Thread for handling the writing and flushing of client messages */
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
                    String jsonMarshall = marshallJSON(clientInput);    // marshall raw input to JSON format
                    writer.println(jsonMarshall);
                    writer.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    private static class Listener extends Thread {
        /* Thread for handling the reading of messages from server */
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