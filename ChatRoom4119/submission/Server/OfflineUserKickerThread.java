/**
 * OfflineUserKicker thread. Periodically wake up to check users' last alive
 * time. And kick out who is muted for a long time.
 * 
 * @author Chao Chen cc3736
 *
 */
public class OfflineUserKickerThread extends Thread {
	InfoCenter infoCenter;

	public OfflineUserKickerThread(InfoCenter infoCenter) {
		this.infoCenter = infoCenter;
	}

	public void run() {
		while (true) {
			try {
				Thread.sleep(ServerConfig.TIMEOUT * 1000);
			} catch (Exception e) {
			}
			this.infoCenter.checkAliveUser();
		}
	}
}
