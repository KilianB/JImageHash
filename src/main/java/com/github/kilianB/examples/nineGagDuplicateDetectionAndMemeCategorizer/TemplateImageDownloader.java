package com.github.kilianB.examples.nineGagDuplicateDetectionAndMemeCategorizer;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

/**
 * @author Kilian
 * @since 3.0.0
 */
public class TemplateImageDownloader {

	private static final Logger LOGGER = Logger.getLogger(TemplateImageDownloader.class.getSimpleName());

	private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/71.0.3578.98 Safari/537.36";

	/**
	 * Download blank meme template images from https://imgflip.com/
	 * 
	 * @param outputDirectory the working directory to save the meme template into.
	 *                        A new folder will be created inside the directory
	 * @throws IOException          if an IOError occurs during file operation.
	 * @throws InterruptedException if an interrupt occurs during multi threaded
	 *                              execution.
	 */
	public static void download(File outputDirectory) throws IOException, InterruptedException {
		Set<String> memeTemplateUrls = new HashSet<>();

		LOGGER.info("Start scrapping download urls for meme template images");

		for (int i = 1;; i++) {
			Document doc = Jsoup.connect("https://imgflip.com/memetemplates?page=" + i)
					// If useragent is not set we get a 403
					.userAgent(USER_AGENT).get();
			Elements memeBox = doc.getElementsByClass("mt-box");
			if (memeBox.isEmpty()) {
				break;
			}
			memeBox.forEach(e -> {
				memeTemplateUrls.add(e.getElementsByTag("a").get(0).attr("href").substring(6));
			});
		}

		LOGGER.info("Scrapping done. " + memeTemplateUrls.size() + " template images found");

		LOGGER.info("Start downloading template images.");
		// Download images
		String baseURL = "https://imgflip.com/s/meme/";
		ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 4);

		String basePath = outputDirectory.getAbsolutePath() + "\\memeTemplate\\";
		new File(basePath).mkdirs();

		List<Callable<Void>> downloadTask = new ArrayList<>();

		for (String memeTarget : memeTemplateUrls) {
			downloadTask.add(() -> {
				try {

					Path imageSavePath = Paths.get(basePath + memeTarget + ".jpg");
					if (imageSavePath.toFile().exists()) {
						LOGGER.warning("Tempalte image: " + memeTarget + " already exist. Skip");
						return null;
					}
					URLConnection urlConnection = new URL(baseURL + memeTarget + ".jpg").openConnection();
					urlConnection.setRequestProperty("User-Agent", USER_AGENT);
					try (InputStream is = urlConnection.getInputStream()) {
						Files.copy(is, imageSavePath);
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
				return null;
			});
		}
		executor.invokeAll(downloadTask);
		executor.shutdown();
	}
}
//

//	}
