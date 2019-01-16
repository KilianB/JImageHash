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
import com.github.kilianB.hashAlgorithms.PerceptiveHash;
import com.github.kilianB.matcher.TypedImageMatcher.AlgoSettings;

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

	private PersitentBinaryTreeMatcher createMatcherAndAddDefaultTestImages() {

		PersitentBinaryTreeMatcher matcher;

		matcher =  new ConsecutiveMatcher(true);
		matcher.addHashingAlgorithm(new AverageHash(64),.4);
		matcher.addHashingAlgorithm(new PerceptiveHash(64),.3);

		
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
			PersitentBinaryTreeMatcher matcher = createMatcherAndAddDefaultTestImages();
			assertMatches(matcher);
		}
	}

	@Test
	public void alterAlgorithmAfterImageHasAlreadyBeenAdded() {
		PersitentBinaryTreeMatcher matcher = createMatcherAndAddDefaultTestImages();

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
		PersitentBinaryTreeMatcher matcher = new ConsecutiveMatcher(false);
		BufferedImage dummyImage = new BufferedImage(1, 1, 0x1);
		assertThrows(IllegalStateException.class, () -> {
			matcher.getMatchingImages(dummyImage);
		});
	}

	@Test
	public void addAndClearAlgorithms() {
		PersitentBinaryTreeMatcher matcher = new ConsecutiveMatcher(true);

		assertEquals(0, matcher.getAlgorithms().size());
		matcher.addHashingAlgorithm(new AverageHash(14), 0.5, true);
		assertEquals(1, matcher.getAlgorithms().size());
		matcher.clearHashingAlgorithms();
		assertEquals(0, matcher.getAlgorithms().size());
	}

	@Test
	public void serializeAndDeSeriazlize() {
		PersitentBinaryTreeMatcher matcher = createMatcherAndAddDefaultTestImages();

		try {
			File target = new File("ConsecutiveMatcherTest.ser");
			matcher.serializeState(target);

			PersitentBinaryTreeMatcher deserialized = (PersitentBinaryTreeMatcher) ConsecutiveMatcher
					.reconstructState(target, true);
			assertMatches(deserialized);

		} catch (IOException | ClassNotFoundException e) {
			e.printStackTrace();
			fail();
		}
	}

	@Test
	public void serializeAndDeSeriazlizeEquality() {
		PersitentBinaryTreeMatcher matcher = createMatcherAndAddDefaultTestImages();

		try {
			File target = new File("ConsecutiveMatcherTest.ser");
			matcher.serializeState(target);
			PersitentBinaryTreeMatcher deserialized = (PersitentBinaryTreeMatcher) ConsecutiveMatcher
					.reconstructState(target, true);
			assertEquals(matcher, deserialized);
		} catch (IOException | ClassNotFoundException e) {
			e.printStackTrace();
			fail();
		}
	}

}
