import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Date;
import java.util.UUID;

/**
 * The data structure of the server to store all the informations.
 * 
 * @author Chao Chen cc3736
 *
 */
public class InfoCenter {

	// Store the user to be blocked and when it starts to be blocked.
	private volatile HashMap<String, Long> blockList = new HashMap<String, Long>();

	// Store the username: infoCell pair of logged-in users.
	private volatile HashMap<String, ClientInfoCell> handlers = new HashMap<String, ClientInfoCell>();

	// Store the identifier message header: username pair to parse messages.
	private volatile HashMap<UUID, String> userUUIDs = new HashMap<UUID, String>();

	// Store users' black list.
	private volatile HashMap<String, HashSet<String>> blackList = new HashMap<String, HashSet<String>>();

	// Store user's offline messages.
	private volatile HashMap<String, ArrayList<String>> offlineMessages = new HashMap<String, ArrayList<String>>();

	// Store how many times a user has attempted but failed to log in.
	private volatile HashMap<String, Integer> count_failed = new HashMap<String, Integer>();

	// Store the ip address requests that is sent but not responded yet.
	private volatile HashSet<String> ipRequests = new HashSet<String>();

	// keys used for synchronized I/O accesses.
	private Object blockListKey = new Object();
	private Object handlersKey = new Object();
	private Object blackListKey = new Object();
	private Object authFileKey = new Object();
	private Object offlineMsgKey = new Object();
	private Object ipRequestsKey = new Object();
	private Object userUUIDsKey = new Object();
	private Object countFailedKey = new Object();

	public void addUserUUID(String userName, UUID uuid) {
		synchronized (this.userUUIDsKey) {
			this.userUUIDs.put(uuid, userName);
		}
	}

	public void removeUserUUID(UUID uuid) {
		synchronized (this.userUUIDsKey) {
			this.userUUIDs.remove(uuid);
		}
	}

	public String getUserFromUUID(UUID uuid) {
		synchronized (this.userUUIDsKey) {
			return this.userUUIDs.get(uuid);
		}
	}

	/**
	 * A method to count up the failed login attempts of users.
	 * 
	 * @param userName
	 * @return true if the user has tried too many times and is added in the
	 *         block list; false otherwise.
	 */
	public boolean addFailedCount(String userName) {
		synchronized (this.countFailedKey) {
			if (this.count_failed.containsKey(userName)) {
				this.count_failed.put(userName,
						this.count_failed.get(userName) + 1);
			} else {
				this.count_failed.put(userName, 1);
			}
			if (this.count_failed.get(userName) >= ServerConfig.LOGINATTEMPMAX) {
				this.count_failed.remove(userName);
				return true;
			}
			return false;
		}
	}

	public void clearFailedCount(String userName) {
		synchronized (this.countFailedKey) {
			this.count_failed.remove(userName);
		}
	}

	public void addIpRequest(String requester, String requested) {
		synchronized (this.ipRequestsKey) {
			if (!this.ipRequests.contains(requester + " " + requested)) {
				this.ipRequests.add(requester + " " + requested);
			}
		}
	}

	public boolean ipRequestExist(String requester, String requested) {
		synchronized (this.ipRequestsKey) {
			return this.ipRequests.contains(requester + " " + requested);

		}
	}

	public void removeIpRequest(String requester, String requested) {
		synchronized (this.ipRequestsKey) {
			if (this.ipRequests.contains(requester + " " + requested)) {
				this.ipRequests.remove(requester + " " + requested);
			}
		}
	}

	public void addOfflineMsg(String sender, String receiver, String msg) {
		synchronized (this.offlineMsgKey) {
			if (!this.offlineMessages.containsKey(receiver)) {
				ArrayList<String> msgList = new ArrayList<String>();
				msgList.add(Protocol.POINTEDMSG + " " + sender + " " + msg);
				this.offlineMessages.put(receiver, msgList);
			} else {
				ArrayList<String> msgList = this.offlineMessages.get(receiver);
				msgList.add(Protocol.POINTEDMSG + " " + sender + " " + msg);
			}
		}
	}

	public boolean hasOfflineMsg(String userName) {
		synchronized (this.offlineMsgKey) {
			if (this.offlineMessages.containsKey(userName))
				return true;
			else
				return false;
		}
	}

	public ArrayList<String> getOfflineMsg(String userName) {
		synchronized (this.offlineMsgKey) {
			ArrayList<String> msgList = this.offlineMessages.get(userName);
			this.offlineMessages.remove(userName);
			return msgList;
		}
	}

	public void addHandler(ClientInfoCell handler) {
		synchronized (this.handlersKey) {
			this.handlers.put(handler.getUserName(), handler);
		}
	}

	public void removeUserHandler(String userName) {
		synchronized (this.handlersKey) {
			this.handlers.remove(userName);
		}

	}

	public boolean isLoggedIn(String userName) {
		synchronized (this.handlersKey) {
			return this.handlers.containsKey(userName);

		}
	}

	/**
	 * Get all online users' names.
	 * 
	 * @return online users' names as "[Bob, ColumbiaABC, David]"
	 */
	public String getOnlineUsers() {
		synchronized (this.handlersKey) {
			return this.handlers.keySet().toString();

		}
	}

	public ClientInfoCell getUserHandler(String userName) {
		synchronized (this.handlersKey) {
			return this.handlers.get(userName);
		}
	}

	public String getAddress(String userName) {
		synchronized (this.handlersKey) {
			if (this.handlers.containsKey(userName)) {
				return this.handlers.get(userName).getAddress();
			} else {
				return "NULL";
			}
		}
	}

