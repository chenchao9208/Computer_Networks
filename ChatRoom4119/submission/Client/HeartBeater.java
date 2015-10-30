/**
 * The Heart Beater class.
 * 
 * @author Chao
 *
 */
public class HeartBeater extends Thread {
	private Client cliApp;

	public HeartBeater(Client cliApp) {
		this.cliApp = cliApp;
	}

	/**
	 * Send heart beat signal to the server periodly to keep logged in.
	 */
	public void run() {
		while (true) {
			try {
				Thread.sleep(ClientConfig.HEATBEAT * 1000);
			} catch (Exception e) {
			}
			cliApp.sendToServer(Protocol.HEARTBEAT);

		}
	}
}
