package com.ChatRoomApplication;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

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


    /********************************************************************************************
    * helper methods - marshalling / unmarshalling, validation checking, List operations ect
    ********************************************************************************************/


    static String marshallToJSON(String messageToClient) {
        /* Method for marshalling messages to client */
        JSONObject messageJSON = new JSONObject();
        messageJSON.put("type", "message");
        messageJSON.put("content", messageToClient);
        return messageJSON+"\n";
    }


    public static JSONObject unmarshallJSON(String serverMessage) throws ParseException {
        /* Unmarshalls messages received from server */
        JSONParser unmarshaller = new JSONParser();
        return (JSONObject) unmarshaller.parse(serverMessage);
    }


    static void updateAdminToNull(ClientConnection connection) {
        /* When a client connection is closed, we must update all rooms that they are admin for to have a null
        admin. */
        List<ChatRoom> connectionRooms = connection.ownedRooms;
        for (ChatRoom chatRoom : connectionRooms) {
            chatRoom.setAdminToNull();
        }
    }


    static boolean guestNameInUse(String name) {
        /* Returns true if guestName is currently used by someone else */
        for (ClientConnection clientConnection : connectionList) {
            String clientConnectionName = clientConnection.guestName;
            if (clientConnectionName.equals(name)) { return true; }
        }
        return false;
    }


    static boolean validIdentity(String potentialID) {
        /* function for determining if potentialID is valid and not in use */
        if (guestNameInUse(potentialID)) { return false; }
        if (!isAlphaNumeric(potentialID)) { return false; }
        if (Character.isDigit(potentialID.charAt(0))) { return false; }
        if (potentialID.length() < 3 | potentialID.length() > 16) { return false; }
        for (ClientConnection connection: connectionList) {
            if (connection.guestName.equals(potentialID)) { return false; }
        }
        return true;
    }


    static boolean validNewRoomID(String potentialRoomID) {
        /* function for determining if potential room ID is not in use, starts with non-digit and
        between 3 and 32 characters. potential room ID must also no currently be in use and only contain
        alphanumeric characters */
        if (!isAlphaNumeric(potentialRoomID)) { return false; }
        if (Character.isDigit(potentialRoomID.charAt(0))) { return false; }
        if (potentialRoomID.length() < 3 | potentialRoomID.length() > 32) { return false; }
        for (ChatRoom chatRoom : roomList) {
            if (chatRoom.roomName.equals(potentialRoomID)) { return false; }
        }
        return true;
    }


    private static boolean roomExists(String potentialRoomID) {
        /* method for determining if potentialRoomID belongs to a current room */
        for (ChatRoom room : roomList) {
            if (room.roomName.equals(potentialRoomID)) { return true; }
        }
        return false;
    }


    private static ChatRoom getRoom(String roomID) {
        /* method for getting ChatRoom with roomID. This method is not called unless room with roomID exists, so
        never returns null */
        for (ChatRoom room : roomList) {
            if (room.roomName.equals(roomID)) { return room; }
        }
        return null;
    }


    private static boolean isAdmin(ClientConnection connection, String roomID) {
        /* returns true iff roomID exists and connection is admin */
        if (roomExists(roomID)) {
            ChatRoom targetRoom = getRoom(roomID);
            if (targetRoom.admin.equals(connection)) {
                return true;
            } else { return false; }
        } else { return false; }
    }


    static String getRooms() {
        /* returns an ArrayList containing room ids and room counts */
        ArrayList<String> rooms = new ArrayList<>();
        for (ChatRoom chatRoom : roomList) {
            JSONObject jsonRepresentation = new JSONObject();
            jsonRepresentation.put("roomid", chatRoom.roomName);
            jsonRepresentation.put("count", chatRoom.getRoomSize());
            rooms.add(jsonRepresentation.toString());
        }
        return rooms + "\n";
    }


    static String getRoomMembers(String roomID) {
        ChatRoom requestedRoom = getRoom(roomID);
        ArrayList<String> roomMembers = new ArrayList<>();
        assert  requestedRoom != null;
        for (ClientConnection connection : requestedRoom.roomMembers) {
            if (connection.equals(requestedRoom.admin)) {
                roomMembers.add(connection.guestName+"*");
            } else {
                roomMembers.add(connection.guestName);
            }
        }
        return roomMembers + "\n";
    }

    static void moveToMainHall(ClientConnection connection) {
        /* Special method to avoid concurrent modification exception when performing a room delete */
        ChatRoom MainHall = getRoom("MainHall");
        assert MainHall != null;    // if this is raised then something has gone very very wrong
        connection.currentRoom = MainHall;
        MainHall.roomMembers.add(connection);
    }


    private static boolean isAlphaNumeric(String s) {
        /* shamelessly taken from: www.techiedelight.com/check-string-contains-alphanumeric-characters-java/ */
        return s != null && s.matches("^[a-zA-Z0-9]*$");
    }


    private static synchronized String generateName() {
        /* Algorithm for generating the basic guest name */
        return String.format("guest %s", connectionList.size()+1);
    }


    static synchronized void addRoom(ChatRoom roomThread) {
        roomList.add(roomThread);
    }


    static synchronized void checkAndDeleteRooms() {
        /* If a room (that is not MainHall) has 0 occupants and no admin, then it is deleted */
        ArrayList<ChatRoom> roomsToDelete = new ArrayList<>();
        for (ChatRoom chatRoom : roomList) {
            if (!chatRoom.roomName.equals("MainHall") && chatRoom.getRoomSize()==0 && chatRoom.admin == null) {
                roomsToDelete.add(chatRoom);
            }
        }
        for (ChatRoom chatRoom : roomsToDelete) {
            roomList.remove(chatRoom);
        }
    }


    /*********************************************************************************************
     *  S2C Responses
     ********************************************************************************************/


    static void newIdentity(ClientConnection connection, String former, String identity) {
        /* the server generates a unique id for the client which is guest followed by the smallest integer
        greater than 0 that is currently not in use by any other connected client, the server tells the client its
        id using an newIdentity message */
        JSONObject newIDJSON = new JSONObject();
        newIDJSON.put("type", "newidentity");
        newIDJSON.put("former", former);
        newIDJSON.put("identity", identity);
        connection.sendMessage(newIDJSON+"\n");
    }


    static synchronized void newIdentityRequest(ClientConnection requester, JSONObject unmarshalledJSONRequest) {
        /* function for managing client connection's new identity request; follows the identity change protocol
        as described within the assignment specification (Slide 10). Method is synchronized so that clients could
        not potentially change to same id simultaneously */
        String newIdentity = unmarshalledJSONRequest.get("identity").toString();
        if (validIdentity(newIdentity)) {
            for (ClientConnection clientConnection : connectionList) {
                newIdentity(clientConnection, requester.guestName, newIdentity);
            }
            System.out.format("[Server] : %s changed their identity to %s", requester.guestName, newIdentity);
            requester.guestName = newIdentity;
        } else {
            // in the event that identity does not change, server responds with NewIdentity message to client
            newIdentity(requester, requester.guestName, requester.guestName);
        }
    }


    static void roomChange(ClientConnection clientConnection, String former, String roomID, String clientIdentity) {
        /* method for creating RoomChange message and sending to client */
        JSONObject roomChangeJSON = new JSONObject();
        roomChangeJSON.put("type", "roomchange");
        roomChangeJSON.put("identity", clientIdentity);
        roomChangeJSON.put("former", former);
        roomChangeJSON.put("roomid", roomID);
        clientConnection.sendMessage(roomChangeJSON+"\n");
    }


    static void joinRoomRequest(ClientConnection requester, JSONObject unmarshalledJSONRequest) {
        /* function for managing client connections join room request; follows the protocol described on slide 13 */
        String roomid = unmarshalledJSONRequest.get("roomid").toString();
        if (roomExists(roomid)) {
            ChatRoom requestedRoom = getRoom(roomid);
            assert requestedRoom != null;
            ChatRoom previousRoom = requester.currentRoom;
            requestedRoom.joinRoom(requester);
            System.out.format("[Server] : %s moved to %s\n", requester.guestName, requestedRoom.roomName);
            roomChange(requester, previousRoom.roomName, requestedRoom.roomName, requester.guestName);

            /* If the room did change, then server will send a RoomChange message to all
            clients currently in the requesting client’s current room and the requesting
            client’s requested room */
            for (ClientConnection connection : previousRoom.roomMembers) {
                if (!connection.equals(requester)) {
                    roomChange(connection, previousRoom.roomName, requestedRoom.roomName, requester.guestName);
                }
            }
            for (ClientConnection connection : requestedRoom.roomMembers) {
                if (!connection.equals(requester)) {
                    roomChange(connection, previousRoom.roomName, requestedRoom.roomName, requester.guestName);
                }
            }
            checkAndDeleteRooms();  // If a room (other than MainHall) is empty, then it is deleted.
        } else {
            String response = "Requested room is invalid or non existent";
            String marshalledResponse = marshallToJSON(response);
            requester.sendMessage(marshalledResponse);
        }
    }


    static synchronized void createRoomRequest(ClientConnection requester, JSONObject unmarshalledJSONRequest) {
        /* method for handling creation of new room requests if room is created, Server replies to requester
        with RoomList response type, showing the newly created room.  */
        String roomid = unmarshalledJSONRequest.get("roomid").toString();
        if (validNewRoomID(roomid)) {
            ChatRoom newRoom = new ChatRoom(roomid, requester);
            roomList.add(newRoom);
            requester.ownedRooms.add(newRoom);
            System.out.format("[Server]: %s created room: %s\n", requester.guestName, roomid);
        }
        roomListRequest(requester);
    }


    static void roomContents(ClientConnection connection, String roomID) {
        /* returns room id, room owner and room members to connection */
        JSONObject roomContents = new JSONObject();
        if (roomExists(roomID)) {
            ChatRoom room = getRoom(roomID);
            assert room != null;
            String roomMembers = getRoomMembers(roomID);  // problem here
            roomContents.put("type", "roomcontents");
            roomContents.put("roomid", roomID);
            roomContents.put("identities", roomMembers); // can't raise NullPointerException, as we always return at least '[]'
            if (room.admin != null) {
                roomContents.put("owner", room.admin.guestName);
            }
            else { roomContents.put("owner", ""); }
            connection.sendMessage(roomContents + "\n");
        }
        else {
            String response = String.format("%s is not a valid room.\n", roomID);
            String marshalledResponse = marshallToJSON(response);
            connection.sendMessage(marshalledResponse);
        }
    }


    static void roomListRequest(ClientConnection requester) {
        /* The RoomList message lists all room ids and the count of identities in each room */
        JSONObject roomList = new JSONObject();
        String roomData = getRooms();
        roomList.put("type", "roomlist");
        roomList.put("rooms", roomData);
        requester.sendMessage(roomList+"\n");
    }


    static void deleteRoom(ClientConnection requester, JSONObject unmarshalled) {
        /* if requester is room admin, moves all current room connections to MainHall, then
        deletes room from room list. Responds with roomList S2C response to requester */
        String roomID = unmarshalled.get("roomid").toString();
        if (isAdmin(requester, roomID)) {
            ChatRoom targetRoom = getRoom(unmarshalled.get("roomid").toString());
            assert targetRoom != null;

            /* if we iterate over the list of live connections in the target room and attempt to reassign
            them to MainHall, we receive a concurrent modification exception, as cannot remove things from
            a list whilst iterating over it. We create a special method for moving connections to "MainHall"
            in the event of a roomDelete. */
            for (ClientConnection connection : targetRoom.roomMembers) {
                String notification = String.format("[Server] : %s has been deleted. Moving to MainHall", roomID);
                String marshalledResponse = marshallToJSON(notification);
                requester.sendMessage(marshalledResponse);
                moveToMainHall(connection);
                System.out.format("[Sever] : moving %s from %s...\n", connection.guestName, roomID);
            }
            System.out.println("[Server] : room has been emptied - now deleting");
            roomList.remove(targetRoom);
            roomListRequest(requester);
            requester.ownedRooms.remove(targetRoom);
        } else {
            String response = String.format("%s is not a valid room or you do not have admin privileges.\n", roomID);
            String marshalledResponse = marshallToJSON(response);
            requester.sendMessage(marshalledResponse);
        }
    }


    /********************************************************************************************
     *  Server management and console logging
     *******************************************************************************************/


    static synchronized void closeClientConnection(ClientConnection connection) {
        /* Manages the closing of a client connection. Broadcasts to all users that client has left. Reduces room
        count of users current room  */
        broadcastToAll(String.format("[Server] : %s has left the chat", connection.guestName));
        updateAdminToNull(connection);
        connection.currentRoom.roomMembers.remove(connection);
        connectionList.remove(connection);
        connection.connectionAlive = false;    // exit the main listening loop
        connection.interrupt();
    }


    static synchronized void addClientConnection(ClientConnection connection) {
        connectionList.add(connection);
        broadcastToAll(String.format("[Server] : %s has entered the Main Hall", connection.guestName));
    }


    static void broadcastToAll(String message) {
        /* broadcasts message to all live connections */
        System.out.println(message);
        for (ClientConnection connection: connectionList) {
            String marshalled = marshallToJSON(message);
            connection.sendMessage(marshalled);
        }
    }


    static void handleNewConnections() {
        /* handles all new connections to the server as per the described Protocol. */
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
                newIdentity(clientConnection, "", generateName());            // assign user their name
                Thread clientConnectionThread = new Thread(clientConnection);
                clientConnectionThread.start();
                addClientConnection(clientConnection);
                mainHall.joinRoom(clientConnection);
                roomListRequest(clientConnection);                                  // user is greeted with list of current rooms
            }
        } catch (IOException e) {
            e.getStackTrace();
        }
    }


    public static void handleClientRequest(String clientMessage, ClientConnection clientConnection) {
        /* Method for managing all C2S Requests. First unmarshalls serverMessage then passes off to appropriate method */
        try {
            JSONObject unmarshalled = unmarshallJSON(clientMessage);
            String messageType = unmarshalled.get("type").toString();

            if (messageType.equals("message")) {
                String content = unmarshalled.get("content").toString();
                String finalised = String.format("[%s] : %s", clientConnection.guestName, content);
                clientConnection.currentRoom.broadcastToRoom(finalised);
                System.out.println(finalised);                       // I print to server here to ease debugging //
            }

            else if (messageType.equals("newidentity")) {
                newIdentityRequest(clientConnection, unmarshalled);
            }

            else if (messageType.equals("roomchange")) {
                joinRoomRequest(clientConnection, unmarshalled);
            }

            else if (messageType.equals("who")) {
                roomContents(clientConnection, unmarshalled.get("roomid").toString());
            }

            else if (messageType.equals("delete")) {
                deleteRoom(clientConnection, unmarshalled);
            }

            else if (messageType.equals("createroom")) {
                createRoomRequest(clientConnection, unmarshalled);
            }

            else if (messageType.equals("list")) {
                roomListRequest(clientConnection);
            }

            else if (messageType.equals("quit")) {
                closeClientConnection(clientConnection);
            }

        } catch (ParseException e) {
            if (!clientMessage.equals("")) {
                System.out.format("Could not unmarshall message from client: message %s\n", clientMessage);
            }
        }
    }


    /*******************************************************************************************
     *  ChatRoom subclass & methods
     ****************************************************************************************** */


    static class ChatRoom {
        /* Implements the different rooms. Within chatRoom subclass, we contain list of all users within room. */
        private final String roomName;
        private ClientConnection admin;
        private final List<ClientConnection> roomMembers = new ArrayList<>();


        public ChatRoom(String roomName, ClientConnection admin) {
            this.roomName = roomName;
            this.admin = admin;
        }


        public int getRoomSize() { return roomMembers.size(); }


        public void leaveRoom(ClientConnection connection) { roomMembers.remove(connection); }


        public void joinRoom(ClientConnection connection) {
            ChatRoom formerRoom = connection.currentRoom;
            formerRoom.leaveRoom(connection);
            connection.currentRoom = this;
            roomMembers.add(connection);
        }


        public void setAdminToNull(){
            admin = null;
        }


        private void broadcastToRoom(String message) {
            /* Broadcasts message to ClientConnection's in roomMembers */
            for (ClientConnection connection: roomMembers) {
                if (connection != null) {
                    String finalisedString = String.format("[%s] : %s\n", roomName, message);
                    String marshalled = marshallToJSON(finalisedString);
                    connection.sendMessage(marshalled);
                }
            }
        }
    }


    /*******************************************************************************************
     *  ClientConnection subclass and individual connection management
     ****************************************************************************************** */


    static class ClientConnection extends Thread {
        /* Implements the separate client connections */
        private final Socket socket;
        private final BufferedReader reader;
        private final PrintWriter writer;
        private String guestName;
        private ChatRoom currentRoom;
        private boolean connectionAlive = true;
        private ArrayList<ChatRoom> ownedRooms = new ArrayList<>();


        public ClientConnection(Socket socket) throws IOException {
            this.socket = socket;
            this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.writer = new PrintWriter(socket.getOutputStream());
            this.guestName = generateName();
            this.currentRoom = roomList.get(0);
        }


        public void sendMessage(String marshalledMessage) {
            writer.print(marshalledMessage);
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
                    if (!input.equals("{}")) {
                        handleClientRequest(input, this);  // pass to main function for request management
                    } else {
                        connectionAlive = false;
                    }
                } catch (IOException | NullPointerException e) {
                    System.out.format("[Server] : TCP connection to %s interrupted, terminating connection\n",
                                      guestName);
                    break;
                }
            }
            close();
        }
    }
}
