import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.UUID;

/**
 * Client class.
 * 
 * @author Chao Chen cc3736
 * 
 */
public class Client {

	private HashMap<String, String> userAddressMap;// cache <username,ip:port>
													// pair
	private HashMap<String, UUID> userUUIDMap; // store <username, uuid> pair

	// Store addition user input identifier, the key is the user input, the
	// value is the message to be sent to the server and displayed on CLI.
	// Example:
	// When user "Bob" request my address, extra tasks will be added as:
	// "Y": ["ADDRREQAGREE Bob emgeif832jg-ff932ngs-329gne",
	// ">Agreed. User Bob will get your IP address."]
	// "y": ["ADDRREQAGREE Bob emgeif832jg-ff932ngs-329gne",
	// ">Agreed. User Bob will get your IP address."]
	// "N": ["ADDRREQDENY Bob",
	// ">Denied. User Bob will not get your IP address."]
	// "n": ["ADDRREQDENY Bob",
	// ">Denied. User Bob will not get your IP address."]
	private HashMap<String, ArrayList<String>> extraTask;
	public BufferedReader cliIn;// System.in in default.
	public PrintWriter cliOut;// System.out in default.
	private HeartBeater heartBeater;
	private String userName = null;
	private UUID uuid;
	private SocketAddress serverAddress; // address of the server.
	private ServerSocket receiverSocket; // socket used to accept and receive
											// messages.
	private int receivePort; // local port number used to receive messages.
	private MessageReceiver messageReceiver; // MessageReceiver object to deal
												// with accepted socket and
												// incoming messages.

	// Key sets that are used to perform synchronized I/O for Multi-Thread
	// program.
	private Object extraTaskKey = new Object();
	private Object sendToServerKey = new Object();
	private Object userAddrMapKey = new Object();

	public Client(String serverName, int portNumber) {
		readConfigFile();
		// Add handler to deal with Ctrl+C signal.
		Runtime.getRuntime().addShutdownHook(new ExitHandler());
		this.serverAddress = new InetSocketAddress(serverName, portNumber);
		this.extraTask = new HashMap<String, ArrayList<String>>();
		this.userAddressMap = new HashMap<String, String>();
		this.userUUIDMap = new HashMap<String, UUID>();
		this.cliIn = new BufferedReader(new InputStreamReader(System.in));
		this.cliOut = new PrintWriter(new OutputStreamWriter(System.out), true);
		// looking for an unused port to be set as the receive port.
		while (true) {
			try {
				this.receivePort = new Random().nextInt(10000) + 20000;
				receiverSocket = new ServerSocket(this.receivePort);
				break;
			} catch (Exception e) {
				System.out.println("Port " + receivePort
						+ " is in use. Finding another one...");
			}
		}
		this.messageReceiver = new MessageReceiver(this, receiverSocket, cliOut);
	}

	/**
	 * Method to read the configuration file.
	 */
	public void readConfigFile() {
		try {
			File cf = new File("configClient.txt");
			BufferedReader br = new BufferedReader(new FileReader(cf));
			String line;
			String tokens[];
			while ((line = br.readLine()) != null) {
				if (line.startsWith("#"))
					continue;
				tokens = line.split(" ");
				if (tokens[0].equals("HEATBEAT")) {
					ClientConfig.HEATBEAT = Integer.valueOf(tokens[2]);
				} else if (tokens[0].equals("HEATBEATER_ON")) {
					if (tokens[2].equals("true"))
						ClientConfig.HEATBEATER_ON = true;
					else
						ClientConfig.HEATBEATER_ON = false;
				} else if (tokens[0].equals("launchGUI")) {
					if (tokens[2].equals("true"))
						ClientConfig.launchGUI = true;
					else
						ClientConfig.launchGUI = false;
				}
			}
			br.close();
		} catch (Exception e) {
			System.out.println("Error occurs when reading configClient.txt");
		}
	}

	/**
	 * MessageReceiver Getter.
	 * 
	 * @return MessageReceiver object of the client
	 */
	public MessageReceiver getMessageReceiver() {
		return this.messageReceiver;
	}

	/**
	 * Register the uuid and hostname for further P2P message exchange.
	 * 
	 * @param userName
	 *            username of another people
	 * @param uuid
	 *            the identifier used to identify the message sender.
	 */
	public void addToUUIDMap(String userName, UUID uuid) {
		this.userUUIDMap.put(userName, uuid);
	}

