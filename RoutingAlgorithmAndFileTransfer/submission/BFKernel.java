import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The core data structure of Bellman Ford client. All the informations are
 * stored in this data structure. It also contains some method to compose and
 * send packet.
 * 
 * @author Chao Chen
 * @version 1.0
 */
public class BFKernel {
	// all the threads of this program is listed below
	DefaultUpdateSenderThread dfUpdateSender;
	TimeoutCheckerThread timeoutChecker;
	MessageReceiverThread msgReceiver;
	CLIProcesserThread cliProcesser;

	String myAddress; // ip:port i.e. 192.168.0.8:1234 or 127.0.0.1:1234
	int port = -1; // port number of the receiving socket.
	DatagramSocket receiveSocket; // udp socket to receive message.
	int timeout = -1; // timeout interval to send UPDATE_ROUTE message.

	/**
	 * <fileName, offset>.
	 * 
	 * store the name of file under receiving and the total size of received
	 * file data.
	 */
	ConcurrentHashMap<String, Long> fileReceivingMap = new ConcurrentHashMap<String, Long>();

	/**
	 * <neighborAddress, cost>.
	 * 
	 * store the address of the neighbor nodes and the cost of the direct links
	 * to them.
	 */
	ConcurrentHashMap<String, Double> neighbors = new ConcurrentHashMap<String, Double>();

	/**
	 * <neighborAddress, cost>.
	 * 
	 * store the address of the neighbor nodes and the initial cost of the
	 * direct links read from the configuration file. Used to restore link cost
	 * when LINKUP.
	 */
	ConcurrentHashMap<String, Double> neighborsBackup = new ConcurrentHashMap<String, Double>();

	/**
	 * <destinationAddress, nextHop>.
	 * 
	 * store the routing table. if the destination is unreachable, the nextHop
	 * is defined as PA2Util.UNREACHABLE
	 */
	ConcurrentHashMap<String, String> routingTable = new ConcurrentHashMap<String, String>();

	/**
	 * <neighborAddress, proxyAddress>.
	 * 
	 * store the proxy table. if stored, all the data transmission routed via
	 * the neighbor will be sent to the corresponding proxy.
	 */
	ConcurrentHashMap<String, String> proxyMap = new ConcurrentHashMap<String, String>();

	/**
	 * <neighborAddress|myAddress, <destinationAddress, cost>>.
	 * 
	 * Implementation of Bellman Ford cost table described in the course slides
	 * in HashMap form
	 */
	ConcurrentHashMap<String, ConcurrentHashMap<String, Double>> bfCostTable = new ConcurrentHashMap<String, ConcurrentHashMap<String, Double>>();

	/**
	 * <neighborAddress, lastHeardTime>.
	 * 
	 * record the time when we receive the latest packet from the neighbor. used
	 * by the timeout dead node checker
	 */
	ConcurrentHashMap<String, Long> lastHeardTimeTable = new ConcurrentHashMap<String, Long>();

	/**
	 * the time when the client sent out the latest UPDATE_ROUTE message to
	 * neighbors.
	 * 
	 * used by the default update sender.
	 */
	volatile long lastUpdateSendTime;

	/**
	 * Constructor
	 * 
	 * @param fileName
	 *            configration file
	 */
	public BFKernel(String fileName) {
		try {
			this.initialize(fileName);
			receiveSocket = new DatagramSocket(port);
		} catch (SocketException se) {
			System.out.println("Failed to use port " + port
					+ ". Try another port number.");
			System.exit(0);
		} catch (FileNotFoundException fe) {
			System.out.println("File " + fileName + " is not found.");
			System.exit(0);
		} catch (UnknownHostException ue) {
			System.out.println("Unable to get IP address of localhost.");
			System.exit(0);
		} catch (IOException ie) {
			System.out.println("Error occurs when reading file " + fileName);
			System.exit(0);
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(0);
		}
	}

