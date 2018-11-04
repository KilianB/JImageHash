package com.github.kilianB.matcher;

import java.awt.image.BufferedImage;
import java.util.PriorityQueue;

import com.github.kilianB.dataStrorage.tree.Result;

/**
 * 
 * Image matcher allowing to precompute hashes of multiple images before being queried 
 * to return similar images may implement this interface
 * 
 * @author Kilian
 * @since 2.0.0
 */
public interface PersistentImageMatcher {

	/**
	 * Add the images to the matcher allowing the image to be found in future searches.
	 * @param images The images whose hash will be added to the matcher
	 */
	default void addImages(BufferedImage... images) {
		for(BufferedImage image : images) {
			addImage(image);
		}
	}

	/**
	 * Add the image to the matcher allowing the image to be found in future searches.
	 * @param image The image whose hash will be added to the matcher
	 */
	void addImage(BufferedImage image);

	/**
	 * Search for all similar images passing the algorithm filters supplied to this matcher. 
	 * If the image itself was added to the tree it will be returned with a distance of 0
	 * @param image The image other images will be matched against
	 * @return	Similar images
	 * 	Return all images sorted by the <a href="https://en.wikipedia.org/wiki/Hamming_distance">hamming distance</a>
	 *  of the last applied algorithms
	 */
	PriorityQueue<Result<BufferedImage>> getMatchingImages(BufferedImage image);

}