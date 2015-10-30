import java.io.File;
import java.io.FileOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

/**
 * A thread to receive and process message from the socket.
 * 
 * @author Chao Chen
 * @version 1.0
 *
 */
public class MessageReceiverThread extends Thread {
	int size = 0;
	BFKernel kernel;
	DatagramSocket receiveSocket;

	public MessageReceiverThread(BFKernel bfInfo) {
		this.kernel = bfInfo;
		this.receiveSocket = bfInfo.receiveSocket;
	}

	public void run() {
		while (true) {
			byte[] receiveData = new byte[PA2Util.MSS];
			try {
				DatagramPacket receivePacket = new DatagramPacket(receiveData,
						PA2Util.MSS);
				receiveSocket.receive(receivePacket);
				byte[] data = receivePacket.getData();
				PA2TCP msg = PA2TCP.deserialize(data, 0,
						receivePacket.getLength());
				if (msg == null) {
					// null means the packet cannot be parsed,
					// this is either caused by bit error or unmatched checksum.
					continue;
				}
				parseMsg(msg);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Parse the "network&transport layer" packet
	 * 
	 * @param msg
	 */
	public void parseMsg(PA2TCP msg) {
		// use the "type" tag to distinguish among different kinds of messages
		switch (msg.type) {
		case (PA2Util.UPDATE_ROUTE_TAG):
			kernel.updateRouteFromMsg(msg);
			break;
		case (PA2Util.LINK_DOWN_TAG):
			kernel.shutDownLinkFromMsg(msg);
			break;
		case (PA2Util.LINK_UP_TAG):
			kernel.setUpLinkFromMsg(msg);
			break;
		case (PA2Util.CHANGE_COST_TAG):
			kernel.changeCostFromMsg(msg);
			break;
		case (PA2Util.TRANSFER_SEND_TAG):
			try {
				dealWithTransferDataMsg(msg);
			} catch (Exception e) {
			}
			break;
		case (PA2Util.TRANSFER_ACK_TAG):
			dealWithTransferACKMsg(msg);
		}
	}

	/**
	 * Method to deal with ACK packet used in file transfer.
	 * 
	 * @param msg
	 *            the "network&transport" layer packet
	 */
	public void dealWithTransferACKMsg(PA2TCP msg) {
		if (msg.destinationAddress.equals(kernel.myAddress)) {
			// if the ACK is sent to me, interrupt in the retransmission loop in
			// CLIProcessor thread to send out the next segment.
			kernel.cliProcesser.interrupt();
		} else {
			// if the ACK is sent to others, route the packet to the next hop.
			String nextHop = kernel.getNextHop(msg.destinationAddress);
			if (!nextHop.equals(PA2Util.UNREACHABLE)) {
				kernel.sendToHost(nextHop, msg);
			}
		}
	}

	public void dealWithTransferDataMsg(PA2TCP msg) throws Exception {
		if (msg.destinationAddress.equals(kernel.myAddress)) {
			System.out.println("Packet received.");
			System.out.println("Source = " + msg.sourceAddress);
			System.out.println("Destination = " + msg.destinationAddress);
			System.out.print(">");
			PA2FTP pf = PA2FTP.deserialize(msg.payloadData, 0,
					msg.payloadData.length);
			File file = new File(pf.fileName);
			if (!file.exists()) {
				kernel.fileReceivingMap.put(pf.fileName, 0L);
				try {
					saveFileFromMsg(pf);
				} catch (Exception e) {
				}
			} else if (file.exists()
					&& kernel.fileReceivingMap.containsKey(pf.fileName)) {
				try {
					saveFileFromMsg(pf);
				} catch (Exception e) {
				}
			} else {
				// file exists, not in receiving list
				System.out.println("File Already Exists.");
				System.out.print(">");
			}
			kernel.sendACKToHost(msg.sourceAddress);
		} else {
			String nextHop = kernel.getNextHop(msg.destinationAddress);
			System.out.println("Packet received.");
			System.out.println("Source = " + msg.sourceAddress);
			System.out.println("Destination = " + msg.destinationAddress);
			System.out.println("Next hop = " + nextHop);
			System.out.print(">");
			if (!nextHop.equals(PA2Util.UNREACHABLE)) {
				kernel.sendToHost(nextHop, msg);
			}
		}
	}

	public void saveFileFromMsg(PA2FTP pf) throws Exception {
		if (pf.offSet <= kernel.fileReceivingMap.get(pf.fileName)) {
			return;
		}
		kernel.fileReceivingMap.put(pf.fileName, pf.offSet);
		File file = new File(pf.fileName);
		if (!file.exists())
			file.createNewFile();

		FileOutputStream fs = new FileOutputStream(file, true);
		fs.write(pf.fileData);

		fs.close();

		if (pf.offSet == pf.fileSize) {
			System.out.println("File received successfully");
			System.out.print(">");
			kernel.fileReceivingMap.remove(pf.fileName);
		}

	}

}
