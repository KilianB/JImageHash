package com.github.kilianB.hashAlgorithms;

import static com.github.kilianB.TestResources.ballon;
import static com.github.kilianB.TestResources.copyright;
import static com.github.kilianB.TestResources.highQuality;
import static com.github.kilianB.TestResources.lowQuality;
import static com.github.kilianB.TestResources.thumbnail;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.github.kilianB.TestResources;
import com.github.kilianB.hash.Hash;
import com.github.kilianB.hashAlgorithms.DifferenceHash.Precision;

//TODO  move difference hash to the default test scenarios
class DifferenceHashTest {

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
				assertEquals(712227304, new DifferenceHash(14, Precision.Simple).algorithmId());
			}, () -> {
				assertEquals(742703497, new DifferenceHash(25, Precision.Simple).algorithmId());
			}, () -> {
				assertEquals(2030454633, new DifferenceHash(14, Precision.Double).algorithmId());
			}, () -> {
				assertEquals(2060930826, new DifferenceHash(25, Precision.Double).algorithmId());
			}, () -> {
				assertEquals(-1844850756, new DifferenceHash(14, Precision.Triple).algorithmId());
			}, () -> {
				assertEquals(-1814374563, new DifferenceHash(25, Precision.Triple).algorithmId());
			});
		}

		@Test
		@DisplayName("Consistent AlgorithmIds v 2.0.0 collision")
		public void notVersionTwo() {
			assertAll(() -> {
				assertNotEquals(-115572257, new DifferenceHash(14, Precision.Simple).algorithmId());
			}, () -> {
				assertNotEquals(-114589154, new DifferenceHash(25, Precision.Simple).algorithmId());
			}, () -> {
				assertNotEquals(758235198, new DifferenceHash(14, Precision.Double).algorithmId());
			}, () -> {
				assertNotEquals(759218301, new DifferenceHash(25, Precision.Double).algorithmId());
			}, () -> {
				assertNotEquals(910320011, new DifferenceHash(14, Precision.Triple).algorithmId());
			}, () -> {
				assertNotEquals(911303114, new DifferenceHash(25, Precision.Triple).algorithmId());
			});
		}
		
		@Test
		@DisplayName("Unique AlgorithmsIds")
		public void uniquely() {

			int id0 = new DifferenceHash(2, Precision.Simple).algorithmId();
			int id1 = new DifferenceHash(14, Precision.Simple).algorithmId();
			int id2 = new DifferenceHash(14, Precision.Double).algorithmId();
			int id3 = new DifferenceHash(2, Precision.Triple).algorithmId();

			assertAll(() -> {
				assertNotEquals(id0, id1);
			}, () -> {
				assertNotEquals(id0, id2);
			}, () -> {
				assertNotEquals(id0, id3);
			}, () -> {
				assertNotEquals(id1, id2);
			}, () -> {
				assertNotEquals(id1, id3);
			}, () -> {
				assertNotEquals(id2, id3);
			});

		}
	}

	/**
	 * The difference hash has the interesting property that it's hashes image
	 * representation if hashed is somewhat the opposite of the original hash
	 * <p>
	 * This only works if the hashes are perfectly aligned. With this test we can
	 * make sure that bits are not shifted.
	 * <p>
	 * For difference hash this only works for single precision since the image
	 * changes a lot if we cramp the other precisions into the image as well.
	 */
	@Test
	void toImageTest() {
		HashingAlgorithm hasher = new DifferenceHash(128, Precision.Simple);

		Hash ballonHash = hasher.hash(TestResources.ballon);
		BufferedImage imageOfHash = ballonHash.toImage(10);
		Hash hashedImage = hasher.hash(imageOfHash);
		assertTrue(ballonHash.normalizedHammingDistance(hashedImage) > 0.8d);
	}

	@Nested
	@DisplayName("Serialization")
	class Serizalization {

		private HashingAlgorithm originalAlgo;
		private HashingAlgorithm deserializedAlgo;

		@BeforeEach
		public void serializeAlgo() {
			originalAlgo = new DifferenceHash(32, Precision.Double);

			File serFile = new File("AverageHash.ser");

			// Write to file
			try (ObjectOutputStream os = new ObjectOutputStream(new FileOutputStream(serFile))) {
				os.writeObject(originalAlgo);
			} catch (IOException e) {
				e.printStackTrace();
			}
			// Read from file
			try (ObjectInputStream is = new ObjectInputStream(new FileInputStream(serFile))) {
				deserializedAlgo = (HashingAlgorithm) is.readObject();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			} finally {
				if (serFile.exists()) {
					serFile.delete();
				}
			}
		}

		@Test
		public void consistentId() {
			assertEquals(originalAlgo.algorithmId(), deserializedAlgo.algorithmId());
		}

		@Test
		public void consistentHash() {
			assertEquals(originalAlgo.hash(ballon), deserializedAlgo.hash(ballon));
		}
	}

	@Test
	public void keyLength() {
		// To get comparable hashes the key length has to be consistent for all
		// resolution of images

		DifferenceHash d1 = new DifferenceHash(32, Precision.Simple);

		Hash ballonHash = d1.hash(ballon);
		Hash copyrightHash = d1.hash(copyright);
		Hash lowQualityHash = d1.hash(lowQuality);
		Hash highQualityHash = d1.hash(highQuality);
		Hash thumbnailHash = d1.hash(thumbnail);

		assertAll(() -> {
			assertEquals(ballonHash.getBitResolution(), copyrightHash.getBitResolution());
		}, () -> {
			assertEquals(ballonHash.getBitResolution(), lowQualityHash.getBitResolution());
		}, () -> {
			assertEquals(ballonHash.getBitResolution(), highQualityHash.getBitResolution());
		}, () -> {
			assertEquals(ballonHash.getBitResolution(), thumbnailHash.getBitResolution());
		});

	}

	/**
	 * The hash length of the algorithm is at least the supplied bits long
	 * 
	 * @param hasher
	 */
	@ParameterizedTest
	@MethodSource(value = "algoInstancesBroad")
	public void keyLengthMinimumBits(HashingAlgorithm hasher) {
		assertTrue(hasher.hash(ballon).getBitResolution() >= hasher.bitResolution);
	}

	/**
	 * The hashes produced by the same algorithms shall return the same hash on
	 * sucessive calls
	 * 
	 * @param d1
	 */
	@ParameterizedTest
	@MethodSource(value = "algoInstances")
	public void consitent(HashingAlgorithm d1) {
		assertEquals(d1.hash(ballon).getHashValue(), d1.hash(ballon).getHashValue());
	}

	/**
	 * < * The hamming distance of the same image has to be 0
	 * 
	 * @deprecated not really a algorithm test case. Same as consistent
	 * @param d1
	 */
	@Deprecated
	@ParameterizedTest
	@MethodSource(value = "algoInstances")
	public void equalImage(HashingAlgorithm d1) {
		assertEquals(0, d1.hash(ballon).hammingDistance(d1.hash(ballon)));
	}

	/**
	 * The hamming distance of similar images shall be lower than the distance of
	 * vastly different pictures
	 * 
	 * @param d1
	 */
	@ParameterizedTest
	@MethodSource(value = "algoInstances")
	public void unequalImage(HashingAlgorithm d1) {
		Hash lowQualityHash = d1.hash(lowQuality);
		Hash highQualityHash = d1.hash(highQuality);
		Hash ballonHash = d1.hash(ballon);

		assertAll(() -> {
			assertTrue(lowQualityHash.hammingDistance(highQualityHash) < lowQualityHash.hammingDistance(ballonHash));
		}, () -> {
			assertTrue(highQualityHash.hammingDistance(lowQualityHash) < highQualityHash.hammingDistance(ballonHash));
		});
	}

	@SuppressWarnings("unused")
	private static Stream<HashingAlgorithm> algoInstances() {
		return Stream.of(new DifferenceHash(32, Precision.Simple), new DifferenceHash(32, Precision.Double),
				new DifferenceHash(32, Precision.Triple));
	}

	@SuppressWarnings("unused")
	private static Stream<HashingAlgorithm> algoInstancesBroad() {
		HashingAlgorithm[] hasher = new HashingAlgorithm[98];
		for (int i = 2; i < 100; i++) {
			hasher[i - 2] = new DifferenceHash(i, Precision.Simple);
		}
		return Stream.of(hasher);
	}

}
