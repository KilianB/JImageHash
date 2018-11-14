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

import com.github.kilianB.matcher.Hash;

/**
 * @author Kilian
 *
 */
class HogHashTest {

	private static BufferedImage ballon;
	// Similar images
	private static BufferedImage copyright;
	private static BufferedImage highQuality;
	private static BufferedImage lowQuality;
	private static BufferedImage thumbnail;
	private static BufferedImage lena;

	@BeforeAll
	static void loadImages() {
		try {
			ballon = ImageIO.read(AverageHashTest.class.getClassLoader().getResourceAsStream("ballon.jpg"));
			copyright = ImageIO.read(AverageHashTest.class.getClassLoader().getResourceAsStream("copyright.jpg"));
			highQuality = ImageIO
					.read(AverageHashTest.class.getClassLoader().getResourceAsStream("highQuality.jpg"));
			lowQuality = ImageIO.read(AverageHashTest.class.getClassLoader().getResourceAsStream("lowQuality.jpg"));
			thumbnail = ImageIO.read(AverageHashTest.class.getClassLoader().getResourceAsStream("thumbnail.jpg"));
			lena = ImageIO.read(AverageHashTest.class.getClassLoader().getResourceAsStream("Lenna.png"));
			//Distance
			
			HogHash h = new HogHash(32);
			
			Hash ballHash = h.hash(ballon);
			Hash copyHash = h.hash(copyright);
			Hash hqHash = h.hash(highQuality);
			Hash lqHash = h.hash(lowQuality);
			Hash thumHash = h.hash(thumbnail);
			Hash lenaHash = h.hash(lena);
			
			System.out.println("Similar images");
			System.out.printf("<tr><td>%s</td><td>%.3f</td></tr>%n","HQ - HQ: ",hqHash.normalizedHammingDistance(hqHash));
			System.out.printf("<tr><td>%s</td><td>%.3f</td></tr>%n","HQ - LQ: ",lqHash.normalizedHammingDistance(hqHash));
			System.out.printf("<tr><td>%s</td><td>%.3f</td></tr>%n","HQ - Copy: ",hqHash.normalizedHammingDistance(copyHash));
			System.out.printf("<tr><td>%s</td><td>%.3f</td></tr>%n","HQ - Thumb",hqHash.normalizedHammingDistance(thumHash));
			System.out.printf("<tr><td>%s</td><td>%.3f</td></tr>%n","LQ - Copy",lqHash.normalizedHammingDistance(copyHash));
			System.out.printf("<tr><td>%s</td><td>%.3f</td></tr>%n","LQ - Thumb",lqHash.normalizedHammingDistance(thumHash));
			System.out.printf("<tr><td>%s</td><td>%.3f</td></tr>%n","Copy - Thumb",copyHash.normalizedHammingDistance(thumHash));
			
			System.out.println("Unlike Images");
			System.out.printf("<tr><td>%s</td><td>%.3f</td></tr>%n","HQ - Ballon: ",hqHash.normalizedHammingDistance(ballHash));
			System.out.printf("<tr><td>%s</td><td>%.3f</td></tr>%n","HQ - Lena: ",hqHash.normalizedHammingDistance(lenaHash));
			System.out.printf("<tr><td>%s</td><td>%.3f</td></tr>%n","LQ - Ballon",lqHash.normalizedHammingDistance(ballHash));
			System.out.printf("<tr><td>%s</td><td>%.3f</td></tr>%n","LQ - Lena",lqHash.normalizedHammingDistance(lenaHash));
			System.out.printf("<tr><td>%s</td><td>%.3f</td></tr>%n","Copy - Ballon",copyHash.normalizedHammingDistance(ballHash));
			System.out.printf("<tr><td>%s</td><td>%.3f</td></tr>%n","Copy - Lena",copyHash.normalizedHammingDistance(lenaHash));
			System.out.printf("<tr><td>%s</td><td>%.3f</td></tr>%n","Thumb - Ballon",thumHash.normalizedHammingDistance(ballHash));
			System.out.printf("<tr><td>%s</td><td>%.3f</td></tr>%n","Thumb - Lena",thumHash.normalizedHammingDistance(lenaHash));

			/*
			 
			 0.0
				0.28098765432098766
				0.11703703703703704
				0.31209876543209875
				0.4049382716049383
				0.388641975308642
				0.4162962962962963
				Ballon
				0.4928395061728395
				0.4854320987654321
				0.4854320987654321
				0.5017283950617284
			 */
			
			/*
			 
			 0.0
				0.2915681639085894
				0.1194641449960599
				0.3186761229314421
				0.42206461780929866
				0.3955870764381403
				0.4239558707643814
				Ballon
				0.49613869188337273
				0.48447596532702913
				0.49141055949566587
				0.5059101654846335
			 */
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Nested
	@DisplayName("Algorithm Id")
	class AlgorithmId {

		/**
		 * The algorithms id shall stay consistent throughout different instances of the
		 * jvm. While simple hashcodes do not guarantee this behavior hash codes
		 * created from strings and integers are by contract consistent.
		 */
		@Test
		@DisplayName("Consistent AlgorithmIds")
		public void consistency() {

			assertAll(() -> {
				assertEquals(1626907603, new AverageHash(14).algorithmId());
			}, () -> {
				assertEquals(1626907944, new AverageHash(25).algorithmId());
			});
		}

		@Test
		@DisplayName("Unique AlgorithmsIds")
		public void uniquely() {

			int id0 = new HogHash(2).algorithmId();
			int id1 = new HogHash(14).algorithmId();
			int id2 = new HogHash(25).algorithmId();

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

		HogHash d1 = new HogHash(32);

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
	 * The hashes produced by the same algorithms shall return the same hash on successive 
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
		return Stream.of(new HogHash(15), new HogHash(20),
				new HogHash(200));
	}

}
