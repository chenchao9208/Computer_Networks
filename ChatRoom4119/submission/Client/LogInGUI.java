import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;

/**
 * The Log In GUI identity
 *
 * @author Chao Chen cc3736
 */

public class LogInGUI extends javax.swing.JFrame {
	/**
	 * Serial Version UID given default.
	 */
	private static final long serialVersionUID = 1L;
	Client client;

	public LogInGUI() {
		initComponents();
	}

	/**
	 * Constructor with client app.
	 * 
	 * @param client
	 */
	public LogInGUI(Client client) {
		this.client = client;
		initComponents();

	}

	private void initComponents() {
		jButton1 = new javax.swing.JButton();
		jLabel1 = new javax.swing.JLabel();
		jLabel2 = new javax.swing.JLabel();
		jTextField1 = new javax.swing.JTextField();
		jLabel3 = new javax.swing.JLabel();
		jPasswordField1 = new javax.swing.JPasswordField();

		setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
		setTitle("Chat Room 4009-Log In");
		setResizable(false);

		jButton1.setText("Log In");
		jButton1.addMouseListener(new java.awt.event.MouseAdapter() {
			public void mouseReleased(java.awt.event.MouseEvent evt) {
				jButton1MouseReleased(evt);
			}
		});

		jLabel1.setText("Username:");

		jLabel2.setText("Password:");

		jLabel3.setForeground(new java.awt.Color(255, 0, 0));

		jPasswordField1.addKeyListener(new java.awt.event.KeyAdapter() {
			public void keyReleased(java.awt.event.KeyEvent evt) {
				jPasswordField1KeyReleased(evt);
			}
		});

		javax.swing.GroupLayout layout = new javax.swing.GroupLayout(
				getContentPane());
		getContentPane().setLayout(layout);
		layout.setHorizontalGroup(layout
				.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
				.addGroup(
						layout.createSequentialGroup()
								.addGap(44, 44, 44)
								.addGroup(
										layout.createParallelGroup(
												javax.swing.GroupLayout.Alignment.LEADING,
												false)
												.addComponent(
														jButton1,
														javax.swing.GroupLayout.DEFAULT_SIZE,
														javax.swing.GroupLayout.DEFAULT_SIZE,
														Short.MAX_VALUE)
												.addGroup(
														layout.createSequentialGroup()
																.addGroup(
																		layout.createParallelGroup(
																				javax.swing.GroupLayout.Alignment.LEADING)
																				.addGroup(
																						javax.swing.GroupLayout.Alignment.TRAILING,
																						layout.createSequentialGroup()
																								.addComponent(
																										jLabel2)
																								.addGap(18,
																										18,
																										18))
																				.addGroup(
																						layout.createSequentialGroup()
																								.addComponent(
																										jLabel1)
																								.addGap(15,
																										15,
																										15)))
																.addGroup(
																		layout.createParallelGroup(
																				javax.swing.GroupLayout.Alignment.LEADING,
																				false)
																				.addComponent(
																						jTextField1,
																						javax.swing.GroupLayout.DEFAULT_SIZE,
																						158,
																						Short.MAX_VALUE)
																				.addComponent(
																						jPasswordField1)))
												.addComponent(
														jLabel3,
														javax.swing.GroupLayout.DEFAULT_SIZE,
														javax.swing.GroupLayout.DEFAULT_SIZE,
														Short.MAX_VALUE))
								.addContainerGap(57, Short.MAX_VALUE)));
		layout.setVerticalGroup(layout
				.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
				.addGroup(
						javax.swing.GroupLayout.Alignment.TRAILING,
						layout.createSequentialGroup()
								.addGap(13, 13, 13)
								.addComponent(jLabel3,
										javax.swing.GroupLayout.PREFERRED_SIZE,
										19,
										javax.swing.GroupLayout.PREFERRED_SIZE)
								.addPreferredGap(
										javax.swing.LayoutStyle.ComponentPlacement.RELATED)
								.addGroup(
										layout.createParallelGroup(
												javax.swing.GroupLayout.Alignment.BASELINE)
												.addComponent(jLabel1)
												.addComponent(
														jTextField1,
														javax.swing.GroupLayout.PREFERRED_SIZE,
														javax.swing.GroupLayout.DEFAULT_SIZE,
														javax.swing.GroupLayout.PREFERRED_SIZE))
								.addPreferredGap(
										javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
								.addGroup(
										layout.createParallelGroup(
												javax.swing.GroupLayout.Alignment.BASELINE)
												.addComponent(jLabel2)
												.addComponent(
														jPasswordField1,
														javax.swing.GroupLayout.PREFERRED_SIZE,
														javax.swing.GroupLayout.DEFAULT_SIZE,
														javax.swing.GroupLayout.PREFERRED_SIZE))
								.addPreferredGap(
										javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
								.addComponent(jButton1)
								.addContainerGap(15, Short.MAX_VALUE)));

		pack();
		int width = this.getWidth();
		int height = this.getHeight();
		Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
		this.setBounds((d.width - width) / 2, (d.height - height) / 2, width,
				height);
	}

	/**
	 * ActionListener of the Enter key to perform log in.
	 */
	private void jPasswordField1KeyReleased(java.awt.event.KeyEvent evt) {
		if (evt.getKeyCode() == KeyEvent.VK_ENTER) {
			loginAction();
		}
	}

	/**
	 * ActionListener of the "LogIn" button.
	 */
	private void jButton1MouseReleased(java.awt.event.MouseEvent evt) {
		loginAction();
	}

	/**
	 * Method to perform LogIn action.
	 */
	private void loginAction() {
		String userName = this.jTextField1.getText();
		String passWord = String.valueOf(this.jPasswordField1.getPassword());
		int reply = this.client.logIn(userName, passWord);
		if (reply == -1) {
			this.setVisible(false);
			javax.swing.JOptionPane
					.showMessageDialog(
							null,
							"Due to multiple login failures, your account has been blocked. Please try again later.");
			System.exit(0);
		} else if (reply == 0) {
			this.jTextField1.setText("");
			this.jPasswordField1.setText("");
			this.jTextField1.repaint();
			this.jPasswordField1.repaint();
			this.jLabel3.setText("Invalid username or password. Try again.");
			this.jLabel3.repaint();
		} else if (reply == 1) {
			this.setVisible(false);
			this.client.startClientWithGUI();
			new ChatRoomGUI(client).setVisible(true);
		} else if(reply == -100){
			javax.swing.JOptionPane
					.showMessageDialog(
							null,
							"Cannot connect to the server. Check the server address or try again later.");
		}
	}

	private javax.swing.JButton jButton1;
	private javax.swing.JLabel jLabel1;
	private javax.swing.JLabel jLabel2;
	private javax.swing.JLabel jLabel3;
	private javax.swing.JPasswordField jPasswordField1;
	private javax.swing.JTextField jTextField1;

}
