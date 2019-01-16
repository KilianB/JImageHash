package com.github.kilianB.hashAlgorithms;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class PerceptiveHashTest {

	@Nested
	@DisplayName("Algorithm Id")
	class AlgorithmId {

		/**
		 * The algorithms id shall stay consistent throughout different instances of the
		 * jvm. While simple hashcodes do not guarantee this behaviour hash codes
		 * created from strings and integers are by contract consistent.
		 */
		@Test
		@DisplayName("Consistent AlgorithmIds")
		public void consistency() {

			assertAll(() -> {
				assertEquals(-1437024773, new PerceptiveHash(14).algorithmId()); // Was 748566082
			}, () -> {
				assertEquals(-1433211525, new PerceptiveHash(25).algorithmId()); // Was 748566093
			});
		}

		@Test
		@DisplayName("Consistent AlgorithmIds v 2.0.0 collision")
		public void notVersionTwo() {
			assertAll(() -> {
				assertNotEquals(1062023020, new PerceptiveHash(14).algorithmId());
			}, () -> {
				assertNotEquals(1062146028, new PerceptiveHash(25).algorithmId());
			});
		}
	}

	// Base Hashing algorithm tests
	@Nested
	class AlgorithmBaseTests extends HashTestBase {

		@Override
		protected HashingAlgorithm getInstance(int bitResolution) {
			return new PerceptiveHash(bitResolution);
		}

		@Override
		protected double differenceBallonHqHash() {
			return 67;
		}

		@Override
		protected double normDifferenceBallonHqHash() {
			return 67 / 132d;
		}
	}

}
