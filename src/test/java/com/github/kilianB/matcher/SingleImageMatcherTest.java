package com.github.kilianB.matcher;

import static com.github.kilianB.TestResources.ballon;
import static com.github.kilianB.TestResources.copyright;
import static com.github.kilianB.TestResources.highQuality;
import static com.github.kilianB.TestResources.lowQuality;
import static com.github.kilianB.TestResources.thumbnail;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.github.kilianB.hashAlgorithms.AverageHash;
import com.github.kilianB.hashAlgorithms.HashingAlgorithm;
import com.github.kilianB.matcher.ImageMatcher.AlgoSettings;
import com.github.kilianB.matcher.ImageMatcher.Setting;
import com.github.kilianB.matcher.unsupervised.SingleImageMatcher;

class SingleImageMatcherTest {

	@Nested
	class CheckDefaultMatcher {

		void assertMatches(SingleImageMatcher matcher) {
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
		@DisplayName("Check Similarity Default")
		void imageMatches() {

			SingleImageMatcher matcher = SingleImageMatcher.createDefaultMatcher();
			assertMatches(matcher);
		}

		@Test
		@DisplayName("Check Similarity Forgiving")
		void imageMatchesForgiving() {
			SingleImageMatcher matcher = SingleImageMatcher.createDefaultMatcher(Setting.Forgiving);
			assertMatches(matcher);
		}

		@Test
		@DisplayName("Check Similarity Fair")
		void imageMatchesFair() {
			SingleImageMatcher matcher = SingleImageMatcher.createDefaultMatcher(Setting.Fair);
			assertMatches(matcher);
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
		matcher.addHashingAlgorithm(new AverageHash(14), 0.5, true);
		assertEquals(1, algos.size());
		matcher.clearHashingAlgorithms();
		assertEquals(0, algos.size());
	}

}
