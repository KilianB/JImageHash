package com.github.kilianB.matcher.unsupervised.database;

import static com.github.kilianB.TestResources.ballon;
import static com.github.kilianB.TestResources.copyright;
import static com.github.kilianB.TestResources.highQuality;
import static com.github.kilianB.TestResources.lowQuality;
import static com.github.kilianB.TestResources.thumbnail;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

import org.h2.tools.DeleteDbFiles;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.github.kilianB.dataStrorage.tree.Result;
import com.github.kilianB.hashAlgorithms.AverageHash;
import com.github.kilianB.hashAlgorithms.HashingAlgorithm;
import com.github.kilianB.hashAlgorithms.PerceptiveHash;
import com.github.kilianB.matcher.Hash;
import com.github.kilianB.matcher.ImageMatcher;
import com.github.kilianB.matcher.ImageMatcher.Setting;

/**
 * @author Kilian
 *
 */
class H2DbImageMatcherTest {

	@Test
	void deleteDatabase() throws ClassNotFoundException, SQLException {
		H2DbImageMatcher matcher;

		String dbName = "testDelete";
		// Getsd closed by deleteDatabse
		matcher = new H2DbImageMatcher(dbName, "rootTest", "");
		File dbFile = new File(System.getProperty("user.home") + "/" + dbName + ".mv.db");
		assertTrue(dbFile.exists());
		matcher.deleteDatabase();
		assertFalse(dbFile.exists());

	}

	@Test
	void deserializeNotPresent() throws ClassNotFoundException, SQLException {

		String user = "rootTest";
		String password = "";
		String dbName = "testSerializeNotPresent";
		try {
			ImageMatcher deserialized = H2DbImageMatcher.getFromDatabase(dbName, user, password, 0);
			assertNull(deserialized);
		} finally {
			DeleteDbFiles.execute("~", dbName, true);
		}
	}

