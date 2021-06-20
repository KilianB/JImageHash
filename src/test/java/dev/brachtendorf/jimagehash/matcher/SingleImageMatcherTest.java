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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.github.kilianB.hashAlgorithms.AverageHash;
import com.github.kilianB.matcher.exotic.SingleImageMatcher;

class SingleImageMatcherTest {

	@Nested
	class CheckDefaultMatcher {

		private void assertMatches(SingleImageMatcher matcher) {
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
		public void imageMatches() {
			SingleImageMatcher matcher = new SingleImageMatcher();
			matcher.addHashingAlgorithm(new AverageHash(64),.4);
			assertMatches(matcher);
		}
	}

	@Test
	@DisplayName("Empty Matcher")
	public void noAlgorithm() {
		SingleImageMatcher matcher = new SingleImageMatcher();
		BufferedImage dummyImage = new BufferedImage(1, 1, 0x1);
		assertThrows(IllegalStateException.class, () -> {
			matcher.checkSimilarity(dummyImage, dummyImage);
		});
	}

	@Test
	public void addAndClearAlgorithms() {
		SingleImageMatcher matcher = new SingleImageMatcher();
		assertEquals(0,matcher.getAlgorithms().size());
		matcher.addHashingAlgorithm(new AverageHash(14), 0.5, true);
		assertEquals(1,matcher.getAlgorithms().size());
		matcher.clearHashingAlgorithms();
		assertEquals(0,matcher.getAlgorithms().size());
	}

}
