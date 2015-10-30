import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.UUID;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JScrollBar;

/**
 * The chat room GUI identity. The Interface is desigend with NetBeans IDE.
 * Functional methods are added by me.
 * 
 * @author Chao Chen cc3736
 */
public class ChatRoomGUI extends JFrame {

	/**
	 * default serial version UID.
	 */
	private static final long serialVersionUID = 1L;
	Client client;
	MessageReceiver messageReceiver;
	String enter = System.getProperty("line.separator");

	/**
	 * Creates new form ChatRoomGUI
	 */
	public ChatRoomGUI() {
		initComponents();
	}

	/**
	 * Creates ChatRoomGUI with a Client object.
	 */
	public ChatRoomGUI(Client client) {
		this.client = client;
		initComponents();// initialize all the GUI components
		// start the receiver thread to receive messages from the server.
		new Receiver().start();
		display("[System]  Welcome to Chat Room 4009!");
		// send an online request to get the initial online list.
		client.sendToServer(Protocol.CHECKONLINE);
	}

	/**
	 * Receiver, used to build a new thread to receive server messages.
	 */
	class Receiver extends Thread {
		public void run() {
			messageReceiver = client.getMessageReceiver();
			messageReceiver.registerServerUUID();
			while (true) {
				try {
					String line = messageReceiver.receiveMsg();
					processReceivedMsg(line);

				} catch (Exception e) {
					break;
				}
			}
		}
	}

	/**
	 * Process message received from the listening socket.
	 */
	public void processReceivedMsg(String line) {
		UUID msgUUID = UUID.fromString(line.split(" ", 2)[0]);
		String sender = this.messageReceiver.getNameFromUUID(msgUUID);
		if (sender == null) {
			// failed to identify the message sender.
			return;
		} else if (sender.equals(ClientConfig.SERVER_IDENTIFIER)) {
			// message sent from the server
			this.processServerMsg(line.split(" ", 2)[1]);
		} else {
			// message sent from other users in P2P mode.
			display("[To You][Private]  " + sender + ": "
					+ line.split(" ", 2)[1]);
		}
	}