	public int addToBlackList(String blocker, String blocked) {
		synchronized (this.blackListKey) {
			if (!this.blackList.containsKey(blocker)) {
				HashSet<String> s = new HashSet<String>();
				s.add(blocked);
				this.blackList.put(blocker, s);
				return 1;
			} else if (!this.blackList.get(blocker).contains(blocked)) {
				this.blackList.get(blocker).add(blocked);
				return 1;
			}
			return 0;
		}
	}

	public int removeFromBlackList(String blocker, String blocked) {
		synchronized (this.blackListKey) {
			if (this.blackList.containsKey(blocker)) {
				if (this.blackList.get(blocker).remove(blocked))
					return 1;
			}
			return 0;
		}
	}

	public boolean isInBlackList(String blocked, String blocker) {
		synchronized (this.blackListKey) {
			if (this.blackList.containsKey(blocker)
					&& this.blackList.get(blocker).contains(blocked)) {
				return true;
			}
			return false;
		}
	}

	public void addToBlockList(String userName) {
		synchronized (this.blockListKey) {
			this.refleshBlockList();
			this.blockList.put(userName, new Date().getTime());
		}
	}

	public boolean isInBlockList(String userName) {
		synchronized (this.blockListKey) {
			this.refleshBlockList();
			return this.blockList.containsKey(userName);
		}
	}

	/**
	 * Remove the users in the block list whose block duration is timeout.
	 */
	private void refleshBlockList() {
		long currentTime = new Date().getTime();
		for (String userName : this.blockList.keySet()) {
			if (currentTime - this.blockList.get(userName) > ServerConfig.BLOCKTIME * 1000) {
				this.blockList.remove(userName);
			}
		}
	}

	/**
	 * A method used to send broadcast message. If type 0 is chosen. The message
	 * will be sent directly to all other users. Usually used for login/logoff
	 * notification. If type 1 is chosen. The message will be encaptulated in a
	 * BROADCASTEDMSG headers.
	 * 
	 * @param message
	 *            message to be sent
	 * @param userName
	 *            the message sender or who the message is related to.
	 * @param type
	 *            message type (0 or 1)
	 * @return true if all the online users receive the message, false if some
	 *         user doesn't receive the message for some reasons like the
	 *         blacklist.
	 */
	public boolean broadcastMessage(String message, String userName, int type) {
		boolean hasTrouble = false;
		String msg;
		if (type == 0) {
			msg = message;
			for (ClientInfoCell cl : this.handlers.values()) {
				String user = cl.getUserName();
				if (user != null && !userName.equals(user)&&!this.isInBlackList(user, userName)) {
					cl.sendMsg(msg);
				}
			}
		} else if (type == 1) {
			msg = Protocol.BROADCASTEDMSG + " " + userName + " " + message;
			for (ClientInfoCell cl : this.handlers.values()) {
				String user = cl.getUserName();
				if (user == null || user.equals(userName)) {
					continue;
				} else if (this.isInBlackList(userName, user)) {
					hasTrouble = true;
					System.out.println("[MSG BLOCKED][FROM: "+userName+"][TO: "+user+"]: "+msg);
				} else {
					cl.sendMsg(msg);
				}
			}
		}
		return !hasTrouble;
	}

	/**
	 * Check whether the username is in the credential file.
	 * 
	 * @param userName
	 * @return true if the username exists, false otherwise.
	 */
	public boolean userExist(String userName) {
		synchronized (this.authFileKey) {
			boolean exists = false;
			if (userName == null)
				return exists;
			try {
				File fl = new File(ServerConfig.CREDENTIALFILE);
				BufferedReader bf = new BufferedReader(new FileReader(fl));
				String line;
				while ((line = bf.readLine()) != null) {
					if (line.split(" ")[0].equals(userName)) {
						exists = true;
						break;
					}
				}
				bf.close();
			} catch (Exception e) {
				System.out.println("[EXCEPTION] Error occurs when reading credential file.");
			}
			return exists;
		}
	}

	/**
	 * Check the username and password in the credential file.
	 * 
	 * @param userName
	 * @param passWord
	 * @return true if the username and password match in the file, false
	 *         otherwise.
	 */
	public boolean checkAuthFile(String userName, String passWord) {
		synchronized (this.authFileKey) {
			boolean isValid = false;
			if (userName == null || passWord == null)
				return isValid;
			String auth = userName + " " + passWord;
			try {
				File fl = new File(ServerConfig.CREDENTIALFILE);
				BufferedReader bf = new BufferedReader(new FileReader(fl));
				String line;
				while ((line = bf.readLine()) != null) {
					if (line.equals(auth)) {
						isValid = true;
					}
				}
				bf.close();
			} catch (Exception e) {
				System.out.println("[EXCEPTION] Failed in file authentication. Error occurs when reading credential file.");
			}
			return isValid;
		}
	}

	/**
	 * A method to check the "alive" users based on the lastLiveTime attribute.
	 * If a user is muted for a too long duration, it will be defined as
	 * logged-off and kicked out. This method will be invoked periodically by
	 * the OfflineUserKicker thread.
	 */
	public void checkAliveUser() {
		long now = new Date().getTime();
		ArrayList<String> names = new ArrayList<String>();
		ArrayList<UUID> uuids = new ArrayList<UUID>();

		for (ClientInfoCell hd : this.handlers.values()) {
			if (now - hd.getLastLiveTime() > ServerConfig.TIMEOUT * 1000) {
				names.add(hd.getUserName());
				uuids.add(hd.getUUID());
			}
		}
		for (int i = 0; i < names.size(); i++) {
			this.removeUserHandler(names.get(i));
			this.removeUserUUID(uuids.get(i));
			this.broadcastMessage(
					"" + Protocol.LOGOFFNOTIFY + " " + names.get(i),
					names.get(i), 0);
		}

	}

}
