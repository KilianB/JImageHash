package com.github.kilianB.matcher;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.Serializable;
import java.math.BigInteger;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLTimeoutException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.PriorityQueue;

import javax.imageio.ImageIO;

import com.github.kilianB.dataStrorage.tree.Result;
import com.github.kilianB.hashAlgorithms.AverageHash;
import com.github.kilianB.hashAlgorithms.DifferenceHash;
import com.github.kilianB.hashAlgorithms.HashingAlgorithm;
import com.github.kilianB.hashAlgorithms.PerceptiveHash;
import com.github.kilianB.hashAlgorithms.DifferenceHash.Precision;
import com.github.kilianB.matcher.ImageMatcher.Setting;

/**
 * A naive database based image matcher implementation. Images indexed by this
 * matcher will be added to the database and retrieved if an image match is
 * queried.
 * 
 * <p>
 * The image matcher supports chaining multiple hashing steps which will be
 * invoked in the order the algorithms were added. Once a hashing algorithm
 * fails to match a specific image the image is discarded pruning the search
 * tree quickly.
 * 
 * <p>
 * Opposed to the {@link InMemoryImageMatcher} this matcher stores a reference
 * to the image data itself but just keeps track of the hash and the url of the
 * image file. Additionally if the hashing algorithms are added after images
 * have been hashed the images will not be found without reindexing.
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
 * For each and every match the hashes have to be read from the database. This
 * allows to persistently stores hashes but might not be as efficient as the
 * {@link InMemoryImageMatcher}. Optimizations may include to store 0 or 1 level
 * hashes (hashes created by the first invoked hashing algorithms at a memory
 * level and only retrieve the later hashes from the database.
 * 
 * @author Kilian
 *
 */
public class DatabaseImageMatcher extends ImageMatcher implements Serializable, AutoCloseable {

	private static final long serialVersionUID = 1L;

	/** Database connection. Maybe use connection pooling? */
	private transient Connection conn;

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
	 * @param password the user's password
	 * @return an image matcher backed by the database
	 * @exception SQLException if a database access error occurs or the url is
	 *                         {@code null}
	 * @throws SQLTimeoutException    when the driver has determined that the
	 *                                timeout value specified by the
	 *                                {@code setLoginTimeout} method has been
	 *                                exceeded and has at least tried to cancel the
	 *                                current database connection attempt
	 * @throws ClassNotFoundException if the h2 driver can not be located
	 */
	public DatabaseImageMatcher(String subname, String user, String password)
			throws SQLException, ClassNotFoundException {
		Class.forName("org.h2.Driver");
		Connection conn = DriverManager.getConnection("jdbc:h2:~/" + subname, user, password);
		initialize(conn);
	}

	/**
	 * Attempts to establish a connection to the given database using the supplied
	 * connection object.. If the database does not yet exist an empty db will be
	 * initialized.
	 * 
	 * @param connection the database connection
	 * @return an image matcher backed by the database
	 * @exception SQLException if a database access error occurs {@code null}
	 * @throws SQLTimeoutException when the driver has determined that the timeout
	 *                             value specified by the {@code setLoginTimeout}
	 *                             method has been exceeded and has at least tried
	 *                             to cancel the current database connection attempt
	 */
	public DatabaseImageMatcher(Connection conn) throws SQLException {
		initialize(conn);
	}

	/**
	 * Get a database image matcher which previously was serialized using
	 * {@link #serializeToDatabase(int)}.
	 * 
	 * @param conn the database connection
	 * @param id   the id supplied to the serializeDatabase call
	 * @return the image matcher found in the database or null if not present
	 * @throws SQLException if an SQL exception occurs
	 */
	public static DatabaseImageMatcher getFromDatabase(Connection conn, int id) throws SQLException {

		try (Statement stmt = conn.createStatement()) {
			ResultSet rs = stmt.executeQuery("SELECT * FROM ImageHasher WHERE ID = " + id);
			if (rs.next()) {
				try {
					DatabaseImageMatcher matcher = (DatabaseImageMatcher) new ObjectInputStream(
							rs.getBlob(2).getBinaryStream()).readObject();
					matcher.conn = conn;
					return matcher;
				} catch (ClassNotFoundException | IOException e) {
					e.printStackTrace();
				}
			}
		}
		return null;
	}