	/**
	 * Method used to process server messages.
	 */
	@SuppressWarnings("unchecked")
	public void processServerMsg(String line) {
		String msgToCliOut = null;
		int prefix = Integer.valueOf(line.substring(0, 3));
		// identify the message function by the header.
		switch (prefix) {
		case Protocol.WRONGCOMMAND:
			msgToCliOut = "[System]  Wrong Command.";
			break;
		case (Protocol.BROADCASTEDMSG):
			String[] broadcastMsgComponents = line.split(" ", 3);
			msgToCliOut = "[To All]  " + broadcastMsgComponents[1] + ": "
					+ broadcastMsgComponents[2];
			break;
		case (Protocol.POINTEDMSG):
			String[] pointedMsgComponents = line.split(" ", 3);
			msgToCliOut = "[To You][Thru Server]  " + pointedMsgComponents[1]
					+ ": " + pointedMsgComponents[2];
			break;
		case (Protocol.ONLINELIST):
			String[] onlineUsers = line.substring(5, line.length() - 1).split(
					", ");
			this.jList1.setListData(onlineUsers);
			break;
		case (Protocol.BROADCASTSUCCESS):
			break;
		case (Protocol.BROADCASTFAILED):
			msgToCliOut = "[System]  Your message could not be delivered to some recipients.";
			break;
		case (Protocol.MESSAGESUCCESS):
			break;
		case (Protocol.MESSAGEFAILED):
			msgToCliOut = "[System]  Your message could not be delivered as the recipient has blocked you.";
			break;
		case (Protocol.BLOCKSUCCESS):
			msgToCliOut = "[System]  User " + line.substring(4)
					+ " has been blocked.";
			break;
		case (Protocol.ALREADYBLOCKED):
			msgToCliOut = "[System]  User " + line.substring(4)
					+ " is already blocked. You don't need to block him again.";
			break;
		case (Protocol.USERINVALID):
			msgToCliOut = "[System]  No such user: " + line.substring(4) + ".";
			break;
		case (Protocol.NOTBLOCKED):
			msgToCliOut = "[System]  User " + line.substring(4)
					+ " is not blocked. You don't need to unblock.";
			break;
		case (Protocol.UNBLOCKSUCCESS):
			msgToCliOut = "[System]  User " + line.substring(4)
					+ " is unblocked.";
			break;
		case (Protocol.LOGINNOTIFY):
			String incomingUser = line.substring(4);
			msgToCliOut = "[System]  User " + incomingUser
					+ " comes in the chat room.";
			// remove the cached address of the new logged-in user.
			client.removeAddressOf(incomingUser);
			// refresh the online list.
			ArrayList<String> lists = new ArrayList<String>();
			for (int i = 0; i < jList1.getModel().getSize(); i++) {
				lists.add(this.jList1.getModel().getElementAt(i).toString());
			}
			if (!lists.contains(incomingUser)) {
				lists.add(incomingUser);
			}
			this.jList1.setListData((String[]) lists.toArray());
			break;
		case (Protocol.LOGOFFNOTIFY):
			String leavingUser = line.substring(4);
			msgToCliOut = "[System]  User " + leavingUser
					+ " leaves the chat room.";
			// remove the cached address of the new logged-in user.
			client.removeAddressOf(leavingUser);
			// refresh the online list.
			ArrayList<String> lists2 = new ArrayList<String>();
			for (int i = 0; i < jList1.getModel().getSize(); i++) {
				lists2.add(this.jList1.getModel().getElementAt(i).toString());
			}
			if (lists2.contains(leavingUser)) {
				lists2.remove(leavingUser);
			}
			this.jList1.setListData(lists2.toArray());
			break;
		case (Protocol.LOGINOTHERPLACE):
			msgToCliOut = "[System]  Your account is logged in in other place.";
			try {
				this.client.getReceiverSocket().close();
			} catch (Exception e) {
			}
			closeGUI();
			break;
		case (Protocol.HOSTADDR):
			String[] hostaddrMsgComponents = line.split(" ");
			if (hostaddrMsgComponents[2].equals("NULL")) {
				msgToCliOut = "[System]  Failed to get IP of user "
						+ hostaddrMsgComponents[1];
			} else if (hostaddrMsgComponents[2].equals("OFFLINE")) {
				msgToCliOut = "[System]  user " + hostaddrMsgComponents[1]
						+ " is offline.";
			} else {
				msgToCliOut = "[System]  Address of user "
						+ hostaddrMsgComponents[1] + ": "
						+ hostaddrMsgComponents[2]
						+ ". You can send private message now.";
				client.addToAddressMap(hostaddrMsgComponents[1],
						hostaddrMsgComponents[2]);
				client.addToUUIDMap(hostaddrMsgComponents[1],
						UUID.fromString(hostaddrMsgComponents[3]));
			}
			break;
		case (Protocol.ADDRESSREQUESTED):
			String requester = line.substring(4);
			UUID uuid = UUID.randomUUID(); // generate a identification uuid for
											// P2P msg exchange.
			this.messageReceiver.addToUUIDMap(uuid, requester);
			int n = JOptionPane.showConfirmDialog(this, "User " + requester
					+ " is requesting your address. Accept or not?",
					"IP request", JOptionPane.YES_NO_OPTION);
			if (n == JOptionPane.YES_OPTION) {
				this.client.sendToServer(Protocol.ADDRREQAGREE + " "
						+ requester + " " + uuid.toString());
			} else if (n == JOptionPane.NO_OPTION) {
				this.client
						.sendToServer(Protocol.ADDRREQDENY + " " + requester);
			}
			break;
		default:
			msgToCliOut = "[System]  Failed to parse server message: " + line;
			break;
		}
		if (msgToCliOut != null) {
			display(msgToCliOut);
		}
	}

