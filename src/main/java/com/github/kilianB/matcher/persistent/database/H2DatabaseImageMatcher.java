package com.github.kilianB.matcher.persistent.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.SQLTimeoutException;
import java.sql.Statement;
import java.util.logging.Logger;

import org.h2.tools.DeleteDbFiles;

import com.github.kilianB.matcher.persistent.ConsecutiveMatcher;

/**
 * A naive database based image matcher implementation. Images indexed by this
 * matcher will be added to the database and retrieved if an image match is
 * queried.
 * 
 * <p>
 * This class is backed by the
 * <a href="http://www.h2database.com/html/main.html">h2 database engine</a>
 * 
 * <p>
 * The image matcher supports chaining multiple hashing steps which will be
 * invoked in the order the algorithms were added. Once a hashing algorithm
 * fails to match a specific image the image is discarded pruning the search
 * tree quickly.
 * 
 * <p>
 * Opposed to the {@link ConsecutiveMatcher} this matcher does not stores a
 * reference to the image data itself but just keeps track of the hash and the
 * url of the image file. Additionally if hashing algorithms are added after
 * images have been hashed the images will not be found without reindexing the
 * image in question..
 * 
 * <p>
 * Multiple database image matchers may use the same database in which case
 * hashes created by the same hashing algorithm will be used in both matchers.
 * 
 * <pre>
 * {@code
 * 
 * DatabaseImageMatcher matcher0, matcher1; 
 * 
 * matcher0.addHashingAlgorithm(new AverageHash(32),...,...)
 * matcher1.addHashingAlgorithm(new AverageHash(32),...,...)
 * 
 * matcher0.addHashingAlgorithm(new AverageHash(24),...,...)
 *
 * matcher0.addImage(Image1)
 * }
 * </pre>
 * 
 * Starting from this point matcher1 would also be able to match against
 * <i>Image1</i>. Be aware that this relationship isn't symmetric. Images added
 * by calling <i>matcher1.addImage(..)</i> method will be matched at the first
 * step in <i>matcher0</i> but fail to find a hash for
 * <code>AverageHash(24)</code> therefore discarding the image as a possible
 * match.
 * <p>
 * If this behaviour is not desired simply choose a different database for each
 * image matcher.
 * 
 * <p>
 * 2 + n Tables are generated to save vales:
 * 
 * <ol>
 * <li>ImageHasher(id,serialize): Allows to serialize an image matcher to the
 * database</li>
 * <li>HashingAlgos(id,keyLenght): Saves the bit resolution of each hashing
 * algorithm</li>
 * <li>... n a table for each hashing algorithm used in an image matcher</li>
 * </ol>
 * 
 * <p>
 * For each and every match the hashes have to be read from the database. This
 * allows to persistently stores hashes but might not be as efficient as the
 * {@link ConsecutiveMatcher}. Optimizations may include to store 0 or 1 level
 * hashes (hashes created by the first invoked hashing algorithms at a memory
 * level and only retrieve the later hashes from the database.
 * 
 * @author Kilian
 * @since 2.0.2 added
 * @since 3.0.0 extract h2 database image matcher into it's own class
 *
 */
public class H2DatabaseImageMatcher extends DatabaseImageMatcher {

	private static final long serialVersionUID = 5629316725655117532L;

	private static final Logger LOG = Logger.getLogger(H2DatabaseImageMatcher.class.getName());

	private static final String CLASS_NOT_FOUND_ERROR = "In order to use the default database image "
			+ "matcher please make sure to add a h2 dependency to the class path. (Last tested version: 1.4.197).";

	/**
	 * Get a database image matcher which previously got serialized by calling
	 * {@link #serializeToDatabase(int)} on the object.
	 * 
	 * @param subname  the database file name. By default the file looks at the base
	 *                 directory of the user.
	 *                 <p>
	 *                 <code>"jdbc:h2:~/" + subname</code>
	 * 
	 * @param user     the database user on whose behalf the connection is being
	 *                 made.
	 * @param password the user's password. May be empty
	 * @param id       the id supplied to the serializeDatabase call
	 * @return the image matcher found in the database or null if not present
	 * @throws SQLException if an error occurs while connecting to the database or
	 *                      the h2 driver could not be found in the classpath
	 * @since 3.0.0
	 */
	public static H2DatabaseImageMatcher getFromDatabase(String subname, String user, String password, int id)
			throws SQLException {
		return (H2DatabaseImageMatcher) getFromDatabase(getConnection(subname, user, password), id);
	}

