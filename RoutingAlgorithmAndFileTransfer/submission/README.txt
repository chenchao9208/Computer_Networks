##################################################################################
                         COMS 4119 Computer Networks
                          Programming Assignment 2
Name: Chao Chen
UNI: cc3736
##################################################################################
My submission contains:
BFClient.java
BFKernel.java
CKLProcessorThread.java
DefaultUpdateSenderThread.java
MessageReceiverThread.java
PA2FTP.java
PA2TCP.java
PA2Util.java
TimeoutCheckerThread.java
makefile
README.txt

##################################################################################
This program contains 9 .java files as the following parts:
1. Description
    - launcher x1:
    - thread x4
    - data structure x1
    - protocol x2
    - configuration x1

2. How to run
    - compile
    - launch
    - command

3. Additional Features
    - reliable file transfer in the presence of loss
    - reliable file transfer in the presence of corruption


################################## Description ###################################

The bellman ford client consists of the following part:

* 1 launcher .java file:

BFClient.java
- The client is launched with this java file. It will initialize all
the data structures and start all the threads.


* 4 thread .java files:

ClIProcessorThread.java
    - The thread to continuously read user’s command, process the command and do the
corresponding reaction.

DefaultUpdateSenderThread.java
    - The thread to periodically send UPDATE_ROUTE message to it’s active neighbors to
exchange the distance vectors.
    Note: If during the period an irregular UPDATE_ROUTE is sent because of network 
topology change, the next time to send default update message will be postponed.
i.e. Suppose the timeout is 5 second. The regular update message sending should be at
t=0, t=5, t=10, t=15, … if at t=7 it received update from a neighbor and find distance
vector changed, it will immediately send out update message, then the time to send
update message becomes: t=0, t=5, t=7, t=12, t=17 …

MessageReceiverThread.java
    - The thread to continuously listen to and accept packet from a specific UDP socket,
and process the information contained in the packet.

TimeoutCheckerThread.java
    - The thread to check the dead/closed neighbor node, set the link to INFINITY, refresh
the distance vectors and if necessary, send UPDATE_ROUTE to neighbors.


* 1 data structure .java file:

BFKernel.java
    - The class contains all the runtime informations and some methods. 
    Some data structures in this file is listed as follows:

fileReceivingMap: <fileName, offset> to store the name of file under receiving and the 
total size of received file data.

neighbors: <neighbor address, link cost> to store the address of the neighbor node and
the cost of the direct links.

neighborsBackup: <neighbor address, link cost> to backup the initial value of neighbors<>.
used when link up to restore the link cost.

routingTable: <destination address, next hop address> to store the routing table.

proxyMap: <neighbor address, proxy address> to store the mapping from the neighbor’s 
address to the corresponding proxy address.

bfCostTable: <node address, the node’s distance vector>, where the node’s distance vector
is defined as <destination address, cost>. Implementation of Bellman Ford cost table 
described in the course slides.

lastHeardTimeTable: <neighbor address, last heard time>: record the time when we last
receive a packet from the neighbor. used by the timeout dead node checker.

lastUpdateSendTime: the time when this client last sent out UPDATE_ROUTE messages to
its neighbors.


* 2 network&transport, application layer protocol related .java file

PA2TCP.java
    - Define the structure of network&transport layer protocol used in this system. (Since
in this case a host is defined by ip:port, and one client has only one socket to listen to,
I define this as network&transport layer protocol named PA2TCP.
    A PA2TCP packet is composed of:
        sourceAddress(sourceIP:sourcePort)
        destinationAddress(destinationIP:destinationPort)
        type(message type)
        length
        payloadData
        checksum(Self designed, not CRC)
   if the type=TRANSFER_SEND, then the payloadData can be parsed with the application layer
protocol: PA2FTP.

PA2FTP.java
    - Defined the structure of application layer protocol used in this system. The protocol
is used to send file data.
    A PA2FTP message consists of:
        fileName
        fileSize
        offset
        fileData


* 1 configuration .java file

PA2Util.java
    - The class to store some default configuration and some useful static methods.
    It contains some information of packet headers, user commands, and some parameters such
as: MSS, ACK_TIMEOUT, MAX_RETRANSMISSION.
    It also contains some static methods to transform data to another type, and method to
calculate the checksum.

################################## How To Run ###################################
1.compile

1) cd on the the current directory
2) make

2.launch
example: java BFClient client0.txt
here client0.txt is the configuration file.

3.command
- LINKDOWN {ip_address port}
- LINKUP {ip_address port}
- CHANGECOST {ip_address port cost}
- SHOWRT
- CLOSE
- TRANSFER {filename destination_ip port}
- ADDPROXY {proxy_ip proxy_port neighbor_ip neighbor_port}
- REMOVEPROXY {neighbor_ip neighbor_port}


############################### Addition Features ################################
1. reliable file transfer in the presence of loss
    Implemented with STOP and WAIT mechanism. 
    Sender: after sending out a file data packet, it will fall into a loop to sleep
and wake up to send the same packet. Only if it’s interrupted by another thread, will
it break out of the loop to send the next packet. In the messageReceiver thread, if it
receives an ACK packet, it will interrupt the thread for file transferring.
    Receiver: reply ACK when received a file data packet.
    * Since it’s STOP and WAIT mechanism here, the timeout to wait for the ACK is
important, the default value here is set to 100ms, you can change it in PA2Util.java to
hit the best performance if the network is very lossy. Remember to recompile if changed.

2. reliable file transfer in the presence of corruption.
    Any received packet will be parsed into PA2TCP protocol, if it cannot return an
instance and return null, that means it fails to parse the packet then drop the packet.
    When parsing the packet, it will take out the checksum field from the packet, and
compare it to recalculated checksum(not CRC, self defined), if the two value are different, 
return null, otherwise go on to parse the packet.


