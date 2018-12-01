package com.github.kilianB.hashAlgorithms.experimental;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.github.kilianB.hashAlgorithms.AverageHash;
import com.github.kilianB.hashAlgorithms.HashTestBase;
import com.github.kilianB.hashAlgorithms.HashingAlgorithm;

/**
 * @author Kilian
 *
 */
class HogHashAngularEncodedTest {

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
				assertEquals(-1105481375, new AverageHash(14).algorithmId());
			}, () -> {
				assertEquals(-1105480383, new AverageHash(25).algorithmId());
			});
		}
	}

	@Test
	void illegalConstructor() {
		assertThrows(IllegalArgumentException.class, () -> {
			new HogHashAngularEncoded(2);
		});
	}

	// Base Hashing algorithm tests
	@Nested
	class AlgorithmBaseTests extends HashTestBase {

		@Override
		protected HashingAlgorithm getInstance(int bitResolution) {
			return new HogHashAngularEncoded(bitResolution);
		}
	}

}
