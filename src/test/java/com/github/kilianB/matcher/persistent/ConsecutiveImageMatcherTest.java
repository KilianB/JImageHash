package com.github.kilianB.matcher.persistent;

import static com.github.kilianB.TestResources.ballon;
import static com.github.kilianB.TestResources.copyright;
import static com.github.kilianB.TestResources.highQuality;
import static com.github.kilianB.TestResources.lowQuality;
import static com.github.kilianB.TestResources.thumbnail;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.PriorityQueue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.github.kilianB.datastructures.tree.Result;
import com.github.kilianB.hashAlgorithms.AverageHash;
import com.github.kilianB.hashAlgorithms.HashingAlgorithm;
import com.github.kilianB.matcher.ImageMatcher.AlgoSettings;
import com.github.kilianB.matcher.ImageMatcher.Setting;

/**
 * @author Kilian
 *
 */
class ConsecutiveImageMatcherTest {

	private void assertMatches(PersitentBinaryTreeMatcher matcher) {
		// We only expect ballon to be returned
		final PriorityQueue<Result<String>> results = matcher.getMatchingImages(ballon);

		assertAll("Ballon", () -> {
			assertEquals(1, results.size());
		}, () -> {
			assertEquals("Ballon", results.peek().value);
		});

		final PriorityQueue<Result<String>> results1 = matcher.getMatchingImages(highQuality);

		assertAll("Matches", () -> {
			assertEquals(4, results1.size());
		}, () -> {
			assertFalse(results1.stream().anyMatch(result -> result.value.equals("Ballon")));
		});
	}

	private PersitentBinaryTreeMatcher createMatcherAndAddDefaultTestImages(Setting algorithmSettings) {

		PersitentBinaryTreeMatcher matcher;
		if (algorithmSettings == null) {
			matcher = ConsecutiveImageMatcher.createDefaultMatcher();
		} else {
			matcher = ConsecutiveImageMatcher.createDefaultMatcher(algorithmSettings);
		}
		matcher.addImage("Ballon", ballon);
		matcher.addImage("Copyright", copyright);
		matcher.addImage("HighQuality", highQuality);
		matcher.addImage("LowQuality", lowQuality);
		matcher.addImage("Thumbnail", thumbnail);
		return matcher;
	}

	@Nested
	class TestDefaultSettings {

		@Test
		@DisplayName("Default")
		public void defaultMatcher() {
			PersitentBinaryTreeMatcher matcher = createMatcherAndAddDefaultTestImages(null);
			assertMatches(matcher);
		}

		@Test
		@DisplayName("Forgiving")
		public void forgiving() {

			PersitentBinaryTreeMatcher matcher = createMatcherAndAddDefaultTestImages(Setting.Forgiving);
			assertMatches(matcher);
		}

		@Test
		@DisplayName("Fair")
		public void imageMatches() {

			PersitentBinaryTreeMatcher matcher = createMatcherAndAddDefaultTestImages(Setting.Fair);
			assertMatches(matcher);
		}
	}

	@Test
	public void alterAlgorithmAfterImageHasAlreadyBeenAdded() {
		PersitentBinaryTreeMatcher matcher = createMatcherAndAddDefaultTestImages(null);

		// It's a linked hashmap get the last algo
		Map<HashingAlgorithm, AlgoSettings> algorithm = matcher.getAlgorithms();
		HashingAlgorithm[] algos = algorithm.keySet().toArray(new HashingAlgorithm[algorithm.size()]);
		assertEquals(2, matcher.getAlgorithms().size());
		matcher.removeHashingAlgo(algos[1]);
		assertEquals(1, matcher.getAlgorithms().size());
	}

	@Test
	@DisplayName("Empty Matcher")
	public void noAlgorithm() {
		PersitentBinaryTreeMatcher matcher = new ConsecutiveImageMatcher();
		BufferedImage dummyImage = new BufferedImage(1, 1, 0x1);
		assertThrows(IllegalStateException.class, () -> {
			matcher.getMatchingImages(dummyImage);
		});
	}

	@Test
	public void addAndClearAlgorithms() {
		PersitentBinaryTreeMatcher matcher = new ConsecutiveImageMatcher();

		assertEquals(0, matcher.getAlgorithms().size());
		matcher.addHashingAlgorithm(new AverageHash(14), 0.5, true);
		assertEquals(1, matcher.getAlgorithms().size());
		matcher.clearHashingAlgorithms();
		assertEquals(0, matcher.getAlgorithms().size());
	}

	@Test
	public void serializeAndDeSeriazlize() {
		PersitentBinaryTreeMatcher matcher = createMatcherAndAddDefaultTestImages(null);

		try {
			File target = new File("ConsecutiveImageMatcherTest.ser");
			matcher.serializeState(target);

			PersitentBinaryTreeMatcher deserialized = (PersitentBinaryTreeMatcher) ConsecutiveImageMatcher
					.reconstructState(target, true);
			assertMatches(deserialized);

		} catch (IOException | ClassNotFoundException e) {
			e.printStackTrace();
			fail();
		}
	}

	@Test
	public void serializeAndDeSeriazlizeEquality() {
		PersitentBinaryTreeMatcher matcher = createMatcherAndAddDefaultTestImages(null);

		try {
			File target = new File("ConsecutiveImageMatcherTest.ser");
			matcher.serializeState(target);
			PersitentBinaryTreeMatcher deserialized = (PersitentBinaryTreeMatcher) ConsecutiveImageMatcher
					.reconstructState(target, true);
			assertEquals(matcher, deserialized);
		} catch (IOException | ClassNotFoundException e) {
			e.printStackTrace();
			fail();
		}
	}

}
