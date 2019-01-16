package com.github.kilianB.examples;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.HashMap;

import javax.imageio.ImageIO;

import com.github.kilianB.hashAlgorithms.AverageHash;
import com.github.kilianB.hashAlgorithms.DifferenceHash;
import com.github.kilianB.hashAlgorithms.DifferenceHash.Precision;
import com.github.kilianB.hashAlgorithms.HashingAlgorithm;
import com.github.kilianB.hashAlgorithms.PerceptiveHash;
import com.github.kilianB.hashAlgorithms.WaveletHash;
import com.github.kilianB.matcher.exotic.SingleImageMatcher;

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
		SingleImageMatcher matcher = new SingleImageMatcher();
		
		//Add hashing algorithms as you please. Both hashes will be queried
		matcher.addHashingAlgorithm(new AverageHash(64),.3);
		matcher.addHashingAlgorithm(new WaveletHash(32,3),.3);

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
		double dHashThreshold = .6;

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
