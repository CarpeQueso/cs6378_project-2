

public class Snapshot {

	private final boolean active;

	private final boolean canBeReactivated;

	private final int numQueuedMessages;

	public Snapshot(boolean active, boolean canBeReactivated, int numQueuedMessages) {
		this.active = active;
		this.canBeReactivated = canBeReactivated;
		this.numQueuedMessages = numQueuedMessages;
	}

	public boolean isActive() {
		return this.active;
	}

	public boolean canBeReactivated() {
		return this.canBeReactivated;
	}

	public int getNumQueuedMessages() {
		return this.numQueuedMessages;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();

		sb.append(this.active);
		sb.append(",");
		sb.append(this.canBeReactivated);
		sb.append(",");
		sb.append(numQueuedMessages);

		return sb.toString();
	}

	public static Snapshot parseSnapshot(String s) {
		String[] snapshotComponentStrings = s.split(",");

		boolean active = Boolean.parseBoolean(snapshotComponentStrings[0]);
		boolean canBeReactivated = Boolean.parseBoolean(snapshotComponentStrings[1]);
		int numQueuedMessages = Integer.parseInt(snapshotComponentStrings[2]);

		return new Snapshot(active, canBeReactivated, numQueuedMessages);
	}
}
