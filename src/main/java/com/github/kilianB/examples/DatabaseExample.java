package com.github.kilianB.examples;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.PriorityQueue;

import com.github.kilianB.datastructures.tree.Result;
import com.github.kilianB.hashAlgorithms.DifferenceHash;
import com.github.kilianB.hashAlgorithms.DifferenceHash.Precision;
import com.github.kilianB.hashAlgorithms.PerceptiveHash;
import com.github.kilianB.matcher.persistent.database.DatabaseImageMatcher;
import com.github.kilianB.matcher.persistent.database.H2DatabaseImageMatcher;

/**
 * @author Kilian
 *
 */
public class DatabaseExample {

	/*
	 * Make sure that your project also defines the h2 dependency <dependency>
	 * <groupId>com.h2database</groupId> <artifactId>h2</artifactId>
	 * <version>1.4.197</version> </dependency>
	 */

	public static void main(String[] args) throws Exception {

		// Here are multiple exaples how you can use the database image matcher

		// 0. Connect with a username and password
		createDatabaseViaCredentials();

		/*
		 * 1. Connect via a connection object. We already added the images in the
		 * createDatabaseViaCredentials(). So we can simply reuse the hashes without re
		 * adding the images.
		 */
		connectViaConnectionObject();

		// 2. retrieve image matcher from database

		// Two image matchers can use the same database. As long as the exact same
		// algorithms are used
		// There is no need to rehash images

		// Serialize and deserialize

//				db.addImage(new File("ImageFile.png"));
//				db.addImage("UniqueId",BufferedImage);
//				
//				//Opposed to all other matchers you either get a filename or the unique id returned
//				PriorityQueue results = db.getMatchingImages(...);
//				
//				//1. Connect via a connection object
//				Class.forName("org.h2.Driver");
//				Connection conn = DriverManager.getConnection("jdbc:h2:~/" + dbName, userName, password);
//				db = DatabaseImageMatcher.createDefaultMatcher(conn);
//				
//				
//				//2. Load from database
//				db = DatabaseImageMatcher.getFromDatabase(conn,1);
//				
//				//2.1 to load from database it has to be saved to the database first
//				db.serializeToDatabase(1);
	}

	private static void createDatabaseViaCredentials() throws Exception {
		String dbName = "imageHashDB";
		String userName = "root";
		String password = "";

		// Wrap in try with block or call close at the end!
		try (H2DatabaseImageMatcher db = new H2DatabaseImageMatcher(dbName, userName, password)) {
			// Proceed as normal
			db.addHashingAlgorithm(new DifferenceHash(32, Precision.Double), 20);
			db.addHashingAlgorithm(new PerceptiveHash(32), 15);

			// Image files
			File ballon = new File("src/test/resources/ballon.jpg");
			File copyright = new File("src/test/resources/copyright.jpg");
			File highQuality = new File("src/test/resources/highQuality.jpg");

			db.addImages(ballon, copyright, highQuality);

			PriorityQueue<Result<String>> results = db.getMatchingImages(copyright);
			results.forEach(System.out::println);

			// Find all images which are similar to any image in the database
			System.out.println(db.getAllMatchingImages());
		}

		/*
		 * finally { //Not necessary since we use a try with otherwise db.close(); }
		 */

	}

	private static void connectViaConnectionObject() throws ClassNotFoundException, SQLException, IOException {

		String dbName = "imageHashDB";
		String userName = "root";
		String password = "";
		Class.forName("org.h2.Driver");

		// You may also use a connection object. Recently all h2 related syntax was
		// stripped
		// Therefore normal SQL databases should work as well. But it's not tested.
		Connection conn = DriverManager.getConnection("jdbc:h2:~/" + dbName, userName, password);

		// Here we can also use the database image matcher instead of the h2 image
		// matcher
		DatabaseImageMatcher db = DatabaseImageMatcher.createDefaultMatcher(conn);

		// Image file
		File copyright = new File("src/test/resources/copyright.jpg");

		// No need to add images anymore. We already did it in
		// createDatabaseViaCredentials();
		// Be aware that this only works because we are using the exact same hashing
		// algorithms as
		// in the earlier function!
		PriorityQueue<Result<String>> results = db.getMatchingImages(copyright);
		results.forEach(System.out::println);
	}

}
