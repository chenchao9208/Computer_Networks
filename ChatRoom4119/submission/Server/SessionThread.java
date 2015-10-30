import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Date;
import java.util.UUID;

/**
 * A thread to deal with one connection.
 * 
 * @author Chao Chen cc3736
 *
 */
public class SessionThread extends Thread {
	Socket connectionSocket;
	InfoCenter infoCenter;
	ClientInfoCell clientHandler;
	BufferedReader fromSocket;
	PrintWriter toSocket;
	String userName;

	public SessionThread(Socket connectionSocket, InfoCenter infoCenter) {
		this.connectionSocket = connectionSocket;
		this.infoCenter = infoCenter;
	}

	public void run() {
		try {
			fromSocket = new BufferedReader(new InputStreamReader(
					connectionSocket.getInputStream()));
			String line = fromSocket.readLine();
			if (line.startsWith(Protocol.HELLO)) {
				// A login request.
				System.out.println("[MSG RECEIVED][LOGIN REQUEST]: " + line);
				processLogIn(line);
			} else {
				fromSocket.close();
				connectionSocket.close();
				// Parse the message header to the corresponding username.
				UUID msgUUID = UUID.fromString(line.split(" ")[0]);
				String name = infoCenter.getUserFromUUID(msgUUID);
				if (name != null) {
					this.userName = name;
					this.clientHandler = infoCenter.getUserHandler(name);
					// refresh the last alive signal.
					this.clientHandler.setLastLiveTime(new Date().getTime());
					// process msg from the client.
					System.out.println("[MSG RECEIVED][FROM " + name + "]: "
							+ line.split(" ", 2)[1]);
					processMsg(line.split(" ", 2)[1]);
				} else {
					System.out.println("[MSG RECEIVED][NOT PARSED]: " + line);
				}
			}
			// unidentified msg will not be processed.
		} catch (Exception e) {
			if (this.userName == null) {
				System.out
						.println("[EXCEPTION] Error occurs when dealing with a new socket connection.");
			} else {
				System.out
						.println("[EXCEPTION] Error occurs when dealing with message from user "
								+ userName);
			}
		}
	}

	/**
	 * Method to process the LogIn procedure.
	 * 
	 * @param line
	 *            : the message read from the socket.
	 * @throws Exception
	 */
	public void processLogIn(String line) throws Exception {
		String msgToClient;
		toSocket = new PrintWriter(new OutputStreamWriter(
				connectionSocket.getOutputStream()), true);
		if (line.split(" ").length != 4) {
			msgToClient = "" + Protocol.RETRY;
			toSocket.println(msgToClient);
			System.out.println("[MSG SENT]: " + msgToClient);
			toSocket.close();
			fromSocket.close();
			connectionSocket.close();
			return;
		}

		String userName = line.split(" ")[1];
		String passWord = line.split(" ")[2];
		int cliRecPort = Integer.valueOf(line.split(" ")[3]);

		if (infoCenter.isInBlockList(userName)) {
			// The user is blocked for too many times of failed attempts.
			msgToClient = "" + Protocol.USERBLOCKED;
			toSocket.println(msgToClient);
			System.out.println("[MSG SENT]: " + msgToClient);
			toSocket.close();
			fromSocket.close();
			connectionSocket.close();
		} else if (infoCenter.checkAuthFile(userName, passWord) == true) {
			infoCenter.clearFailedCount(userName);
			// generate a header identifier for the user.
			UUID uuid = UUID.randomUUID();
			msgToClient = "" + Protocol.LOGINSUCCESS + " " + uuid.toString();
			System.out.println("[MSG SENT][TO " + userName + "]: "
					+ msgToClient);
			toSocket.println(msgToClient);
			toSocket.close();
			fromSocket.close();
			connectionSocket.close();
			loginCheck(userName);
			ClientInfoCell cl = new ClientInfoCell(this.infoCenter, userName,
					uuid, this.connectionSocket.getInetAddress().toString()
							.split("/")[1], cliRecPort);
			infoCenter.addHandler(cl);
			infoCenter.addUserUUID(userName, uuid);
			// Check and send offline messages.
			if (infoCenter.hasOfflineMsg(userName)) {
				ArrayList<String> offlineMsgs = infoCenter
						.getOfflineMsg(userName);
				for (String msg : offlineMsgs) {
					cl.sendMsg(msg);
				}
			}
		} else {
			// user name or password incorrect.
			msgToClient = "" + Protocol.RETRY;
			toSocket.println(msgToClient);
			System.out.println("[MSG SENT]: " + msgToClient);
			toSocket.close();
			fromSocket.close();
			connectionSocket.close();
			// Count up failed login attempts.
			if (infoCenter.addFailedCount(userName)) {
				infoCenter.addToBlockList(userName);
			}
		}
	}

