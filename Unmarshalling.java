package com.ChatRoomApplication;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.lang.reflect.Array;
import java.util.ArrayList;

public class Unmarshalling {
    public static void main(String[] args) {
        String marshalledToString = marshallJSON();
        System.out.println(marshalledToString);
        try {
            JSONObject unmarshalled = unmarshallJSON(marshalledToString);
            System.out.println(unmarshalled);
            System.out.println(unmarshalled.get("people"));
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    public static String marshallJSON() {
        String arrayExample = marshallExample();
        JSONObject marshelled = new JSONObject();
        marshelled.put("admin", "me");
        marshelled.put("people", arrayExample);
        return marshelled+"\n";
    }


    private static JSONObject unmarshallJSON(String serverMessage) throws ParseException{
        /* Unmarshalls messages received from server */
        JSONParser unmarshaller = new JSONParser();
        return (JSONObject) unmarshaller.parse(serverMessage);
    }


    private static String marshallExample() {
        ArrayList<String> exampleArray = new ArrayList<>();
        ArrayList<String> names = new ArrayList<>();
        names.add("John");
        names.add("Michael");
        names.add("William");

        for (String name : names) {
            JSONObject JSONRepresenation = new JSONObject();
            JSONRepresenation.put("name", name);
            JSONRepresenation.put("age", 15);
            exampleArray.add(JSONRepresenation.toString());
        }

        return exampleArray + "\n";
    }
}
