package com.ChatRoomApplication;

import org.json.simple.JSONObject;

public class Main {

    public static void main(String[] args) {
        String test1 = "#createroom nigger";
        String output1 = marshallJSON(test1);
        System.out.println(output1);

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
                jsonRepresentation.put("type", "createroom");
                jsonRepresentation.put("roomid", remainder);
            }

            else if (type.equals("#delete")) {
                jsonRepresentation.put("type", "delete");
                jsonRepresentation.put("roomid", remainder);
            }

            else if (type.equals("#list")) {
                jsonRepresentation.put("type", "list");
            }

            else {
                System.out.format("It appears you have tried to enter a server request.\nUnfortunately, %s is not a " +
                                  "recognised request format. This will be transmitted as a standard message instead.\n",
                                  type);
            }
        }
        return jsonRepresentation+"\n";
    }
}
