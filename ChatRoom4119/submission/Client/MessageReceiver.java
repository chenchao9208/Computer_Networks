import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;
import java.io.PrintWriter;

/**
 * Message Receiver used to listen and receive message from the server and P2P
 * peers.
 * 
 * @author Chao Chen cc3736
 *
 */
public class MessageReceiver extends Thread {

	private Client client;
	private ServerSocket receiverSocket;// the receive socket to listen and
										// accept
	// The mapping from the message header to the user or server.
	private HashMap<UUID, String> uuidMap = new HashMap<UUID, String>();
	private PrintWriter cliOut; // The output printwriter, usually print
								// information to System.out.

	// Synchronized Key used to safely perform multithread data access.
	private Object uuidMapKey = new Object();

	public void setCliOut(PrintWriter pr) {
		this.cliOut = pr;
	}

	/**
	 * Constructor
	 * 
	 * @param client
	 *            clientApp identity
	 * @param receiverSocket
	 *            the socket to listen and accept on.
	 * @param cliOut
	 *            the output printwriter.
	 */
	public MessageReceiver(Client client, ServerSocket receiverSocket,
			PrintWriter cliOut) {
		this.client = client;
		this.receiverSocket = receiverSocket;
		this.cliOut = cliOut;
	}

	public void run() {
		this.registerServerUUID();
		while (true) {
			try {
				String received = receiveMsg();
				String toCliOut = processMsg(received);
				if (toCliOut != null) {
					cliOut.println(toCliOut);
					cliOut.print(">");
					cliOut.flush();
				}
			} catch (Exception e) {
				// if the socket is closed, the program is closed.
				// System.exit(0) will invoke
				// the Ctrl+C signal handler in client class.
				System.exit(0);
			}
		}

	}

	public void addToUUIDMap(UUID uuid, String name) {
		synchronized (this.uuidMapKey) {
			this.uuidMap.put(uuid, name);
		}
	}

	public void removeFromUUIDMap(UUID uuid) {
		synchronized (this.uuidMapKey) {
			this.uuidMap.remove(uuid);
		}
	}

	public String getNameFromUUID(UUID uuid) {
		synchronized (this.uuidMapKey) {
			return this.uuidMap.get(uuid);
		}
	}

	/**
	 * Register new UUID-userName pair for P2P private message exchange.
	 */
	public void registerServerUUID() {
		this.addToUUIDMap(client.getUUID(), ClientConfig.SERVER_IDENTIFIER);
	}

	/**
	 * A method to accept a connection, read a message and close the socket
	 * 
	 * @return the message read from one socket.
	 * @throws Exception
	 *             exception caused by socket I/O.
	 */
	public String receiveMsg() throws Exception {
		String line = "";
		Socket connectionSocket = receiverSocket.accept();
		if (connectionSocket.isConnected()) {
			connectionSocket.setSoTimeout(2000);
			BufferedReader socketIn = new BufferedReader(new InputStreamReader(
					connectionSocket.getInputStream()));
			line = socketIn.readLine();
			socketIn.close();
		}
		connectionSocket.close();
		return line;
	}

	/**
	 * Process the raw message read from the socket.
	 * 
	 * @param line
	 *            the raw message
	 * @return The message to be shown on CLI based on the information. null if
	 *         no information to show.
	 * @throws Exception
	 */
	public String processMsg(String line) throws Exception {
		UUID msgUUID = UUID.fromString(line.split(" ", 2)[0]);
		String sender = this.getNameFromUUID(msgUUID);
		if (sender == null) {
			// The header UUID is not identified. Do nothing.
			return null;
		} else if (sender.equals(ClientConfig.SERVER_IDENTIFIER)) {
			// The header UUID is that of the server. Deal with it as a server
			// message.
			return processServerMsg(line.split(" ", 2)[1]);
		} else {
			// The header UUID is from other P2P users. Display the message
			// directly.
			return sender + ": " + line.split(" ", 2)[1];
		}

	}

