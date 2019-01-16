package com.github.kilianB.examples.nineGagDuplicateDetectionAndMemeCategorizer;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import javax.imageio.ImageIO;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import com.github.kilianB.examples.nineGagDuplicateDetectionAndMemeCategorizer.database.DatabaseManager;
import com.github.kilianB.examples.nineGagDuplicateDetectionAndMemeCategorizer.model.PostItem;

/**
 * 
 * <p>
 * This class parses the model of 9gag as of 13.01.2019. The json format has
 * changed a few times throughout the last year, which might result in JSON
 * exceptions to be thrown if executed in the future.
 * 
 * 
 * @author Kilian
 * @since 3.0.0
 *
 */
public class GagUtility implements AutoCloseable {

	private static final Logger LOGGER = Logger.getLogger(GagUtility.class.getSimpleName());

	enum Section {
		HOT, TRENDING, NEW
	};

	private final String BASE_URL = "https://9gag.com/v1/group-posts/group/default/type/";

	// Database
	private DatabaseManager dbManager;
	// private DatabaseImageMatcher dbImageMatcher;
	/**
	 * Maximum threads while scrapping info.
	 */
	private int scrappingParallelisationLevel = Runtime.getRuntime().availableProcessors();

	private int scrappingRecursionDepth;

	/*---------------------------------*/
	private File workingDirectory;

	/*---------------------------------*/

	/**
	 * 
	 * @param workingDirectory The directory used to save the database file, and
	 *                         downloaded images. Since we can not keep all the
	 *                         images in memory they might be read multiple times (
	 * @param scrapDepth       How often shall the nextToken query be followed. If
	 *                         no more tokens are available the method will exist
	 *                         early. Usual maximum depths are in the range of [600
	 *                         - 1000]
	 *                         <p>
	 *                         Lower values will retrieve less (older posts) data
	 *                         but is faster.
	 * @throws SQLException if an SQL error occurs during database access
	 */
	public GagUtility(File workingDirectory, int scrapDepth) throws SQLException {
		this.workingDirectory = workingDirectory;
		dbManager = new DatabaseManager(workingDirectory.getAbsolutePath() + "/gagDatabase", "sa", "");
		scrappingRecursionDepth = scrapDepth;
	}

	/**
	 * Scrap metadata for the section. The metadata will be downloaded in form of a
	 * JSON file and saved in a database located at the base of the working
	 * direction.
	 * <p>
	 * If the section already was scrapped beforehand this call is a NOP
	 * 
	 * @param section to scrap
	 * @throws SQLException if an error occurs accessing the database
	 */
	public void scrapMetadata(Section section) throws SQLException {
		scrapMetadata(section, true);
	}

	/**
	 * Scrap metadata for the current section. The metadata will be downloaded in
	 * form of a JSON file and saved in a database located at the base of the
	 * working direction.
	 * <p>
	 * <b>Implnote:</b> abortIfAnEntryAlreadyExists was inserted because users will
	 * most likely forget to uncomment this method when running the example a second
	 * time.
	 * 
	 * @param section                     to scrap
	 * @param abortIfAnEntryAlreadyExists skip this method call if the database
	 *                                    already contains at least 1 entry to this
	 *                                    section.
	 *                                    <p>
	 *                                    Pass false if the section shall still be
	 *                                    parsed. Duplicate entries by id are still
	 *                                    ignored.
	 * @throws SQLException if an error occurs accessing the database
	 */
	public void scrapMetadata(Section section, boolean abortIfAnEntryAlreadyExists) throws SQLException {
		scrapMetadata(section.name().toLowerCase(), abortIfAnEntryAlreadyExists);
	}

	private ExecutorService boundCachedThreadPoolExecutor;

