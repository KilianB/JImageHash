package com.github.kilianB.matcher;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.PriorityQueue;

import javax.imageio.ImageIO;

import org.h2.tools.DeleteDbFiles;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.github.kilianB.dataStrorage.tree.Result;
import com.github.kilianB.hashAlgorithms.AverageHash;
import com.github.kilianB.hashAlgorithms.HashingAlgorithm;
import com.github.kilianB.matcher.ImageMatcher.Setting;

/**
 * @author Kilian
 *
 */
/*
 * TODO Starting with JUnit 5.4.0 Snapshot @TestMethodOrder reordering methods
 * may allow us to construct more test more meaningful scenarios.
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
	void deleteDatabase() {
		DatabaseImageMatcher matcher;
		try {
			String dbName = "testDelete";
			// Getsd closed by deleteDatabse
			matcher = new DatabaseImageMatcher(dbName, "rootTest", "");
			File dbFile = new File(System.getProperty("user.home") + "/" + dbName + ".mv.db");
			assertTrue(dbFile.exists());
			matcher.deleteDatabase();
			assertFalse(dbFile.exists());
		} catch (SQLException | ClassNotFoundException e) {
			e.printStackTrace();
		}
	}

	@Test
	void deserializeNotPresent() {

		String user = "rootTest";
		String password = "";
		String dbName = "testSerializeNotPresent";
		try {
			ImageMatcher deserialized = DatabaseImageMatcher.getFromDatabase(dbName, user, password, 0);
			assertNull(deserialized);
		} catch (ClassNotFoundException | SQLException e) {
			e.printStackTrace();
		} finally {
			DeleteDbFiles.execute("~", dbName, true);
		}
	}

	@Test
	@DisplayName("Empty Matcher")
	void noAlgorithm() {

		String user = "rootTest";
		String password = "";
		String dbName = "testEmpty";

		DatabaseImageMatcher matcher = null;
		try {
			matcher = new DatabaseImageMatcher(dbName, user, password);
			DatabaseImageMatcher effectiveFinal = matcher;
			BufferedImage dummyImage = new BufferedImage(1, 1, 0x1);
			assertThrows(IllegalStateException.class, () -> {
				effectiveFinal.getMatchingImages(dummyImage);
			});
		} catch (ClassNotFoundException | SQLException e) {
			e.printStackTrace();
		} finally {
			try {
				matcher.deleteDatabase();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}

	@Test
	void addAndClearAlgorithms() {
		String user = "rootTest";
		String password = "";
		String dbName = "addAndClear";

		DatabaseImageMatcher matcher = null;
		try {
			matcher = new DatabaseImageMatcher(dbName, user, password);
			assertEquals(0, matcher.getAlgorithms().size());
			matcher.addHashingAlgorithm(new AverageHash(14), 0.5f, true);
			assertEquals(1, matcher.getAlgorithms().size());
			matcher.clearHashingAlgorithms();
			assertEquals(0, matcher.getAlgorithms().size());
		} catch (ClassNotFoundException | SQLException e) {
			e.printStackTrace();
		} finally {
			try {
				matcher.deleteDatabase();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}

	@Test
	void serializeAndDeSeriazlize() {
		DatabaseImageMatcher matcher = null;
		try {
			String user = "rootTest";
			String password = "";
			String dbName = "testSerialize";
			// Getsd closed by deleteDatabse
			matcher = new DatabaseImageMatcher(dbName, user, password);
			HashingAlgorithm h = new AverageHash(32);
			matcher.addHashingAlgorithm(h, 0.3f);
			matcher.serializeToDatabase(0);

			DatabaseImageMatcher deserialized = DatabaseImageMatcher.getFromDatabase(dbName, user, password, 0);
			// Close before assertion to ensure it's called
			deserialized.close();
			assertEquals(matcher, deserialized);
		} catch (SQLException | ClassNotFoundException e) {
			e.printStackTrace();
		} finally {
			try {
				matcher.deleteDatabase();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}
	@Nested
	class TestDefaultSettings{
		@Test
		@DisplayName("Check Similarity String Label")
		void imageMatches() {
			try {
				Class.forName("org.h2.Driver");
				Connection conn = DriverManager.getConnection("jdbc:h2:~/imageHashTest", "sa", "");

				DatabaseImageMatcher matcher = null;
				try {
					matcher = DatabaseImageMatcher.createDefaultMatcher(conn);
					matcher.addImage("Ballon", ballon);
					matcher.addImage("CopyRight", copyright);
					matcher.addImage("HighQuality", highQuality);
					matcher.addImage("LowQuality", lowQuality);
					matcher.addImage("Thumbnail", thumbnail);

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
				} finally {
					matcher.deleteDatabase();
				}
			} catch (SQLException e) {
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
		}
		
		@Test
		@DisplayName("Check Similarity Fair Setting")
		void imageMatchesFait() {
			try {
				Class.forName("org.h2.Driver");
				Connection conn = DriverManager.getConnection("jdbc:h2:~/imageHashTest", "sa", "");

				DatabaseImageMatcher matcher = null;
				try {
					matcher = DatabaseImageMatcher.createDefaultMatcher(Setting.Fair,conn);
					matcher.addImage("Ballon", ballon);
					matcher.addImage("CopyRight", copyright);
					matcher.addImage("HighQuality", highQuality);
					matcher.addImage("LowQuality", lowQuality);
					matcher.addImage("Thumbnail", thumbnail);

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
				} finally {
					matcher.deleteDatabase();
				}
			} catch (SQLException e) {
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
		}
		
		@Test
		@DisplayName("Check Similarity Forgiving Setting")
		void imageMatchesForgiving() {
			try {
				Class.forName("org.h2.Driver");
				Connection conn = DriverManager.getConnection("jdbc:h2:~/imageHashTest", "sa", "");

				DatabaseImageMatcher matcher = null;
				try {
					matcher = DatabaseImageMatcher.createDefaultMatcher(Setting.Forgiving,conn);
					matcher.addImage("Ballon", ballon);
					matcher.addImage("CopyRight", copyright);
					matcher.addImage("HighQuality", highQuality);
					matcher.addImage("LowQuality", lowQuality);
					matcher.addImage("Thumbnail", thumbnail);

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
				} finally {
					matcher.deleteDatabase();
				}
			} catch (SQLException e) {
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
		}
		
		
	}
	

//

}
