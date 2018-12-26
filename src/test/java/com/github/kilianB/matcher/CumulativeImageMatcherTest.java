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
import com.github.kilianB.matcher.unsupervised.CumulativeImageMatcher;
import com.github.kilianB.matcher.unsupervised.InMemoryImageMatcher;

/**
 * @author Kilian
 *
 */
class CumulativeImageMatcherTest {

	@Test
	@DisplayName("Check Similarity")
	void imageMatches() {

		CumulativeImageMatcher matcher = CumulativeImageMatcher.createDefaultMatcher();

		matcher.addImage(ballon);
		matcher.addImage(copyright);
		matcher.addImage(highQuality);
		matcher.addImage(lowQuality);
		matcher.addImage(thumbnail);

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
	@DisplayName("Check Similarity Non Normalized")
	void imageMatcheNonNormalizedVersion() {

		CumulativeImageMatcher matcher = new CumulativeImageMatcher(20,false);

		matcher.addHashingAlgorithm(new AverageHash(64));
		
		matcher.addImage(ballon);
		matcher.addImage(copyright);
		matcher.addImage(highQuality);
		matcher.addImage(lowQuality);
		matcher.addImage(thumbnail);

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
	class TestDefaultSettings{
		@Test
		@DisplayName("Default")
		void defaultMatcher() {

			CumulativeImageMatcher matcher = CumulativeImageMatcher.createDefaultMatcher();

			matcher.addImage(ballon);
			matcher.addImage(copyright);
			matcher.addImage(highQuality);
			matcher.addImage(lowQuality);
			matcher.addImage(thumbnail);

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
		@DisplayName("Forgiving")
		void forgiving() {

			CumulativeImageMatcher matcher = CumulativeImageMatcher.createDefaultMatcher(Setting.Forgiving);

			matcher.addImage(ballon);
			matcher.addImage(copyright);
			matcher.addImage(highQuality);
			matcher.addImage(lowQuality);
			matcher.addImage(thumbnail);

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
		@DisplayName("Fair")
		void imageMatches() {

			CumulativeImageMatcher matcher = CumulativeImageMatcher.createDefaultMatcher(Setting.Fair);

			matcher.addImage(ballon);
			matcher.addImage(copyright);
			matcher.addImage(highQuality);
			matcher.addImage(lowQuality);
			matcher.addImage(thumbnail);

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
		matcher.addHashingAlgorithm(algos[1], setting.threshold, setting.normalized);
		assertEquals(2, matcher.getAlgorithms().size());

		// Check if it still performs the same matches
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
		matcher.addHashingAlgorithm(new AverageHash(14), 0.5);
		assertEquals(1, matcher.getAlgorithms().size());
		matcher.clearHashingAlgorithms();
		assertEquals(0, matcher.getAlgorithms().size());
	}

}
