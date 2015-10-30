public class ClientConfig {

	public static int HEATBEAT = 30; // heart beat in seconds.
	public static boolean HEATBEATER_ON = true; // whether to turn on the heart
												// beater.
	public static String SERVER_IDENTIFIER = "ChatRoom4009"; // Used to identify
																// the serever.
																// The server's
																// username.
	public static boolean launchGUI = false;

	public static String help_cli = "\nCommand                      Function\n"
			+ "------------------------------------------------------------------\n"
			+ "broadcast <message>          broadcast message to all online users.\n"
			+ "message <user> <message>     send message to a specific user.\n"
			+ "private <user> <message>     send message to user in P2P mode.\n"
			+ "getaddress <user>            get the address of a user. needed prior to send private message.\n"
			+ "block <user>                 put a user into black list.\n"
			+ "unblock <user>               remove a user from the black list.\n"
			+ "online                       check out all online users.\n"
			+ "logout                       logout from the chat room.\n"
			+ "------------------------------------------------------------------\n";
	public static String help_gui = "Help yourself LOL.";
	public static String author_info = "Name: Chao Chen/n" + "UNI: cc3736/n"
			+ "Programing Assignment of Computer Networks.";
}
