package com.github.kilianB.examples.nineGagDuplicateDetectionAndMemeCategorizer.database;

import java.awt.Dimension;
import java.net.URL;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.h2.jdbcx.JdbcDataSource;

import com.github.kilianB.examples.nineGagDuplicateDetectionAndMemeCategorizer.model.AnimatedPost;
import com.github.kilianB.examples.nineGagDuplicateDetectionAndMemeCategorizer.model.ImagePost;
import com.github.kilianB.examples.nineGagDuplicateDetectionAndMemeCategorizer.model.PostItem;
import com.github.kilianB.examples.nineGagDuplicateDetectionAndMemeCategorizer.model.PostItem.Type;
import com.github.kilianB.examples.nineGagDuplicateDetectionAndMemeCategorizer.model.Section;
import com.github.kilianB.examples.nineGagDuplicateDetectionAndMemeCategorizer.model.Tag;

public class DatabaseManager implements AutoCloseable {

	private static Logger LOGGER = Logger.getLogger(DatabaseManager.class.getSimpleName());

	private Connection conn;

	/**
	 * Inserts generic post metadata
	 */
	private PreparedStatement addPostDatabase;

	/**
	 * Insert tags
	 */
	private PreparedStatement addTagsIntoDatabase;

	/**
	 * Insert sections
	 */
	private PreparedStatement addSection;

	/**
	 * Insert image post specific data
	 */
	private PreparedStatement addImagePost;

	private PreparedStatement doesPostEntryExist;

	private PreparedStatement doesPostEntryExistSingle;

