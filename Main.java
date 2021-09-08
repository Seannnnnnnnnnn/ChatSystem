package com.ChatRoomApplication;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.util.ArrayList;

public class Main {

    public static void main(String[] args) {
        String test1 = "#changeid hello cunt";
        String output1 = marshallJSON(test1);

        String test2 = "hello world";
        String output2 = marshallJSON(test2);

        System.out.println(output1);
        System.out.println(output2);
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
}