	/**
	 * To read parameters from the configuration file
	 * 
	 * @param fileName
	 * @throws UnknownHostException
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public void initialize(String fileName) throws UnknownHostException,
			FileNotFoundException, IOException, Exception {
		File configFile = new File(fileName);
		if (!configFile.canRead()) {
			System.out.println("File " + fileName + "is unreadable.");
			System.exit(0);
		}

		BufferedReader br = new BufferedReader(new FileReader(configFile));
		String line;
		String[] pair;

		line = br.readLine();
		pair = line.split(" ");

		port = Integer.valueOf(pair[0]);
		myAddress = InetAddress.getLocalHost().getHostAddress() + ":" + port;

		timeout = Integer.valueOf(pair[1]);
		bfCostTable.put(myAddress, new ConcurrentHashMap<String, Double>());
		String neighborAddress = "";
		long currentTime = new Date().getTime();
		while ((line = br.readLine()) != null) {
			if(line.length()==0){
				break;
			}
			pair = line.split(" ");
			neighborAddress = pair[0];
			double cost = Double.valueOf(pair[1]);

			neighbors.put(neighborAddress, cost);
			neighborsBackup.put(neighborAddress, cost);
			routingTable.put(neighborAddress, neighborAddress);
			bfCostTable.get(myAddress).put(neighborAddress, cost);
			bfCostTable.put(neighborAddress,
					new ConcurrentHashMap<String, Double>());
			lastHeardTimeTable.put(neighborAddress, currentTime);
		}
		br.close();

		if (neighborAddress.contains("127.0.0.1")) {
			// for the localhost simulated network environment, convert my
			// address to the same format
			String correctAddress = "127.0.0.1:" + port;
			bfCostTable.put(correctAddress, bfCostTable.get(myAddress));
			bfCostTable.remove(myAddress);
			myAddress = correctAddress;
		}

	}

	/**
	 * get the next hop to send the data towards the destination.
	 * 
	 * the method first acquire the next hop neighbor from the routing table,
	 * then check if the neighbor is in the proxy table, if yes, the next hop is
	 * the proxy, otherwise is the neighbor
	 * 
	 * @param dst
	 *            destinationAddress
	 * @return address of next hop
	 */
	public String getNextHop(String dst) {
		String neighbor = routingTable.get(dst);
		if (proxyMap.containsKey(neighbor)) {
			return proxyMap.get(neighbor);
		} else {
			return neighbor;
		}
	}

	/**
	 * get the distance vector table
	 * 
	 * @return distance vector table
	 */
	public String getDVTable() {
		StringBuilder sb = new StringBuilder();
		sb.append(new Date().toString());
		sb.append(" Distance vector list is:");
		for (String dst : routingTable.keySet()) {
			sb.append("\nDestination = ");
			sb.append(dst);
			sb.append(", Cost = ");
			double cost = bfCostTable.get(myAddress).get(dst);
			if (cost < PA2Util.INFINTE_COST) {
				sb.append(cost);
			} else {
				sb.append(PA2Util.INFINITY_TAG);
			}
			sb.append(", Link = (");
			sb.append(routingTable.get(dst));
			sb.append(")");
		}
		return sb.toString();
	}

