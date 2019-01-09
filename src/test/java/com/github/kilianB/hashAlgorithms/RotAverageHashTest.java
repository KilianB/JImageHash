package com.github.kilianB.hashAlgorithms;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * @author Kilian
 *
 */
class RotAverageHashTest {

	/**
	 * The algorithms id shall stay consistent throughout different instances of the
	 * jvm. While simple hashcodes do not guarantee this behavior hash codes created
	 * from strings and integers are by contract consistent.
	 */
	@Test
	@DisplayName("Consistent AlgorithmIds")
	public void consistency() {

		assertAll(() -> {
			assertEquals(2086431459, new RotAverageHash(14).algorithmId());
		}, () -> {
			assertEquals(2087108003, new RotAverageHash(25).algorithmId());
		});
	}

	@Test
	@DisplayName("Consistent AlgorithmIds v 2.0.0 collision")
	public void notVersionTwo() {
		assertAll(() -> {
			assertNotEquals(-1456716412, new RotAverageHash(14).algorithmId());
		}, () -> {
			assertNotEquals(-1456694588, new RotAverageHash(25).algorithmId());
		});
	}

//Base Hashing algorithm tests
	@Nested
	class AlgorithmBaseTests extends RotationalTestBase {
		@Override
		protected HashingAlgorithm getInstance(int bitResolution) {
			return new RotAverageHash(bitResolution);
		}

		@Override
		protected double differenceBallonHqHash() {
			return 65;
		}

		@Override
		protected double normDifferenceBallonHqHash() {
			return 65 / 128d;
		}
	}
}
