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

import com.github.kilianB.hashAlgorithms.HashingAlgorithm;
import com.github.kilianB.hashAlgorithms.PerceptiveHash;
import com.github.kilianB.matcher.Hash;

class PerceptiveHashTest {

	private static BufferedImage ballon;
	// Similar images
	private static BufferedImage copyright;
	private static BufferedImage highQuality;
	private static BufferedImage lowQuality;
	private static BufferedImage thumbnail;

	@BeforeAll
	static void loadImages() {
		try {
			ballon = ImageIO.read(PerceptiveHashTest.class.getClassLoader().getResourceAsStream("ballon.jpg"));
			copyright = ImageIO.read(PerceptiveHashTest.class.getClassLoader().getResourceAsStream("copyright.jpg"));
			highQuality = ImageIO
					.read(PerceptiveHashTest.class.getClassLoader().getResourceAsStream("highQuality.jpg"));
			lowQuality = ImageIO.read(PerceptiveHashTest.class.getClassLoader().getResourceAsStream("lowQuality.jpg"));
			thumbnail = ImageIO.read(PerceptiveHashTest.class.getClassLoader().getResourceAsStream("thumbnail.jpg"));

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
				assertEquals(748566082, new PerceptiveHash(14).algorithmId()); //Was 748566082
			}, () -> {
				assertEquals(748566093, new PerceptiveHash(25).algorithmId()); //Was 748566093
			});
		}

		@Test
		@DisplayName("Unique AlgorithmsIds")
		public void uniquely() {

			int id0 = new PerceptiveHash(2).algorithmId();
			int id1 = new PerceptiveHash(14).algorithmId();
			int id2 = new PerceptiveHash(25).algorithmId();

			assertAll(() -> {
				assertNotEquals(id0, id1);
			}, () -> {
				assertNotEquals(id0, id2);
			}, () -> {
				assertNotEquals(id1, id2);
			});

		}

	}

	@Test
	void keyLength() {
		// To get comparable hashes the key length has to be consistent for all
		// resolution of images

		PerceptiveHash d1 = new PerceptiveHash(32);

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
		return Stream.of(new PerceptiveHash(15), new PerceptiveHash(20),
				new PerceptiveHash(200));
	}
}
