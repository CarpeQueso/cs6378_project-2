import java.io.*;
import java.net.Socket;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Random;
import java.util.Set;


public class Node implements Runnable {

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
	
	private volatile boolean mapActive;
	
	private volatile int mapTotalMessagesSent;
	
	private boolean running;
	
	private ServerController serverController;
	
	private HashMap<Integer, Neighbor> neighbors;

	private ConcurrentLinkedQueue<Message> messageQueue;

	private LinkedList<Message> deferQueue;

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

		this.mapTotalMessagesSent = 0;
		this.serverController = new ServerController(port, messageQueue);
		this.neighbors = new HashMap<>();
		this.messageQueue = new ConcurrentLinkedQueue<>();
		this.deferQueue = new LinkedList<>();
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
		if (this.id == 0) {
			mapActivate();
			// Do other snapshot setup stuff
		}

		this.running = true;

		while (running) {
			if (!messageQueue.isEmpty()) {
				processMessage(messageQueue.poll());
			}
		}

			 
	}

	public void startServer() {
		new Thread(serverController).start();
	}

	public void stopServer() {
		serverController.stop();
	}

	public synchronized void mapActivate() {
		this.mapActive = true;
	}

	public synchronized void mapDeactivate() {
		this.mapActive = false;
	}

	public synchronized boolean isMapActive() {
		return this.mapActive;
	}
		
	public synchronized void unicast(int neighborId, Message message) {
		Neighbor neighbor = neighbors.get(neighborId);

		if (neighbor == null) return;

		try(
			PrintWriter out = new PrintWriter(neighbor.getSocket().getOutputStream(), true)
			) {
			if (neighbor.isEnabled()) {
				out.println(message.toString());
			}
		} catch (IOException e) {
			System.err.println("Unable to send unicast message to neighbor with id: "
							   + neighbor.getId());
		}
	}
	
	public synchronized void broadcast(Message message) {
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

	public void run() {
		// Assumes node has been activated prior to running
		Random random = new Random();
		int messageRange = maxPerActive - minPerActive;
		int messagesToSend = random.nextInt(messageRange) + minPerActive; 
		int messagesSentThisCycle = 0;
		Set<Integer> neighborIdSet = neighbors.keySet();
		Integer[] neighborIds = neighborIdSet.toArray(new Integer[0]);

		while (messagesSentThisCycle < messagesToSend && mapTotalMessagesSent < maxNumber) {
			int nextMessageNeighborId = random.nextInt(neighborIds.length);
			this.vectorClock.increment(this.id);
			String vectorClockString = this.vectorClock.toString();

			unicast(nextMessageNeighborId, new Message(MessageType.MAP, this.id, vectorClockString));
			messagesSentThisCycle++;
			mapTotalMessagesSent++;
			try {
				Thread.sleep(minSendDelay);
			} catch (InterruptedException e) {

			}
		}

		mapDeactivate();
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
	
	private void processMessage(Message message) {
		switch (message.getType()) {
		case MAP:
			VectorClock vc = VectorClock.parseVectorClock(message.getBody());
			this.vectorClock.updateAndIncrement(vc, this.id);

			if (!isMapActive() && mapTotalMessagesSent < maxNumber) {
				mapActivate();
				new Thread(this).start();
			}
			break;
		case SNAPSHOT:
			break;
		case HALT:
			break;
		default:
		}
	}
}