	/**
	 * Disable GUI when logged out.
	 */
	public void closeGUI() {
		try {
			client.getReceiverSocket().close();
		} catch (Exception e) {
		}
		this.jButton1.setEnabled(false);
		this.jCheckBox1.setEnabled(false);
		this.jList1.setEnabled(false);
		this.jMenuBar1.setEnabled(false);
		this.jTextArea1.setEnabled(false);
		this.jTextArea2.setEnabled(false);
		this.jTextField1.setEnabled(false);
		this.jRadioButton1.setEnabled(false);
		this.jRadioButton2.setEnabled(false);
		this.jMenu1.setEnabled(false);
		this.jMenu2.setEnabled(false);
		this.jMenu3.setEnabled(false);
		this.jLabel1.setEnabled(false);
	}

	/**
	 * ActionListener of the "broadcast" radio button.
	 */
	private void jRadioButton1ActionPerformed(java.awt.event.ActionEvent evt) {
		this.jCheckBox1.setEnabled(false);
		this.jTextField1.setEnabled(false);
	}

	/**
	 * ActionListener of the "private" radio button.
	 */
	private void jRadioButton2ActionPerformed(java.awt.event.ActionEvent evt) {
		this.jCheckBox1.setEnabled(true);
		this.jTextField1.setEnabled(true);
	}

	/**
	 * ActionListener of the "send" button.
	 */
	private void jButton1MouseReleased(java.awt.event.MouseEvent evt) {
		String content = this.jTextArea1.getText();
		process(content);
	}

	/**
	 * ActionListener of the ENTER key when writing messages.
	 */
	private void jTextArea1KeyReleased(java.awt.event.KeyEvent evt) {
		if (evt.getKeyCode() == KeyEvent.VK_ENTER) {
			String content = this.jTextArea1.getText();
			content = content.substring(0, content.length() - 1);
			process(content);
		}
	}

	/**
	 * ActionListener of the "logout" menu button.
	 */
	private void jMenuItem1ActionPerformed(java.awt.event.ActionEvent evt) {
		this.client.processCliInputEntity("logout");
		display("[System]  Logged out from the chat room. Bye~");
		closeGUI();
	}

	/**
	 * ActionListener of the "block" menu button.
	 */
	private void jMenuItem2ActionPerformed(java.awt.event.ActionEvent evt) {
		String name = JOptionPane.showInputDialog(this,
				"Enter the username add to black list", "Block User",
				JOptionPane.OK_CANCEL_OPTION);
		if (name == null) {
			return;
		} else if (name.equals(this.client.getUserName())) {
			display("[System] You cannot block yourself.");
		} else {
			this.client.processCliInputEntity("block " + name);
		}
	}

	/**
	 * ActionListener of the "unblock" menu button.
	 */
	private void jMenuItem3ActionPerformed(java.awt.event.ActionEvent evt) {
		String name = JOptionPane.showInputDialog(this,
				"Enter the username remove from black list", "Unblock User",
				JOptionPane.OK_CANCEL_OPTION);
		if (name == null) {
			return;
		} else if (name.equals(this.client.getUserName())) {
			display("[System] You cannot unblock yourself.");
		} else {
			this.client.processCliInputEntity("unblock " + name);
		}
	}

	/**
	 * ActionListener of the "user manual" menu button.
	 */
	private void jMenuItem4ActionPerformed(java.awt.event.ActionEvent evt) {
		JOptionPane.showMessageDialog(this, ClientConfig.help_gui,
				"User Manual", JOptionPane.INFORMATION_MESSAGE);
	}

	/**
	 * ActionListener of the "author information" menu button.
	 */
	private void jMenuItem5ActionPerformed(java.awt.event.ActionEvent evt) {
		JOptionPane.showMessageDialog(this,
				ClientConfig.author_info.split("/n"), "About me",
				JOptionPane.INFORMATION_MESSAGE);
	}

	/**
	 * Method to display message on the message frame.
	 */
	public void display(String msg) {
		this.jTextArea2.append(msg + enter);
		this.jTextArea2.setCaretPosition(jTextArea2.getText().length());
		JScrollBar sbar = this.jScrollPane2.getVerticalScrollBar();
		sbar.setValue(sbar.getMaximum());
	}

