import java.util.Date;

/**
 * The thread to check and kick out dead neighbor node.
 * 
 * @author Chao Chen
 * @version 1.0
 *
 */
public class TimeoutCheckerThread extends Thread {
	BFKernel kernel;

	public TimeoutCheckerThread(BFKernel kernel) {
		this.kernel = kernel;
	}

	public void run() {
		while (true) {
			kernel.checkDeadNode();

			// The next time this thread should wake up and check dead node is
			// related to
			// the neighbor who sends the UPDATE_ROUTE earliest.
			long nextCheckTimestamp = Long.MAX_VALUE;
			for (String neighbor : kernel.neighbors.keySet()) {
				if (kernel.neighbors.get(neighbor) != PA2Util.INFINTE_COST) {
					// ignore already-dead node
					nextCheckTimestamp = Math.min(nextCheckTimestamp,
							kernel.lastHeardTimeTable.get(neighbor) + 3
									* kernel.timeout * 1000);
				}
			}
			long currentTime = new Date().getTime();
			// if there is no active neighbor, sleep for 3*timeout, otherwise
			// calculate the remaining time to the next wake up & check time stamp.
			long timeout = (nextCheckTimestamp == Long.MAX_VALUE ? 3 * kernel.timeout * 1000
					: nextCheckTimestamp - currentTime);

			try {
				Thread.sleep(timeout);
			} catch (Exception e) {
			}

		}
	}
}
