package dev.brachtendorf.jimagehash.matcher.categorize;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dev.brachtendorf.jimagehash.matcher.PlainImageMatcher;

/**
 * An abstract implementation of the categorical image matcher supporting
 * classes to keep track of the categories an image was matched to.
 * <p>
 * It is up to the implementing class to keep the field
 * <code>cachedImagesInCategory</code> and <code>reverseImageCategoryMap</code>
 * in a valid state.
 * 
 * @author Kilian
 * @since 3.0.0
 *
 */
public abstract class AbstractCategoricalMatcher extends PlainImageMatcher implements CategoricalImageMatcher {

	protected Map<Integer, List<String>> cachedImagesInCategory = new HashMap<>();
	protected Map<String, Integer> reverseImageCategoryMap = new HashMap<>();

	public abstract CategorizationResult categorizeImageAndAdd(BufferedImage bi, String uniqueId);

	@Override
	public int getCategory(String uniqueId) {
		int category = reverseImageCategoryMap.get(uniqueId);
		return category;
	}

	@Override
	public CategorizationResult categorizeImage(BufferedImage bi) {
		return categorizeImage(null, bi);
	}

	protected abstract CategorizationResult categorizeImage(String uniqueId, BufferedImage bi);

	/**
	 * Check if an image has already been added to the categorizer
	 * 
	 * @param uniqueId the unique id of the image
	 * @return true if it has been added, false otherwise
	 */
	public boolean isCategorized(String uniqueId) {
		return reverseImageCategoryMap.containsKey(uniqueId);
	}

	@Override
	public List<Integer> getCategories() {
		List<Integer> categoriesAsList = new ArrayList<>(cachedImagesInCategory.keySet());
		categoriesAsList.sort(null);
		return Collections.unmodifiableList(categoriesAsList);
	}

	@Override
	public List<String> getImagesInCategory(int category) {
		return cachedImagesInCategory.get(category);
	}

	/**
	 * Get the number of images that are were added in this category.
	 * 
	 * @param category to retrieve the number of images from.
	 * @return he number of images that were mapped and added to this category
	 */
	public int getImageCountInCategory(int category) {
		return cachedImagesInCategory.get(category).size();
	}

}