	/**
	 * Method to process user input. Either to send broadcast message or private
	 * message.
	 */
	private void process(String content) {
		this.jTextArea1.setText("");
		if(content==null||content.equals(""))
			return;
		if (this.jRadioButton1.isSelected()) {
			this.client.processCliInputEntity("broadcast " + content);
			display("[To All]  YOU: " + content);
		} else if (this.jRadioButton2.isSelected()) {
			String host = this.jTextField1.getText();
			if (host.equals("")) {
				display("[System]  Please enter the name of the receiver.");
			} else if (host.equals(this.client.getUserName())) {
				display("[System]  You don't need to send msg to yourself.");
			} else if (this.jCheckBox1.isSelected()) {
				this.client.processCliInputEntity("message " + host + " "
						+ content);
				display("[To " + host + "][Thru server]  YOU: " + content);
			} else if (this.client.getAddressOf(host) == null) {
				int n = JOptionPane.showConfirmDialog(this,
						"You don't have the address of " + host
								+ ". Would you want to request it?",
						"Request IP", JOptionPane.YES_NO_OPTION);
				if (n == JOptionPane.YES_OPTION) {
					this.client.processCliInputEntity("getaddress " + host);
					display("[System]  Address request sent. Please wait.");
				} else if (n == JOptionPane.NO_OPTION) {
					display("[System]  You can't send private message to "
							+ host + " without  his address.");
				}
			} else {
				this.client.processCliInputEntity("private " + host + " "
						+ content);
				display("[To " + host + "][Private]  YOU: " + content);
			}
		}

	}

