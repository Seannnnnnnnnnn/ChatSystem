# Chat System
Implementation of distributed Chat room system with server and client class implementations. 
System supports variety of C2S requests and responses, such as ability to make separate chat rooms, 
change between rooms, change user id and so forth. 


### Dependancies:
 - added simple-json 1.1.1 for marshalling/unmarshalling JSON requests for S2C and C2S interactions
 - added args4j for command line parsing. Default port location of 4444 can be overridden from command line with `-p PORTNUMBER`


### Why did I make this?
Being new to Java and multi-threaded programming, I figured this would be a suitable challenge to 
familiarise myself with these concepts, and gain a deeper understanding of implementing the central
ideas of a distributed system.


### What does it do? 
The `chatserver` acts (unsurprisingly) as the server, maintaining information and handling requests from client connections. The server also logs all activity to the console for our convenience. The `chatclient` works as the client; running from the command line, users connect via the client and can message back and forth by writing to the console. Users can make the following requests: 
  - `#createroom` followed by a valid room name to create a new chatroom
  - `#join` followed by a valid room name to join a new room
  - `#delete` followed by a valid room name that the client created, to delete the room and move all members to a Main Hall. 
  - `#who` followed by a valid room name to query the members of said room
  - `#newidentity` followed by a valid user identity to change how their name appears
