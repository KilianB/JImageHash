package com.github.kilianB.matcher.categorize;

import java.awt.image.BufferedImage;
import java.util.List;

import com.github.kilianB.datastructures.Pair;

/**
 * 
 * @author Kilian
 *
 */
public interface CategoricalImageMatcher {

	/**
	 * Compute the category of the supplied image. A category is a collection of
	 * similar images mapped to a common hash which minimizes the distance of all
	 * hashes mapped to this category.
	 * 
	 * @param bi The buffered image to categorize
	 * @return a pair whose first value returns the category and second value
	 *         returns a distance measure between the category and the supplied
	 *         image. Smaller distances meaning a closer match
	 */
	Pair<Integer, Double> categorizeImage(BufferedImage bi);

	Pair<Integer, Double> categorizeImageAndAdd(BufferedImage bi, double maxThreshold, String uniqueId);

	List<Integer> getCategories();

	/**
	 * Recompute the category definition of this clustering matcher.
	 * <p>
	 * Recomputing categories will take recently added images into account and
	 * update image/category affiliation if necessary. This operation needs to be
	 * called manually due to the potential high cost of this method call.
	 * 
	 * <p>
	 * Unless otherwise noted the matcher makes no guarantee that the image category
	 * does not change with this method execution.
	 * 
	 */
	void recomputeCategories();

	/**
	 * Get the unique id's of all images mapped to this category
	 * 
	 * @param category to check for
	 * @return a list of all unique id's mapped to this category
	 */
	List<String> getImagesInCategory(int category);

	void addNestedMatcher(int category, CategoricalImageMatcher catMatcher);

	/**
	 * Get the current category the image with the unique id is matched to
	 * 
	 * @param uniqueId the id of a previously added image
	 * @return the category
	 */
	int getCategory(String uniqueId);

}