	private javax.swing.ButtonGroup buttonGroup1;
	private javax.swing.JButton jButton1;
	private javax.swing.JCheckBox jCheckBox1;
	private javax.swing.JLabel jLabel1;
	@SuppressWarnings("rawtypes")
	private javax.swing.JList jList1;
	private javax.swing.JMenu jMenu1;
	private javax.swing.JMenu jMenu2;
	private javax.swing.JMenu jMenu3;
	private javax.swing.JMenuBar jMenuBar1;
	private javax.swing.JMenuItem jMenuItem1;
	private javax.swing.JMenuItem jMenuItem2;
	private javax.swing.JMenuItem jMenuItem3;
	private javax.swing.JMenuItem jMenuItem4;
	private javax.swing.JMenuItem jMenuItem5;
	private javax.swing.JRadioButton jRadioButton1;
	private javax.swing.JRadioButton jRadioButton2;
	private javax.swing.JScrollPane jScrollPane2;
	private javax.swing.JScrollPane jScrollPane3;
	private javax.swing.JScrollPane jScrollPane5;
	private javax.swing.JTextArea jTextArea1;
	private javax.swing.JTextArea jTextArea2;
	private javax.swing.JTextField jTextField1;

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void initComponents() {
		buttonGroup1 = new javax.swing.ButtonGroup();
		jButton1 = new javax.swing.JButton();
		jRadioButton1 = new javax.swing.JRadioButton();
		jRadioButton2 = new javax.swing.JRadioButton();
		jCheckBox1 = new javax.swing.JCheckBox();
		jScrollPane3 = new javax.swing.JScrollPane();
		jList1 = new javax.swing.JList();
		jScrollPane5 = new javax.swing.JScrollPane();
		jTextArea1 = new javax.swing.JTextArea();
		jScrollPane2 = new javax.swing.JScrollPane();
		jTextArea2 = new javax.swing.JTextArea();
		jLabel1 = new javax.swing.JLabel();
		jTextField1 = new javax.swing.JTextField();
		jMenuBar1 = new javax.swing.JMenuBar();
		jMenu1 = new javax.swing.JMenu();
		jMenuItem1 = new javax.swing.JMenuItem();
		jMenu2 = new javax.swing.JMenu();
		jMenuItem2 = new javax.swing.JMenuItem();
		jMenuItem3 = new javax.swing.JMenuItem();
		jMenu3 = new javax.swing.JMenu();
		jMenuItem4 = new javax.swing.JMenuItem();
		jMenuItem5 = new javax.swing.JMenuItem();

		setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
		setTitle("ChatRoom4009");
		setResizable(false);

		jButton1.setText("Send/[Enter]");
		jButton1.addMouseListener(new java.awt.event.MouseAdapter() {
			public void mouseReleased(java.awt.event.MouseEvent evt) {
				jButton1MouseReleased(evt);
			}
		});

		buttonGroup1.add(jRadioButton1);
		jRadioButton1.setSelected(true);
		jRadioButton1.setText("broadcast");
		jRadioButton1.setAutoscrolls(true);
		jRadioButton1.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				jRadioButton1ActionPerformed(evt);
			}
		});

		buttonGroup1.add(jRadioButton2);
		jRadioButton2.setText("personal");
		jRadioButton2.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				jRadioButton2ActionPerformed(evt);
			}
		});

		jCheckBox1.setSelected(true);
		jCheckBox1.setText("thru server");
		jCheckBox1.setEnabled(false);

		jList1.setModel(new javax.swing.AbstractListModel() {
			/**
			 * default serial version UID.
			 */
			private static final long serialVersionUID = 1L;
			String[] strings = { "" };

			public int getSize() {
				return strings.length;
			}

			public String getElementAt(int i) {
				return strings[i];
			}
		});
		jScrollPane3.setViewportView(jList1);

		jScrollPane5.setHorizontalScrollBar(null);

		jTextArea1.setColumns(20);
		jTextArea1.setRows(5);
		jTextArea1.setLineWrap(true);
		jTextArea1.addKeyListener(new java.awt.event.KeyAdapter() {
			public void keyReleased(java.awt.event.KeyEvent evt) {
				jTextArea1KeyReleased(evt);
			}
		});
		jScrollPane5.setViewportView(jTextArea1);

		jScrollPane2.setHorizontalScrollBar(null);

		jTextArea2.setEditable(false);
		jTextArea2.setColumns(20);
		jTextArea2.setRows(5);
		jTextArea2.setLineWrap(true);
		jTextArea2.setWrapStyleWord(true);
		jScrollPane2.setViewportView(jTextArea2);

		jLabel1.setText("Online Users:");

		jTextField1.setText("username");
		jTextField1.setEnabled(false);

		jMenu1.setText("System");

		jMenuItem1.setText("LogOut");
		jMenuItem1.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				jMenuItem1ActionPerformed(evt);
			}
		});
		jMenu1.add(jMenuItem1);

		jMenuBar1.add(jMenu1);

		jMenu2.setText("BlackList");

		jMenuItem2.setText("Block User");
		jMenuItem2.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				jMenuItem2ActionPerformed(evt);
			}
		});
		jMenu2.add(jMenuItem2);

		jMenuItem3.setText("Unblock User");
		jMenuItem3.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				jMenuItem3ActionPerformed(evt);
			}
		});
		jMenu2.add(jMenuItem3);

		jMenuBar1.add(jMenu2);

		jMenu3.setText("Help");

		jMenuItem4.setText("User Manual");
		jMenuItem4.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				jMenuItem4ActionPerformed(evt);
			}
		});
		jMenu3.add(jMenuItem4);

		jMenuItem5.setText("About Author");
		jMenuItem5.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				jMenuItem5ActionPerformed(evt);
			}
		});
		jMenu3.add(jMenuItem5);

		jMenuBar1.add(jMenu3);

		setJMenuBar(jMenuBar1);

		javax.swing.GroupLayout layout = new javax.swing.GroupLayout(
				getContentPane());
		getContentPane().setLayout(layout);
		layout.setHorizontalGroup(layout
				.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
				.addGroup(
						layout.createSequentialGroup()
								.addGap(34, 34, 34)
								.addGroup(
										layout.createParallelGroup(
												javax.swing.GroupLayout.Alignment.LEADING)
												.addGroup(
														layout.createSequentialGroup()
																.addComponent(
																		jScrollPane2,
																		javax.swing.GroupLayout.PREFERRED_SIZE,
																		555,
																		javax.swing.GroupLayout.PREFERRED_SIZE)
																.addGap(39, 39,
																		39)
																.addGroup(
																		layout.createParallelGroup(
																				javax.swing.GroupLayout.Alignment.LEADING)
																				.addComponent(
																						jLabel1)
																				.addComponent(
																						jScrollPane3,
																						javax.swing.GroupLayout.PREFERRED_SIZE,
																						92,
																						javax.swing.GroupLayout.PREFERRED_SIZE)))
												.addGroup(
														layout.createSequentialGroup()
																.addComponent(
																		jRadioButton1)
																.addGap(6, 6, 6)
																.addComponent(
																		jRadioButton2)
																.addGap(6, 6, 6)
																.addComponent(
																		jCheckBox1)
																.addPreferredGap(
																		javax.swing.LayoutStyle.ComponentPlacement.RELATED)
																.addComponent(
																		jTextField1,
																		javax.swing.GroupLayout.PREFERRED_SIZE,
																		121,
																		javax.swing.GroupLayout.PREFERRED_SIZE))
												.addGroup(
														layout.createSequentialGroup()
																.addComponent(
																		jScrollPane5,
																		javax.swing.GroupLayout.PREFERRED_SIZE,
																		555,
																		javax.swing.GroupLayout.PREFERRED_SIZE)
																.addGap(39, 39,
																		39)
																.addComponent(
																		jButton1,
																		javax.swing.GroupLayout.PREFERRED_SIZE,
																		92,
																		javax.swing.GroupLayout.PREFERRED_SIZE)))
								.addContainerGap(32, Short.MAX_VALUE)));
		layout.setVerticalGroup(layout
				.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
				.addGroup(
						layout.createSequentialGroup()
								.addGap(23, 23, 23)
								.addGroup(
										layout.createParallelGroup(
												javax.swing.GroupLayout.Alignment.LEADING)
												.addComponent(
														jScrollPane2,
														javax.swing.GroupLayout.PREFERRED_SIZE,
														329,
														javax.swing.GroupLayout.PREFERRED_SIZE)
												.addGroup(
														layout.createSequentialGroup()
																.addGap(6, 6, 6)
																.addComponent(
																		jLabel1)
																.addGap(4, 4, 4)
																.addComponent(
																		jScrollPane3,
																		javax.swing.GroupLayout.PREFERRED_SIZE,
																		299,
																		javax.swing.GroupLayout.PREFERRED_SIZE)))
								.addGap(6, 6, 6)
								.addGroup(
										layout.createParallelGroup(
												javax.swing.GroupLayout.Alignment.LEADING)
												.addComponent(jRadioButton1)
												.addComponent(jRadioButton2)
												.addGroup(
														layout.createParallelGroup(
																javax.swing.GroupLayout.Alignment.BASELINE)
																.addComponent(
																		jCheckBox1)
																.addComponent(
																		jTextField1,
																		javax.swing.GroupLayout.PREFERRED_SIZE,
																		javax.swing.GroupLayout.DEFAULT_SIZE,
																		javax.swing.GroupLayout.PREFERRED_SIZE)))
								.addGap(10, 10, 10)
								.addGroup(
										layout.createParallelGroup(
												javax.swing.GroupLayout.Alignment.LEADING)
												.addComponent(
														jScrollPane5,
														javax.swing.GroupLayout.PREFERRED_SIZE,
														javax.swing.GroupLayout.DEFAULT_SIZE,
														javax.swing.GroupLayout.PREFERRED_SIZE)
												.addGroup(
														layout.createSequentialGroup()
																.addGap(3, 3, 3)
																.addComponent(
																		jButton1,
																		javax.swing.GroupLayout.PREFERRED_SIZE,
																		84,
																		javax.swing.GroupLayout.PREFERRED_SIZE)))
								.addContainerGap(18, Short.MAX_VALUE)));

		pack();
		int width = this.getWidth();
		int height = this.getHeight();
		Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
		this.setBounds((d.width - width) / 2, (d.height - height) / 2, width,
				height);
	}

}
