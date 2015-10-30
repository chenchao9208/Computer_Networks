import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * The thread to process user's CLI commands.
 * 
 * @author Chao Chen
 * @version 1.0
 */
public class CLIProcesserThread extends Thread {
	BFKernel kernel;
	
	public CLIProcesserThread(BFKernel kernel) {
		this.kernel = kernel;
	}

	public void run() {
		try {
			processCLI();
		} catch (IOException ie) {
			System.out.println("Error occurs when processing user input.");
			System.exit(0);
		}
	}

	public void processCLI() throws IOException {
		BufferedReader cliIn = new BufferedReader(new InputStreamReader(
				System.in));
		while (true) {
			String command = cliIn.readLine();
			if (command.startsWith(PA2Util.SHOWRT_COMMAND)) {
				System.out.println(kernel.getDVTable());
				System.out.print(">");
			} else if (command.startsWith(PA2Util.LINK_DOWN_COMMAND)) {
				processLinkDownCommand(command);
			} else if (command.startsWith(PA2Util.LINK_UP_COMMAND)) {
				processLinkUpCommand(command);
			} else if (command.startsWith(PA2Util.CHANGE_COST_COMMAND)) {
				processChangeCostCommand(command);
			} else if (command.startsWith(PA2Util.TRANSFER_COMMAND)) {
				processTransferCommand(command);
			} else if (command.startsWith(PA2Util.ADD_PROXY_COMMAND)) {
				processAddProxyCommand(command);
			} else if (command.startsWith(PA2Util.REMOVE_PROXY_COMMAND)) {
				processRemoveProxyCommand(command);
			}else if (command.equals(PA2Util.CLOSE_COMMAND)) {
				kernel.receiveSocket.close();
				System.out.println("Closed. Bye~");
				System.exit(0);
			}else {
				System.out.println("Wrong command.");
				System.out.print(">");
			}
		}

	}

	public void processLinkDownCommand(String command) {
		String[] tmp = command.split(" ");
		
		if (tmp.length != 3) {
			// invalid command
			System.out.println("Usage: " + PA2Util.LINK_DOWN_COMMAND
					+ " {ip_address port}");
			System.out.print(">");
			return;
		}
		String dst = tmp[1] + ":" + tmp[2];
		
		if (!kernel.neighbors.containsKey(dst)) {
			// the link is not its neighbor
			System.out.println("No such link.");
			System.out.print(">");
			return;
		}
		if (kernel.neighbors.get(dst) == PA2Util.INFINTE_COST) {
			System.out.println("Link is already down.");
			System.out.print(">");
			return;
		}

		kernel.sendLinkDownToNeighbor(dst);
		kernel.shutDownLink(dst);
		System.out.print(">");
	}

	public void processLinkUpCommand(String command) {
		String[] tmp = command.split(" ");
		if (tmp.length != 3) {
			//invalid command
			System.out.println("Usage: " + PA2Util.LINK_UP_COMMAND
					+ " {ip_address port}");
			System.out.print(">");
			return;
		}
		String dst = tmp[1] + ":" + tmp[2];
		if (!kernel.neighbors.containsKey(dst)) {
			//the link is not its neighbor
			System.out.println("No such link.");
			System.out.print(">");
			return;
		}
		if (kernel.neighbors.get(dst) != PA2Util.INFINTE_COST) {
			System.out.println("Link is already up.");
			System.out.print(">");
			return;
		}

		kernel.sendLinkUpToNeighbor(dst);
		kernel.setUpLink(dst);
		System.out.print(">");
	}

	public void processChangeCostCommand(String command) {
		String[] tmp = command.split(" ");
		if (tmp.length != 4) {
			//invalid command
			System.out.println("Usage: " + PA2Util.CHANGE_COST_COMMAND
					+ " {ip_address port cost}");
			System.out.print(">");
			return;
		}
		String dst = tmp[1] + ":" + tmp[2];
		double newCost;
		try {
			newCost = Double.valueOf(tmp[3]);
		} catch (NumberFormatException ne) {
			//invalid command parameter
			System.out.println("Usage: " + PA2Util.CHANGE_COST_COMMAND
					+ " {ip_address port cost}");
			System.out.print(">");
			return;
		}
		if (!kernel.neighbors.containsKey(dst)) {
			// the link is not its neighbor.
			System.out.println("No such link.");
			System.out.print(">");
			return;
		}
		if (kernel.neighbors.get(dst) == PA2Util.INFINTE_COST) {
			System.out.println("Link is already down. Set up first.");
			System.out.print(">");
			return;
		}

		kernel.sendChangeCostToNeighbor(dst, newCost);
		kernel.changeCost(dst, newCost);
		System.out.print(">");
	}

	
	public void processTransferCommand(String command) {
		String[] tmp = command.split(" ");
		if (tmp.length != 4) {
			//invalid command
			System.out.println("Usage: " + PA2Util.TRANSFER_COMMAND
					+ " {filename destination_ip port}");
			System.out.print(">");
			return;
		}
		String filePath = tmp[1];
		String dst = tmp[2] + ":" + tmp[3];
		if (!kernel.routingTable.containsKey(dst)||kernel.routingTable.get(dst).equals(PA2Util.UNREACHABLE)) {
			// no route to the destination
			System.out.println("Destination Address Unreachable.");
			System.out.print(">");
			return;
		}
		if (!new File(filePath).exists()) {
			//file does not exist
			System.out.println("File does not exit.");
			System.out.print(">");
			return;
		}
		System.out.println("Next hop = " + kernel.routingTable.get(dst));

		try {
			kernel.transferFile(dst, filePath);
		} catch (Exception e) {
		}
	}

	public void processAddProxyCommand(String command) {
		String[] tmp = command.split(" ");
		if (tmp.length != 5) {
			//invald command
			System.out.println("Usage: " + PA2Util.ADD_PROXY_COMMAND
					+ " {proxy_ip proxy_port neighbor_ip neighbor_port}");
			System.out.print(">");
			return;
		}
		String neighbor = tmp[3] + ":" + tmp[4];
		String proxy = tmp[1] + ":" + tmp[2];
		if (!kernel.neighbors.containsKey(neighbor)) {
			System.out.println("Neighbor not found.");
			System.out.print(">");
			return;
		}
		kernel.proxyMap.put(neighbor, proxy);
		System.out.println("Done.");
		System.out.print(">");
	}

	public void processRemoveProxyCommand(String command) {
		String[] tmp = command.split(" ");
		if (tmp.length != 3) {
			//invalid command
			System.out.println("Usage: " + PA2Util.REMOVE_PROXY_COMMAND
					+ " {neighbor_ip neighbor_port}");
			System.out.print(">");
			return;
		}
		String neighbor = tmp[1] + ":" + tmp[2];
		kernel.proxyMap.remove(neighbor);
		System.out.println("Done");
		System.out.print(">");
	}
}
