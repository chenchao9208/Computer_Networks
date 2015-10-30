import java.net.InetAddress;
import java.nio.ByteBuffer;

/**
 * The Class entity of network&transport layer protocol packet. Named PA2TCP
 * 
 * For Bellman Ford information exchange use, it behaves like udp. For file
 * transfer use, it behaves like stop-and-wait tcp.
 * 
 * @author Chao Chen
 * @version 1.0
 *
 */
public class PA2TCP {
	//In the byte stream, the packet is arranged as the following sequence.

	// | component         |  type   |  length(byte)        |
	// ______________________________________________________
	// | source IP         |  int    |   4                  |
	// | destination IP    |  int    |   4                  |
	// | source port       |  short  |   2                  |
	// | destination port  |  short  |   2                  |
	// | type              |  short  |   2                  |
	// | length            |  int    |   4                  |
	// | payloadData       |  byte[] |   payloadData.length |
	// | checksum          |  long   |   8                  |

	String sourceAddress; //source IP:source port i.e. 192.168.0.8:12340
	String destinationAddress; // destination IP:destination port i.e. 192.168.0.10:43210

	/**
	 * to indicate message type, specified in PA2Util.java
	 * 
	 * @see PA2Util
	 */
	short type;
	int length;
	long checksum;

	public byte[] payloadData;

	public PA2TCP() {
	}

	/**
	 * Constructor with string-type payload
	 * 
	 * @param type
	 *            message type
	 * @param sender
	 *            sender's ip:port
	 * @param receiver
	 *            receiver's ip:port
	 * @param payloadString
	 *            payload in the format of string
	 */
	public PA2TCP(short type, String sender, String receiver,
			String payloadString) {

		this.type = type;
		this.sourceAddress = sender;
		this.destinationAddress = receiver;
		if (payloadString == null) {
			this.payloadData = null;
		} else {
			this.payloadData = payloadString.getBytes();
		}
	}

	/**
	 * Constructor with PA2FTP-type payload used to encapsulate file data to
	 * send.
	 * 
	 * @param type
	 *            message type
	 * @param sender
	 *            sender's ip:port
	 * @param receiver
	 *            receiver's ip:port
	 * @param pf
	 *            PA2FTP-type payload
	 */
	public PA2TCP(short type, String sender, String receiver, PA2FTP pf) {
		this.type = type;
		this.sourceAddress = sender;
		this.destinationAddress = receiver;
		try {
			this.payloadData = pf.serialize();
		} catch (Exception e) {
			this.payloadData = null;
		}

	}

	/**
	 * Convert the packet into byte array
	 * 
	 * @return byte array of the packet
	 * @throws Exception
	 */
	public byte[] serialize() throws Exception {
		length = (26 + ((payloadData == null) ? 0 : payloadData.length));
		byte[] output = new byte[length];
		ByteBuffer bb = ByteBuffer.wrap(output);
		bb.put(InetAddress.getByName(sourceAddress.split(":")[0]).getAddress());
		bb.put(InetAddress.getByName(destinationAddress.split(":")[0])
				.getAddress());
		bb.putShort(Short.valueOf(sourceAddress.split(":")[1]));
		bb.putShort(Short.valueOf(destinationAddress.split(":")[1]));
		bb.putShort(type);
		bb.putInt(length);
		if (payloadData != null) {
			bb.put(payloadData);
		}
		checksum = PA2Util.calculateCheckSum(output, 0, length - 8);
		bb.putLong(checksum);

		return output;
	}

	/** extract the packet instance from the byte array
	 * @param data
	 * @param offset
	 * @param length
	 * @return the PA2TCP packet instance
	 * @throws Exception
	 */
	public static PA2TCP deserialize(byte[] data, int offset, int length)
			throws Exception {
		if (length < 26) {
			//the packet length must >=26
			return null;
		}
		//checksum calculated
		long checksum1 = PA2Util.calculateCheckSum(data, offset, length - 8);
		//checksum recorded in the packet
		long checksum2 = PA2Util.bytesToLong(data, offset + length - 8);
		if (checksum1 != checksum2) {
			return null;
		}
		PA2TCP pp = new PA2TCP();
		ByteBuffer bb = ByteBuffer.wrap(data, offset, length);
		pp.sourceAddress = InetAddress.getByAddress(
				PA2Util.intToBytes(bb.getInt())).toString();
		pp.destinationAddress = InetAddress.getByAddress(
				PA2Util.intToBytes(bb.getInt())).toString();
		pp.sourceAddress += ":" + bb.getShort();
		pp.destinationAddress += ":" + bb.getShort();

		pp.sourceAddress = pp.sourceAddress.split("/")[1];
		pp.destinationAddress = pp.destinationAddress.split("/")[1];
		pp.type = bb.getShort();
		pp.length = bb.getInt();

		if (pp.type == PA2Util.CHANGE_COST_TAG
				|| pp.type == PA2Util.UPDATE_ROUTE_TAG
				|| pp.type == PA2Util.TRANSFER_SEND_TAG) {
			pp.payloadData = new byte[length - 26];
			bb.get(pp.payloadData);
		} else {
			pp.payloadData = null;
		}
		pp.checksum = bb.getLong();
		return pp;
	}

	
	/** 
	 * Used when debugging, to inspect the packet sent and received.
	 * 
	 */
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("From: ");
		sb.append(sourceAddress);
		sb.append("\nTo: ");
		sb.append(destinationAddress);
		sb.append("\nType: ");
		switch (type) {
		case PA2Util.CHANGE_COST_TAG:
			sb.append("CHANGE_COST");
			break;
		case PA2Util.LINK_DOWN_TAG:
			sb.append("LINK_DOWN");
			break;
		case PA2Util.LINK_UP_TAG:
			sb.append("LINK_UP");
			break;
		case PA2Util.TRANSFER_SEND_TAG:
			sb.append("TRANSFER");
			break;
		case PA2Util.UPDATE_ROUTE_TAG:
			sb.append("UPDATE_ROUTE");
			break;
		case PA2Util.TRANSFER_ACK_TAG:
			sb.append("ACK");
			break;
		default:
			sb.append("UNKNOWN");
			break;
		}
		sb.append("\nLength: ");
		sb.append(length);
		if (type == PA2Util.UPDATE_ROUTE_TAG || type == PA2Util.CHANGE_COST_TAG) {
			sb.append("\nValue: ");
			sb.append(new String(payloadData));
		}
		sb.append("\nPayloadData: ");
		sb.append(payloadData);
		sb.append("\nChecksum: ");
		sb.append(checksum);

		return sb.toString();
	}

}
