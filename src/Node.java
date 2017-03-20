import java.io.*;
import java.net.Socket;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.HashMap;


public class Node {

	private final int id;

	private final String hostname;

	private final int port;

	private final int minPerActive;

	private final int maxPerActive;

	private final int minSendDelay;

	private final int snapshotDelay;

	private final int maxNumber;

	private final int parentNodeId;

	// Only used to size the vector clock.
	private final int totalNodes;
	
	private ServerController serverController;
	
	private HashMap<Integer, Neighbor> neighbors;

	private ConcurrentLinkedQueue<Message> messageQueue;

	private VectorClock vectorClock;

/**
 * static int messageSent;
 * public sendMessage(){messageSent++;}
 * if messageSent >= maxNumber && passive
 * send HaltMessage;
 */
// every time login to a remote machine.we create a node on that machine?
	
	public Node(int id, String hostname, int port, int totalNodes, int minPerActive,
				int maxPerActive, int minSendDelay, int snapshotDelay, int maxNumber,
				int parentNodeId) {
		this.id = id;
		this.hostname = hostname;
		this.port = port;
		this.totalNodes = totalNodes;
		this.minPerActive = minPerActive;
		this.maxPerActive = maxPerActive;
		this.minSendDelay = minSendDelay;
		this.snapshotDelay = snapshotDelay;
		this.maxNumber = maxNumber;
		this.parentNodeId = parentNodeId;

		this.serverController = new ServerController(port, messageQueue);
		this.neighbors = new HashMap<>();
		this.messageQueue = new ConcurrentLinkedQueue<>();
		this.vectorClock = new VectorClock(totalNodes);
	}

	public void addNeighbor(int id, String hostname, int port) {
		try {
			Socket socket = new Socket(hostname, port);
			this.neighbors.put(id, new Neighbor(id, hostname, port, socket));
		} catch (IOException e) {
			System.err.println("Could not create neighbor socket");
		}
	}

	public void begin() {

	}

	public void startServer() {
		new Thread(serverController).start();
	}

	public void stopServer() {
		serverController.stop();
	}

	public void broadcast(Message message) {
		for (Neighbor neighbor : neighbors.values()) {
			try(
				PrintWriter out = new PrintWriter(neighbor.getSocket().getOutputStream(), true)
			   ) {
				if (neighbor.isEnabled()) {
					out.println(message.toString());
				}
			} catch (IOException e) {
				System.err.println("Unable to send message to neighbor with id: "
								   + neighbor.getId());
			}
		}
	}


	public int getId() {
		return this.id;
	}

	public String getHostname() {
		return this.hostname;
	}

	public int getPort() {
		return this.port;
	}
	

}
