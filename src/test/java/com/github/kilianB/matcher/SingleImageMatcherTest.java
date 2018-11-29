package com.github.kilianB.matcher;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Map;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.github.kilianB.hashAlgorithms.AverageHash;
import com.github.kilianB.hashAlgorithms.HashingAlgorithm;
import com.github.kilianB.matcher.ImageMatcher.AlgoSettings;
import com.github.kilianB.matcher.ImageMatcher.Setting;

class SingleImageMatcherTest {

	private static BufferedImage ballon;
	// Similar images
	private static BufferedImage copyright;
	private static BufferedImage highQuality;
	private static BufferedImage lowQuality;
	private static BufferedImage thumbnail;

	@BeforeAll
	static void loadImages() {
		try {
			ballon = ImageIO.read(SingleImageMatcherTest.class.getClassLoader().getResourceAsStream("ballon.jpg"));
			copyright = ImageIO
					.read(SingleImageMatcherTest.class.getClassLoader().getResourceAsStream("copyright.jpg"));
			highQuality = ImageIO
					.read(SingleImageMatcherTest.class.getClassLoader().getResourceAsStream("highQuality.jpg"));
			lowQuality = ImageIO
					.read(SingleImageMatcherTest.class.getClassLoader().getResourceAsStream("lowQuality.jpg"));
			thumbnail = ImageIO
					.read(SingleImageMatcherTest.class.getClassLoader().getResourceAsStream("thumbnail.jpg"));

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Nested
	class CheckDefaultMatcher {
		@Test
		@DisplayName("Check Similarity Default")
		void imageMatches() {

			SingleImageMatcher matcher = SingleImageMatcher.createDefaultMatcher();

			// Identical
			assertAll("Identical images", () -> {
				assertTrue(matcher.checkSimilarity(ballon, ballon));
			}, () -> {
				assertTrue(matcher.checkSimilarity(copyright, copyright));
			}, () -> {
				assertTrue(matcher.checkSimilarity(highQuality, highQuality));
			}, () -> {
				assertTrue(matcher.checkSimilarity(lowQuality, lowQuality));
			}, () -> {
				assertTrue(matcher.checkSimilarity(thumbnail, thumbnail));
			});

			// Similar images
			assertAll("Similar images", () -> {
				assertTrue(matcher.checkSimilarity(highQuality, copyright));
			}, () -> {
				assertTrue(matcher.checkSimilarity(highQuality, lowQuality));
			}, () -> {
				assertTrue(matcher.checkSimilarity(highQuality, thumbnail));
			}, () -> {
				assertTrue(matcher.checkSimilarity(lowQuality, copyright));
			}, () -> {
				assertTrue(matcher.checkSimilarity(lowQuality, thumbnail));
			}, () -> {
				assertTrue(matcher.checkSimilarity(copyright, thumbnail));
			});

			// Mismatches
			assertAll("Mismtaches", () -> {
				assertFalse(matcher.checkSimilarity(highQuality, ballon));
			}, () -> {
				assertFalse(matcher.checkSimilarity(lowQuality, ballon));
			}, () -> {
				assertFalse(matcher.checkSimilarity(copyright, ballon));
			}, () -> {
				assertFalse(matcher.checkSimilarity(thumbnail, ballon));
			});

		}

		@Test
		@DisplayName("Check Similarity Forgiving")
		void imageMatchesForgiving() {

			SingleImageMatcher matcher = SingleImageMatcher.createDefaultMatcher(Setting.Forgiving);

			// Identical
			assertAll("Identical images", () -> {
				assertTrue(matcher.checkSimilarity(ballon, ballon));
			}, () -> {
				assertTrue(matcher.checkSimilarity(copyright, copyright));
			}, () -> {
				assertTrue(matcher.checkSimilarity(highQuality, highQuality));
			}, () -> {
				assertTrue(matcher.checkSimilarity(lowQuality, lowQuality));
			}, () -> {
				assertTrue(matcher.checkSimilarity(thumbnail, thumbnail));
			});

			// Similar images
			assertAll("Similar images", () -> {
				assertTrue(matcher.checkSimilarity(highQuality, copyright));
			}, () -> {
				assertTrue(matcher.checkSimilarity(highQuality, lowQuality));
			}, () -> {
				assertTrue(matcher.checkSimilarity(highQuality, thumbnail));
			}, () -> {
				assertTrue(matcher.checkSimilarity(lowQuality, copyright));
			}, () -> {
				assertTrue(matcher.checkSimilarity(lowQuality, thumbnail));
			}, () -> {
				assertTrue(matcher.checkSimilarity(copyright, thumbnail));
			});

			// Mismatches
			assertAll("Mismtaches", () -> {
				assertFalse(matcher.checkSimilarity(highQuality, ballon));
			}, () -> {
				assertFalse(matcher.checkSimilarity(lowQuality, ballon));
			}, () -> {
				assertFalse(matcher.checkSimilarity(copyright, ballon));
			}, () -> {
				assertFalse(matcher.checkSimilarity(thumbnail, ballon));
			});

		}

		@Test
		@DisplayName("Check Similarity Fair")
		void imageMatchesFair() {

			SingleImageMatcher matcher = SingleImageMatcher.createDefaultMatcher(Setting.Fair);

			// Identical
			assertAll("Identical images", () -> {
				assertTrue(matcher.checkSimilarity(ballon, ballon));
			}, () -> {
				assertTrue(matcher.checkSimilarity(copyright, copyright));
			}, () -> {
				assertTrue(matcher.checkSimilarity(highQuality, highQuality));
			}, () -> {
				assertTrue(matcher.checkSimilarity(lowQuality, lowQuality));
			}, () -> {
				assertTrue(matcher.checkSimilarity(thumbnail, thumbnail));
			});

			// Similar images
			assertAll("Similar images", () -> {
				assertTrue(matcher.checkSimilarity(highQuality, copyright));
			}, () -> {
				assertTrue(matcher.checkSimilarity(highQuality, lowQuality));
			}, () -> {
				assertTrue(matcher.checkSimilarity(highQuality, thumbnail));
			}, () -> {
				assertTrue(matcher.checkSimilarity(lowQuality, copyright));
			}, () -> {
				assertTrue(matcher.checkSimilarity(lowQuality, thumbnail));
			}, () -> {
				assertTrue(matcher.checkSimilarity(copyright, thumbnail));
			});

			// Mismatches
			assertAll("Mismtaches", () -> {
				assertFalse(matcher.checkSimilarity(highQuality, ballon));
			}, () -> {
				assertFalse(matcher.checkSimilarity(lowQuality, ballon));
			}, () -> {
				assertFalse(matcher.checkSimilarity(copyright, ballon));
			}, () -> {
				assertFalse(matcher.checkSimilarity(thumbnail, ballon));
			});
		}
	}

	@Test
	@DisplayName("Empty Matcher")
	void noAlgorithm() {
		SingleImageMatcher matcher = new SingleImageMatcher();
		BufferedImage dummyImage = new BufferedImage(1, 1, 0x1);
		assertThrows(IllegalStateException.class, () -> {
			matcher.checkSimilarity(dummyImage, dummyImage);
		});
	}

	@Test
	void addAndClearAlgorithms() {
		SingleImageMatcher matcher = new SingleImageMatcher();
		Map<HashingAlgorithm, AlgoSettings> algos = matcher.getAlgorithms();

		assertEquals(0, algos.size());
		matcher.addHashingAlgorithm(new AverageHash(14), 0.5f, true);
		assertEquals(1, algos.size());
		matcher.clearHashingAlgorithms();
		assertEquals(0, algos.size());
	}

}