	/**
	 * Process the message from the server.
	 * 
	 * @param line
	 *            the message information excluding the header UUID.
	 * @return the information to be displayed on the CLI.
	 */
	public String processServerMsg(String line) {
		String msgToCliOut = null;
		int prefix = Integer.valueOf(line.substring(0, 3));
		switch (prefix) {
		case Protocol.WRONGCOMMAND:
			msgToCliOut = "Invalid Username or Password, try again.";
			break;
		case (Protocol.BROADCASTEDMSG):
			String[] tmp = line.split(" ", 3);
			msgToCliOut = tmp[1] + ": " + tmp[2];
			break;
		case (Protocol.POINTEDMSG):
			String[] tmp4 = line.split(" ", 3);
			msgToCliOut = tmp4[1] + ": " + tmp4[2];
			break;
		case (Protocol.ONLINELIST):
			String[] onlineUsers = line.substring(5, line.length() - 1).split(", ");
			if(onlineUsers.length==1){
				msgToCliOut = "You are alone here. :)";
			}else{
				msgToCliOut = "Online Users:";
				for (String name: onlineUsers){
					if(!name.equals(this.client.getUserName())){
						msgToCliOut += "\n>"+name;
					}
				}
			}
			break;
		case (Protocol.BROADCASTSUCCESS):
			break;
		case (Protocol.BROADCASTFAILED):
			msgToCliOut = "Your message could not be delivered to some recipients.";
			break;
		case (Protocol.MESSAGESUCCESS):
			break;
		case (Protocol.MESSAGEFAILED):
			msgToCliOut = "Your message could not be delivered as the recipient has blocked you.";
			break;
		case (Protocol.BLOCKSUCCESS):
			msgToCliOut = "User " + line.substring(4) + " has been blocked.";
			break;
		case (Protocol.ALREADYBLOCKED):
			msgToCliOut = "User " + line.substring(4)
					+ " is already blocked. You don't need to block him again.";
			break;
		case (Protocol.USERINVALID):
			msgToCliOut = "No such user: " + line.substring(4) + ".";
			break;
		case (Protocol.NOTBLOCKED):
			msgToCliOut = "User " + line.substring(4)
					+ " is not blocked. You don't need to unblock.";
			break;
		case (Protocol.UNBLOCKSUCCESS):
			msgToCliOut = "User " + line.substring(4) + " is unblocked.";
			break;
		case (Protocol.LOGINNOTIFY):
			msgToCliOut = "User " + line.substring(4)
					+ " comes in the chat room.";
			client.removeAddressOf(line.substring(4));
			break;
		case (Protocol.LOGOFFNOTIFY):
			msgToCliOut = "User " + line.substring(4)
					+ " leaves the chat room.";
			client.removeAddressOf(line.substring(4));
			break;
		case (Protocol.LOGINOTHERPLACE):
			msgToCliOut = "Your account is logged in in other place.";
			cliOut.print(msgToCliOut);
			cliOut.flush();
			System.exit(0);
			break;
		case (Protocol.HOSTADDR):
			String[] hostAddrComponents = line.split(" ");
			if (hostAddrComponents[2].equals("NULL")) {
				msgToCliOut = "Failed to get IP of user "
						+ hostAddrComponents[1];
			} else if (hostAddrComponents[2].equals("OFFLINE")) {
				msgToCliOut = "user " + hostAddrComponents[1] + " is offline.";
			} else {
				msgToCliOut = "Address of user " + hostAddrComponents[1] + ": "
						+ hostAddrComponents[2];
				client.addToAddressMap(hostAddrComponents[1],
						hostAddrComponents[2]);
				client.addToUUIDMap(hostAddrComponents[1],
						UUID.fromString(hostAddrComponents[3]));
			}
			break;
		case (Protocol.ADDRESSREQUESTED):
			String requester = line.substring(4);
			UUID uuid = UUID.randomUUID();
			this.addToUUIDMap(uuid, requester);
			//Add extra tasks to reserver user input "Y","y","N" and "n" for this task.
			ArrayList<String> yes = new ArrayList<String>();
			ArrayList<String> no = new ArrayList<String>();
			yes.add(Protocol.ADDRREQAGREE + " " + requester + " "
					+ uuid.toString());
			yes.add(">Agreed. User " + requester + " will get your IP address.");
			no.add(Protocol.ADDRREQDENY + " " + requester);
			no.add(">Denied. User " + requester
					+ " will NOT get your IP address.");
			client.resetExtraTask();
			client.addExtraTask("y", yes);
			client.addExtraTask("Y", yes);
			client.addExtraTask("n", no);
			client.addExtraTask("N", no);
			msgToCliOut = "User " + requester
					+ " is requesting your IP address. Agree or not?(y/n)";
			break;
		default:
			msgToCliOut = ">" + "Failed to parse server message: " + line;
			break;
		}
		return msgToCliOut;
	}
}
