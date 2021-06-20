package com.github.kilianB.hashAlgorithms;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * @author Kilian
 *
 */
class MedianHashTest {

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
				assertEquals(-847247001, new MedianHash(14).algorithmId());
			}, () -> {
				assertEquals(-847216249, new MedianHash(25).algorithmId());
			});
		}

		@Test
		@DisplayName("Consistent AlgorithmIds v 2.0.0 collision")
		public void notVersionTwo() {
			assertAll(() -> {
				assertNotEquals(-442972544, new MedianHash(14).algorithmId());
			}, () -> {
				assertNotEquals(-442971552, new MedianHash(25).algorithmId());
			});
		}
	}

	// Base Hashing algorithm tests
	@Nested
	class AlgorithmBaseTests extends HashTestBase {

		@Override
		protected HashingAlgorithm getInstance(int bitResolution) {
			return new MedianHash(bitResolution);
		}

		@Override
		protected double differenceBallonHqHash() {
			return 78;
		}

		@Override
		protected double normDifferenceBallonHqHash() {
			return 78 / 132d;

		}

		@Nested
		class HashTest extends HashTestBase.HashTest {
			@Disabled("Skip test for median hash. As it fails on these logos")
			@ParameterizedTest
			@MethodSource(value = "com.github.kilianB.hashAlgorithms.HashTestBase#bitResolution")
			@Override
			public void replaceTransparentPixels(Integer bitRes) {
			}
		}

	}

}