	/**
	 * Method to process user's message(excluding log in procedures).
	 * 
	 * @param line
	 *            message
	 * @throws Exception
	 */
	public void processMsg(String line) throws Exception {
		if (line.equals(Protocol.CHECKONLINE)) {
			// List the online users.
			this.clientHandler.sendMsg(Protocol.ONLINELIST + " "
					+ this.infoCenter.getOnlineUsers());
			return;
		} else if (line.equals(Protocol.HEARTBEAT)) {
			return;
		} else if (line.startsWith(Protocol.GETADDR)) {
			// request for a user's address
			String target = line.substring(Protocol.GETADDR.length() + 1);
			if (!infoCenter.userExist(target)
					|| infoCenter.isInBlackList(this.userName, target)) {
				// user not exit or in the black list.
				this.clientHandler.sendMsg(Protocol.HOSTADDR + " " + target
						+ " NULL");
			} else if (!infoCenter.isLoggedIn(target)) {
				// user is offline.
				this.clientHandler.sendMsg(Protocol.HOSTADDR + " " + target
						+ " OFFLINE");
			} else {
				this.infoCenter.addIpRequest(this.userName, target);
				this.infoCenter.getUserHandler(target).sendMsg(
						Protocol.ADDRESSREQUESTED + " " + this.userName);
			}
		} else if (line.startsWith(Protocol.ADDRREQAGREE)) {
			String requester = line.split(" ")[1];
			String uuid = line.split(" ")[2];
			// check if the request is cached. If not, the response will be
			// regarded as faked.
			if (this.infoCenter.ipRequestExist(requester, this.userName)) {
				this.infoCenter.removeIpRequest(requester, this.userName);
				if (this.infoCenter.isLoggedIn(requester)) {
					// send the address to the requester.
					this.infoCenter.getUserHandler(requester).sendMsg(
							Protocol.HOSTADDR + " " + this.userName + " "
									+ infoCenter.getAddress(this.userName)
									+ " " + uuid);
				}
			}
		} else if (line.startsWith(Protocol.ADDRREQDENY)) {
			String requester = line
					.substring(Protocol.ADDRREQDENY.length() + 1);
			if (this.infoCenter.ipRequestExist(requester, this.userName)) {
				this.infoCenter.removeIpRequest(requester, this.userName);
				if (this.infoCenter.isLoggedIn(requester)) {
					this.infoCenter.getUserHandler(requester).sendMsg(
							Protocol.HOSTADDR + " " + this.userName + " NULL");
				}
			}
		} else if (line.equals(Protocol.LOGOUT)) {
			this.infoCenter.removeUserHandler(this.userName);
			this.infoCenter.removeUserUUID(this.clientHandler.getUUID());
			// broadcast log off notification.
			this.infoCenter.broadcastMessage("" + Protocol.LOGOFFNOTIFY + " "
					+ this.userName, this.userName, 0);
		} else if (line.startsWith(Protocol.SENDMESSAGE)) {
			String[] sendMsgComponents = line.split(" ", 3);
			if (sendMsgComponents.length != 3
					|| sendMsgComponents[1].equals("")
					|| !this.infoCenter.userExist(sendMsgComponents[1])) {
				// invalid recipient username.
				this.clientHandler.sendMsg("" + Protocol.USERINVALID + " "
						+ sendMsgComponents[1]);

			} else if (this.infoCenter.isInBlackList(this.userName,
					sendMsgComponents[1])) {
				// in black list.
				this.clientHandler.sendMsg("" + Protocol.MESSAGEFAILED);
			} else if (this.infoCenter.isLoggedIn(sendMsgComponents[1])) {
				// recipient is online.
				this.infoCenter.getUserHandler(sendMsgComponents[1]).sendMsg(
						Protocol.POINTEDMSG + " " + this.userName + " "
								+ sendMsgComponents[2]);
				this.clientHandler.sendMsg("" + Protocol.MESSAGESUCCESS);
			} else {
				// recipient is offline, offline message will be cached.
				this.infoCenter.addOfflineMsg(this.userName,
						sendMsgComponents[1], sendMsgComponents[2]);
				this.clientHandler.sendMsg("" + Protocol.MESSAGESUCCESS);
			}
		} else if (line.startsWith(Protocol.BLOCKUSER)
				&& line.length() > Protocol.BLOCKUSER.length() + 1) {
			// Block a user into blacklist
			String userToBlock = line
					.substring(Protocol.BLOCKUSER.length() + 1);
			if (userToBlock.equals(this.clientHandler.getUserName())
					|| !this.infoCenter.userExist(userToBlock)) {
				this.clientHandler.sendMsg(Protocol.USERINVALID + " "
						+ userToBlock);
				return;
			}
			switch (infoCenter.addToBlackList(this.userName, userToBlock)) {
			case 0:
				// the user is already blocked, no need to block again.
				this.clientHandler.sendMsg(Protocol.ALREADYBLOCKED + " "
						+ userToBlock);
				return;
			case 1:
				// block successfully.
				this.clientHandler.sendMsg(Protocol.BLOCKSUCCESS + " "
						+ userToBlock);
				return;
			}
		} else if (line.startsWith(Protocol.UNBLOCKUSER)
				&& line.length() > Protocol.UNBLOCKUSER.length() + 1) {
			// Unblock a user.
			String userToUnblock = line
					.substring(Protocol.UNBLOCKUSER.length() + 1);
			if (userToUnblock.equals(this.userName)
					|| !this.infoCenter.userExist(userToUnblock)) {
				this.clientHandler.sendMsg(Protocol.USERINVALID + " "
						+ userToUnblock);
				return;
			} else {
				switch (infoCenter.removeFromBlackList(this.userName,
						userToUnblock)) {
				case 0:
					// the user is not blocked. no need to unblock again.
					this.clientHandler.sendMsg(Protocol.NOTBLOCKED + " "
							+ userToUnblock);
					return;
				case 1:
					// unblock successful.
					this.clientHandler.sendMsg(Protocol.UNBLOCKSUCCESS + " "
							+ userToUnblock);
					return;
				}
			}
		} else if (line.startsWith(Protocol.BROADCAST)
				&& line.length() > Protocol.BROADCAST.length() + 1) {
			// Broadcast message
			if (infoCenter.broadcastMessage(
					line.substring(Protocol.BROADCAST.length() + 1),
					this.userName, 1)) {
				this.clientHandler.sendMsg("" + Protocol.BROADCASTSUCCESS);
				return;
			} else {
				// some online users will not receive the message because of the
				// black list.
				this.clientHandler.sendMsg("" + Protocol.BROADCASTFAILED);
				return;
			}
		} else {
			// invalid message format.
			this.clientHandler.sendMsg("" + Protocol.WRONGCOMMAND);
			return;
		}
	}

	/**
	 * A method to check infomation when a user is logging in. If the user is
	 * logged in some other place, the former session will be forced to close. A
	 * broadcast log in notification will be sent to all other users.
	 * 
	 * @param userName
	 */
	public void loginCheck(String userName) {
		// Kick out the logging session of the user in other place.
		if (infoCenter.isLoggedIn(userName)) {
			// get the handler
			ClientInfoCell formerHandler = infoCenter.getUserHandler(userName);
			// remove former information and send message to notify the log off.
			infoCenter.removeUserUUID(formerHandler.getUUID());
			infoCenter.removeUserHandler(userName);
			formerHandler.sendMsg("" + Protocol.LOGINOTHERPLACE);
		}
		// broadcast log in notification to other users.
		this.infoCenter.broadcastMessage("" + Protocol.LOGINNOTIFY + " "
				+ userName, userName, 0);
	}
}
