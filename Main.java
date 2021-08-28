package com.ChatRoomApplication;

/* Simple play around with JSON marshalling and unmarshalling */


import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class Main {

    public static void main(String[] args) {
	    // Playing around with JSON :
        JSONObject obj = new JSONObject();
        obj.put("type", "roomchange");
        obj.put("identity", "__ID__");
        obj.put("former", "__CURRENT ROOM__");
        obj.put("roomid", "__NEWROOM__");

        String serialised = obj.toJSONString();

        JSONParser parser = new JSONParser();
        try {
            JSONObject deseralised = (JSONObject) parser.parse(serialised);
            System.out.println(deseralised);
        } catch (ParseException e) {
            e.printStackTrace();
        }


    }
}