	/**
	 * UUID getter
	 * 
	 * @return uuid that is used to identify message exchange of the server and
	 *         this user.
	 */
	public UUID getUUID() {
		return this.uuid;
	}

	/**
	 * ReceiverSocket Getter
	 * 
	 * @return the receiver socket used to receive messages.
	 */
	public ServerSocket getReceiverSocket() {
		return this.receiverSocket;
	}

	/**
	 * start the client in Command Line Interface.
	 */
	private void startClient() {
		if (!this.authenticate()) {
			return;
		}
		messageReceiver.start();
		if (ClientConfig.HEATBEATER_ON) {
			this.heartBeater = new HeartBeater(this);
			this.heartBeater.start();
		}
		this.processCliInput();
		try {
			this.receiverSocket.close();
		} catch (Exception e) {
		}
	}

	/**
	 * Invoked when starting the client with GUI
	 */
	public void startClientWithGUI() {
		if (ClientConfig.HEATBEATER_ON) {
			this.heartBeater = new HeartBeater(this);
			this.heartBeater.start();
		}
	}

	/**
	 * Method to process the whole LogIn procedure.
	 * 
	 * @return true if login successfully; false otherwise.
	 */
	public boolean authenticate() {
		System.out.println("Log In:");
		while (true) {
			try {
				// User input username and password.
				this.cliOut.print(">Username: ");
				this.cliOut.flush();
				String userName = this.cliIn.readLine();
				this.cliOut.print(">Password: ");
				this.cliOut.flush();
				String passWord = String.valueOf(System.console()
						.readPassword());
				// Send login message to the server
				int reply = logIn(userName, passWord);
				if (reply == 1) {
					// login successfully.
					cliOut.println(">Welcome to simple chat room!");
					cliOut.print(">");
					cliOut.flush();
					return true;
				} else if (reply == -1) {
					// blocked for multiple login failures.
					cliOut.println(">Due to multiple login failures, your account has been blocked. Please try again after sometime.");
					return false;
				} else if (reply == 0) {
					// wrong username or password.
					cliOut.println(">Invalid username or password. Please try again.");
				} else if (reply == -100) {
					System.exit(0);
				}
			} catch (Exception e) {
				System.out.println("Error occurs in authentication step.");
				return false;
			}
		}
	}

	/**
	 * Method used to send login message and receive reply.
	 * 
	 * @param userName
	 *            username
	 * @param passWord
	 *            password
	 * @return -1: trying too many times and blocked by the server; 0: username
	 *         or password incorrect; 1:success
	 * @throws Exception
	 */
	public int logIn(String userName, String passWord) {
		try {
			Socket sk = new Socket();
			sk.connect(serverAddress);
			PrintWriter pw = new PrintWriter(new OutputStreamWriter(
					sk.getOutputStream()), true);
			BufferedReader br = new BufferedReader(new InputStreamReader(
					sk.getInputStream()));
			pw.println(Protocol.HELLO + " " + userName + " " + passWord + " "
					+ this.receivePort);
			String msg = br.readLine();
			pw.close();
			br.close();
			sk.close();
			if (msg.startsWith("" + Protocol.USERBLOCKED)) {
				return -1;
			} else if (msg.startsWith("" + Protocol.RETRY)) {
				return 0;
			} else if (msg.startsWith("" + Protocol.LOGINSUCCESS)) {
				this.uuid = UUID.fromString(msg.split(" ")[1]);
				this.userName = userName;
				return 1;
			}
		} catch (Exception e) {
			System.out
					.println("Cannot connect to the server. Check if you input the correct address.");
			return -100;

		}
		return 0;
	}

	/**
	 * Add commands for extra tasks. For example when the user's ip is
	 * requested, the input "Y" and "N" should be identified to reply the
	 * request.
	 * 
	 * @param command
	 *            the user input to reserve. Usually "Y", "y", "N" and "n".
	 * @param messageToServerAndCLI
	 *            the message sent to the server and CLI corresponding to the
	 *            reserved input.
	 */
	public void addExtraTask(String command,
			ArrayList<String> messageToServerAndCLI) {
		synchronized (this.extraTaskKey) {
			this.extraTask.put(command, messageToServerAndCLI);
		}
	}

	/**
	 * Check if the string is reserved to be a command.
	 * 
	 * @param command
	 *            the input identity.
	 * @return whether the input is reserved to be an extra command.
	 */
	public boolean isInExtraTask(String command) {
		synchronized (this.extraTaskKey) {
			return this.extraTask.containsKey(command);
		}
	}

