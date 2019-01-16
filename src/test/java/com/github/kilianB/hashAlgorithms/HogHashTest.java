package com.github.kilianB.hashAlgorithms;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.github.kilianB.hashAlgorithms.experimental.HogHash;

/**
 * @author Kilian
 *
 */
@SuppressWarnings("deprecation")
class HogHashTest {

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
				assertEquals(-1909360295, new HogHash(14).algorithmId());
			}, () -> {
				assertEquals(-1850254951, new HogHash(25).algorithmId());
			});
		}
		
		@Test
		@DisplayName("Consistent AlgorithmIds v 2.0.0 collision")
		public void notVersionTwo() {
			assertAll(() -> {
				assertNotEquals(769691726, new HogHash(14).algorithmId());
			}, () -> {
				assertNotEquals(771598350, new HogHash(25).algorithmId());
			});
		}
	}

	@Test
	public void illegalConstructor() {
		assertThrows(IllegalArgumentException.class, () -> {
			new HogHash(2);
		});
	}

	// Base Hashing algorithm tests
	@Nested
	class AlgorithmBaseTests extends HashTestBase {

		@Override
		protected HashingAlgorithm getInstance(int bitResolution) {
			return new HogHash(bitResolution);
		}
		
		//Hog hash requires higher bit resolution. override default offset
		@Override
		protected int offsetBitResolution() {
			return 10;
		}
		
		@Override
		protected double differenceBallonHqHash() {
			return 50;
		}

		@Override
		protected double normDifferenceBallonHqHash() {
			return 50 / 144d;
		}
	}

}