	/**
	 * Attempts to establish a connection to the given database URL using the h2
	 * database driver. If the database does not yet exist an empty db will be
	 * initialized.
	 * 
	 * @param subname  the database file name. By default the file looks at the base
	 *                 directory of the user.
	 *                 <p>
	 *                 <code>"jdbc:h2:~/" + subname</code>
	 * 
	 * @param user     the database user on whose behalf the connection is being
	 *                 made
	 * @param password the user's password. May be empty
	 * @exception SQLException if an error occurs while connecting to the database
	 *                         or the h2 driver could not be found in the classpath
	 * @throws SQLTimeoutException when the driver has determined that the timeout
	 *                             value specified by the {@code setLoginTimeout}
	 *                             method has been exceeded and has at least tried
	 *                             to cancel the current database connection attempt
	 * @since 3.0.0
	 */
	public H2DatabaseImageMatcher(String subname, String user, String password) throws SQLException {
		this(getConnection(subname, user, password));
	}

	/**
	 * Attempts to establish a connection to the given database using the supplied
	 * connection object. If the database does not yet exist an empty db will be
	 * initialized.
	 * 
	 * @param dbConnection the database connection
	 * @throws SQLException             if a database access error occurs
	 *                                  {@code null}
	 * @throws SQLTimeoutException      when the driver has determined that the
	 *                                  timeout value specified by the
	 *                                  {@code setLoginTimeout} method has been
	 *                                  exceeded and has at least tried to cancel
	 *                                  the current database connection attempt
	 * @throws IllegalArgumentException if the supplied dbConnection is not an h2
	 *                                  connection object
	 */
	public H2DatabaseImageMatcher(Connection dbConnection) throws SQLException {
		super(checkConnection(dbConnection));
	}

	/**
	 * Drop all data in the tables and delete the database files. This method
	 * currently only supports h2 databases. After this method terminates
	 * successfully all further method calls of this object will throw an
	 * SQLException if applicable.
	 * 
	 * <p>
	 * Calling this method will close() all database connections. Therefore calling
	 * close() on this object is not necessary.
	 * 
	 * @throws SQLException if an SQL error occurs
	 * @since 3.0.0
	 */
	public void deleteDatabase() throws SQLException {

		try (Statement stm = conn.createStatement()) {
			String url = conn.getMetaData().getURL();
			String needle = "jdbc:h2:";
			if (url.startsWith(needle)) {
				int dbNameIndex = url.lastIndexOf("/");
				String path = url.substring(needle.length(), dbNameIndex);
				String dbName = url.substring(dbNameIndex + 1);
				close();
				DeleteDbFiles.execute(path, dbName, true);
			} else {
				String msg = "deleteDatabase currently not supported for non h2 drivers.";
				LOG.severe(msg);
				throw new UnsupportedOperationException(msg);
			}
		}
	}

	// Utility methods

	/**
	 * Get a h2 database connection with the supplied settings.
	 * 
	 * @param subname  the database file name. By default the file looks at the base
	 *                 directory of the user.
	 *                 <p>
	 *                 <code>"jdbc:h2:~/" + subname</code>
	 * 
	 * @param user     the database user on whose behalf the connection is being
	 *                 made.
	 * @param password the user's password. May be empty
	 * @return a connection object pointing to the database
	 * @throws SQLException if an sql error occurs or the h2 driver could not be
	 *                      found.
	 */
	private static Connection getConnection(String subname, String user, String password) throws SQLException {
		try {
			return DriverManager.getConnection("jdbc:h2:~/" + subname, user, password);
		} catch (SQLException exception) {
			if (exception.getMessage().contains("No suitable driver found")) {
				LOG.severe(CLASS_NOT_FOUND_ERROR);
			}
			throw exception;
		}
	}

	/**
	 * Make sure that the supplied connection is a h2 database connection.
	 * 
	 * @param dbConnection The connection to check
	 * @return the supplied connection if it's an h2 connection
	 * @throws IllegalArgumentException if the connection does not match.
	 */
	private static Connection checkConnection(Connection dbConnection) throws IllegalArgumentException {
		if ("org.h2.jdbc.JdbcConnection".contains(dbConnection.getClass().getName())) {
			return dbConnection;
		} else {
			throw new IllegalArgumentException(
					"To intialize a h2dbimagematcher you must supply a h2 connection. Did you want to create a DatabaseImageMatcher instead?");
		}
	}

}
