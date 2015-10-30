/**
 * ChatRoom Protocol.
 * 
 * @author Chao Chen cc3736
 *
 */
public class Protocol {
	//
	// Message Header from server to client is defined as integers.
	//
	// 1XX indicates authentication process.
	public static final int RETRY = 111; // Reply with "HELLO" to reset.
	public static final int USERBLOCKED = 113;
	public static final int LOGINOTHERPLACE = 114;
	public static final int WRONGCOMMAND = 115;
	public static final int LOGINSUCCESS = 120;

	//Block/Unblock related header.
	public static final int BLOCKSUCCESS = 211; // 211 username
	public static final int ALREADYBLOCKED = 212; // 212 username
	public static final int USERINVALID = 213; // 213 username
	public static final int UNBLOCKSUCCESS = 214; // 214 username
	public static final int NOTBLOCKED = 215; // 215 username

	public static final int HOSTADDR = 216; // 216 username ip:port UUID

	public static final int ONLINELIST = 203; // 203 [a,b]
	public static final int LOGINNOTIFY = 204; // 204 username
	public static final int LOGOFFNOTIFY = 205; // 205 username
	public static final int BROADCASTSUCCESS = 301; // 301
	public static final int BROADCASTFAILED = 302; // 302
	public static final int MESSAGESUCCESS = 303; // 303
	public static final int MESSAGEFAILED = 304; // 304
	public static final int ADDRESSREQUESTED = 305; // 305 Requesster_Name

	public static final int BROADCASTEDMSG = 401;// 401 sender message
	public static final int POINTEDMSG = 402; // 402 sender message

	//
	// Message prefixes used from clients to the server.
	//
	public static final String HELLO = "HELLO"; //HELLO username password receiving_port
	public static final String CHECKONLINE = "ONLINE"; // ONLINE
	public static final String BROADCAST = "BROADCAST"; // BROADCAST message
	public static final String BLOCKUSER = "BLOCK"; // BLOCK username
	public static final String UNBLOCKUSER = "UNBLOCK"; // UNBLOCK user
	public static final String LOGOUT = "LOGOUT"; // LOGOUT
	public static final String GETADDR = "GETADDR"; // GETADDR username
	public static final String ADDRREQAGREE = "ADDRREQAGREE"; // ADDRREQAGREE requester UUID
	public static final String ADDRREQDENY = "ADDRREQDENY"; // ADDRREQDENY requester
	public static final String SENDMESSAGE = "MESSAGE"; // MESSAGE receiver message
	public static final String HEARTBEAT = "ALIVE"; // ALIVE

}
