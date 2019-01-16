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
class AverageColorHashTest {

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
				assertEquals(1901018147, new AverageColorHash(14).algorithmId());
			}, () -> {
				assertEquals(1901048899, new AverageColorHash(25).algorithmId());
			});
		}
		
		@Test
		@DisplayName("Consistent AlgorithmIds v 2.0.0 collision")
		public void notVersionTwo() {
			assertAll(() -> {
				assertNotEquals(1308249156, new AverageColorHash(14).algorithmId());
			}, () -> {
				assertNotEquals(1308249156, new AverageColorHash(14).algorithmId());
			});
		}
	}
	
	//Base Hashing algorithm tests
	@Nested
	class AlgorithmBaseTests extends HashTestBase{

		@Override
		protected HashingAlgorithm getInstance(int bitResolution) {
			return new AverageColorHash(bitResolution);
		}
		
		@Override
		protected double differenceBallonHqHash() {
			return 76;
		}

		@Override
		protected double normDifferenceBallonHqHash() {
			return 76 / 132d;
		}
		
	}

}
