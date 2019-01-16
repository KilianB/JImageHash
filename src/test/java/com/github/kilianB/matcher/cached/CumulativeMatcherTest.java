package com.github.kilianB.matcher.cached;

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

import com.github.kilianB.datastructures.tree.Result;
import com.github.kilianB.hashAlgorithms.AverageHash;
import com.github.kilianB.hashAlgorithms.HashingAlgorithm;
import com.github.kilianB.hashAlgorithms.PerceptiveHash;
import com.github.kilianB.matcher.TypedImageMatcher.AlgoSettings;

/**
 * @author Kilian
 *
 */
class CumulativeMatcherTest {

	private void addDefaultTestImages(CumulativeMatcher matcher) {
		matcher.addImage(ballon);
		matcher.addImage(copyright);
		matcher.addImage(highQuality);
		matcher.addImage(lowQuality);
		matcher.addImage(thumbnail);
	}

	private void assertImageMatches(CumulativeMatcher matcher) {
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

	@Test
	@DisplayName("Check Similarity")
	public void imageMatches() {
		CumulativeMatcher matcher = createMatcher();
		addDefaultTestImages(matcher);
		assertImageMatches(matcher);
	}

	@Test
	@DisplayName("Check Similarity Non Normalized")
	public void imageMatcheNonNormalizedVersion() {

		CumulativeMatcher matcher = new CumulativeMatcher(20, false);

		matcher.addHashingAlgorithm(new AverageHash(64));

		addDefaultTestImages(matcher);
		assertImageMatches(matcher);
	}

	@Nested
	class TestDefaultSettings {
		@Test
		@DisplayName("Default")
		public void defaultMatcher() {

			CumulativeMatcher matcher = createMatcher();

			addDefaultTestImages(matcher);
			assertImageMatches(matcher);
		}
	}

	@Test
	public void alterAlgorithmAfterImageHasAlreadyBeenAdded() {
		CumulativeMatcher matcher = createMatcher();

		addDefaultTestImages(matcher);

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

		assertImageMatches(matcher);

	}

	@Test
	@DisplayName("Empty Matcher")
	public void noAlgorithm() {
		ConsecutiveMatcher matcher = new ConsecutiveMatcher();
		BufferedImage dummyImage = new BufferedImage(1, 1, 0x1);
		assertThrows(IllegalStateException.class, () -> {
			matcher.getMatchingImages(dummyImage);
		});
	}

	@Test
	public void addAndClearAlgorithms() {
		ConsecutiveMatcher matcher = new ConsecutiveMatcher();

		assertEquals(0, matcher.getAlgorithms().size());
		matcher.addHashingAlgorithm(new AverageHash(14), 0.5);
		assertEquals(1, matcher.getAlgorithms().size());
		matcher.clearHashingAlgorithms();
		assertEquals(0, matcher.getAlgorithms().size());
	}

	private static CumulativeMatcher createMatcher() {
		CumulativeMatcher matcher = new CumulativeMatcher(.5);
		matcher.addHashingAlgorithm(new AverageHash(32));
		matcher.addHashingAlgorithm(new PerceptiveHash(64));
		return matcher;
	}

}
