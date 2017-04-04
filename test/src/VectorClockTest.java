import org.junit.*;


public class VectorClockTest {

	@Test public void Should__IncrementClockValueAtIndex1By1__When__IncrementCalled() {
		VectorClock vc = new VectorClock(3);
		int[] expectedClockVector = { 0, 1, 0 };

		vc.increment(1);

		Assert.assertArrayEquals(expectedClockVector, vc.getClockVector());
	}

	@Test public void Should__UpdateClockVectorBasedOnGivenClock__When__UpdateCalled() {
		int clockSize = 4;
	
		VectorClock clockToUpdate = new VectorClock(clockSize);
		int[] clockValues = { 2, 4, 1, 8 };
		int[] expectedClockValues = { 2, 5, 1, 8 };
		VectorClock clockToPass = new VectorClock(clockSize, clockValues);
	
		clockToUpdate.updateAndIncrement(clockToPass, 1);
	
		Assert.assertArrayEquals(expectedClockValues, clockToUpdate.getClockVector());
	}

	@Test public void Should__ReturnSpaceSeparatedClockVectorString__When__ToStringCalled() {
		int[] clockValues = { 0, 2, 4, 6 };
		VectorClock vc = new VectorClock(4, clockValues);

		Assert.assertEquals("0 2 4 6", vc.toString());
	}

	@Test public void Should__ReturnLessThanComparisonType__When__LargerVectorCompared() {
		int[] smallerClockVector = { 0, 1, 1, 3 };
		int[] largerClockVector = { 1, 4, 8, 8 };
		VectorClock smallerVC = new VectorClock(4, smallerClockVector);
		VectorClock largerVC = new VectorClock(4, largerClockVector);

		Assert.assertEquals(VectorComparisonType.LESS_THAN, smallerVC.compareTo(largerVC));
	}

	@Test public void Should__ReturnIncomparableComparisonType__When__VectorsAreIncomparable() {
		int[] clockVector1 = { 0, 1, 1, 3 };
		int[] clockVector2 = { 1, 0, 8, 8 };
		VectorClock vc1 = new VectorClock(4, clockVector1);
		VectorClock vc2 = new VectorClock(4, clockVector2);

		Assert.assertEquals(VectorComparisonType.INCOMPARABLE, vc1.compareTo(vc2));
	}
}
