import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Chat Room server identity.
 * 
 * @author Chao Chen cc3736
 *
 */
public class Server {
	private ServerSocket welcomeSocket; // the socket to accept connections from
										// clients.
	private InfoCenter infoCenter; // information center to store all the
									// runtime informations.
	private OfflineUserKickerThread liveHandler; // handler to deal with muted users
											// and kick them out.

	public Server(int portNumber) {
		readConfigFile();
		try {
			welcomeSocket = new ServerSocket(portNumber);
		} catch (Exception e) {
			System.out.println("[EXCEPTION] Failed to set up server at port " + portNumber
					+ ". Try another port.");
			System.exit(-1);
		}
		infoCenter = new InfoCenter();
		liveHandler = new OfflineUserKickerThread(infoCenter);
		liveHandler.start();
		System.out.println("[INFO] Server is started.");
		while (true) {
			try {
				Socket connectionSocket = welcomeSocket.accept();
				if (connectionSocket.isConnected()) {
					new SessionThread(connectionSocket, infoCenter).start();
				}
			} catch (Exception e) {
				System.out.println("[EXCEPTION] Erorr occurs when accepting a connection.");
				
			}
		}
	}

	/**
	 * Method to read the .txt configuration file and overwrite the
	 * ServerConfig.java if any modification exists.
	 */
	public void readConfigFile() {
		try {
			File cf = new File("configServer.txt");
			BufferedReader br = new BufferedReader(new FileReader(cf));
			String line;
			String tokens[];
			while ((line = br.readLine()) != null) {
				if (line.startsWith("#"))
					continue;
				tokens = line.split(" ");
				if (tokens[0].equals("BLOCKTIME")) {
					ServerConfig.BLOCKTIME = Integer.valueOf(tokens[2]);
				} else if (tokens[0].equals("TIMEOUT")) {
					ServerConfig.TIMEOUT = Integer.valueOf(tokens[2]);
				} else if (tokens[0].equals("LOGINATTEMPMAX")) {
					ServerConfig.LOGINATTEMPMAX = Integer.valueOf(tokens[2]);
				}
			}
			br.close();
		} catch (Exception e) {
			System.out.println("[EXCEPTION] Error occurs when reading configServer.txt");
		}
	}

	public static void main(String args[]) throws Exception {
		if (args.length != 1) {
			System.out.println("Usage: java Server <Server Port>");
			System.exit(-1);
		}
		new Server(Integer.valueOf(args[0]));
	}

}
