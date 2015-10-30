import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * The class contains the packet header, user command format, retransmission
 * timeout, MSS, maximum retransmission attempt... As well as a checksum
 * calculator and some data type converter.
 * 
 * @author Chao Chen
 * @version 1.0
 *
 */
public final class PA2Util {

	private PA2Util() {
	}

	/**
	 * MSS in the simulated network. Total length of "network&transport" layer
	 * and application layer(FTP) payload. Actually in real network its the MTU
	 * of the UDP payload.
	 */
	public static final int MSS = 1400;
	/**
	 * Time in million second a file sender should wait for an ACK before
	 * retransmission.
	 */
	public static final long ACK_TIMEOUT = 100;
	/**
	 * Maximum number of times should the sender try to retransmit one packet.
	 */
	public static final int MAX_RETRANSMISSION = 10;

	// Message Header
	public static final short UPDATE_ROUTE_TAG = 1;
	public static final short LINK_DOWN_TAG = 2;
	public static final short LINK_UP_TAG = 3;
	public static final short CHANGE_COST_TAG = 4;
	public static final short TRANSFER_SEND_TAG = 5;
	public static final short TRANSFER_ACK_TAG = 6;

	// marks to help store data.
	public static final double INFINTE_COST = 1.0E100;
	public static final String INFINITY_TAG = "INF";
	public static final String UNREACHABLE = "-";

	// User Command
	public static final String LINK_DOWN_COMMAND = "LINKDOWN";
	public static final String LINK_UP_COMMAND = "LINKUP";
	public static final String CHANGE_COST_COMMAND = "CHANGECOST";
	public static final String SHOWRT_COMMAND = "SHOWRT";
	public static final String CLOSE_COMMAND = "CLOSE";
	public static final String TRANSFER_COMMAND = "TRANSFER";
	public static final String ADD_PROXY_COMMAND = "ADDPROXY";
	public static final String REMOVE_PROXY_COMMAND = "REMOVEPROXY";

	/**
	 * Convert the filename to a byte array of fixed length=50. if the filename
	 * is too long, it will be truncated. if the filename is shorter than 50
	 * byte, '0' will be padded at the last of byte array
	 * 
	 * @param value
	 *            filename string
	 * @return byte array of length=50.
	 */
	public static byte[] strToBytes(String value) {
		if (value == null) {
			return new byte[50];
		}

		byte[] tmp = value.getBytes();
		return Arrays.copyOf(tmp, 50);
	}

	/**
	 * Convert a byte array if fixed length=50 to a string. the padded "\0" on
	 * the string will be deleted.
	 * 
	 * @param input
	 *            byte array
	 * @return string
	 */
	public static String bytesToStr(byte[] input) {
		if (Arrays.equals(input, new byte[50])) {
			return null;
		} else {
			return new String(input).split("\0")[0];
		}
	}

	/**
	 * Convert an integer to a byte array of 4 bytes.
	 * 
	 * @param value
	 *            integer
	 * @return byte array
	 */
	public static byte[] intToBytes(int value) {
		byte[] src = new byte[4];
		src[0] = (byte) ((value >> 24) & 0xFF);
		src[1] = (byte) ((value >> 16) & 0xFF);
		src[2] = (byte) ((value >> 8) & 0xFF);
		src[3] = (byte) (value & 0xFF);
		return src;
	}

	/**
	 * Convert a part of a byte array to Long type value. Used to retrieve
	 * checksum field in the packet.
	 * 
	 * @param b
	 *            the byte array
	 * @param offset
	 *            the first byte of the target data
	 * @return a long-typed integer
	 */
	public static long bytesToLong(byte[] b, int offset) {
		ByteBuffer bb = ByteBuffer.wrap(b, offset, 8);
		return bb.getLong();
	}

	/**
	 * Method to calculate the checksum of a part of byte array
	 * 
	 * @param b
	 *            byte array
	 * @param offSet
	 *            the index of the first byte
	 * @param length
	 *            the length of the subarray need calculate.
	 * @return checksum of the specific part of byte array.
	 */
	public static long calculateCheckSum(byte[] b, int offSet, int length) {
		long sum = 0;
		long data;

		// Handle all pairs
		while (length > 1) {
			data = (((b[offSet] << 8) & 0xFF00) | ((b[offSet + 1]) & 0xFF));
			sum += data;
			// 1's complement carry bit correction in 16-bits (detecting sign
			// extension)
			if ((sum & 0xFFFF0000) > 0) {
				sum = sum & 0xFFFF;
				sum += 1;
			}
			offSet += 2;
			length -= 2;
		}
		// Handle remaining byte in odd length buffers
		if (length > 0) {
			sum += (b[offSet] << 8 & 0xFF00);
			// 1's complement carry bit correction in 16-bits (detecting sign
			// extension)
			if ((sum & 0xFFFF0000) > 0) {
				sum = sum & 0xFFFF;
				sum += 1;
			}
		}
		// Final 1's complement value correction to 16-bits
		sum = ~sum;
		sum = sum & 0xFFFF;
		return sum;
	}

}
