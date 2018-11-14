package com.github.kilianB.hashAlgorithms;

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

import javax.imageio.ImageIO;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.github.kilianB.matcher.Hash;

class AverageHashTest {

	private static BufferedImage ballon;
	// Similar images
	private static BufferedImage copyright;
	private static BufferedImage highQuality;
	private static BufferedImage lowQuality;
	private static BufferedImage thumbnail;

	@BeforeAll
	static void loadImages() {
		try {
			ballon = ImageIO.read(AverageHashTest.class.getClassLoader().getResourceAsStream("ballon.jpg"));
			copyright = ImageIO.read(AverageHashTest.class.getClassLoader().getResourceAsStream("copyright.jpg"));
			highQuality = ImageIO.read(AverageHashTest.class.getClassLoader().getResourceAsStream("highQuality.jpg"));
			lowQuality = ImageIO.read(AverageHashTest.class.getClassLoader().getResourceAsStream("lowQuality.jpg"));
			thumbnail = ImageIO.read(AverageHashTest.class.getClassLoader().getResourceAsStream("thumbnail.jpg"));

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
				assertEquals(1626907296, new AverageHash(14).algorithmId());
			}, () -> {
				assertEquals(1626907328, new AverageHash(25).algorithmId());
			});
		}

		/**
		 * The algorithm ids shall not collide
		 */
		@Test
		@DisplayName("Unique AlgorithmsIds")
		public void uniquely() {

			int id0 = new AverageHash(2).algorithmId();
			int id1 = new AverageHash(14).algorithmId();
			int id2 = new AverageHash(25).algorithmId();

			assertAll(() -> {
				assertNotEquals(id0, id1);
			}, () -> {
				assertNotEquals(id0, id2);
			}, () -> {
				assertNotEquals(id1, id2);
			});
		}
	}

	
	@Nested
	@DisplayName("Serialization")
	class Serizalization{
		
		HashingAlgorithm originalAlgo;
		HashingAlgorithm deserializedAlgo;
		
		@BeforeEach
		void serializeAlgo() {
			originalAlgo = new AverageHash(32);
		
			File serFile = new File("AverageHash.ser");
			
			//Write to file
			try(ObjectOutputStream os = new ObjectOutputStream(new FileOutputStream(serFile))){
				os.writeObject(originalAlgo);
			} catch (IOException e) {
				e.printStackTrace();
			}
			//Read from file
			try(ObjectInputStream is = new ObjectInputStream(new FileInputStream(serFile))){
				deserializedAlgo = (HashingAlgorithm) is.readObject();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}finally {
				if(serFile.exists()) {
					serFile.delete();
				}
			}
		}
		
		@Test
		void consistentId() {
			assertEquals(originalAlgo.algorithmId(),deserializedAlgo.algorithmId());
		}
		@Test
		void consistentHash() {
			assertEquals(originalAlgo.hash(ballon),deserializedAlgo.hash(ballon));
		}
	}
	
	
	@Test
	void keyLength() {
		// To get comparable hashes the key length has to be consistent for all
		// resolution of images

		AverageHash d1 = new AverageHash(32);

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
	 * @param hasher
	 */
	@ParameterizedTest
	@MethodSource(value = "algoInstancesBroad")
	void keyLengthMinimumBits(HashingAlgorithm hasher) {
		assertTrue(hasher.hash(ballon).getBitResolution() >= hasher.bitResolution);
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

	@SuppressWarnings("unused")
	private static Stream<HashingAlgorithm> algoInstances() {
		return Stream.of(new AverageHash(15), new AverageHash(20), new AverageHash(200));
	}
	
	@SuppressWarnings("unused")
	private static Stream<HashingAlgorithm> algoInstancesBroad() {
		HashingAlgorithm[] hasher = new HashingAlgorithm[98];
		for(int i = 2; i < 100; i++) {
			hasher[i-2] = new AverageHash(i);
		}
		return Stream.of(hasher);
	}

}
