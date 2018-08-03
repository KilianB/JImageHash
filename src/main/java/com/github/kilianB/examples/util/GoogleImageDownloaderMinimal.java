package com.github.kilianB.examples.util;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;
import javax.net.ssl.HttpsURLConnection;
import javax.xml.bind.DatatypeConverter;

import org.apache.commons.io.FileUtils;

import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.DomElement;
import com.gargoylesoftware.htmlunit.html.DomNodeList;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

/**
 * Utility functions to download images from the google. Stripped down version
 * to not include any search parameters or full image downloads
 * 
 * @author Kilian
 *
 */
public class GoogleImageDownloaderMinimal {

	public GoogleImageDownloaderMinimal() {

	};

	/**
	 * Download the first 100 thumbnail images found on the google image search page
	 * 
	 * @param searchTerm
	 *            The search query
	 */
	public static ArrayList<BufferedImage> downloadThumbnailImagesInMemory(String searchTerm) {

		ArrayList<BufferedImage> downloadedImages = new ArrayList<>();

		/*
		 * Mute logger warnings
		 */
		java.util.logging.Logger.getLogger("com.gargoylesoftware").setLevel(Level.SEVERE);

		System.out.println("Download thumbnail images: " + searchTerm);
		
		
		/*
		 * Connect to google
		 */
		try (final WebClient webClient = new WebClient()) {
			webClient.waitForBackgroundJavaScript(5000);

			final HtmlPage page = webClient.getPage("https://www.google.com/search?q=" + searchTerm + "&tbm=isch");

			DomNodeList<DomElement> imgs = page.getElementsByTagName("img");

			/* @formatter:off
			 * Download more than 100 pictures
			 */
			//Object nextButton =  page.getFirstByXPath("input[@class='ksb']");
//			HtmlPage updatedPage = null;
//			DomNodeList<DomElement> input = page.getElementsByTagName("input");
//			for(DomElement domEle : input) {
//				if(domEle.getAttribute("class").equals("ksb")){
//					updatedPage = domEle.click();
//				}				
//			} 
			//@formatter:on

			int i = 0;
			for (DomElement domEle : imgs) {
				System.out.print(i + " ");

				try {
					BufferedImage image = praseImage(domEle);
					if (image != null) {
						downloadedImages.add(image);
					}
				} catch (IllegalStateException ise) {
					System.err.println(ise.getMessage());
				}
				i++;
			}
		} catch (FailingHttpStatusCodeException | IOException e) {
			e.printStackTrace();
		}

		java.util.logging.Logger.getLogger("com.gargoylesoftware").setLevel(Level.WARNING);
		// Line break
		System.out.println();
		return downloadedImages;
	}

	/**
	 * Download the first 100 thumbnail images found on the google image search page
	 * 
	 * @param searchTerm
	 *            The search query
	 * @param targetDirectory
	 *            The directory images shall be saved in
	 * @param clearDirectory
	 *            If true delete all the content found in the target directory
	 *            before downloading images
	 */
	public static ArrayList<BufferedImage> downloadThumbnailImages(String searchTerm, File targetDirectory, boolean clearDirectory) {

		if (!targetDirectory.exists()) {
			targetDirectory.mkdir();
		} else {
			if (clearDirectory) {
				FileUtils.deleteQuietly(targetDirectory);
				targetDirectory.mkdir();
			}
		}

		ArrayList<BufferedImage> downloadedImages = downloadThumbnailImagesInMemory(searchTerm);

		try {
			int i = 0;
			for (BufferedImage image : downloadedImages) {
				File outputFile = new File(targetDirectory.getAbsolutePath() + "/" + searchTerm + i + ".jpg");
				ImageIO.write(image, "jpg", outputFile);
				i++;
			}
		} catch (IOException io) {
			io.printStackTrace();
		}
		return downloadedImages;
	}

	/**
	 * Regex to extract file extension and data portion of base64Patterns
	 */
	private static Pattern base64Pattern = Pattern.compile("data:image/(?<extension>[a-zA-Z]*);base64,(?<imgData>.*)");

	private static BufferedImage praseImage(DomElement imgElement) throws IOException, IllegalStateException {

		String dataSrc = imgElement.getAttribute("data-src");

		if (dataSrc.equals("ATTRIBUTE_NOT_DEFINED") || dataSrc.length() == 0) {

			// Base 64 image
			String base64 = imgElement.getAttribute("src");

			Matcher base64Matcher = base64Pattern.matcher(base64);

			if (base64Matcher.find()) {

				@SuppressWarnings("unused")
				String imageExtension = base64Matcher.group("extension");
				String base64Data = base64Matcher.group("imgData");

				byte[] imageData = DatatypeConverter.parseBase64Binary(base64Data);

				return ImageIO.read(new ByteArrayInputStream(imageData));
			} else {
				throw new IllegalStateException("Misformed base 64? " + base64);
			}

		} else {
			// Link to image
			HttpsURLConnection imageConnection = (HttpsURLConnection) new URL(dataSrc).openConnection();
			imageConnection.setRequestProperty("User-Agent",
					"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/67.0.3396.99 Safari/537.36");
			return ImageIO.read(imageConnection.getInputStream());
		}
	}

	// private static void downloadFullSizeImages(){//htmlunit click on image and
	// &tbs=isz:l Size settings
	// download from src elem}

}