	/**
	 * Get a database image matcher which previously got serialized by calling
	 * {@link #serializeToDatabase(int)} on the object.
	 * 
	 * @param conn the database connection
	 * @param id   the id supplied to the serializeDatabase call
	 * @return the image matcher found in the database or null if not present
	 * @throws SQLException if an SQL exception occurs
	 */
	public static DatabaseImageMatcher getFromDatabase(String subname, String user, String password, int id)
			throws SQLException, ClassNotFoundException {
		Class.forName("org.h2.Driver");
		Connection conn = DriverManager.getConnection("jdbc:h2:~/" + subname, user, password);
		return getFromDatabase(conn, id);
	}

	/**
	 * A preconfigured image matcher chaining dHash and pHash algorithms for fast
	 * high quality results.
	 * <p>
	 * The dHash is a quick algorithms allowing to filter images which are very
	 * unlikely to be similar images. pHash is computationally more expensive and
	 * used to inspect possible candidates further
	 * 
	 * @return The matcher used to check if images are similar
	 * @throws SQLException if an error occurs while connecting to the database
	 */
	public static DatabaseImageMatcher createDefaultMatcher(Connection dbConnection) throws SQLException {
		return createDefaultMatcher(Setting.Quality, dbConnection);
	}

	/**
	 * A preconfigured image matcher chaining dHash and pHash algorithms for fast
	 * high quality results.
	 * <p>
	 * The dHash is a quick algorithms allowing to filter images which are very
	 * unlikely to be similar images. pHash is computationally more expensive and
	 * used to inspect possible candidates further
	 * 
	 * @param algorithmSetting
	 *                         <p>
	 *                         How aggressive the algorithm advances while comparing
	 *                         images
	 *                         </p>
	 *                         <ul>
	 *                         <li><b>Forgiving:</b> Matches a bigger range of
	 *                         images</li>
	 *                         <li><b>Fair:</b> Matches all sample images</li>
	 *                         <li><b>Quality:</b> Recommended: Does not initially
	 *                         filter as aggressively as Fair but returns usable
	 *                         results</li>
	 *                         <li><b>Strict:</b> Only matches images which are
	 *                         closely related to each other</li>
	 *                         </ul>
	 * 
	 * @return The matcher used to check if images are similar
	 * @throws SQLException if an error occurs while connecting to the database
	 */
	public static DatabaseImageMatcher createDefaultMatcher(Setting algorithmSetting, Connection dbConnection)
			throws SQLException {

		DatabaseImageMatcher matcher = new DatabaseImageMatcher(dbConnection);

		switch (algorithmSetting) {
		case Forgiving:
			matcher.addHashingAlgorithm(new DifferenceHash(32, Precision.Double), 25);
			matcher.addHashingAlgorithm(new PerceptiveHash(32), 15);
			break;
		case Fair:
			matcher.addHashingAlgorithm(new DifferenceHash(32, Precision.Double), 15);
			matcher.addHashingAlgorithm(new PerceptiveHash(32), 10);
			break;
		case Strict:
			matcher.addHashingAlgorithm(new DifferenceHash(32, Precision.Double), 10);
			matcher.addHashingAlgorithm(new PerceptiveHash(32), 6);
			break;
		case Quality:
			matcher.addHashingAlgorithm(new DifferenceHash(32, Precision.Double), 20);
			matcher.addHashingAlgorithm(new PerceptiveHash(32), 15);
		}
		return matcher;
	}

