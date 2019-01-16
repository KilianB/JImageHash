package com.github.kilianB.hashAlgorithms;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.awt.image.BufferedImage;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.github.kilianB.TestResources;
import com.github.kilianB.hash.Hash;

class AverageHashTest {

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
				assertEquals(89815718, new AverageHash(14).algorithmId());
			}, () -> {
				assertEquals(89846470, new AverageHash(25).algorithmId());
			});
		}
		
		@Test
		@DisplayName("Consistent AlgorithmIds v 2.0.0 collision")
		public void notVersionTwo() {
			assertAll(() -> {
				assertNotEquals(-1105481375, new AverageHash(14).algorithmId());
			}, () -> {
				assertNotEquals(-1105480383, new AverageHash(14).algorithmId());
			});
		}
	}

	/**
	 * The average hash has the interesting property that it's hashes image
	 * representation if hashed is the exact opposite of the first hash.
	 * <p>
	 * This only works if the hashes are perfectly aligned. With this test we can make
	 * sure that bits are not shifted
	 */
	@Test
	void toImageTest() {
		AverageHash hasher = new AverageHash(512);

		Hash ballonHash = hasher.hash(TestResources.ballon);
		BufferedImage imageOfHash = ballonHash.toImage(10);
		Hash hashedImage = hasher.hash(imageOfHash);
		assertEquals(1,ballonHash.normalizedHammingDistance(hashedImage));
	}

	// Base Hashing algorithm tests
	@Nested
	class AlgorithmBaseTests extends HashTestBase {
		@Override
		protected HashingAlgorithm getInstance(int bitResolution) {
			return new AverageHash(bitResolution);
		}

		@Override
		protected double differenceBallonHqHash() {
			return 77;
		}

		@Override
		protected double normDifferenceBallonHqHash() {
			return 77 / 132d;
		}
	}

}
