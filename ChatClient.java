package com.ChatRoomApplication;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;


public class ChatClient {
    private static final int serverPort = 4444;
    private static final String location = "localhost";
    private static boolean connectionAlive = true;
    private static String id = "";
    private static String newRoomName = null;// when client makes #createroom request, they receive a roomList response
                                             // with the new room if successful. This variable is overridden at the
                                             // #createroom request, to verify if the room was successfully created.
                                             // This is all managed within the roomList response handler method.

    public static void main(String[] args) { establishConnection(); }


    /*******************************************************************************************
     *  Client connection management
     *******************************************************************************************/


    public static void establishConnection() {
        try {
            Socket socket = new Socket(location, serverPort);
            Thread listener = new Listener(socket);
            Thread writer = new Writer(socket);
            listener.start();
            writer.start();
            if (!socket.isConnected()) {
                listener.interrupt();
                writer.interrupt();
                System.out.println("Exited process on local host");
            }
        } catch (IOException e) {
            e.getStackTrace();
        }
    }


    /*******************************************************************************************
     *  JSON Handling - marshalling / unmarshalling, C2S request management
     *******************************************************************************************/


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
                jsonRepresentation.put("type","quit");   // close on Server
                connectionAlive = false;                 // close on Client
                System.out.println("Disconnected from localhost");
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

        } catch (ParseException e) {
            System.out.println("Could not unmarshall message from server\n");
        }
    }


    private static JSONObject unmarshallJSON(String serverMessage) throws ParseException{
        /* Unmarshalls messages received from server */
        JSONParser unmarshaller = new JSONParser();
        return (JSONObject) unmarshaller.parse(serverMessage);
    }


    private static JSONArray unmarshallJSONArray(String serverMessage) throws ParseException{
        /* Unmarshalls messages received from server */
        JSONParser unmarshaller = new JSONParser();
        return (JSONArray) unmarshaller.parse(serverMessage);
    }


    /*******************************************************************************************
     *  S2C Response Management
     *******************************************************************************************/


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
        List<String> currentRoomsArray = getRoomsFromResponse(unmarshalledResponse);
        if (newRoomName != null && currentRoomsArray.contains(newRoomName)) {  // if newRoomName is not null, then roomList message was received as part of the #creatroom protocol
            System.out.format("Room %s created\n", newRoomName);
            newRoomName = null;

        } else if (newRoomName != null && !currentRoomsArray.contains(newRoomName)){
            System.out.format("Room %s is invalid, or already in use\n", newRoomName);
            newRoomName = null;

        } else {
            System.out.println("current rooms: ");
            printRoomsFromResponse(unmarshalledResponse);
        }
    }


    static void printRoomsFromResponse(JSONObject unmarshalledResponse) {
        /* Method to pretty print the room contents from the server response */
        String roomsResponse = unmarshalledResponse.get("rooms").toString();
        try {
            JSONArray roomsList = unmarshallJSONArray(roomsResponse);
            for (Object entry : roomsList) {
                JSONObject unmarshalledEntry = unmarshallJSON(entry.toString());
                System.out.format("%s : %s guests\n", unmarshalledEntry.get("roomid"), unmarshalledEntry.get("count"));
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }


    static List<String> getRoomsFromResponse(JSONObject unmarshalledResponse) {
        /* Method to extract room ids from response to a List */
        String roomsResponse = unmarshalledResponse.get("rooms").toString();
        List<String> roomArray = new ArrayList<>();
        try {
            JSONArray roomsList = unmarshallJSONArray(roomsResponse);
            for (Object entry : roomsList) {
                JSONObject unmarshalledEntry = unmarshallJSON(entry.toString());
                roomArray.add(unmarshalledEntry.get("roomid").toString());
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return roomArray;
    }


    static String getNamesFromResponse(JSONObject unmarshalledResponse) {
        /* Method to extract room ids from response to a List */
        String identitiesResponse = unmarshalledResponse.get("identities").toString();
        String parsed = identitiesResponse.substring(1, identitiesResponse.length()-2); // JSON response wraps names in [ ]
        String[] parsedArray = parsed.split(",\\s");
        StringBuilder roomMembers = new StringBuilder();
        for (String s : parsedArray) {
            roomMembers.append(s).append(" ");
        }
        return roomMembers.toString();
    }


    static void roomContents(JSONObject unmarshalledResponse) {
        String roomid = unmarshalledResponse.get("roomid").toString();
        String roomContents = getNamesFromResponse(unmarshalledResponse);
        System.out.format("[%s] contains : %s\n", roomid, roomContents);
    }


    /*******************************************************************************************
     *  Writer Thread for listening to client keyboard input
     *******************************************************************************************/


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


    /*******************************************************************************************
     *  Listener thread - listening for server response / messages
     *******************************************************************************************/


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
