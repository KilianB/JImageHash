package com.github.kilianB.hashAlgorithms;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

import com.github.kilianB.matcher.Hash;

/**
 * @author Kilian
 * @since 2.0.0
 */
class RotPHashTest {

	private static BufferedImage ballon;
	// Similar images
	private static BufferedImage copyright;
	private static BufferedImage highQuality;
	private static BufferedImage lowQuality;
	private static BufferedImage thumbnail;
	private static BufferedImage lenna;
	private static BufferedImage lenna90;
	private static BufferedImage lenna180;
	private static BufferedImage lenna270;

	// Rotational images

	@BeforeAll
	static void loadImages() {
		try {
			ballon = ImageIO.read(RotPHashTest.class.getClassLoader().getResourceAsStream("ballon.jpg"));
			copyright = ImageIO.read(RotPHashTest.class.getClassLoader().getResourceAsStream("copyright.jpg"));
			highQuality = ImageIO.read(RotPHashTest.class.getClassLoader().getResourceAsStream("highQuality.jpg"));
			lowQuality = ImageIO.read(RotPHashTest.class.getClassLoader().getResourceAsStream("lowQuality.jpg"));
			thumbnail = ImageIO.read(RotPHashTest.class.getClassLoader().getResourceAsStream("thumbnail.jpg"));

			lenna = ImageIO.read(RotPHashTest.class.getClassLoader().getResourceAsStream("Lenna.png"));
			lenna90 = ImageIO.read(RotPHashTest.class.getClassLoader().getResourceAsStream("Lenna90.png"));
			lenna180 = ImageIO.read(RotPHashTest.class.getClassLoader().getResourceAsStream("Lenna180.png"));
			lenna270 = ImageIO.read(RotPHashTest.class.getClassLoader().getResourceAsStream("Lenna270.png"));

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

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
				assertEquals(1884687159, new RotPHash(14, false).algorithmId());
			}, () -> {
				assertEquals(1884687494, new RotPHash(25, true).algorithmId());
			});
		}

		@Test
		@DisplayName("Unique AlgorithmsIds")
		public void uniquely() {

			int id0 = new RotPHash(2, false).algorithmId();
			int id1 = new RotPHash(14, false).algorithmId();
			int id2 = new RotPHash(25, false).algorithmId();
			int id3 = new RotPHash(2, true).algorithmId();
			int id4 = new RotPHash(14, true).algorithmId();

			assertAll(() -> {
				assertNotEquals(id0, id1);
			}, () -> {
				assertNotEquals(id0, id2);
			}, () -> {
				assertNotEquals(id0, id3);
			}, () -> {
				assertNotEquals(id0, id4);
			}, () -> {
				assertNotEquals(id1, id2);
			}, () -> {
				assertNotEquals(id1, id3);
			}, () -> {
				assertNotEquals(id1, id4);
			}, () -> {
				assertNotEquals(id2, id3);
			}, () -> {
				assertNotEquals(id2, id4);
			}, () -> {
				assertNotEquals(id3, id4);
			});

		}

	}

	@Test
	void keyLength() {
		// To get comparable hashes the key length has to be consistent for all
		// resolution of images

		RotPHash d1 = new RotPHash(32, false);

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
	 * RotPHash has a setting specifying that the key length is truly the one
	 * speicfied
	 */
	@Test
	void keyLengthExact() {

		HashingAlgorithm hasher = new RotPHash(5, true);
		assertEquals(5, hasher.hash(lenna).getHashValue().bitLength());

		hasher = new RotPHash(25, true);
		assertEquals(25, hasher.hash(lenna).getHashValue().bitLength());

		hasher = new RotPHash(200, true);
		assertEquals(200, hasher.hash(lenna).getHashValue().bitLength());
	}

	/**
	 * The hashes produced by the same algorithms shall return the same hash on
	 * successive calls
	 * 
	 * @param d1
	 */
	@ParameterizedTest
	@MethodSource(value = "algoInstances")
	void consitent(HashingAlgorithm d1) {
		assertEquals(d1.hash(ballon).getHashValue(), d1.hash(ballon).getHashValue());
	}

	/**
	 * The hemming distance of the same image has to be 0
	 * 
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
	 * 
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

	@ParameterizedTest
	@MethodSource(value = "algoInstances")
	void rotatedImages(HashingAlgorithm h) {

		Hash rot0 = h.hash(lenna);
		Hash rot90 = h.hash(lenna90);
		Hash rot180 = h.hash(lenna180);
		Hash rot270 = h.hash(lenna270);
		assertAll(() -> {
			assertTrue(rot0.normalizedHammingDistance(rot90) < 0.1);
		}, () -> {
			assertTrue(rot90.normalizedHammingDistance(rot180) < 0.1);
		}, () -> {
			assertTrue(rot180.normalizedHammingDistance(rot270) < 0.1);
		});
	}

	@SuppressWarnings("unused")
	private static Stream<HashingAlgorithm> algoInstances() {
		return Stream.of(new RotPHash(15, true), new RotPHash(20, true), new RotPHash(200, true));
	}

}
