package com.github.kilianB.matcher;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Map.Entry;

import javax.imageio.ImageIO;

import com.github.kilianB.hashAlgorithms.DifferenceHash;
import com.github.kilianB.hashAlgorithms.HashingAlgorithm;
import com.github.kilianB.hashAlgorithms.PerceptiveHash;
import com.github.kilianB.hashAlgorithms.DifferenceHash.Precision;

/**
 * Convenience class to chain multiple hashing algorithms to check if two images
 * are similar
 * 
 * @author Kilian
 *
 */
public class SingleImageMatcher extends ImageMatcher {

	/**
	 * A preconfigured image matcher chaining dHash and pHash algorithms for fast
	 * high quality results. <p> The dHash is a quick algorithms allowing to filter
	 * images which are very unlikely to be similar images. pHash is computationally
	 * more expensive and used to inspect possible candidates further
	 * 
	 * @return The matcher used to check if images are similar
	 */
	public static SingleImageMatcher createDefaultMatcher() {
		return createDefaultMatcher(Setting.Quality);
	}

	/**
	 * A preconfigured image matcher chaining dHash and pHash algorithms for fast
	 * high quality results. <p> The dHash is a quick algorithms allowing to filter
	 * images which are very unlikely to be similar images. pHash is computationally
	 * more expensive and used to inspect possible candidates further
	 * 
	 * @param algorithmSetting
	 *            How agressive the algorithm advances while comparing images </p>
	 *            <ul> <li><b>Forgiving:</b> Matches a bigger range of images</li>
	 *            <li><b>Fair:</b> Matches all sample images</li>
	 *            <li><b>Quality:</b> Recommended: Does not initially filter as
	 *            aggressively as Fair but returns usable results</li>
	 *            <li><b>Strict:</b> Only matches images which are closely related
	 *            to each other</li> </ul>
	 * 
	 * @return The matcher used to check if images are similar
	 */
	public static SingleImageMatcher createDefaultMatcher(Setting algorithmSetting) {

		SingleImageMatcher matcher = new SingleImageMatcher();

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

	/**
	 * Execute all supplied hashing algorithms in the order they were supplied and
	 * check if the images are similar
	 * 
	 * @param image
	 *            First input image
	 * @param image1
	 *            Second input image
	 * @return true if images are considered similar
	 * @throws IOException
	 *             if an error occurred during image loading
	 * @see #checkSimilarity(BufferedImage, BufferedImage)
	 */
	public boolean checkSimilarity(File image, File image1) throws IOException {
		return (checkSimilarity(ImageIO.read(image), ImageIO.read(image1)));
	}

	/**
	 * Execute all supplied hashing algorithms in the order they were supplied and
	 * check if the images are similar
	 * 
	 * @param image
	 *            First input image
	 * @param image1
	 *            Second input image
	 * @return true if images are considered similar
	 */
	public boolean checkSimilarity(BufferedImage image, BufferedImage image1) {
		if (steps.isEmpty())
			throw new IllegalStateException(
					"Please supply at least one hashing algorithm prior to invoking the match method");

		for (Entry<HashingAlgorithm, AlgoSettings> entry : steps.entrySet()) {
			Hash hash = entry.getKey().hash(image);
			Hash hash1 = entry.getKey().hash(image1);

			// Check if the hashing algo is within the threshold. If it's not return early
			if (!entry.getValue().apply(hash, hash1)) {
				return false;
			}
		}
		// Everything matched
		return true;
	}

}