	public DatabaseManager(String subPath, String username, String password) throws SQLException {
		// Setup database connection
		JdbcDataSource ds = new JdbcDataSource();
		ds.setURL("jdbc:h2:" + subPath);
		ds.setUser(username);
		ds.setPassword(password);
		conn = ds.getConnection();

		createTables();

		prepareStatements();

		// Make sure to release the database at shutdown. lose connection
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			try {
				if (!conn.isClosed()) {
					conn.close();
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}));

	}

	private void createTables() throws SQLException {
		Statement stmt = conn.createStatement();

		if (!doesTableExist(conn, "SECTION")) {
			stmt.execute(
					"CREATE TABLE SECTION (name VARCHAR(50) PRIMARY KEY, url VARCHAR(255), imageUrl VARCHAR(255) )");
		}

		if (!doesTableExist(conn, "POSTS")) {

			// we do it

			// BASE
			stmt.execute("CREATE TABLE POSTS (id VARCHAR(15), title VARCHAR(1024), url VARCHAR(255), nsfw BOOLEAN,"
					+ "promoted BOOLEAN,  hasLongPostCover BOOLEAN, sectionName VARCHAR(50),curSection VARCHAR(30) , creationDate DATETIME,"
					+ "descriptionURL VARCHAR(255), upvotes INTEGER,downvotes INTEGER, comments INTEGER, isVoteMasked BOOLEAN, lastQueried Datetime,"
					+ "type VARCHAR(25),PRIMARY KEY (id,curSection), FOREIGN KEY (sectionName) REFERENCES SECTION(name))");

			// stmt.execute("CREATE TABLE TAGS (key VARCHAR(150) PRIMARY KEY, url
			// VARCHAR(150))");
		}

		if (!doesTableExist(conn, "TAGS"))
			stmt.execute("CREATE TABLE TAGS "
					+ "(postId  VARCHAR(15), tagName VARCHAR(150), tagURL VARCHAR(200), PRIMARY KEY( postId, tagName))"/*
																														 * ,
																														 * FOREIGN
																														 * KEY
																														 * (postId)
																														 * REFERENCES
																														 * POSTS
																														 * (
																														 * id
																														 * )
																														 * )
																														 * "
																														 */);

		if (!doesTableExist(conn, "IMAGEPOST"))
			stmt.execute("CREATE TABLE IMAGEPOST "
					+ "(postId  VARCHAR(15), url VARCHAR(200), width INTEGER, height INTEGER,format VARCHAR(5), PRIMARY KEY( postId, url))"/*
																																			 * ,
																																			 * FOREIGN
																																			 * KEY
																																			 * (postId)
																																			 * REFERENCES
																																			 * POSTS
																																			 * (
																																			 * id
																																			 * )
																																			 * )
																																			 * "
																																			 */);

//		if (!doesTableExist(conn, "LABELEDTESTIMAGES"))
//			stmt.execute("CREATE TABLE LABELEDTESTIMAGES "
//					+ "(postId  VARCHAR(15), postId2 VARCHAR(15), match BOOLEAN, distance DOUBLE, PRIMARY KEY( postId, postId2), FOREIGN KEY (postId) REFERENCES POSTS(id), FOREIGN KEY (postId2) REFERENCES POSTS(id))");
	}

	private void prepareStatements() throws SQLException {

		addSection = conn.prepareStatement("MERGE INTO SECTION KEY(name) VALUES(?,?,?)");

		addPostDatabase = conn.prepareStatement(
				"INSERT INTO POSTS(id,title,url,nsfw,promoted,hasLongPostCover,sectionName,curSection, creationDate, descriptionURL, upvotes,downvotes,comments,isVoteMasked,lastQueried,"
						+ "type) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");

		addTagsIntoDatabase = conn.prepareStatement("INSERT INTO TAGS (postId,tagName,tagURL) VALUES(?,?,?)");

		addImagePost = conn
				.prepareStatement("INSERT INTO IMAGEPOST (postId,url,width,height,format) VALUES(?,?,?,?,?)");

		doesPostEntryExist = conn.prepareStatement("SELECT 1 FROM POSTS WHERE id = ? AND curSection = ?");
		doesPostEntryExistSingle = conn.prepareStatement("SELECT 1 FROM POSTS WHERE id = ?");
	}

	private boolean doesTableExist(Connection connection, String tableName) throws SQLException {
		DatabaseMetaData metadata = connection.getMetaData();
		ResultSet res = metadata.getTables(null, null, tableName, new String[] { "TABLE" });
		return res.next();
	}

	@Override
	public void close() throws Exception {
		if (!conn.isClosed()) {
			conn.close();
		}
	}

	// DO we need to synchronize or can we recreate the prepared statements?
	// TODO implement a lock for each statement
	public synchronized void addPostItem(PostItem item, String sectionEndpoint) throws SQLException {

		if (doesEntryExist(item.getId(), sectionEndpoint)) {
			LOGGER.info("Post id already exist. Skip");
			return;
		}

		boolean otherSectionExists = doesEntryExist(item.getId());

		Section section = item.getSection();
		// Merge section
		addSection.setString(1, section.getName());
		addSection.setString(2, section.getUrl());
		addSection.setString(3, section.getImageUrl());

		addSection.executeUpdate();

		int id = 1;
		addPostDatabase.setString(id++, item.getId());
		addPostDatabase.setString(id++, item.getTitle());
		addPostDatabase.setString(id++, item.getUrl());
		addPostDatabase.setBoolean(id++, item.isNsfw());
		addPostDatabase.setBoolean(id++, item.isPromoted());
		addPostDatabase.setBoolean(id++, item.isHasLongPostCover());
		addPostDatabase.setString(id++, section.getName());
		addPostDatabase.setString(id++, sectionEndpoint);
		addPostDatabase.setTimestamp(id++, new Timestamp(item.getCreationTime().getEpochSecond() * 1000));
		addPostDatabase.setString(id++, item.getDescriptionHtml());
		addPostDatabase.setInt(id++, item.getUpvoteCount());
		addPostDatabase.setInt(id++, item.getDownvoteCount());
		addPostDatabase.setInt(id++, item.getCommentCount());
		addPostDatabase.setBoolean(id++, item.isVoteMasked());
		addPostDatabase.setTimestamp(id++, new Timestamp(item.getQueryTime().toInstant().toEpochMilli()));
		addPostDatabase.setString(id++, item.getType().toString());

		try {
			addPostDatabase.executeUpdate();
		} catch (SQLException ex) {
			ex.printStackTrace();
			System.out.println("Error adding: " + item + " " + item.getInternalId() + " " + item.getId());
		}

		List<Tag> tags = item.getTags();
		// If we have a duplicate hot and trending this will result in a primary key
		// exception if we do not check
		if (!otherSectionExists) {
			for (Tag t : tags) {
				addTagsIntoDatabase.setString(1, item.getId());
				addTagsIntoDatabase.setString(2, t.getKey());
				addTagsIntoDatabase.setString(3, t.getUrl());
				addTagsIntoDatabase.executeUpdate();
			}

			if (item instanceof AnimatedPost || item.getType().equals(Type.Photo)) {

				List<Dimension> dims;
				List<URL> imageUrl;
				List<URL> webPUrl;

				if (item.getType().equals(Type.Photo)) {
					ImagePost iPost = (ImagePost) item;
					dims = iPost.getAvailableDimensions();
					imageUrl = iPost.getImageUrl();
					webPUrl = iPost.getImageUrlWebp();
				} else {
					// TODO same base class ?
					AnimatedPost iPost = (AnimatedPost) item;
					dims = iPost.getAvailableDimensionsThumbnail();
					imageUrl = iPost.getImageUrlThumbnail();
					webPUrl = iPost.getImageUrlWebpThumbnail();
				}

				for (int i = 0; i < dims.size(); i++) {

					String urlS = imageUrl.get(i).toString();
					addImagePost.setString(1, item.getId());
					addImagePost.setString(2, urlS);
					addImagePost.setInt(3, (int) dims.get(i).getWidth());
					addImagePost.setInt(4, (int) dims.get(i).getHeight());
					// So far always jpg
					String extension = urlS.substring(urlS.lastIndexOf(".") + 1);
					addImagePost.setString(5, extension);
					addImagePost.executeUpdate();
					// So far always webp
					addImagePost.setString(2, webPUrl.get(i).toString());
					urlS = webPUrl.get(i).toString();
					extension = urlS.substring(urlS.lastIndexOf(".") + 1);
					addImagePost.setString(5, extension);
					addImagePost.executeUpdate();
				}
			}
		}

		// TODO save test of video gif and article

	}

	/**
	 * @param maxResolution if true return max resolution image urls if false return
	 *                      min resolution
	 * @return a map containing jpg image urls to downlaod the 9gag images from
	 * @throws SQLException if an sql error occurs
	 */
	public Map<String, String> getImageDownloadURLs(boolean maxResolution) throws SQLException {

		Map<String, String> result = new HashMap<>();

		String res = maxResolution ? "MAX" : "MIN";

		// My h2 sql is pretty weka. Can probably be done in 1 query.

		// Get the maximum resolution image ? or do we want the min resolution image?
		Statement s = conn.createStatement();

		ResultSet rs = s.executeQuery("SELECT POSTID, " + res + "(WIDTH)," + res
				+ "(HEIGHT) FROM IMAGEPOST WHERE FORMAT = 'jpg' GROUP BY  POSTID");

		PreparedStatement p = conn.prepareStatement(
				"SELECT POSTID,URL FROM IMAGEPOST WHERE POSTID = ? AND WIDTH = ? AND HEIGHT = ? AND FORMAT = ?");

		while (rs.next()) {

			p.setString(1, rs.getString(1));
			p.setInt(2, rs.getInt(2));
			p.setInt(3, rs.getInt(3));
			p.setString(4, "jpg");
			ResultSet rss = p.executeQuery();
			rss.next();
			result.put(rss.getString(1), rss.getString(2));
		}
		return result;
	}

	private boolean doesEntryExist(String uId, String sectionEndpoint) throws SQLException {
		doesPostEntryExist.setString(1, uId);
		doesPostEntryExist.setString(2, sectionEndpoint);
		ResultSet result = doesPostEntryExist.executeQuery();
		return result.next();
	}

	private boolean doesEntryExist(String uId) throws SQLException {
		doesPostEntryExistSingle.setString(1, uId);
		ResultSet result = doesPostEntryExistSingle.executeQuery();

		return result.next();
	}

	/**
	 * Check if the database contains images from this section
	 * @param sectionEndpoint the section name
	 * @return true if it does, false if no entries exist
	 * @throws SQLException if an sql error occurs
	 */
	public boolean containsPostItemFromSection(String sectionEndpoint) throws SQLException {
		try (Statement stmt = conn.createStatement()) {
			ResultSet rs = stmt
					.executeQuery("SELECT COUNT(*)>0 FROM POSTS WHERE CURSECTION = '" + sectionEndpoint + "'");
			rs.next();
			return rs.getBoolean(1);
		}
	}
}
