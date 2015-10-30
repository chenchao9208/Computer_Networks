
public class ServerConfig {
	//Timeout in seconds to define how long will a user be blocked for multiple failed logging in attempts.
	public static int BLOCKTIME = 60;
	
	//If the server has not heard from a client for TIMEOUT(s), the client will be labeled as logged off and kicked out from the Chat Room.
	public static int TIMEOUT = 40;
	
    public static String CREDENTIALFILE = "credentials.txt";
	
    //The tolerance of failed logging in. When a user attempt and fail to login for too many time, it will be blocked.
	public static int LOGINATTEMPMAX = 3;
}
