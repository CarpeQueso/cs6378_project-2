import java.io.*;
import java.net.Socket;
import java.util.Arrays;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Random;
import java.util.Set;


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

	private volatile boolean mapActive;

	private volatile int mapTotalMessagesSent;

	private boolean running;

	private boolean hasSentHaltMessage;

	private boolean hasSentMarkerMessage;

	private volatile boolean snapshotInProgress;

	private int numHaltMessagesReceived;

	private int numMarkerMessagesReceived;

	private int numSnapshotsCollectedThisRound;

	private ServerController serverController;

	private HashMap<Integer, Neighbor> neighbors;

	private ConcurrentLinkedQueue<Message> messageQueue;

	private LinkedList<Message> deferQueue;

	private LinkedList<Integer> mapMessageNeighborIdQueue;
	
	private LinkedList<Snapshot> snapshotsTaken;

	private VectorClock vectorClock;

	private Snapshot[] snapshotsCollectedThisRound;

	private int[] currentSnapshotVector;

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
		this.mapMessageNeighborIdQueue = new LinkedList<>();
		this.snapshotsTaken = new LinkedList<>();
		this.vectorClock = new VectorClock(totalNodes);
		this.currentSnapshotVector = new int[totalNodes];
		this.snapshotsCollectedThisRound = new Snapshot[totalNodes];
		this.serverController = new ServerController(port, messageQueue);
	}

	public void addNeighbor(int id, String hostname, int port) {
		this.neighbors.put(id, new Neighbor(id, hostname, port));
	}

	public void begin() {
		long timer = System.currentTimeMillis();
		long mapTimer = 0;
		this.running = true;

		if (this.id == 0) {
			mapActivate();
			buildMapMessageQueue();
		}

		while (numHaltMessagesReceived < neighbors.size()) {
			if (!snapshotInProgress) {
				while (!deferQueue.isEmpty()) {
					processMessage(deferQueue.poll());
				}
			}

			if (!messageQueue.isEmpty()) {
				processMessage(messageQueue.poll());
			}

			if (!mapMessageNeighborIdQueue.isEmpty()
				&& System.currentTimeMillis() - mapTimer > this.minSendDelay
				&& mapTotalMessagesSent < maxNumber) {
				int nextMessageNeighborId = mapMessageNeighborIdQueue.poll();
				this.vectorClock.increment(this.id);
				String vectorClockString = this.vectorClock.toString();

				unicast(nextMessageNeighborId,
						new Message(MessageType.MAP, this.id, vectorClockString));
				mapTotalMessagesSent++;
				if (mapMessageNeighborIdQueue.isEmpty()
						|| mapTotalMessagesSent == maxNumber) {
					mapDeactivate();
				}
				mapTimer = System.currentTimeMillis();
			}

			if (this.id == 0 && System.currentTimeMillis() - timer > this.snapshotDelay
					&& numSnapshotsCollectedThisRound == 0) {
				// Initiate Snapshot
				hasSentMarkerMessage = true;
				snapshotInProgress = true;
				this.vectorClock.increment(this.id);
				this.currentSnapshotVector = this.vectorClock.getClockVector();
				broadcast(new Message(MessageType.MARKER, this.id, "none"));
				timer = System.currentTimeMillis();
			}	
		}

		try (FileWriter fw = new FileWriter(
					"/home/012/j/ja/jac161530/CS6378/cs6378_project-2/config-" + this.id + ".out");
				BufferedWriter bw = new BufferedWriter(fw)) {
			for (Snapshot snapshot : snapshotsTaken) {
				int[] clockVector = snapshot.getClockVector();
				bw.write("" + clockVector[0]);
				for (int i = 1; i < clockVector.length; i++) {
					bw.write(" " + clockVector[i]);
				}
				bw.write("\n");
			}
			bw.flush();
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

	public void mapActivate() {
		this.mapActive = true;
	}

	public void mapDeactivate() {
		this.mapActive = false;
	}

	public boolean isMapActive() {
		return this.mapActive;
	}

	public void unicast(int neighborId, Message message) {
		Neighbor neighbor = neighbors.get(neighborId);

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

	public void broadcast(Message message) {
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

	public void buildMapMessageQueue() {
		// Assumes node has been activated prior to running
		Random random = new Random();
		int messageRange = maxPerActive - minPerActive;
		int messagesToSend = random.nextInt(messageRange) + minPerActive; 
		Set<Integer> neighborIdSet = neighbors.keySet();
		Integer[] neighborIds = neighborIdSet.toArray(new Integer[0]);

		for (int i = 0; i < messagesToSend; i++) {
			mapMessageNeighborIdQueue.add(neighborIds[random.nextInt(neighborIds.length)]);
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
					buildMapMessageQueue();
				}
			}
			break;
		case MARKER:
			this.vectorClock.increment(this.id);
			if (!hasSentMarkerMessage) {
				hasSentMarkerMessage = true;
				snapshotInProgress = true;	
				this.currentSnapshotVector = this.vectorClock.getClockVector();
				broadcast(new Message(MessageType.MARKER, this.id, "none"));
			}

			numMarkerMessagesReceived++;
			if (numMarkerMessagesReceived >= neighbors.size()) {
				snapshotInProgress = false;
				boolean active = isMapActive();
				boolean canBeReactivated = mapTotalMessagesSent < maxNumber;
				int numQueuedMessages = deferQueue.size();
				numMarkerMessagesReceived = 0;
				hasSentMarkerMessage = false;

				Snapshot snapshot
					= new Snapshot(active, canBeReactivated, numQueuedMessages, this.currentSnapshotVector);
				snapshotsTaken.add(snapshot);
				if (id == 0) {
					snapshotsCollectedThisRound[0] = snapshot;
					numSnapshotsCollectedThisRound++;
				} else {
					this.vectorClock.increment(this.id);
					unicast(parentNodeId,
							new Message(MessageType.SNAPSHOT, this.id, snapshot.toString()));
				}
			}

			break;
		case SNAPSHOT:
			this.vectorClock.increment(this.id);
			if (this.id == 0) {
				processSnapshotMessage(message);
			} else {
				this.vectorClock.increment(this.id);
				// Send it upward toward the root of the spanning tree.
				unicast(parentNodeId, message);
			}
			break;
		case HALT:
			this.vectorClock.increment(this.id);
			if (!hasSentHaltMessage) {
				this.vectorClock.increment(this.id);
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
				if (systemHasTerminated(snapshotsCollectedThisRound)) {
					this.vectorClock.increment(this.id);
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
