package com.github.kilianB.hashAlgorithms;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.stream.Stream;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.github.kilianB.hashAlgorithms.DifferenceHash;
import com.github.kilianB.hashAlgorithms.DifferenceHash.Precision;
import com.github.kilianB.matcher.Hash;
import com.github.kilianB.hashAlgorithms.HashingAlgorithm;

class DifferenceHashTest {

	private static BufferedImage ballon;
	// Similar images
	private static BufferedImage copyright;
	private static BufferedImage highQuality;
	private static BufferedImage lowQuality;
	private static BufferedImage thumbnail;

	@BeforeAll
	static void loadImages() {
		try {
			ballon = ImageIO.read(DifferenceHashTest.class.getClassLoader().getResourceAsStream("ballon.jpg"));
			copyright = ImageIO.read(DifferenceHashTest.class.getClassLoader().getResourceAsStream("copyright.jpg"));
			highQuality = ImageIO
					.read(DifferenceHashTest.class.getClassLoader().getResourceAsStream("highQuality.jpg"));
			lowQuality = ImageIO.read(DifferenceHashTest.class.getClassLoader().getResourceAsStream("lowQuality.jpg"));
			thumbnail = ImageIO.read(DifferenceHashTest.class.getClassLoader().getResourceAsStream("thumbnail.jpg"));

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

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
				assertEquals(228339096, new DifferenceHash(14, Precision.Simple).algorithmId());
			}, () -> {
				assertEquals(228339437, new DifferenceHash(25, Precision.Simple).algorithmId());
			}, () -> {
				assertEquals(-195332169, new DifferenceHash(14, Precision.Double).algorithmId());
			}, () -> {
				assertEquals(-195331828, new DifferenceHash(25, Precision.Double).algorithmId());
			}, () -> {
				assertEquals(265160772, new DifferenceHash(14, Precision.Triple).algorithmId());
			}, () -> {
				assertEquals(265161113, new DifferenceHash(25, Precision.Triple).algorithmId());
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

	@Test
	void keyLength() {
		// To get comparable hashes the key length has to be consistent for all
		// resolution of images

		DifferenceHash d1 = new DifferenceHash(32, Precision.Simple);

		Hash ballonHash = d1.hash(ballon);
		Hash copyrightHash = d1.hash(copyright);
		Hash lowQualityHash = d1.hash(lowQuality);
		Hash highQualityHash = d1.hash(highQuality);
		Hash thumbnailHash = d1.hash(thumbnail);

		assertAll(() -> {
			assertEquals(ballonHash.getHashValue().bitLength(), copyrightHash.getHashValue().bitLength());
		}, () -> {
			assertEquals(ballonHash.getHashValue().bitLength(), lowQualityHash.getHashValue().bitLength());
		}, () -> {
			assertEquals(ballonHash.getHashValue().bitLength(), highQualityHash.getHashValue().bitLength());
		}, () -> {
			assertEquals(ballonHash.getHashValue().bitLength(), thumbnailHash.getHashValue().bitLength());
		});

	}

	
	/**
	 * The hashes produced by the same algorithms shall return the same hash on sucessive 
	 * calls
	 * @param d1
	 */
	@ParameterizedTest
	@MethodSource(value = "algoInstances")
	void consitent(HashingAlgorithm d1) {
		assertEquals(d1.hash(ballon).getHashValue(), d1.hash(ballon).getHashValue());
	}

	/**
	 * The hemming distance of the same image has to be 0
	 * @deprecated not really a algorithm test case. Same as consistent
	 * @param d1
	 */
	@Deprecated
	@ParameterizedTest
	@MethodSource(value = "algoInstances")
	void equalImage(HashingAlgorithm d1) {
		assertEquals(0, d1.hash(ballon).hammingDistance(d1.hash(ballon)));
	}

	/**
	 * The hemming distance of similar images shall be lower than the distance of 
	 * vastly different picutres
	 * @param d1
	 */
	@ParameterizedTest
	@MethodSource(value = "algoInstances")
	void unequalImage(HashingAlgorithm d1) {
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

}
