package com.github.kilianB.examples;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.HashMap;

import javax.imageio.ImageIO;

import com.github.kilianB.hashAlgorithms.DifferenceHash;
import com.github.kilianB.hashAlgorithms.DifferenceHash.Precision;
import com.github.kilianB.hashAlgorithms.HashingAlgorithm;
import com.github.kilianB.hashAlgorithms.PerceptiveHash;
import com.github.kilianB.matcher.ImageMatcher.Setting;
import com.github.kilianB.matcher.pairwise.SingleImageMatcher;

/**
 * To increase the quality of the returned results it can be useful to chain
 * multiple algorithms back to back due to the different features each
 * algorithms compares.
 * 
 * @author Kilian
 *
 */
public class ChainAlgorithms {

	// Images used for testing
	private HashMap<String, BufferedImage> images = new HashMap<>();

	public void defaultMatcher() {

		/*
		 * A single image matcher allows to compare two images against each other. The
		 * default matcher chains an average hash followed by a perceptive hash
		 */
		SingleImageMatcher matcher = SingleImageMatcher.createDefaultMatcher();

		// Lets get two images
		BufferedImage img1 = images.get("ballon");
		BufferedImage img2 = images.get("lowQuality");

		// Check if the images are similar
		if (matcher.checkSimilarity(img1, img2)) {
			System.out.println("Ballon & Low Quality are likely duplicates");
		} else {
			System.out.println("Ballon & Low Quality are distinct images");
		}
	}

	/**
	 * Demonstrates the different presets of the default matcher
	 */
	@SuppressWarnings("unused")
	public void configuredDefaultMatcher() {

		// Example use one of those
		SingleImageMatcher fair;
		SingleImageMatcher forgiving;
		SingleImageMatcher strict;
		SingleImageMatcher quality;

		// Lets get two images
		BufferedImage img1 = images.get("highQuality");
		BufferedImage img2 = images.get("thumbnail");

		// More or less aggressive presets are available
		fair = SingleImageMatcher.createDefaultMatcher(Setting.Fair);
		forgiving = SingleImageMatcher.createDefaultMatcher(Setting.Forgiving);
		strict = SingleImageMatcher.createDefaultMatcher(Setting.Strict);
		// Same as no argument constructor
		quality = SingleImageMatcher.createDefaultMatcher(Setting.Quality);

		// Will return true
		System.out.println("Quality Matcher: Images are likely "
				+ (quality.checkSimilarity(img1, img2) ? "duplicates" : "distinct"));

		// Will return false
		System.out.println("Strict Matcher : Images are likely "
				+ (strict.checkSimilarity(img1, img2) ? "duplicates" : "distinct"));
	}

	/**
	 * Demonstrates how to fully configure a SingleImageMatcher. Choose own
	 * algorithms and thresholds
	 * 
	 * @param image1 First image to be matched against 2nd image
	 * @param image2 Second image to be matched against the first image
	 */
	public void chainAlgorithms(BufferedImage image1, BufferedImage image2) {

		/*
		 * Create multiple algorithms we want to test the images against
		 */

		HashingAlgorithm dHash = new DifferenceHash(32, Precision.Double);
		// When shall an image be classified as a duplicate [0 - keyLenght]
		// DHashes double precision doubles the key length supplied in the constructor
		int dHashThreshold = 15;

		HashingAlgorithm pHash = new PerceptiveHash(32);
		// When shall an image be counted as a duplicate? [0-1]
		double normalizedPHashThreshold = 0.6;
		boolean normalized = true;

		// This instance can be reused. No need to recreate it every time you want to
		// match 2 images
		SingleImageMatcher matcher = new SingleImageMatcher();

		// Add algorithm to the matcher

		// First dirty filter
		matcher.addHashingAlgorithm(dHash, dHashThreshold);
		// If successful apply second filer
		matcher.addHashingAlgorithm(pHash, normalizedPHashThreshold, normalized);

		System.out.println(matcher.checkSimilarity(image1, image2));
	}

	public static void main(String[] args) {
		new ChainAlgorithms();
	}

	public ChainAlgorithms() {
		loadImages();

		System.out.println("defaultMatcher()");
		defaultMatcher();

		System.out.println("\nconfiguredDefaultMatcher()");
		configuredDefaultMatcher();

		System.out.println("\nchainAlgorithms()");
		chainAlgorithms(images.get("highQuality"), images.get("lowQuality"));
	}

	private void loadImages() {
		// Load images
		try {
			images.put("ballon", ImageIO.read(getClass().getResourceAsStream("images/ballon.jpg")));
			images.put("copyright", ImageIO.read(getClass().getResourceAsStream("images/copyright.jpg")));
			images.put("highQuality", ImageIO.read(getClass().getResourceAsStream("images/highQuality.jpg")));
			images.put("lowQuality", ImageIO.read(getClass().getResourceAsStream("images/lowQuality.jpg")));
			images.put("thumbnail", ImageIO.read(getClass().getResourceAsStream("images/thumbnail.jpg")));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
