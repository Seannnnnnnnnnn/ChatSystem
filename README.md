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
ideas of distributed programming.