	/**
	 * Reset the extra task HashMap.
	 */
	public void resetExtraTask() {
		synchronized (this.extraTaskKey) {
			this.extraTask.clear();
		}
	}

	/**
	 * Given the command, get the messages to be sent to the server and
	 * displayed on CLI.
	 * 
	 * @param command
	 * @return A list formed of the message to be sent to the server and to bi
	 *         displayed on CLI.
	 */
	public ArrayList<String> getExtraTaskMessage(String command) {
		synchronized (this.extraTaskKey) {
			return this.extraTask.get(command);
		}
	}

	/**
	 * A procedure to process user's CLI input in while loop.
	 */
	public void processCliInput() {
		String line;
		while (true) {
			line = "";
			try {
				line = this.cliIn.readLine();
			} catch (Exception e) {
				System.out.println("Error occurs when reading user input.");
				break;
			}
			if (processCliInputEntity(line) == -1) {
				break;
			}
		}
	}

	/**
	 * Process a single user input command.
	 * 
	 * @param line
	 *            the command by user in CLI.
	 * @return -1 if the Client should close; 0 otherwise.
	 */
	public int processCliInputEntity(String line) {
		String msgToServer = null;
		String msgToCliOut = null;
		if (line.equals("help")) {
			msgToCliOut = ClientConfig.help_cli;
		} else if (line.equals("online")) {
			msgToServer = Protocol.CHECKONLINE;
		} else if (line.startsWith("block ") && line.length() > 6
				&& line.charAt(6) != ' ') {
			String name = line.substring(6);
			if (name.equals(this.userName)) {
				msgToCliOut = ">You can't block yourself.";
			} else {
				msgToServer = Protocol.BLOCKUSER + " " + name;
				UUID id = this.userUUIDMap.get(name);
				if (id != null) {
					this.userUUIDMap.remove(name);
					this.messageReceiver.removeFromUUIDMap(id);
				}
			}
		} else if (line.startsWith("unblock ") && line.length() > 8
				&& line.charAt(8) != ' ') {
			String unblockName = line.substring(8);
			if (unblockName.equals(this.userName)) {
				msgToCliOut = ">You don't need to unblock yourself.";
			} else {
				msgToServer = Protocol.UNBLOCKUSER + " " + unblockName;
			}
		} else if (line.startsWith("broadcast ") && line.length() > 10) {
			msgToServer = Protocol.BROADCAST + " " + line.substring(10);
		} else if (line.startsWith("message ")) {
			String[] tmp = line.split(" ", 3);
			if (tmp.length != 3 || tmp[1].equals("") || tmp[2].equals("")) {
				msgToCliOut = ">Wrong Command.";
			} else if (tmp[1].equals(this.getUserName())) {
				msgToCliOut = ">You don't need to send message to yourself.";
			} else {
				msgToServer = Protocol.SENDMESSAGE + " " + tmp[1] + " "
						+ tmp[2];
			}
		} else if (line.startsWith("private ")) {
			String[] tmp2 = line.split(" ", 3);
			if (tmp2.length != 3 || tmp2[1].equals("") || tmp2[2].equals("")) {
				msgToCliOut = ">Wrong Command.";
			} else if (tmp2[1].equals(this.getUserName())) {
				msgToCliOut = ">You don't need to send message to yourself.";
			} else if (this.getAddressOf(tmp2[1]) == null) {
				msgToCliOut = ">You should run 'getaddress <host>' command first.";
			} else {
				this.sendToHost(tmp2[1], tmp2[2]);
			}
		} else if (line.equals("logout")) {
			msgToServer = Protocol.LOGOUT;
			sendToServer(msgToServer);
			return -1;
		} else if (line.startsWith("getaddress ") && line.length() > 11
				&& line.charAt(11) != ' ') {
			String name = line.substring(11);
			if (this.hasAddressOf(name)) {
				msgToCliOut = ">Address of user " + name + ": "
						+ this.getAddressOf(name);
			} else {
				msgToServer = Protocol.GETADDR + " " + name;
			}
		} else if (this.isInExtraTask(line)) {
			ArrayList<String> msgs = this.getExtraTaskMessage(line);
			this.resetExtraTask();
			msgToServer = msgs.get(0);
			msgToCliOut = msgs.get(1);
		} else {
			msgToCliOut = ">Wrong Command.";
		}
		if (msgToCliOut != null) {
			this.cliOut.println(msgToCliOut);
		}
		if (msgToServer != null) {
			sendToServer(msgToServer);
		}
		this.cliOut.print(">");
		this.cliOut.flush();
		return 0;
	}

