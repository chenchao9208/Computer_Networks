/**
 * The Java Program for Computer Networks Program Assignment 2 Initialize this
 * class to launch the program.
 * 
 * @author Chao Chen
 * @version 1.0
 * 
 */
public class BFClient {

	/**
	 * The data structure to store informations and some methods to compose and
	 * send message.
	 */
	BFKernel kernel;

	/**
	 * The thread to periodically send UPDATE_ROUTE messages to neighbors.
	 */
	DefaultUpdateSenderThread dfUpdateSender;

	/**
	 * The thread to check and kick out dead neighbor node.
	 */
	TimeoutCheckerThread timeoutChecker;

	/**
	 * The thread to listen to a socket. receive messages and process messages.
	 */
	MessageReceiverThread msgReceiver;

	/**
	 * The thread to process user's CLI commands.
	 */
	CLIProcesserThread cliProcesser;

	/**
	 * Initialize the program from the configuration file.
	 * 
	 * @param fileName
	 *            the configuration file name
	 */
	public BFClient(String fileName) {
		// initialize data structure and threads.
		kernel = new BFKernel(fileName);
		dfUpdateSender = new DefaultUpdateSenderThread(kernel);
		timeoutChecker = new TimeoutCheckerThread(kernel);
		msgReceiver = new MessageReceiverThread(kernel);
		cliProcesser = new CLIProcesserThread(kernel);

		// set up reference links.
		kernel.dfUpdateSender = dfUpdateSender;
		kernel.timeoutChecker = timeoutChecker;
		kernel.msgReceiver = msgReceiver;
		kernel.cliProcesser = cliProcesser;

		// send the first bunch of UPDATE_ROUTE messages to neighbors.
		kernel.sendVectorsToNeighbors();

		// activates all the threads.
		msgReceiver.start();
		dfUpdateSender.start();
		timeoutChecker.start();
		cliProcesser.start();
		System.out.println("Started.");
		System.out.print(">");
	}

	public static void main(String[] args) {
		if (args.length != 1) {
			System.out.println("Usage: java bfclient <CONFIG_FILE_NAME>");
			System.exit(0);
		} else {
			new BFClient(args[0]);
		}
	}
}
