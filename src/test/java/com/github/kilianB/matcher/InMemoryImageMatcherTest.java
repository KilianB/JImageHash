package com.github.kilianB.matcher;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Map;
import java.util.PriorityQueue;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.github.kilianB.dataStrorage.tree.Result;
import com.github.kilianB.hashAlgorithms.AverageHash;
import com.github.kilianB.hashAlgorithms.HashingAlgorithm;
import com.github.kilianB.matcher.ImageMatcher.AlgoSettings;
import com.github.kilianB.matcher.ImageMatcher.Setting;

class InMemoryImageMatcherTest {

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
	class TestDefaultSettings{
		@Test
		@DisplayName("Default")
		void defaultMatcher() {

			InMemoryImageMatcher matcher = InMemoryImageMatcher.createDefaultMatcher();

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

			InMemoryImageMatcher matcher = InMemoryImageMatcher.createDefaultMatcher(Setting.Forgiving);

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

			InMemoryImageMatcher matcher = InMemoryImageMatcher.createDefaultMatcher(Setting.Fair);

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
		matcher.addHashingAlgorithm(new AverageHash(14), 0.5f, true);
		assertEquals(1, matcher.getAlgorithms().size());
		matcher.clearHashingAlgorithms();
		assertEquals(0, matcher.getAlgorithms().size());
	}

}