	/**
	 * Method used to send message to the server.
	 * 
	 * @param msgToServer
	 *            the message to be sent.
	 */
	public void sendToServer(String msgToServer) {
		synchronized (this.sendToServerKey) {
			try {
				Socket sk = new Socket();
				sk.connect(this.serverAddress);
				PrintWriter pr = new PrintWriter(new OutputStreamWriter(
						sk.getOutputStream()), true);
				pr.println(this.uuid.toString() + " " + msgToServer);
				pr.close();
				sk.close();
			} catch (Exception e) {
				System.out
						.println("[System]: Error. Cannot connect to the server.");
				System.out.print(">");
				System.out.flush();
			}
		}
	}

	/**
	 * Method to send P2P message to another user
	 * 
	 * @param hostName
	 *            : the receiver username
	 * @param msg
	 *            : message to be sent.
	 */
	public void sendToHost(String hostName, String msg) {
		try {
			Socket sk = new Socket();
			String address = this.getAddressOf(hostName);
			SocketAddress addr = new InetSocketAddress(address.split(":")[0],
					Integer.valueOf(address.split(":")[1]));
			sk.connect(addr);
			PrintWriter pr = new PrintWriter(new OutputStreamWriter(
					sk.getOutputStream()), true);
			pr.println(this.userUUIDMap.get(hostName).toString() + " " + msg);
			pr.close();
			sk.close();
		} catch (Exception e) {
			cliOut.println(">Cannot connect to the host, the message will be cached and sent thru the server.");
			this.sendToServer(Protocol.SENDMESSAGE + " " + hostName + " " + msg);
		}
	}

	/**
	 * A method to check if the user has another one's ip.
	 * 
	 * @param userName
	 * @return true if the address is already cached locally; false otherwise.
	 */
	public boolean hasAddressOf(String userName) {
		synchronized (this.userAddrMapKey) {
			return this.userAddressMap.containsKey(userName);
		}
	}

	/**
	 * A method to cache locally the address of another user.
	 * 
	 * @param userName
	 * @param address
	 */
	public void addToAddressMap(String userName, String address) {
		synchronized (this.userAddrMapKey) {
			this.userAddressMap.put(userName, address);
		}
	}

	/**
	 * Delete the local cached address information of another user.
	 * 
	 * @param userName
	 */
	public void removeAddressOf(String userName) {
		synchronized (this.userAddrMapKey) {
			if (this.userAddressMap.containsKey(userName)) {
				this.userAddressMap.remove(userName);
			}
		}
	}

	/**
	 * Get the address of another user
	 * 
	 * @param userName
	 * @return null if the address is not cache; the address identity otherwise.
	 */
	public String getAddressOf(String userName) {
		synchronized (this.userAddrMapKey) {
			return this.userAddressMap.get(userName);
		}
	}

	/**
	 * Get the logged-in user's name.
	 * 
	 * @return username of current user.
	 */
	public String getUserName() {
		return this.userName;
	}

	/**
	 * A class to deal with Ctrl+C signal.
	 * 
	 * @author Chao
	 *
	 */
	private class ExitHandler extends Thread {
		public ExitHandler() {
			super("Exit Handler");
		}

		public void run() {
			if (userName != null) {
				try {
					receiverSocket.close();
					Socket sk = new Socket();
					sk.connect(serverAddress);
					PrintWriter pr = new PrintWriter(new OutputStreamWriter(
							sk.getOutputStream()), true);
					pr.println(uuid.toString() + " " + Protocol.LOGOUT);
					pr.close();
					sk.close();
				} catch (Exception e) {
					System.out
							.println("\n[System]: Error. Cannot connect to the server.");
				}
				System.out.println("\n>Logged out. Bye~");
			}
		}
	}

	public static void main(String args[]) throws Exception {
		if (args.length != 2) {
			System.out.println("Usage: java Client <Server IP> <Server Port>");
			System.exit(0);
		}
		Client client = new Client(args[0], Integer.valueOf(args[1]));
		if (ClientConfig.launchGUI) {
			System.out.println("Starting GUI...");
			new LogInGUI(client).setVisible(true);
		} else {
			client.startClient();
		}
	}
}
