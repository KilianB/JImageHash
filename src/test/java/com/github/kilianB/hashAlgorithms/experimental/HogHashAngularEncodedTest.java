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
				assertEquals(431747525, new HogHashAngularEncoded(14).algorithmId());
			}, () -> {
				assertEquals(490852869, new HogHashAngularEncoded(25).algorithmId());
			});
		}
		
		@Test
		@DisplayName("Consistent AlgorithmIds v 2.0.0 collision")
		public void notVersionTwo() {
			assertAll(() -> {
				assertNotEquals(1815042658, new HogHashAngularEncoded(14).algorithmId());
			}, () -> {
				assertNotEquals(1816949282, new HogHashAngularEncoded(14).algorithmId());
			});
		}
		
		
	}

	@Test
	public void illegalConstructor() {
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
		
		@Override
		protected double differenceBallonHqHash() {
			return 71;
		}

		@Override
		protected double normDifferenceBallonHqHash() {
			return 71 / 144d;
		}
	}

}
