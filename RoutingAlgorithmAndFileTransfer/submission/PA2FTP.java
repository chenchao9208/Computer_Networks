import java.nio.ByteBuffer;

/**
 * The class entity for application layer protocol used for file transferring.
 * 
 * @author Chao Chen
 * @version 1.0
 *
 */
public class PA2FTP {
	//In the byte stream, the packet is arranged as the following sequence.

	// | component         |  type   |  length(byte)        |
	// ______________________________________________________
	// | file name         |  String |   50                 |
	// | file size         |  long   |   8                  |
	// | offset            |  long   |   8                  |
	// | fileData          |  byte[] |   fileData.length    |
	
	String fileName;
	long fileSize;
	long offSet; // the index of fileData's last byte in the original file .
	byte[] fileData;
	
	/**
	 * Constructor
	 */
	public PA2FTP(){
	}

	/**
	 * Constructor
	 * 
	 * @param fileName
	 * @param fileSize
	 * @param offSet
	 * @param data
	 */
	public PA2FTP(String fileName, long fileSize, long offSet, byte[] data) {
		this.fileName = fileName;
		this.fileSize = fileSize;
		this.offSet = offSet;
		this.fileData = data;
	}

	// fileName=50
	// fileSize=8
	// offSet=8
	// fileData=xx
	
	/**
	 * Convert the message into byte array.
	 * 
	 * @return byte array format of the message
	 * @throws Exception
	 */
	public byte[] serialize() throws Exception {
		byte[] output = new byte[66 + fileData.length];
		ByteBuffer bb = ByteBuffer.wrap(output);
		bb.put(PA2Util.strToBytes(fileName));
		bb.putLong(fileSize);
		bb.putLong(offSet);
		bb.put(fileData);
		return output;
	}

	/**
	 * Extract the PA2FTP message from the byte array
	 * 
	 * @param data
	 * @param offset
	 * @param length
	 * @return instance of PA2FTP
	 * @throws Exception
	 */
	public static PA2FTP deserialize(byte[] data, int offset, int length)
			throws Exception {
		PA2FTP pf = new PA2FTP();
		ByteBuffer bb = ByteBuffer.wrap(data, offset, length);
		byte[] fileNameBytes = new byte[50];
		bb.get(fileNameBytes);
		pf.fileName = PA2Util.bytesToStr(fileNameBytes);
		pf.fileSize = bb.getLong();
		pf.offSet = bb.getLong();
		pf.fileData = new byte[length - 66];
		bb.get(pf.fileData);
		return pf;
	}

}