	private void initialize(Connection conn) throws SQLException {
		this.conn = conn;
		// Setup. register alias
		try (Statement stmt = conn.createStatement()) {
//			stmt.execute(
//					"CREATE ALIAS IF NOT EXISTS HAMMINGDIST FOR \"com.github.kilianB.dataStrorage.database.DatabaseManager.hammingtonDistance\" ");
//			stmt.execute(
//					"CREATE ALIAS IF NOT EXISTS HAMMINGDISTS FOR \"com.github.kilianB.dataStrorage.database.DatabaseManager.hammingtonDistanceSearch\" ");

			if (!doesTableExist("ImageHasher")) {
				stmt.execute("CREATE TABLE ImageHasher (Id INTEGER PRIMARY KEY, SerializeData BLOB)");
			}
			
			if (!doesTableExist("HashingAlgos")) {
				stmt.execute("CREATE TABLE HashingAlgos (Id VARCHAR PRIMARY KEY, keyLength INTEGER)");
			}

			
		}
	}

	/**
	 * {@inheritDoc}
	 * 
	 * throws a wrapped SQL exception as RuntimeException if an SQL error occurs
	 * during table creation.
	 */
	public void addHashingAlgorithm(HashingAlgorithm algo, float threshold) {
		try {
			if (!doesTableExist(resolveTableName(algo))) {
				createHashTable(algo);
			}
		} catch (SQLException e) {
			/*
			 * We can't rethrow the error due to the interface not pretty but wrap inside a
			 * runtime exception for now
			 */
			throw new RuntimeException(e);
		}
		super.addHashingAlgorithm(algo, threshold);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * throws a wrapped SQL exception as RuntimeException if an SQL error occurs
	 * during table creation.
	 */
	public void addHashingAlgorithm(HashingAlgorithm algo, float threshold, boolean normalized) {
		try {
			if (!doesTableExist(resolveTableName(algo))) {
				createHashTable(algo);
			}
		} catch (SQLException e) {
			/*
			 * We can't rethrow the error due to the interface not pretty but wrap inside a
			 * runtime exception for now
			 */
			throw new RuntimeException(e);
		}
		super.addHashingAlgorithm(algo, threshold, normalized);
	}

	/**
	 * Index the image. This enables the image matcher to find the image in future
	 * searches. The database image matcher does not store the image data itself but
	 * indexes the hash bound to the absolute path of the image.
	 * 
	 * The path of the file has to be unique in order for this operation to return
	 * deterministic results.
	 * 
	 * @param imageFile The image whose hash will be added to the matcher
	 * @throws IOException  if an error exists reading the file
	 * @throws SQLException if an SQL error occurs
	 */
	public void addImage(File imageFile) throws IOException, SQLException {
		BufferedImage img = ImageIO.read(imageFile);
		addImage(imageFile.getAbsolutePath(), img);
	}

	/**
	 * Index the image. This enables the image matcher to find the image in future
	 * searches. The database image matcher does not store the image data itself but
	 * indexes the hash bound to a user supplied string.
	 * 
	 * If the id does not uniquely identify a single image the results are
	 * undetermined.
	 * 
	 * @param uniqueId a unique index returned if querying for the image
	 * @param image    The image to hash
	 * @throws SQLException if an SQL error occurs
	 */
	public void addImage(String uniqueId, BufferedImage image) throws SQLException {
		for (Entry<HashingAlgorithm, AlgoSettings> entry : steps.entrySet()) {
			HashingAlgorithm algo = entry.getKey();
			addImage(algo, uniqueId, image);
		}
	}

	/**
	 * Serialize this image matcher to the database. The image matcher object can be
	 * later be retrieved by calling {@link #getFromDatabase(Connection, int)}
	 * 
	 * @param id The id this image matcher object will be associated with
	 * @throws SQLException if an SQL error occurs
	 */
	public void serializeToDatabase(int id) throws SQLException {
		PreparedStatement ps = conn.prepareStatement("MERGE INTO ImageHasher (Id,SerializeData) VALUES(?,?)");
		PipedOutputStream pipeOut = new PipedOutputStream();
		try {
			PipedInputStream pipe = new PipedInputStream(pipeOut);
			ObjectOutputStream oos = new ObjectOutputStream(pipeOut);
			oos.writeObject(this);
			oos.close();
			ps.setInt(1, id);
			ps.setBinaryStream(2, pipe);
			ps.execute();
		} catch (IOException e) {
			// should not occur
			e.printStackTrace();
		}

	}

	/**
	 * Removes the hashing algorithm from the image matcher.
	 * 
	 * @param algo               The algorithm to remove
	 * @param forceTableDeletion if true also delete all hashes in the database
	 *                           created by this particular algorithm. false keep
	 *                           the table and hashes stored. If two or more image
	 *                           matcher use the same database caution should be
	 *                           used when using this command.
	 * 
	 * @return true if the algorithm was removed. False if it wasn't present
	 * @throws SQLException if connection to the database failed. An SQL exception
	 *                      can only be thrown if forceTableDeletion is set to true.
	 *                      Even if an exception is thrown the algorithm will be
	 *                      removed from this particular image matcher object.
	 */
	public boolean removeHashingAlgo(HashingAlgorithm algo, boolean forceTableDeletion) throws SQLException {

		boolean removed = removeHashingAlgo(algo);

		if (removed && forceTableDeletion) {
			String tableName = resolveTableName(algo);
			if (doesTableExist(tableName)) {
				try (Statement stmt = conn.createStatement()) {
					stmt.execute("DROP TABLE " + tableName);
				}
			}
		}
		return removed;
	}

	/**
	 * Removes all hashing algorithm from the image matcher.
	 * 
	 * @param forceTableDeletion if true also delete all hashes in the database
	 *                           created by this particular algorithm. false keep
	 *                           the table and hashes stored. If two or more image
	 *                           matcher use the same database caution should be
	 *                           used when using this command.
	 * @throws SQLException if connection to the database failed. An SQL exception
	 *                      can only be thrown if forceTableDeletion is set to true.
	 *                      Even if an exception is thrown the algorithm will be
	 *                      removed from this particular image matcher object.
	 */
	public void clearHashingAlgorithms(boolean forceTableDeletion) throws SQLException {

		if (forceTableDeletion) {

			// Copy list to prevent concurrent mod exception. In turn this is not thread
			// save!
			List<HashingAlgorithm> hasher = new ArrayList(this.getAlgorithms().keySet());
			for (HashingAlgorithm hashingAlgo : hasher) {
				removeHashingAlgo(hashingAlgo, true);
			}
		}
		// Just to be sure
		super.clearHashingAlgorithms();
	}

	/**
	 * Search for all similar images passing the algorithm filters supplied to this
	 * matcher. If the image itself was added to the matcher it will be returned
	 * with a distance of 0
	 * 
	 * @param imageFile The image other images will be matched against
	 * @return Return all unique ids/file paths sorted by the
	 *         <a href="https://en.wikipedia.org/wiki/Hamming_distance">hamming
	 *         distance</a> of the last applied algorithms
	 * @throws SQLException if an SQL error occurs
	 * @throws IOException  if an error occurs when reading the file
	 */
	public PriorityQueue<Result<String>> getMatchingImages(File imageFile) throws SQLException, IOException {
		return getMatchingImages(ImageIO.read(imageFile));
	}

	/**
	 * Search for all similar images passing the algorithm filters supplied to this
	 * matcher. If the image itself was added to the matcher it will be returned
	 * with a distance of 0
	 * 
	 * @param image The image other images will be matched against
	 * @return Return all unique ids/file paths sorted by the
	 *         <a href="https://en.wikipedia.org/wiki/Hamming_distance">hamming
	 *         distance</a> of the last applied algorithms
	 * @throws SQLException if an SQL error occurs
	 */
	public PriorityQueue<Result<String>> getMatchingImages(BufferedImage image) throws SQLException {

		if (steps.isEmpty())
			throw new IllegalStateException(
					"Please supply at least one hashing algorithm prior to invoking the match method");

		PriorityQueue<Result<String>> returnValues = null;

		for (Entry<HashingAlgorithm, AlgoSettings> entry : steps.entrySet()) {
			HashingAlgorithm algo = entry.getKey();
			Hash targetHash = algo.hash(image);
			AlgoSettings settings = entry.getValue();

			int threshold = 0;
			if (settings.normalized) {
				int hashLength = targetHash.getHashValue().bitLength();
				threshold = Math.round(settings.threshold * hashLength);
			} else {
				threshold = (int) settings.threshold;
			}

			PriorityQueue<Result<String>> temp = new PriorityQueue<Result<String>>(
					getSimilarImages(targetHash, threshold, algo));

			if (returnValues == null) {
				returnValues = temp;
			} else {
				temp.retainAll(returnValues);
				returnValues = temp;
			}
		}
		return returnValues;
	}

	/**
	 * Return all url descriptors which describe images within the provided
	 * hammington distance of the supplied hash
	 * 
	 * @param targetHash  The hash to check the database against
	 * @param maxDistance The maximum distance the hashes may have
	 * @param hasher      the hashing algorithm used to identify the table
	 * @return all urls within distance x of the supplied hash
	 * @throws SQLException if an SQL error occurs
	 */
	private List<Result<String>> getSimilarImages(Hash targetHash, int maxDistance, HashingAlgorithm hasher)
			throws SQLException {

		String tableName = resolveTableName(hasher);
		List<Result<String>> urls = new ArrayList<>();
		try (Statement stmt = conn.createStatement()) {
			// We could implement the interface SQLData in. This will not improve
			// performance and looks out every other database family. Lets do it the old
			// traditional ways
			// PreparedStatement pS = conn.prepareStatement("SELECT *,HAMMINGDISTS(HASH,?)
			// AS Distance FROM AVERAGEHASH1626907789)");
			// ResultSet rs = stmt.executeQuery("SELECT * FROM(SELECT
			// *,HAMMINGDISTS(HASH,"+targetHash+") AS Distance FROM AVERAGEHASH1626907789)
			// WHERE Distance < " +maxDistance);
			ResultSet rs = stmt.executeQuery("SELECT url,hash FROM " + tableName);
			while (rs.next()) {
				// Url
				
				//TODO Not entirely possible if we drop the sign bit is it?
				
				
				BigInteger bInt = new BigInteger(rs.getBytes(2));
				int distance = targetHash.hammingDistanceFast(bInt);
				if (distance <= maxDistance) {
					String url = rs.getString(1);
					urls.add(new Result<String>(url, distance));
				}
			}
		}
		return urls;
	}

	private void addImage(HashingAlgorithm hashAlgo, String url, BufferedImage image) throws SQLException {
		String tableName = resolveTableName(hashAlgo);

		if (!doesTableExist(tableName)) {
			createHashTable(hashAlgo);
		}
		try (PreparedStatement insertHash = conn
				.prepareStatement("MERGE INTO " + tableName + " (url,hash) VALUES(?,?)")) {
			Hash hash = hashAlgo.hash(image);
			// insertHash.setString(1, tableName);

			System.out.println(hashAlgo + " " + hash + " " + hash.getHashValue().bitLength());
			System.out.println(hashAlgo + " " + hash + " " + hash.getHashValue().toByteArray().length);
			
			System.out.println(Arrays.toString(hash.getHashValue().toByteArray()));
			
			insertHash.setString(1, url);
			//insertHash.setBytes(2, hash.getHashValue().toByteArray());
			insertHash.setBytes(2, hash.toByteArray());
			insertHash.execute();
		}
	}

	/**
	 * Create a table to hold image hashes for a particular image hashing algorithm
	 * 
	 * @param hasher the hashing algorithm
	 * @throws SQLException if an SQL error occurs
	 */
	protected void createHashTable(HashingAlgorithm hasher) throws SQLException {

		String tableName = resolveTableName(hasher);

		if (doesTableExist(tableName)) {
			return;
		}

		try (Statement stmt = conn.createStatement()) {
			// Compute a sample hash to retrieve the exact bit resolution the hashes will
			// have
			BufferedImage bi = new BufferedImage(1, 1, BufferedImage.TYPE_3BYTE_BGR);
			Hash sampleHash = hasher.hash(bi);
			int bytes = (int) Math.ceil(sampleHash.getHashValue().bitLength() / 8d);
			stmt.execute("CREATE TABLE " + tableName + " (url VARCHAR(260) PRIMARY KEY, hash BINARY(" + bytes + "))");
			stmt.execute("MERGE INTO HashingAlgos (Id,keyLength) VALUES("+tableName+","+bytes+") ");
		}
	}

	/**
	 * Query if the database contains a table with the given name
	 * 
	 * @param tableName The table name to check for
	 * @return true if a table with the name exists, false otherwise
	 * @throws SQLException if an SQLError occurs
	 */
	private boolean doesTableExist(String tableName) throws SQLException {
		DatabaseMetaData metadata = conn.getMetaData();
		ResultSet res = metadata.getTables(null, null, tableName.toUpperCase(), new String[] { "TABLE" });
		boolean exist = res.next();
		return exist;
	}

	/**
	 * Map a hashing algorithm to a table name
	 * 
	 * @param hashAlgo The hashing algorithm
	 * @return the table name to identify the table used to save hashes produced by
	 *         this algorithm into
	 */
	private String resolveTableName(HashingAlgorithm hashAlgo) {
		//if algorithm id is negative an SQL error will be thrown. Replace sign by random symbol.
		return hashAlgo.getClass().getSimpleName() + (hashAlgo.algorithmId() > 0 ? hashAlgo.algorithmId() : "m"+Math.abs(hashAlgo.algorithmId()));
	}

	// Serialization
	private void writeObject(ObjectOutputStream oos) throws IOException {
		oos.defaultWriteObject();
		oos.writeObject(this.steps);
	}

	@SuppressWarnings("unchecked")
	private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException {
		ois.defaultReadObject();
		this.steps = (LinkedHashMap<HashingAlgorithm, AlgoSettings>) ois.readObject();
	}

	@Override
	public void close() throws Exception {
		conn.commit();
		conn.close();
	}

	public static void main(String[] args) {

		try {
			DatabaseImageMatcher matcher = new DatabaseImageMatcher("imageHash", "sa", "");
			matcher.addHashingAlgorithm(new AverageHash(32), 5);
			matcher.addHashingAlgorithm(new AverageHash(5), 5);
			matcher.addHashingAlgorithm(new AverageHash(20), 5);
			matcher.addHashingAlgorithm(new AverageHash(35), 5);

			matcher.serializeToDatabase(0);

			File ballonImage = new File("ballon.jpg");
			File copyright = new File("copyright.jpg");
			File highQuality = new File("highQuality.jpg");
			File loqQuality = new File("lowQuality.jpg");
			File thumbnail = new File("thumbnail.jpg");

			BufferedImage bimg = ImageIO.read(ballonImage);

			// matcher.addImage(ballonImage);
			PriorityQueue<Result<String>> images = matcher.getMatchingImages(bimg);
			System.out.println(images);

			images = matcher.getMatchingImages(copyright);
			System.out.println("Copy: " + images);

			matcher.addImage(copyright);
			matcher.addImage(highQuality);

			images = matcher.getMatchingImages(copyright);
			System.out.println("Copy: " + images);

			matcher.clearHashingAlgorithms(true);
			matcher.addHashingAlgorithm(new AverageHash(32), 5);
			images = matcher.getMatchingImages(copyright);
			System.out.println("Copy: " + images);
//			DatabaseImageMatcher matcher1 = DatabaseImageMatcher.getFromDatabase("imageHash", "sa", "", 0);
//
//			System.out.println(matcher.getAlgorithms());
//			System.out.println(matcher1.getAlgorithms());

			matcher.close();
//			matcher1.close();

		} catch (ClassNotFoundException | SQLException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}