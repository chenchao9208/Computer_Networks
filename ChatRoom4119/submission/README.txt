####################################################################
                     COMS 4119 Computer Networks
                      Programming Assignment 1
Name: Chao Chen
UNI: cc3736
####################################################################
This file contains the following parts:
1. Description
   - Client
       - files
       - thread
       - data structure
   - Server
       - files
       - thread
       - data structure
2. Explanation of source code
3. How to run
    - Server
    - Client
4. Sample Commands
    - CLI
    - GUI
5. Additional Features
    - P2P Privacy and Consent
    - Guaranteed Message Delivery
    - GUI


########################### Description #############################

---------------------------    Client    ----------------------------
The client app consists of the following part:

* 3 major .java files:

Client.java          -- The main entity of the program.

HeartBeater.java     -- A thread to send heart beat signal periodically

MessageReceiver.java -- A thread to listen to a local port, receive
                        and process messages from the server or P2P
                        peers.

* 2 GUI related .java files:

LogInGUI.java        -- The log in interface.

ChatRoomGUI.java     -- The chat room interface and some processing
                        methods.

* 2 configuration .java files with some static variables:

ClientConfig.java    -- Default client configuration. You don't need 
                        to modify this.

Protocol.java        -- Define protocol headers. DO NOT modify this.

* 1 configuration .txt file

configClient.txt     -- Please modify this file to change the 
                        configuration. Such as to modify the heartbeat
                        rate, to turn on/off heart beater, or to turn
                        on/off to run with GUI.


Thread:

When the client program is correctly running in CLI mode, three threads
will run: The main thread from Client.java to continuously read the 
user input and do corresponding process; The thread from 
MessageReceiver.java to listen to a specific local port, receive and 
parse messages from the server and P2P peers; A thread to send heart
beat signal to the server.

Here, since the message received by this client is not as large as the 
server, thus the "MessageReceiver" only use one thread to accept and 
read from the socket one by one.


Essential Data Structure:

In Client.java
1. uuid             -- Since it's a non-permanent connection(a socket
                       will be set up only when there's some message 
                       to send, and will be close immediately after 
                       the message is sent), I use the message header 
                       to identify the message sender. It's java.util.
                       UUID. A kind of globally unique tag. It looks 
                       like:
                             4a01d3d7-d055-453d-9803-af74f1056a9b
                       Message exchange between the server and one 
                       client will use one unique uuid. The same when
                       doing P2P private message exchange.

2. userAddressMap   -- Map from username to it's ip:port address. Used
                       to send private message.

3. userUUIDMap      -- Map from username to it's uuid. Used to send 
                       private message.

4. extraTaskMap     -- Map from user command to corresponding messages.
                       Example: normally "Y" will not be considered as 
                       a command. But when some one request for my IP,
                       my input of "Y"/"N" should be responding to this
                       task to agree or deny. Then this extra task will
                       bee add here. When reading user's input, this map
                       will be checked. Details shown in Client.java

GUI related:
The GUI JFrame is designed with NetBeans GUI tools, then I implemented 
some actionListeners and some processing methods. Details will be shown
in the following content.

---------------------------    Server    ----------------------------
The server consists of the following part:

* 3 thread related .java files:

Server.java          -- The main entity of the server, which will 
                        continuously listen and accept connections. And
                        every time a connection is accepted, it will 
                        create a new thread with SessionThread to deal 
                        with the connection.

SessionThread.java   -- A thread to deal with a specific socket 
                        connection case. Usually the thread will read
                        one message from the socket, and then close it,
                        and doing some job based on the information.
                        (Different when handling log in request, the 
                        reply message will be sent back in the same 
                        socket.)

OfflineUserKickerThread.java
                     -- A thread to periodically wake up and check the 
                        online users' status. If a user is muted for a
                        long time, it will be regarded as logged off.
                        Relevant procedure will be taken.

* 2 information data based .java files:

InfoCenter.java      -- A centric data structure that contains all the
                        informations. Information sharing among threads
                        are handled through this data structure.

ClientInfoCell.java  -- A data structure binding ONE online user. Every
                        online user will be assigned with one cell, 
                        recording its username, ip address, receiving
                        port, synchronized method to send him a message
                        and so on. All the cells are included in the
                        InfoCenter.

* 2 configuration .java files with some static variables:

ServerConfig.java    -- Default server configuration. You don't need 
                        to modify this.

Protocol.java        -- Define protocol headers. DO NOT modify this.

* 1 configuration .txt file

configServer.txt     -- Please modify this file to change the 
                        configuration. Such as to change the block 
                        time, to change the "muted" timeout, and 
                        to change the maximum failed login attempts.

Thread:

When the server program is correctly running in CLI mode, three kinds
 of threads will run: The main thread in Server.java to continuously
 accept socket connections and create a session thread to handle it. 
