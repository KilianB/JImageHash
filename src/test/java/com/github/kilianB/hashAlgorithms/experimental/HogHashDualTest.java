package com.github.kilianB.hashAlgorithms.experimental;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.github.kilianB.hashAlgorithms.HashTestBase;
import com.github.kilianB.hashAlgorithms.HashingAlgorithm;

/**
 * @author Kilian
 *
 */
@SuppressWarnings("deprecation")
class HogHashDualTest {

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
				assertEquals(-1320953867, new HogHashDual(14).algorithmId());
			}, () -> {
				assertEquals(-1261848523, new HogHashDual(25).algorithmId());
			});
		}
		
		@Test
		@DisplayName("Consistent AlgorithmIds v 2.0.0 collision")
		public void notVersionTwo() {
			assertAll(() -> {
				assertNotEquals(234483250, new HogHashDual(14).algorithmId());
			}, () -> {
				assertNotEquals(236389874, new HogHashDual(14).algorithmId());
			});
		}
		
	}

	@SuppressWarnings("deprecation")
	@Test
	public void illegalConstructor() {
		assertThrows(IllegalArgumentException.class, () -> {
			new HogHashDual(2);
		});
	}

	// Base Hashing algorithm tests
	@Nested
	class AlgorithmBaseTests extends HashTestBase {

		@Override
		protected HashingAlgorithm getInstance(int bitResolution) {
			return new HogHashDual(bitResolution);
		}
		
		@Override
		protected double differenceBallonHqHash() {
			return 66;
		}

		@Override
		protected double normDifferenceBallonHqHash() {
			return 66 / 144d;
		}
	}

}
