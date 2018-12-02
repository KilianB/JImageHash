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
import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

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
	class TestDefaultSettings {
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
		void imageMatchesFair() {
			try {
				Class.forName("org.h2.Driver");
				Connection conn = DriverManager.getConnection("jdbc:h2:~/imageHashTest", "sa", "");

				DatabaseImageMatcher matcher = null;
				try {
					matcher = DatabaseImageMatcher.createDefaultMatcher(Setting.Fair, conn);
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
					matcher = DatabaseImageMatcher.createDefaultMatcher(Setting.Forgiving, conn);
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

	@Test
	void reconstructHash() {

		AverageHash aHash = new AverageHash(32);
		Hash h = aHash.hash(ballon);

		DatabaseImageMatcher dbMatcher = null;

		try {
			dbMatcher = new DatabaseImageMatcher("TestReconstruct", "sa", "") {
				public byte[] getBytesFromTable() {
					try (Statement s = conn.createStatement()) {
						ResultSet rs = s.executeQuery("SELECT hash FROM " + resolveTableName(aHash));
						rs.next();
						return rs.getBytes(1);
					} catch (SQLException e) {
						e.printStackTrace();
					}
					return null;
				}
			};
			dbMatcher.addHashingAlgorithm(aHash, 0.4f);
			dbMatcher.addImage("ballon", ballon);
			byte[] rawDbBytes = (byte[]) dbMatcher.getClass().getMethod("getBytesFromTable").invoke(dbMatcher);
			Hash hReconstructed = dbMatcher.reconstructHashFromDatabase(aHash, rawDbBytes);
			assertEquals(h, hReconstructed);
		} catch (ClassNotFoundException | SQLException | IllegalAccessException | IllegalArgumentException
				| InvocationTargetException | NoSuchMethodException | SecurityException e) {
			e.printStackTrace();
		} finally {
			try {
				dbMatcher.deleteDatabase();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}

	@Test
	void getAllMatchingImages() {
		DatabaseImageMatcher dbMatcher = null;
		try {
			dbMatcher = DatabaseImageMatcher.createDefaultMatcher("TestAllMatching", "sa", "");
			dbMatcher.addImage("ballon", ballon);
			dbMatcher.addImage("highQuality", highQuality);
			dbMatcher.addImage("lowQuality", lowQuality);
			dbMatcher.addImage("thumbnail", thumbnail);

			Map<String, PriorityQueue<Result<String>>> allMatchingImages = dbMatcher.getAllMatchingImages();

			Set<String> images = allMatchingImages.keySet();

			// Check if we get a result for every supplied image
			assertAll("All images matched", () -> {
				assertTrue(images.contains("ballon"));
			}, () -> {
				assertTrue(images.contains("highQuality"));
			}, () -> {
				assertTrue(images.contains("lowQuality"));
			}, () -> {
				assertTrue(images.contains("thumbnail"));
			});

			// Check if we get the expected results
			assertAll("Matches", () -> {
				assertEquals(1, allMatchingImages.get("ballon").size());
			}, () -> {
				assertTrue(allMatchingImages.get("ballon").peek().value.equals("ballon"));
			});

			assertAll("Matches", () -> {
				assertEquals(3, allMatchingImages.get("highQuality").size());
			}, () -> {
				assertFalse(allMatchingImages.get("highQuality").stream().anyMatch(result -> result.value.equals("ballon")));
			});

			assertAll("Matches", () -> {
				assertEquals(3, allMatchingImages.get("lowQuality").size());
			}, () -> {
				assertFalse(allMatchingImages.get("lowQuality").stream().anyMatch(result -> result.value.equals("ballon")));
			});

			assertAll("Matches", () -> {
				assertEquals(3, allMatchingImages.get("thumbnail").size());
			}, () -> {
				assertFalse(allMatchingImages.get("thumbnail").stream().anyMatch(result -> result.value.equals("ballon")));
			});

		} catch (ClassNotFoundException | SQLException e) {
			e.printStackTrace();
		} finally {
			try {
				dbMatcher.deleteDatabase();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}

}
