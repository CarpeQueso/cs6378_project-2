

public class VectorClock {

	private final int size;

	private int[] clock;

	public VectorClock(int clockSize) {
		this.size = clockSize;
		this.clock = new int[size];
	}

	public VectorClock(int clockSize, int[] initialValues) {
		this.size = clockSize;
		this.clock = new int[size];
		System.arraycopy(initialValues, 0, this.clock, 0, size);
	}

	public void increment(int index) {
		assert index >= 0 && index < size;

		clock[index]++;
	}

	/**
	 * Update this vector clock based on the given vector clock.
	 * The maximum value for a given position will be stored in this clock.
	 */
	public void update(VectorClock vc) {
		assert vc.getSize() == this.size;
		int[] passedClockVector = vc.getClockVector();

		for (int i = 0; i < this.size; i++) {
			if (passedClockVector[i] > this.clock[i]) {
				this.clock[i] = passedClockVector[i];
			}
		}
	}

	public VectorComparisonType compareTo(VectorClock vc) {
		if (this.size != vc.getSize()) {
			return VectorComparisonType.INCOMPARABLE;
		}

		int difference;
		VectorComparisonType tentativeComparisonType = VectorComparisonType.EQUAL;
		int[] passedClockVector = vc.getClockVector();

		for (int i = 0; i < this.size; i++) {
			difference = this.clock[i] - passedClockVector[i];

			if (difference > 0) {
				if (tentativeComparisonType == VectorComparisonType.LESS_THAN) {
					return VectorComparisonType.INCOMPARABLE;
				} else {
					tentativeComparisonType = VectorComparisonType.GREATER_THAN;
				}
			} else if (difference < 0) {
				if (tentativeComparisonType == VectorComparisonType.GREATER_THAN) {
					return VectorComparisonType.INCOMPARABLE;
				} else {
					tentativeComparisonType = VectorComparisonType.LESS_THAN;
				}
			}
		}
		return tentativeComparisonType; 
	}

	public int getClockValueAtIndex(int index) {
		return this.clock[index];
	}

	public int[] getClockVector() {
		int[] clockCopy = new int[this.size];

		System.arraycopy(clock, 0, clockCopy, 0, this.size);
		return clockCopy;
	}

	public int getSize() {
		return size;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder(this.size * 2);

		sb.append(clock[0]);
		for (int i = 1; i < this.size; i++) {
			sb.append(' ');
			sb.append(clock[i]);
		}

		return sb.toString();
	}

	public static VectorClock parseVectorClock(String clockString) {
		String[] clockValueStrings = clockString.split("\\s+");
		int[] clockValues = new int[clockValueStrings.length];

		for (int i = 0; i < clockValueStrings.length; i++) {
			clockValues[i] = Integer.parseInt(clockValueStrings[i]);
		}

		return new VectorClock(clockValues.length, clockValues);
	}
}
