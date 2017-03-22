

public class Snapshot {

	private final boolean active;

	private final boolean canBeReactivated;

	private final int numQueuedMessages;

	private final int[] clockVector;

	public Snapshot(boolean active, boolean canBeReactivated, int numQueuedMessages,
					int[] clockVector) {
		this.active = active;
		this.canBeReactivated = canBeReactivated;
		this.numQueuedMessages = numQueuedMessages;
		this.clockVector = new int[clockVector.length];
		System.arraycopy(clockVector, 0, this.clockVector, 0, clockVector.length);
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

	public int[] getClockVector() {
		return this.clockVector;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();

		sb.append(this.active);
		sb.append(",");
		sb.append(this.canBeReactivated);
		sb.append(",");
		sb.append(this.numQueuedMessages);
		sb.append(",");
		sb.append(this.clockVector[0]);
		for (int i = 1; i < this.clockVector.length; i++) {
			sb.append(" ");
			sb.append(this.clockVector[i]);
		}

		return sb.toString();
	}

	public static Snapshot parseSnapshot(String s) {
		String[] snapshotComponentStrings = s.split(",");

		boolean active = Boolean.parseBoolean(snapshotComponentStrings[0]);
		boolean canBeReactivated = Boolean.parseBoolean(snapshotComponentStrings[1]);
		int numQueuedMessages = Integer.parseInt(snapshotComponentStrings[2]);
		String[] clockVectorStringValues = snapshotComponentStrings[3].split("\\s+");
		int[] clockVector = new int[clockVectorStringValues.length];
		for (int i = 0; i < clockVectorStringValues.length; i++) {
			clockVector[i] = Integer.parseInt(clockVectorStringValues[i]);
		}

		return new Snapshot(active, canBeReactivated, numQueuedMessages, clockVector);
	}
}
