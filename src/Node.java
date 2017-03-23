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
		this.neighbors = new HashMap<>();
		this.messageQueue = new ConcurrentLinkedQueue<>();
		this.deferQueue = new LinkedList<>();
		this.snapshotsTaken = new LinkedList<>();
		this.vectorClock = new VectorClock(totalNodes);
		this.snapshotsCollectedThisRound = new Snapshot[totalNodes];
		this.serverController = new ServerController(port, messageQueue);
	}

	public void addNeighbor(int id, String hostname, int port) {
		this.neighbors.put(id, new Neighbor(id, hostname, port));
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
					processMessage(deferQueue.poll());
				}
			}

			if (this.id == 0 && System.currentTimeMillis() - timer > this.snapshotDelay
					&& numSnapshotsCollectedThisRound == 0) {
				// Initiate Snapshot
				System.out.println("Snapshot initiated");
				broadcast(new Message(MessageType.MARKER, this.id, "none"));
				hasSentMarkerMessage = true;
				snapshotInProgress = true;
				timer = System.currentTimeMillis();
			}	
		}

		try (PrintWriter pw = new PrintWriter("../" + this.id + ".out", "UTF-8")) {
			for (Snapshot snapshot : snapshotsTaken) {
				int[] clockVector = snapshot.getClockVector();
				pw.print(clockVector[0]);
				for (int i = 1; i < clockVector.length; i++) {
					pw.print(" " + clockVector[i]);
				}
				pw.println();
			}
		} catch (IOException e) {
			System.err.println("Could not open output file");
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
		System.out.println("Unicast " + message.getType().name() + ","
				+ message.getSenderId() + "->" + neighborId);

		if (neighbor == null) return;

		try(
			Socket socket = new Socket(neighbor.getHostname(), neighbor.getPort());
			PrintWriter out = new PrintWriter(socket.getOutputStream(), true)
			) {
			if (neighbor.isEnabled()) {
				out.println(message.toString());
			}
		} catch (IOException e) {
			System.err.println("Unable to send unicast message to neighbor with id: "
							   + neighbor.getId());
			e.printStackTrace();
		}
	}
	
	public synchronized void broadcast(Message message) {
		System.out.println("Broadcast " + message.getType().name() + " from " + this.id);
		for (Neighbor neighbor : neighbors.values()) {
			try(
				Socket socket = new Socket(neighbor.getHostname(), neighbor.getPort());
				PrintWriter out = new PrintWriter(socket.getOutputStream(), true)
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
			int nextMessageNeighborId = neighborIds[random.nextInt(neighborIds.length)];
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
				numMarkerMessagesReceived = 0;
				hasSentMarkerMessage = false;
				
				Snapshot snapshot
					= new Snapshot(active, canBeReactivated, numQueuedMessages, clockVector);
				snapshotsTaken.add(snapshot);
				if (id == 0) {
					snapshotsCollectedThisRound[0] = snapshot;
					numSnapshotsCollectedThisRound++;
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
			neighbors.get(message.getSenderId()).disable();
			numHaltMessagesReceived++;
			break;
		default:
		}
	}

	private void processSnapshotMessage(Message snapshotMessage) {
		Snapshot snapshot = Snapshot.parseSnapshot(snapshotMessage.getBody());
		if (snapshotsCollectedThisRound[snapshotMessage.getSenderId()] == null) {
			snapshotsCollectedThisRound[snapshotMessage.getSenderId()] = snapshot;
			numSnapshotsCollectedThisRound++;

			if (numSnapshotsCollectedThisRound == totalNodes) {
				System.out.println("Node 0 has received all snapshots");
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