	/**
	 * Scrap metadata for the current section. The metadata will be downloaded in
	 * form of a JSON file and saved in a database located at the base of the
	 * working direction.
	 * 
	 * <b>Implnote:</b> abortIfAnEntryAlreadyExists was inserted because users will
	 * most likely forget to uncomment this method when running the example a second
	 * time.
	 * 
	 * @param sectionEndpoint             The name of the category or section to
	 *                                    scrap
	 * @param abortIfAnEntryAlreadyExists skip this method call if the database
	 *                                    already contains at least 1 entry to this
	 *                                    section.
	 *                                    <p>
	 *                                    Pass false if the section shall still be
	 *                                    parsed. Duplicate entries by id are still
	 *                                    ignored.
	 * @throws SQLException if an SQL error occurs during database access
	 */
	public void scrapMetadata(String sectionEndpoint, boolean abortIfAnEntryAlreadyExists) throws SQLException {
		LOGGER.info("Begin scrapping metadata");
		if (abortIfAnEntryAlreadyExists && dbManager.containsPostItemFromSection(sectionEndpoint)) {
			LOGGER.info(
					"It appears the section was already scrapped. Ignore request. If you want to reparse the section either clear the databse"
							+ " or call the scrapMetadata with abortIfAnEntryAlreadyExists = false");
			return;
		}

		boundCachedThreadPoolExecutor = new ThreadPoolExecutor(0, scrappingParallelisationLevel, 60, TimeUnit.SECONDS,
				new SynchronousQueue<Runnable>());

		scrap9GagContent(sectionEndpoint.toLowerCase(), "", 0);

		try {
			boundCachedThreadPoolExecutor.awaitTermination(1, TimeUnit.HOURS);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 
	 * @param sectionEndpoint endpoint (Trending, Hot, Fresh)
	 * @param tokenQuery      (optional) search term
	 * @param currentDepth    current depth for recursive scraping
	 * @throws SQLException if an SQL error occurs during database access
	 */
	private void scrap9GagContent(String sectionEndpoint, String tokenQuery, int currentDepth) throws SQLException {

		// Construct the target url to query

		LOGGER.info("Scrap: " + sectionEndpoint + " " + currentDepth + "/" + scrappingRecursionDepth);
		try {
			URL content = new URL(BASE_URL + sectionEndpoint + tokenQuery);

			if (currentDepth > scrappingRecursionDepth) {
				return;
			}

			try (InputStream is = content.openStream()) {
				JSONTokener tokener = new JSONTokener(is);
				JSONObject obj = new JSONObject(tokener);

				JSONObject data = (JSONObject) obj.get("data");

				// Do some reassignments to get the effective final state required by lambdas
				String pathToNextData = data.getString("nextCursor");
				int nextDepth = ++currentDepth;

				// The new path is known. We can already spawn the next thread.

				if (pathToNextData != null && !boundCachedThreadPoolExecutor.isShutdown()) {
					boundCachedThreadPoolExecutor.execute(() -> {
						try {
							scrap9GagContent(sectionEndpoint, "?" + pathToNextData, nextDepth);
						} catch (SQLException e) {
							e.printStackTrace();
						}
					});
				}
				JSONArray postData = data.getJSONArray("posts");

				for (int i = 0; i < postData.length(); i++) {
					JSONObject postObj = postData.getJSONObject(i);
					// System.out.println(postObj);

					PostItem item = PostItem.parseItem(postObj);

					dbManager.addPostItem(item, sectionEndpoint);
				}
			} catch (JSONException j) {
				if (j.getMessage().contains("JSONObject[\"nextCursor\"] not found")) {
					System.out.println("Done: lastToken" + tokenQuery);
					boundCachedThreadPoolExecutor.shutdown();
				} else {
					j.printStackTrace();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		} catch (MalformedURLException e1) {
			e1.printStackTrace();
		}
	}

	/**
	 * Download all images referenced in the database this GagParsers working
	 * directory is pointing to. Already downloaded images will be skipped.
	 * 
	 * @param downloadMaxResolution if true download the max resolution available.
	 *                              Otherwise the minimum resolution will be used.
	 *                              Which resolution is available solely depends on
	 *                              9gag and may differ from between different
	 *                              posts. Take a look at the parsed data for this.
	 *                              9Gag rescales images internally therefore the
	 *                              max resolution is still capped at a reasonable
	 *                              level.
	 * @throws SQLException         If an error occurs accessing the database
	 * @throws InterruptedException If the thread pool gets interrupted while
	 *                              Performing the download requests
	 * 
	 */
	public void downloadImages(boolean downloadMaxResolution) throws SQLException, InterruptedException {

		String downloadTargetDirectory = workingDirectory.getAbsoluteFile() + "/gagImages";
		File imageDownloadDirectory = new File(downloadTargetDirectory);
		imageDownloadDirectory.mkdirs();
		File blackDir = new File(workingDirectory.getAbsoluteFile() + "/invalidImages");
		blackDir.mkdirs();

		AtomicInteger counter = new AtomicInteger(0);

		// Postid //url
		Map<String, String> imagesToDownload = dbManager.getImageDownloadURLs(downloadMaxResolution);

		int numImagesToDownload = imagesToDownload.size();
		LOGGER.info("Start downloading " + numImagesToDownload + " images from 9Gag");

		// Multi thread the download
		ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(8);

		List<Callable<Void>> tasks = new ArrayList<>();

		for (Entry<String, String> downloadToken : imagesToDownload.entrySet()) {
			tasks.add(() -> {
				String url = downloadToken.getValue();
				String id = downloadToken.getKey();
				File saveLocation = new File(downloadTargetDirectory + "/" + id + ".jpg");
				if (!saveLocation.exists()) {
					// We could first query the connection header and decide if we want to discard
					// the result beforehand. But we are dealing with very small files we ignore.
					BufferedImage image = ImageIO.read(new URL(url));
					ImageIO.write(image, "jpg", saveLocation);

					// Check if it's a valid image or a black frame
					long fileSizeInBytes = saveLocation.length();
					if (saveLocation.length() < 5000) {
						double ratio = fileSizeInBytes / (double) (image.getWidth() * image.getHeight());
						if (ratio < 0.03) {
							// Move it to potential black images
							Files.move(saveLocation.toPath(), Paths.get(workingDirectory.getAbsoluteFile().getPath(),
									"invalidImages", id + ".jpg"), StandardCopyOption.REPLACE_EXISTING);
						}
					}

				} else {
					LOGGER.fine("Image already exists. Skip download");
				}

				int downloaded = counter.getAndIncrement();
				if (downloaded % 100 == 0) {
					LOGGER.info("Progress: " + downloaded + "/" + numImagesToDownload);
				}
				return null;
			});
		}

		executor.invokeAll(tasks);
		executor.shutdown();
		System.out.println("Download done");
	}

	@Override
	public void close() throws Exception {
		dbManager.close();
	}

}