	@Test
	@DisplayName("Empty Matcher")
	void noAlgorithm() throws ClassNotFoundException, SQLException {

		String user = "rootTest";
		String password = "";
		String dbName = "testEmpty";

		H2DbImageMatcher matcher = null;
		try {
			matcher = new H2DbImageMatcher(dbName, user, password);
			DatabaseImageMatcher effectiveFinal = matcher;
			BufferedImage dummyImage = new BufferedImage(1, 1, 0x1);
			assertThrows(IllegalStateException.class, () -> {
				effectiveFinal.getMatchingImages(dummyImage);
			});
		} finally {
			try {
				matcher.deleteDatabase();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}

	@Test
	void addAndClearAlgorithms() throws ClassNotFoundException, SQLException {
		String user = "rootTest";
		String password = "";
		String dbName = "addAndClear";

		H2DbImageMatcher matcher = null;
		try {
			matcher = new H2DbImageMatcher(dbName, user, password);
			assertEquals(0, matcher.getAlgorithms().size());
			matcher.addHashingAlgorithm(new AverageHash(14), 0.5f, true);
			assertEquals(1, matcher.getAlgorithms().size());
			matcher.clearHashingAlgorithms();
			assertEquals(0, matcher.getAlgorithms().size());
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
		H2DbImageMatcher matcher = null;
		try {
			String user = "rootTest";
			String password = "";
			String dbName = "testSerialize";
			// Getsd closed by deleteDatabse
			matcher = new H2DbImageMatcher(dbName, user, password);
			HashingAlgorithm h = new AverageHash(32);
			matcher.addHashingAlgorithm(h, 0.3f);
			matcher.serializeToDatabase(0);

			DatabaseImageMatcher deserialized = H2DbImageMatcher.getFromDatabase(dbName, user, password, 0);
			// Close before assertion to ensure it's called
			deserialized.close();
			assertEquals(matcher, deserialized);
		} catch (SQLException e) {
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
		void imageMatches() throws SQLException, ClassNotFoundException {
			Class.forName("org.h2.Driver");
			Connection conn = DriverManager.getConnection("jdbc:h2:~/imageHashTest", "sa", "");

			H2DbImageMatcher matcher = null;
			try {
				matcher = H2DbImageMatcher.createDefaultMatcher(conn);
				addDefaultImages(matcher);

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

		}

		@Test
		@DisplayName("Check Similarity Fair Setting")
		void imageMatchesFair() throws ClassNotFoundException, SQLException {

			Class.forName("org.h2.Driver");
			Connection conn = DriverManager.getConnection("jdbc:h2:~/imageHashTest", "sa", "");

			H2DbImageMatcher matcher = null;
			try {
				matcher = H2DbImageMatcher.createDefaultMatcher(Setting.Fair, conn);
				addDefaultImages(matcher);

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

		}

		@Test
		@DisplayName("Check Similarity Forgiving Setting")
		void imageMatchesForgiving() throws SQLException, ClassNotFoundException {
			H2DbImageMatcher matcher = null;
			try {
				Class.forName("org.h2.Driver");
				Connection conn = DriverManager.getConnection("jdbc:h2:~/imageHashTest", "sa", "");

				matcher = H2DbImageMatcher.createDefaultMatcher(Setting.Forgiving, conn);

				addDefaultImages(matcher);

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

		}

	}

	@Test
	void reconstructHash() throws ClassNotFoundException, SQLException, IllegalAccessException,
			IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {

		AverageHash aHash = new AverageHash(32);
		Hash h = aHash.hash(ballon);

		H2DbImageMatcher dbMatcher = null;

		try {
			dbMatcher = new H2DbImageMatcher("TestReconstruct", "sa", "") {
				private static final long serialVersionUID = 1L;
				@SuppressWarnings("unused")
				public byte[] getBytesFromTable() {
					try (Statement s = conn.createStatement()) {
						ResultSet rs = s.executeQuery("SELECT hash FROM " + resolveTableName(aHash));
						if (rs.next()) {
							return rs.getBytes(1);
						} else {
							throw new IllegalStateException("No result found");
						}
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
		} finally {
			try {
				dbMatcher.deleteDatabase();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}

	@Test
	void getAllMatchingImages() throws SQLException, ClassNotFoundException {
		H2DbImageMatcher dbMatcher = null;
		try {
			dbMatcher = H2DbImageMatcher.createDefaultMatcher("TestAllMatching", "sa", "");
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
				assertFalse(allMatchingImages.get("highQuality").stream()
						.anyMatch(result -> result.value.equals("ballon")));
			});

			assertAll("Matches", () -> {
				assertEquals(3, allMatchingImages.get("lowQuality").size());
			}, () -> {
				assertFalse(
						allMatchingImages.get("lowQuality").stream().anyMatch(result -> result.value.equals("ballon")));
			});

			assertAll("Matches", () -> {
				assertEquals(3, allMatchingImages.get("thumbnail").size());
			}, () -> {
				assertFalse(
						allMatchingImages.get("thumbnail").stream().anyMatch(result -> result.value.equals("ballon")));
			});

		} finally {
			try {
				dbMatcher.deleteDatabase();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}

	@Nested
	class EntryExist {
		@Test
		void doesEntryExistEmpty() throws ClassNotFoundException, SQLException {

			H2DbImageMatcher dbMatcher = null;
			try {
				dbMatcher = new H2DbImageMatcher("testEntryExist", "sa", "");
				HashingAlgorithm h0 = new PerceptiveHash(64);
				dbMatcher.addHashingAlgorithm(h0, 0.5f);
				assertFalse(dbMatcher.doesEntryExist("ballon", h0));
			} finally {
				try {
					dbMatcher.deleteDatabase();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		}

		@Test
		void doesEntryExistTrue() throws ClassNotFoundException, SQLException {

			H2DbImageMatcher dbMatcher = null;
			try {
				dbMatcher = new H2DbImageMatcher("testEntryExist0", "sa", "");
				HashingAlgorithm h0 = new PerceptiveHash(64);
				dbMatcher.addHashingAlgorithm(h0, 0.5f);
				dbMatcher.addImage("ballon", ballon);
				assertTrue(dbMatcher.doesEntryExist("ballon", h0));
			} finally {
				try {
					dbMatcher.deleteDatabase();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		}

		@Test
		void doesEntryExistAddAlgoLater() throws SQLException, ClassNotFoundException {

			H2DbImageMatcher dbMatcher = null;
			try {
				dbMatcher = new H2DbImageMatcher("testEntryExist0", "sa", "");
				HashingAlgorithm h0 = new PerceptiveHash(64);
				dbMatcher.addHashingAlgorithm(h0, 0.5f);
				dbMatcher.addImage("ballon", ballon);
				assertTrue(dbMatcher.doesEntryExist("ballon", h0));

				HashingAlgorithm h1 = new AverageHash(64);
				dbMatcher.addHashingAlgorithm(h1, 0.1f);

				assertFalse(dbMatcher.doesEntryExist("ballon", h1));
				dbMatcher.addImage("ballon", ballon);
				assertTrue(dbMatcher.doesEntryExist("ballon", h1));

			} finally {
				try {
					dbMatcher.deleteDatabase();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private static void addDefaultImages(DatabaseImageMatcher matcher) throws SQLException {
		matcher.addImage("Ballon", ballon);
		matcher.addImage("CopyRight", copyright);
		matcher.addImage("HighQuality", highQuality);
		matcher.addImage("LowQuality", lowQuality);
		matcher.addImage("Thumbnail", thumbnail);
	}
	
}
