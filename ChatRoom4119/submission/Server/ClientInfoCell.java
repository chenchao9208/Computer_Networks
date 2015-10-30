import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Date;
import java.util.UUID;

/**
 * A information cell datastructure to store the information of one logged-in user.
 * 
 * @author Chao Chen cc3736
 *
 */
public class ClientInfoCell {
	private String userName = null;
	
	private InfoCenter infoCenter;
	
	// IP address and receiving port of the client
	private String ipAddress;
	private int cliRecPort;

	// Record when the last messsage was received from the client.
	private long lastLiveSignal = 0;

	// Identify header used to send message.
	private UUID uuid;

	// Synchronize key used for multi-thread I/O.
	private Object sendMsgKey = new Object();
	private Object liveTimeKey = new Object();

	public ClientInfoCell(InfoCenter infoCenter, String userName, UUID uuid, String ip, int cliRecPort) {
		this.infoCenter = infoCenter;
		this.userName = userName;
		this.uuid = uuid;
		this.ipAddress = ip;
		this.cliRecPort = cliRecPort;
		this.lastLiveSignal = new Date().getTime();
	}

	/**
	 * UUID Getter
	 * 
	 * @return uuid. Header identifier of the user.
	 */
	public UUID getUUID() {
		return this.uuid;
	}

	/**
	 * Method to send message to the user.
	 * 
	 * @param msg
	 *            message to be sent.
	 */
	public void sendMsg(String msg) {
		synchronized (this.sendMsgKey) {
			try {
				Socket sk = new Socket();
				sk.setReuseAddress(true);
				InetSocketAddress client = new InetSocketAddress(ipAddress,
						cliRecPort);
				sk.connect(client);
				PrintWriter writer = new PrintWriter(new OutputStreamWriter(
						sk.getOutputStream()), true);
				writer.println(this.uuid.toString() + " " + msg);
				writer.flush();
				System.out.println("[MSG SENT][TO " + this.userName + "]: " + msg);
				sk.close();
			} catch (Exception e) {
				System.out.println("[EXCEPTION] Cannot send message to host: "+this.userName);
				if(msg.startsWith(""+Protocol.POINTEDMSG)){
					this.infoCenter.addOfflineMsg(msg.split(" ",3)[1], this.userName, msg.split(" ",3)[2]);
				}
			}
		}
	}

	/**
	 * IP:port Getter.
	 * 
	 * @return "IP:Port" of the client as a string.
	 */
	public String getAddress() {
		return this.ipAddress + ":" + this.cliRecPort;
	}

	public String getUserName() {
		return this.userName;
	}

	public long getLastLiveTime() {
		synchronized (this.liveTimeKey) {
			return this.lastLiveSignal;
		}
	}

	public void setLastLiveTime(long time) {
		synchronized (this.liveTimeKey) {
			this.lastLiveSignal = time;
		}
	}
}
