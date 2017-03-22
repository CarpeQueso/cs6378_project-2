import java.io.*;
import java.net.Socket;
import java.util.Arrays;
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

	private boolean hasSentHaltMessage;

    private boolean hasSentMarkerMessage;

	private boolean snapshotInProgress;
	
	private int numHaltMessagesReceived;
	
	private int numMarkerMessagesReceived;
	
	private int numSnapshotsCollectedThisRound;
	
	private ServerController serverController;
	
	private HashMap<Integer, Neighbor> neighbors;

	private ConcurrentLinkedQueue<Message> messageQueue;

	private LinkedList<Message> deferQueue;

	private LinkedList<Snapshot> snapshotsTaken;
	
	private VectorClock vectorClock;

	private Snapshot[] snapshotsCollectedThisRound;

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

		this.hasSentHaltMessage = false;
		this.hasSentMarkerMessage = false;
		this.snapshotInProgress = false;
		this.numHaltMessagesReceived = 0;
		this.numMarkerMessagesReceived = 0;
		this.mapTotalMessagesSent = 0;
		this.numSnapshotsCollectedThisRound = 0;
		this.serverController = new ServerController(port, messageQueue);
		this.neighbors = new HashMap<>();
		this.messageQueue = new ConcurrentLinkedQueue<>();
		this.deferQueue = new LinkedList<>();
		this.snapshotsTaken = new LinkedList<>();
		this.vectorClock = new VectorClock(totalNodes);
		this.snapshotsCollectedThisRound = new Snapshot[totalNodes];
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
		long timer = System.currentTimeMillis();
		this.running = true;

		if (this.id == 0) {
			mapActivate();
			new Thread(this).start();

		}

		while (numHaltMessagesReceived < neighbors.size()) {
			if (!messageQueue.isEmpty()) {
				processMessage(messageQueue.poll());
			}

			if (!deferQueue.isEmpty() && !snapshotInProgress) {
				while (!deferQueue.isEmpty()) {
					processMessage(messageQueue.poll());
				}
			}

			if (this.id == 0 && System.currentTimeMillis() - timer > this.snapshotDelay) {
				// Initiate Snapshot
				broadcast(new Message(MessageType.MARKER, this.id, "none"));
				hasSentMarkerMessage = true;
				snapshotInProgress = true;
			}	
		}

		// TODO: Print config file information
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

			unicast(nextMessageNeighborId,
					new Message(MessageType.MAP, this.id, vectorClockString));
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
			if (snapshotInProgress) {
				deferQueue.add(message);
			} else {
				VectorClock vc = VectorClock.parseVectorClock(message.getBody());
				this.vectorClock.updateAndIncrement(vc, this.id);

				if (!isMapActive() && mapTotalMessagesSent < maxNumber) {
					mapActivate();
					new Thread(this).start();
				}
			}
			break;
		case MARKER:
			if (!hasSentMarkerMessage) {
				broadcast(new Message(MessageType.MARKER, this.id, "none"));
				hasSentMarkerMessage = true;
				snapshotInProgress = true;	
			}

			numMarkerMessagesReceived++;
			if (numMarkerMessagesReceived >= neighbors.size()) {
				snapshotInProgress = false;
				boolean active = isMapActive();
				boolean canBeReactivated = mapTotalMessagesSent < maxNumber;
				int numQueuedMessages = deferQueue.size();
				int[] clockVector = vectorClock.getClockVector();
				
				Snapshot snapshot
					= new Snapshot(active, canBeReactivated, numQueuedMessages, clockVector);
				snapshotsTaken.add(snapshot);
				if (id == 0) {
					snapshotsCollectedThisRound[0] = snapshot;
				} else {
					unicast(parentNodeId,
							new Message(MessageType.SNAPSHOT, this.id, snapshot.toString()));
				}
			}
				
			break;
		case SNAPSHOT:
			if (this.id == 0) {
				processSnapshotMessage(message);
			} else {
				// Send it upward toward the root of the spanning tree.
				unicast(parentNodeId, message);
			}
			break;
		case HALT:
			if (!hasSentHaltMessage) {
				broadcast(new Message(MessageType.HALT, this.id, "none"));
				hasSentHaltMessage = true;
			}
			numHaltMessagesReceived++;
			break;
		default:
		}
	}

	private void processSnapshotMessage(Message snapshotMessage) {
		// TODO: Do something with snapshot message.
		Snapshot snapshot = Snapshot.parseSnapshot(snapshotMessage.getBody());
		if (snapshotsCollectedThisRound[snapshotMessage.getSenderId()] == null) {
			snapshotsCollectedThisRound[snapshotMessage.getSenderId()] = snapshot;
			numSnapshotsCollectedThisRound++;

			if (numSnapshotsCollectedThisRound == totalNodes) {
				if (systemHasTerminated(snapshotsCollectedThisRound)) {
					broadcast(new Message(MessageType.HALT, this.id, "none"));
					hasSentHaltMessage = true;
				} else {
					numSnapshotsCollectedThisRound = 0;
					Arrays.fill(snapshotsCollectedThisRound, null);
				}
			}
		}
	}

	private static boolean systemHasTerminated(Snapshot[] snapshots) {
		for (Snapshot snapshot : snapshots) {
			if (snapshot.isActive()
				|| (snapshot.canBeReactivated() && snapshot.getNumQueuedMessages() > 0)) {
				return false;
			}
		}

		return true;
	}
}
