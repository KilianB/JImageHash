package com.github.kilianB.hashAlgorithms;

import static org.junit.jupiter.api.Assertions.*;

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
				assertEquals(1308249156, new AverageColorHash(14).algorithmId());
			}, () -> {
				assertEquals(1308250148, new AverageColorHash(25).algorithmId());
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
		
	}

}
