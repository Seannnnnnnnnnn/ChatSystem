package com.ChatRoomApplication;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class Main {

    public static void main(String[] args) {
	    String test = "{\"type\":\"message\",\"content\":\"[Server] : guest1 has joined MainHall\\n\"}";
        JSONParser unmarshaller = new JSONParser();
        try {
            JSONObject unmarshalled = (JSONObject) unmarshaller.parse(test);
            System.out.println(unmarshalled.get("conent"));
        } catch (ParseException e) {
            e.printStackTrace();
        }

    }
}