Some SessionThread, each works on one ongoing socket connection, to 
read message from the client and deal with it. OfflineUseKickerThread
to periodically check and kick out muted users.


Essential Data Structure:

In ClientInfoCell:
1. uuid               -- Introduced in Client above.

2. lastLiveSignal     -- Record the timestamp when the last message
                         was received from this user. Updated every
                         time new message arrives. Used by the
                         OfflineUserKickerThread.

In InfoCenter:
1. handlers           -- HashMap<String, ClientInfoCell> to retrieve
                         the information cell by a username.

2. blockList          -- HashMap<String, Long> to record the users that
                         are blocked for logging in, and the blocking
                         timestamp.

3. userUUIDs          -- HashMap<UUID, String> used when parsing 
                         message headers to map onto a username.

4. blacklist          -- HashMap<String, HashSet<String>> to store
                         <blocker: set of blocked users> pairs.

5. offlineMessages    -- HashMap<String, ArrayList<String>> to store
                         users' offline messages.

6. count_failed       -- HashMap<String, Integer> to store how many
                         times a user attempted but failed to log in.
                         A user will be removed from this when he 
                         successfully log in even if he has tried 1
                         or 2 times before.

7. ipRequests         -- HashSet<String> to store requested but not 
                         responded ip requests. Prevent faked replies
                         to form a kind of harass.

##################### Explanation of source code #####################
The explaination is included in Description part and commented in the 
source code.

############################# How to run #############################

---------------------------    Server    ----------------------------

Put all these 9 files together under the same directory:
1. Server.java
2. SessionThread.java
3. ServerConfig.java
4. ClientInfoCell.java
5. InfoCenter.java
6. OfflineUserKickerThread.java
7. Protocol.java
8. configServer.txt
9. credentials.txt

cd to this directory.

compile: javac Server.java

modify the configuration in configServer.txt if you want.

run: java Server 4009

---------------------------    Client    ----------------------------
Put all these 8 files together under the same directory:
1. Client.java
2. ClientConfig.java
3. HeartBeater.java
4. MessageReceiver.java
5. Protocol.java
6. LogInGUI.java
7. ChatRoomGUI.java
8. configClient.txt

cd to this directory.

compile: javac Client.java

It doesn’t matter if the following message shows up:
    Note: ChatRoomGUI.java uses unchecked or unsafe operations.
    Note: Recompile with -Xlint:unchecked for details.
This might also show up when you run makefile.
If you don’t want to see this, use “javac -Xlint Client.java”

modify the configuration in configClient.txt if you want.
especially try to set true on "launchGUI" to launch in GUI mode.

run: java Client <server ip> 4009

########################### Sample Commands ###########################
For CLI mode:
you can type in "help" to see the all the commands:

Command                      Function
------------------------------------------------------------------
broadcast <message>          broadcast message to all online users.
message <user> <message>     send message to a specific user.
private <user> <message>     send message to user in P2P mode.
getaddress <user>            get the address of a user. needed prior
                             to send private message.
block <user>                 put a user into black list.
unblock <user>               remove a user from the black list.
online                       check out all online users.
logout                       logout from the chat room.
------------------------------------------------------------------

For GUI mode:
if you ssh on clic machine to test my code, please ensure you ssh 
with: ssh -Y uni@clic.cs.columbia.edu to enable x11 forwarding.

GUI is easy to use. The "online" function is not provided on the panel.
Because it will be processed at the time when logging in. And the online
user list will automatically be refreshed every time login/logoff 
notification is received.

########################## Addition Features ##########################
All these features are introduced in PA1.pdf
The features and some related code lines are shown below.

1. P2P privacy and consent
When A requests for B's IP address, the message centre should notify
B that A wants to talk it.
                                ---- SessionThread.java line 149-165
If B agrees to the conversation, the server should provide A with B's
IP address.
                                ---- SessionThread.java line 166-180
Else, A cannot initiate a conversation with B.
                                ---- SessionThread.java line 181-190

When A requests for B’s IP address, the message centre should check
B’s blacklist preferences. If B’s blacklist includes A, the message 
centre should not provide B’s IP address to A.
                                ---- SessionThread.java line 153

2. Guaranteed message delivery
...Such failure should be handled and the sender can recontact the 
server to leave an offline message.
                                ---- Client.java line 479-482
                                ---- ClientInfoCell.java line 71-75
Also, if the receiving client logs in with a new IP, the sending client
 should also be aware of this and not sending message to the old IP any
 more.
                                ---- SessionThread.java line 309
                                ---- MessageReceiver.java line 210

3. GUI
A simple Client GUI is implemented.

In configClient.txt, set launchGUI = true. 

run normally as java Client <Server IP> <Server Port>
to launch the GUI. 

The GUI is easy to use so no further introduction here. Note that 
there is no "online" option in GUI, because it will be sent when the 
GUI is first started, and every time when a user is logged in or off, 
the online list on the right will be updated.