	/**
	 * encapsulate the message in UDP segment and send it DIRECTLY to the
	 * destination host
	 * 
	 * @param hostAddress
	 * @param msg
	 */
	public void sendToHost(String hostAddress, PA2TCP msg) {
		try {
			DatagramSocket clientSocket = new DatagramSocket();
			InetAddress IPAddress = InetAddress.getByName(hostAddress
					.split(":")[0]);
			int port = Integer.valueOf(hostAddress.split(":")[1]);
			byte[] sendData = msg.serialize();
			DatagramPacket sendPacket = new DatagramPacket(sendData,
					sendData.length, IPAddress, port);
			clientSocket.send(sendPacket);
			clientSocket.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * send Distance Vectors to all the active neighbors
	 */
	public void sendVectorsToNeighbors() {
		lastUpdateSendTime = new Date().getTime();
		for (String neighbor : neighbors.keySet()) {
			if (neighbors.get(neighbor) == PA2Util.INFINTE_COST) {
				continue;
			}
			PA2TCP msg = composeVectorMsg(neighbor);
			sendToHost(neighbor, msg);
		}
	}

	/**
	 * send LINK_DOWN message to a specific neighbor
	 * 
	 * @param dst
	 *            neighbor's address
	 */
	public void sendLinkDownToNeighbor(String dst) {
		PA2TCP msg = composeLinkDownMsg(dst);
		sendToHost(dst, msg);
	}

	/**
	 * send LINK_UP message to a specific neighbor
	 * 
	 * @param dst
	 *            neighbor's address
	 */
	public void sendLinkUpToNeighbor(String dst) {
		PA2TCP msg = composeLinkUpMsg(dst);
		sendToHost(dst, msg);
	}

	/**
	 * send CHANGE_COST message to a specific neighbor
	 * 
	 * @param dst
	 *            neighbor's address
	 * @param newCost
	 *            the new link cost value
	 */
	public void sendChangeCostToNeighbor(String dst, double newCost) {
		PA2TCP msg = composeChangeCostMsg(dst, newCost);
		sendToHost(dst, msg);
	}

	/**
	 * send an ACK message to a host
	 * 
	 * used in file transfer
	 * 
	 * @param dst
	 */
	public void sendACKToHost(String dst) {
		PA2TCP msg = composeTransferACKMsg(dst);
		sendToHost(getNextHop(dst), msg);
	}

	/**
	 * send a file data message to a host
	 * 
	 * used in file transfer
	 * 
	 * @param dst
	 * @param pf
	 *            PA2FTP instance containing the file data
	 */
	public void sendFileDataToHost(String dst, PA2FTP pf) {
		PA2TCP msg = composeTransferDataMsg(dst, pf);
		sendToHost(getNextHop(dst), msg);
	}

	/**
	 * PA2TCP packet composer,contains file data
	 * 
	 * @param dst
	 * @param pf
	 * @return
	 */
	public PA2TCP composeTransferDataMsg(String dst, PA2FTP pf) {
		return new PA2TCP(PA2Util.TRANSFER_SEND_TAG, myAddress, dst, pf);
	}

	/**
	 * PA2TCP packet composer, contains ACK
	 * 
	 * @param dst
	 * @return
	 */
	public PA2TCP composeTransferACKMsg(String dst) {
		return new PA2TCP(PA2Util.TRANSFER_ACK_TAG, myAddress, dst,
				(String) null);
	}

	/**
	 * PA2TCP packet composer, contains distance vectors
	 * 
	 * @param neighbor
	 * @return
	 */
	public PA2TCP composeVectorMsg(String neighbor) {
		String vectorTable = "";
		for (String dst : routingTable.keySet()) {
			double cost;
			if (routingTable.get(dst).equals(neighbor)) {
				cost = PA2Util.INFINTE_COST;
			} else {
				cost = bfCostTable.get(myAddress).get(dst);
			}
			vectorTable += dst
					+ "="
					+ (cost == PA2Util.INFINTE_COST ? PA2Util.INFINITY_TAG
							: cost) + ";";
		}
		vectorTable = vectorTable.substring(0, vectorTable.length() - 1);
		return new PA2TCP(PA2Util.UPDATE_ROUTE_TAG, myAddress, neighbor,
				vectorTable);
	}

	/**
	 * PA2TCP packet composer, contains link up tag
	 * 
	 * @param neighbor
	 * @return
	 */
	public PA2TCP composeLinkUpMsg(String neighbor) {
		return new PA2TCP(PA2Util.LINK_UP_TAG, myAddress, neighbor,
				(String) null);
	}

	/**
	 * PA2TCP packet composer, contains link down tag
	 * 
	 * @param neighbor
	 * @return
	 */
	public PA2TCP composeLinkDownMsg(String neighbor) {
		return new PA2TCP(PA2Util.LINK_DOWN_TAG, myAddress, neighbor,
				(String) null);
	}

	/**
	 * PA2TCP packet composer, contains change cost information
	 * 
	 * @param neighbor
	 * @param newCost
	 * @return
	 */
	public PA2TCP composeChangeCostMsg(String neighbor, double newCost) {
		return new PA2TCP(PA2Util.CHANGE_COST_TAG, myAddress, neighbor, newCost
				+ "");
	}

	/**
	 * update the distance vectors from the message and refresh the routing table
	 * 
	 * @param msg
	 */
	public void updateRouteFromMsg(PA2TCP msg) {
		//payload: 192.168.0.8:1111=323;192.168.0.8:2323 223

		String sourceAddress = msg.sourceAddress;
		if (!neighbors.containsKey(sourceAddress)) {
			return;
		}
		lastHeardTimeTable.put(sourceAddress, new Date().getTime());

		// if the source neighbor is set to close(INFINITE_COST), recover the link
		if (neighbors.get(sourceAddress) == PA2Util.INFINTE_COST) {
			neighbors.put(sourceAddress, neighborsBackup.get(sourceAddress));
		}

		String[] tmp = new String(msg.payloadData).split(";");
		ConcurrentHashMap<String, Double> neighborCostTable = new ConcurrentHashMap<String, Double>();
		for (int i = 0; i < tmp.length; i++) {
			String[] item = tmp[i].split("=");
			if (item[1].startsWith(PA2Util.INFINITY_TAG)) {
				neighborCostTable.put(item[0], PA2Util.INFINTE_COST);
			} else {
				neighborCostTable.put(item[0], Double.valueOf(item[1]));
			}
		}

		bfCostTable.replace(sourceAddress, neighborCostTable);

		if (refreshBFCostTable(sourceAddress)) {
			sendVectorsToNeighbors();
		}

	}

	public void shutDownLink(String sourceAddress) {
		if (!neighbors.containsKey(sourceAddress)) {
			return;
		}
		neighbors.put(sourceAddress, PA2Util.INFINTE_COST);
		if (refreshBFCostTable(myAddress)) {
			sendVectorsToNeighbors();
		}
	}

	public void shutDownLinkFromMsg(PA2TCP msg) {
		shutDownLink(msg.sourceAddress);

	}

	public void setUpLink(String sourceAddress) {
		if (!neighbors.containsKey(sourceAddress)) {
			return;
		}
		neighbors.put(sourceAddress, neighborsBackup.get(sourceAddress));
		if (refreshBFCostTable(myAddress)) {
			sendVectorsToNeighbors();
		}
	}

	public void setUpLinkFromMsg(PA2TCP msg) {
		setUpLink(msg.sourceAddress);
	}

	public void changeCost(String sourceAddress, double newCost) {
		if (!neighbors.containsKey(sourceAddress)) {
			return;
		}
		neighbors.put(sourceAddress, newCost);
		// neighborsBackup.put(sourceAddress, newCost);
		if (refreshBFCostTable(myAddress)) {
			sendVectorsToNeighbors();
		}

	}

	public void changeCostFromMsg(PA2TCP msg) {
		changeCost(msg.sourceAddress,
				Double.valueOf(new String(msg.payloadData)));
	}

	/**
	 * recalculate the distance vector
	 * @param sourceAddress
	 * @return true if the distance vector is changed; false otherwise
	 */
	public boolean refreshBFCostTable(String sourceAddress) {
		// to refresh the dst one by one
		Set<String> candidates = bfCostTable.get(sourceAddress).keySet();
		// ConcurrentHashMap<String, Double> neighborCostTable =
		// bfCostTable.get(sourceAddress);
		boolean changed = false;
		for (String dst : candidates) {
			if (dst.equals(myAddress)) {
				continue;
			}

			// initialize
			String bestRoute = neighbors.containsKey(dst) ? dst
					: PA2Util.UNREACHABLE;
			double lowestCost = neighbors.containsKey(dst) ? neighbors.get(dst)
					: PA2Util.INFINTE_COST;

			for (String neighbor : bfCostTable.keySet()) {
				if (neighbor.equals(myAddress)) {
					continue;
				}
				double firstHopCost = neighbors.get(neighbor);

				double cost1 = bfCostTable.get(neighbor).containsKey(dst) ? bfCostTable
						.get(neighbor).get(dst) + firstHopCost
						: PA2Util.INFINTE_COST;
				cost1 = Math.min(cost1, PA2Util.INFINTE_COST);
				if (cost1 < lowestCost) {
					bestRoute = neighbor;
					lowestCost = cost1;
				}
			}
			if (!bfCostTable.get(myAddress).containsKey(dst)
					|| lowestCost != bfCostTable.get(myAddress).get(dst)) {
				changed = true;
			}
			bfCostTable.get(myAddress).put(dst, lowestCost);

			if (lowestCost == PA2Util.INFINTE_COST) {
				routingTable.put(dst, PA2Util.UNREACHABLE);
			} else {
				routingTable.put(dst, bestRoute);
			}
		}
		return changed;
	}

	/**
	 * check the lastHeardTime of each neighbor
	 * 
	 * if the client has not heard from a neighbor for 3*timeout time,
	 * then we regard the neighbor to be dead and shutdown the direct link.
	 */
	public void checkDeadNode() {
		long currentTime = new Date().getTime();
		for (String neighbor : neighbors.keySet()) {
			if (neighbors.get(neighbor) == PA2Util.INFINTE_COST) {
				continue;
			}
			if (currentTime - lastHeardTimeTable.get(neighbor) > 3 * timeout * 1000) {
				shutDownLink(neighbor);
			}
		}

	}

	/**
	 * A method to send the file to the destination
	 * 
	 * @param dst
	 * @param filePath
	 * @throws Exception
	 */
	public void transferFile(String dst, String filePath) throws Exception {
		String[] tmp = filePath.split("/");
		String fileName = tmp[tmp.length - 1];//retrieve the filename

		File file = new File(filePath);
		if (!file.exists()) {
			return;
		}
		InputStream is = new FileInputStream(file);

		long fileSize = file.length();
		long offSet = 0;
		// PA2PACKET header+checksum = 26
		// PAFTP header = 66
		byte[] data = new byte[PA2Util.MSS - 92];
		int size;
		int countRetransmit;
		while ((size = is.read(data)) != -1) {
			countRetransmit = 0;
			offSet += size;
			byte[] toSend = new byte[size];
			for (int i = 0; i < size; i++) {
				toSend[i] = data[i];
			}
			PA2FTP pf = new PA2FTP(fileName, fileSize, offSet, toSend);
			while (true) {
				sendFileDataToHost(dst, pf);
				//wait for messageReceiveThread the receive the ACK and interrupt the loop of sleep.
				try {
					Thread.sleep(PA2Util.ACK_TIMEOUT);
				} catch (InterruptedException ie) {
					break;
				}
				countRetransmit++;
				if (countRetransmit >= PA2Util.MAX_RETRANSMISSION) {
					//Give up the process if retransmit a packet for too many time
					System.out
							.println("Retransmit too many times. Transfer cancelled.");
					System.out.print(">");
					is.close();
					return;
				}
			}
		}
		is.close();
		System.out.println("File sent successfully.");
		System.out.print(">");
	}

}
