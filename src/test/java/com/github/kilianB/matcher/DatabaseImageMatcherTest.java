package com.github.kilianB.matcher;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.PriorityQueue;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.github.kilianB.dataStrorage.tree.Result;

/**
 * @author Kilian
 *
 */
class DatabaseImageMatcherTest {

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

	@Test
	@DisplayName("Check Similarity String Label")
	void imageMatches() {
		try {
			Class.forName("org.h2.Driver");
			Connection conn = DriverManager.getConnection("jdbc:h2:~/imageHashTest", "sa", "");
			
			DatabaseImageMatcher matcher = null;
			try {
				matcher = DatabaseImageMatcher.createDefaultMatcher(conn);
				System.out.println(matcher);
				matcher.addImage("Ballon",ballon);
				matcher.addImage("CopyRight",copyright);
				matcher.addImage("HighQuality",highQuality);
				matcher.addImage("LowQuality",lowQuality);
				matcher.addImage("Thumbnail",thumbnail);
				
				// We only expect ballon to be returned
				PriorityQueue<Result<String>> results = matcher.getMatchingImages(ballon);
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
			}finally {
				//System.out.println(matcher);
				//matcher.clearHashingAlgorithms(true);
//				try {
//					matcher.close();
//				} catch (Exception e) {
//					e.printStackTrace();
//				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}

//	@Test
//	@DisplayName("Empty Matcher")
//	void noAlgorithm() {
//		InMemoryImageMatcher matcher = new InMemoryImageMatcher();
//		BufferedImage dummyImage = new BufferedImage(1, 1, 0x1);
//		assertThrows(IllegalStateException.class, () -> {
//			matcher.getMatchingImages(dummyImage);
//		});
//	}
//
//	@Test
//	void addAndClearAlgorithms() {
//		InMemoryImageMatcher matcher = new InMemoryImageMatcher();
//
//		assertEquals(0, matcher.getAlgorithms().size());
//		matcher.addHashingAlgorithm(new AverageHash(14), 0.5f, true);
//		assertEquals(1, matcher.getAlgorithms().size());
//		matcher.clearHashingAlgorithms();
//		assertEquals(0, matcher.getAlgorithms().size());
//	}

}
