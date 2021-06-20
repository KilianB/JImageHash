package dev.brachtendorf.jimagehash.matcher.categorize;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.imageio.ImageIO;

/**
 * Image matcher allowing to group images into similar categories.
 * <p>
 * Usually cluster results are requested immediately and images may get added
 * over time resulting in clusters being moved around depending on the
 * implementation details.
 * 
 * <p>
 * Classes shall make a best effort to provide current clusters but usually
 * require a call to {@link #recomputeCategories()} be made to reflect changes
 * of newly added images to be fully incorporated.
 * <p>
 * Images are only referenced by a string identifier to allow computation with
 * many images.
 * 
 * @author Kilian
 * @since 3.0.0
 */
public interface CategoricalImageMatcher {

	/**
	 * Compute the category of the supplied image. A category is a collection of
	 * similar images mapped to a common hash which minimizes the distance of all
	 * hashes mapped to this category.
	 * 
	 * @param imageFile The image to categorize
	 * @return a pair whose first value returns the category and second value
	 *         returns a distance measure between the category and the supplied
	 *         image. Smaller distances meaning a closer match
	 * @throws IOException if an error occurs during file reading
	 */
	default CategorizationResult categorizeImage(File imageFile) throws IOException {
		return categorizeImage(ImageIO.read(imageFile));
	}

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
	CategorizationResult categorizeImage(BufferedImage bi);

	/**
	 * Compute the closest category of an image and afterwards add it to the
	 * internal categorization queue. Some matchers may choose to immediately update
	 * the current category to reflect the changes.
	 * 
	 * <p>
	 * The <b>add</b> action is implementation depended. Some categorizers may
	 * choose to directly incorporate the image and update it's category
	 * representation other algorithms may require a call to
	 * {@link #recomputeCategories()} before the addition takes effect.
	 * 
	 * @param bi       the image to categorize
	 * @param uniqueId the unique id to reference the image by.
	 * @return the currently closest cluster for this image.
	 */
	CategorizationResult categorizeImageAndAdd(BufferedImage bi, String uniqueId);

	/**
	 * Compute the closest category of an image and afterwards add it to the
	 * internal categorization queue. Some matchers may choose to immediately update
	 * the current category to reflect the changes.
	 * 
	 * <p>
	 * The <b>add</b> action is implementation depended. Some categorizers may
	 * choose to directly incorporate the image and update it's category
	 * representation other algorithms may require a call to
	 * {@link #recomputeCategories()} before the addition takes effect.
	 * <p>
	 * The image will be referenced by the absolute file path as uniqueId.
	 * 
	 * @param imageFile the image file to categorize
	 * @return the currently closest cluster for this image.
	 * @throws IOException if an error occurs during file reading operation
	 */
	default CategorizationResult categorizeImageAndAdd(File imageFile) throws IOException {
		return categorizeImageAndAdd(ImageIO.read(imageFile), imageFile.getAbsolutePath());
	}

	/**
	 * Get a list of available categories this matcher matched images to. Each
	 * category represents a set of images with high similarity.
	 * 
	 * @return A list of id's
	 */
	List<Integer> getCategories();

	/**
	 * Recompute the category definition of this clustering matcher and it's nested
	 * matchers.
	 * <p>
	 * Recomputing categories will take recently added images into account and
	 * update image/category affiliation if necessary. This operation needs to be
	 * called manually due to the potential high cost of this method call.
	 * 
	 * <p>
	 * Unless otherwise noted the matcher makes no guarantee that the image category
	 * does not change with this method execution.
	 */
	void recomputeCategories();

	/**
	 * Get the unique id's of all images mapped to this category
	 * 
	 * @param category to check for
	 * @return a list of all unique id's mapped to this category
	 */
	List<String> getImagesInCategory(int category);

	// TODO next release
	/**
	 * Register a nested matcher for this category. Only one matcher can be register
	 * for each category. If is already present the old categorizer will be
	 * replaced.
	 * 
	 * <p>
	 * Image categorizers can be nested in order to achieve more fine grained
	 * categorization result. Once an image gets matched to this category the added
	 * matcher will categorize the image and <b>append</b> it's categorization
	 * result to the
	 * {@link com.github.kilianB.matcher.categorize.CategorizationResult result
	 * object}.
	 * 
	 * Nested matchers do not alter the categorization of the current matcher but
	 * add a layer beneath.
	 * 
	 * @param category
	 * @param catMatcher
	 */
	// void addChainedMatcher(CategoricalImageMatcher catMatcher);

	/**
	 * Get the current category of the image described by this unique id. A category
	 * usually maps an image to a cluster.
	 * 
	 * @param uniqueId the id of a previously added image
	 * @return the category
	 */
	int getCategory(String uniqueId);

}