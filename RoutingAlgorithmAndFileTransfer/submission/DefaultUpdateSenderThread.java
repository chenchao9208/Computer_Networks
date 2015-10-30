import java.util.Date;

/**
 * The thread to periodically send UPDATE_ROUTE message to neighbors.
 * 
 * @author Chao Chen
 * @version 1.0
 */
public class DefaultUpdateSenderThread extends Thread {
	BFKernel bfInfo;

	public DefaultUpdateSenderThread(BFKernel bfInfo) {
		this.bfInfo = bfInfo;
	}

	public void run() {
		while (true) {
			long currentTime = new Date().getTime();
			long sleepTime;

			if (currentTime - bfInfo.lastUpdateSendTime >= bfInfo.timeout * 1000) {
				//last time to send UPDATE_ROUTE was TIMEOUT ago, then send
				//UPDATE_ROUTE now.
				bfInfo.sendVectorsToNeighbors();
				sleepTime = bfInfo.timeout * 1000;
			} else {
				//during the last sleep period, BellmanFord distance vector changing
				//has triggered an unregular UPDATE_ROUTE, then the next time to
				//send UPDATE_ROUTE should be recalculated.
				sleepTime = bfInfo.timeout * 1000 - currentTime
						+ bfInfo.lastUpdateSendTime;
			}
			try {
				Thread.sleep(sleepTime);
			} catch (Exception e) {
			}
		}
	}
}
