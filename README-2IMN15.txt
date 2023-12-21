README for getting started with Leshan within the course 2IMN15 (IoT)

Leshan is the Java based LWM2M implementation of Eclipse.
For the 2IMN15 course, the Leshan client and server demo
applications are extended with scenario specific logic and
custom object definitions.  Modifications to the original
Leshan code are marked with "2IMN15" in a comment.


=== Implementation ===

For the second assignment of 2IMN15, relevant code segments where
modifications are expected were marked with a comment

    // 2IMN15:  TODO  :  fill in

Modifications were required in the client and server applications.

For the last assignment fo 2IMN15, those specific code segments are
not indicated in detail, as you should now be more familiar
with the Java code structure.

Since the last assignment is larger an consists of multiple client
and server application, separate folders are provided.

energy-control
	A Leshan server. Check src/main/java/org/course/IoT.java
flow-control
	A Leshan server. Check src/main/java/org/course/IoT.java
room-control
	A Leshan server. Extend your solution to the second assignment
iot-client
	A Leshan client, which can be used to create different objects
	based on command line options
	For details, see src/main/java/org/eclipse/leshan/client/demo/
energy-market
	A Leshan client, which can connect to multiple server.
	The standard server is assumed to be the room-control server.
	Use -flow-url and -energy-url to specify the flow-control
	and energy-control server.
	Once the client has started, type 'update' in its terminal
	to connect to the additional servers.

In leshan-client-demo/src/main/java/org/eclipse/leshan/client/demo/
   Luminaire.java
	display the status of the luminaire.
   PresenceDetection.java
	implement a method to simulate presence detection.

In leshan-server-demo/src/main/java/org/course/
   RoomControl.java
	implement the application scenarios.


=== Compilation ===

To compile the Java code, use the command

	mvn install -P CompileOnly

Maven (mvn) is a Java build environment. If your system doesn't have
it, you can follow the general instructions on compiling Leshan.
After the compilation is finished, the server and client are
available in leshan-server-demo/target en leshan-client-demo/target.


=== Testing ===

To test the application, start the servers and one or more clients.
In seperate terminals, use the commands

   java -jar room-control/target/room-control-2.0.0-SNAPSHOT-jar-with-dependencies.jar

   java -jar energy-control/target/energy-control-2.0.0-SNAPSHOT-jar-with-dependencies.jar -lp=5685 -slp=5686 -wp=8082

   java -jar flow-control/target/flow-control-2.0.0-SNAPSHOT-jar-with-dependencies.jar -lp=5687 -slp=5688 -wp=8084

   java -jar iot-client/target/iot-client-2.0.0-SNAPSHOT-jar-with-dependencies.jar -n ep-name [-presence] [-luminaire] [-demand] [-battery] [-solar]

   java -jar energy-market/target/energy-market-2.0.0-SNAPSHOT-jar-with-dependencies.jar -energy-url=coap://localhost:5685 -flow-url=coap://localhost:5687 

For the client, options are available to create certain LWM2M objects.
Use the option -h or --help to see which other options are available.

For the energy-market client, you can specify additional URLs for the
servers of Flow-Control and Energy-Control.

For more convenient testing, you can put the relevant commands in
batch scripts for your preferred platform.

NOTE: the Java applications listen to network ports. Depending on your
      platform, the Java application might be blocked to open the network
      port (and print an error message) or the firewall might block
      the communication. 


=== Modifications to Leshan ===

For the 2IMN15 course, the following modifications were applied to
the standard Leshan code (compared to its git repository). If your
implementation requires additional modifications, you can check
those files first.

 * LeshanClientDemo.java  creates additional objects based on
   command line options.
 * LeshanClientDemoCLI.java  specifies additional command line
   options for different LwM2M objects.
 * LwM2mDemoConstant.java  specifies application specific
   LWM2M objects.
 * ClientServlet.java  initializes org.course.IoT .
 * EventServlet.java  passes on events to org.course.IoT.
 * CaliforniumEndpointsManager.java to support multiple servers.
 * DefaultRegistrationEngine.java to support multiple servers.


In addition, the LWM2M object models (in XML format) for
Luminaire, PresenceDetector and other objects are provided
in leshan-core-demo/src/main/resources/models/3300?.xml.

Summaries of the modifications are provide in the files 
git-diff.txt and git-status.txt.
