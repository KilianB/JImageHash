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

import java.awt.image.BufferedImage;
import java.util.Map;
import java.util.PriorityQueue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.github.kilianB.dataStrorage.tree.Result;
import com.github.kilianB.hashAlgorithms.AverageHash;
import com.github.kilianB.hashAlgorithms.HashingAlgorithm;
import com.github.kilianB.matcher.ImageMatcher.AlgoSettings;
import com.github.kilianB.matcher.ImageMatcher.Setting;
import com.github.kilianB.matcher.unsupervised.InMemoryImageMatcher;

class InMemoryImageMatcherTest {

	private void assertMatches(InMemoryImageMatcher matcher) {
		// We only expect ballon to be returned
		final PriorityQueue<Result<BufferedImage>> results = matcher.getMatchingImages(ballon);

		assertAll("Ballon", () -> {
			assertEquals(1, results.size());
		}, () -> {
			assertEquals(ballon, results.peek().value);
		});

		final PriorityQueue<Result<BufferedImage>> results1 = matcher.getMatchingImages(highQuality);

		assertAll("Matches", () -> {
			assertEquals(4, results1.size());
		}, () -> {
			assertFalse(results1.stream().anyMatch(result -> result.value.equals(ballon)));
		});
	}

	@Nested
	class TestDefaultSettings {

		InMemoryImageMatcher createMatcherAndAddDefaultTestImages(Setting algorithmSettings) {

			InMemoryImageMatcher matcher;
			if (algorithmSettings == null) {
				matcher = InMemoryImageMatcher.createDefaultMatcher();
			} else {
				matcher = InMemoryImageMatcher.createDefaultMatcher(algorithmSettings);
			}

			matcher.addImage(ballon);
			matcher.addImage(copyright);
			matcher.addImage(highQuality);
			matcher.addImage(lowQuality);
			matcher.addImage(thumbnail);

			return matcher;
		}

		@Test
		@DisplayName("Default")
		void defaultMatcher() {
			InMemoryImageMatcher matcher = createMatcherAndAddDefaultTestImages(null);
			assertMatches(matcher);
		}

		@Test
		@DisplayName("Forgiving")
		void forgiving() {

			InMemoryImageMatcher matcher = createMatcherAndAddDefaultTestImages(Setting.Forgiving);
			assertMatches(matcher);
		}

		@Test
		@DisplayName("Fair")
		void imageMatches() {

			InMemoryImageMatcher matcher = createMatcherAndAddDefaultTestImages(Setting.Fair);
			assertMatches(matcher);
		}
	}

	@Test
	void alterAlgorithmAfterImageHasAlreadyBeenAdded() {
		InMemoryImageMatcher matcher = InMemoryImageMatcher.createDefaultMatcher();

		matcher.addImage(ballon);
		matcher.addImage(copyright);
		matcher.addImage(highQuality);
		matcher.addImage(lowQuality);
		matcher.addImage(thumbnail);

		// It's a linked hashmap get the last algo
		Map<HashingAlgorithm, AlgoSettings> algorithm = matcher.getAlgorithms();
		HashingAlgorithm[] algos = algorithm.keySet().toArray(new HashingAlgorithm[algorithm.size()]);
		AlgoSettings setting = algorithm.get(algos[1]);

		assertEquals(2, algorithm.size());

		matcher.removeHashingAlgo(algos[1]);
		assertEquals(1, matcher.getAlgorithms().size());

		// Recreated original state of the matcher
		matcher.addHashingAlgorithm(algos[1], setting.getThreshold(), setting.isNormalized());
		assertEquals(2, matcher.getAlgorithms().size());

		assertMatches(matcher);

	}

	@Test
	@DisplayName("Empty Matcher")
	void noAlgorithm() {
		InMemoryImageMatcher matcher = new InMemoryImageMatcher();
		BufferedImage dummyImage = new BufferedImage(1, 1, 0x1);
		assertThrows(IllegalStateException.class, () -> {
			matcher.getMatchingImages(dummyImage);
		});
	}

	@Test
	void addAndClearAlgorithms() {
		InMemoryImageMatcher matcher = new InMemoryImageMatcher();

		assertEquals(0, matcher.getAlgorithms().size());
		matcher.addHashingAlgorithm(new AverageHash(14), 0.5, true);
		assertEquals(1, matcher.getAlgorithms().size());
		matcher.clearHashingAlgorithms();
		assertEquals(0, matcher.getAlgorithms().size());
	}

}
