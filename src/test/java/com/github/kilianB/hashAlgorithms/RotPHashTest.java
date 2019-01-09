package com.github.kilianB.hashAlgorithms;
import static com.github.kilianB.TestResources.lenna;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * @author Kilian
 * @since 2.0.0
 */
class RotPHashTest {

	@Nested
	@DisplayName("Algorithm Id")
	class AlgorithmId {

		/**
		 * The algorithms id shall stay consistent throughout different instances of the
		 * jvm. While simple hashcodes do not guarantee this behavior hash codes created
		 * from strings and integers are by contract consistent.
		 */
		@Test
		@DisplayName("Consistent AlgorithmIds")
		public void consistency() {

			assertAll(() -> {
				assertEquals(-1427552469, new RotPHash(14, false).algorithmId());
			}, () -> {
				assertEquals(-1425651611, new RotPHash(25, true).algorithmId());
			});
		}
		
		@Test
		@DisplayName("Consistent AlgorithmIds v 2.0.0 collision")
		public void notVersionTwo() {
			assertAll(() -> {
				assertNotEquals(-1292976068, new RotPHash(14, false).algorithmId());
			}, () -> {
				assertNotEquals(-1292914750, new RotPHash(25, true).algorithmId());
			});
		}
	}


	/**
	 * RotPHash has a setting specifying that the key length is truly the one
	 * specified
	 */
	@Test
	void keyLengthExact() {

		HashingAlgorithm hasher = new RotPHash(5, true);
		assertEquals(5, hasher.hash(lenna).getBitResolution());

		hasher = new RotPHash(25, true);
		assertEquals(25, hasher.hash(lenna).getBitResolution());

		hasher = new RotPHash(200, true);
		assertEquals(200, hasher.hash(lenna).getBitResolution());
	}

	//Base Hashing algorithm tests
	@Nested
	class AlgorithmBaseTests extends RotationalTestBase{
		@Override
		protected HashingAlgorithm getInstance(int bitResolution) {
			return new RotPHash(bitResolution);
		}
		
		/**
		 * PHash requires a higher bit resolution.
		 */
		protected int offsetBitResolution() {
			return 10;
		}
		
		@Override
		protected double differenceBallonHqHash() {
			return 54;
		}

		@Override
		protected double normDifferenceBallonHqHash() {
			return 54 / 137d;
		}
		
	}

}
