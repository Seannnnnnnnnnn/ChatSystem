package com.ChatRoomApplication;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class Unmarshalling {
    public static void main(String[] args) {
        String example = "[{\"count\":0,\"roomid\":\"MainHall\"}, {\"count\":1,\"roomid\":\"jokes\"}]";
        try {
            JSONArray unmarshalled = unmarshallJSONArray(example);
            for (Object entry : unmarshalled) {
                JSONObject unmarshalledEntry = unmarshallJSON(entry.toString());
                System.out.println(unmarshalledEntry.get("roomid").toString());
            }
        } catch (ParseException e) {
            e.printStackTrace();
            System.out.println("can't unmarshall that");
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
}
