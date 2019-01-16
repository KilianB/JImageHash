package com.github.kilianB.matcher.categorize;

import static com.github.kilianB.TestResources.ballon;
import static com.github.kilianB.TestResources.copyright;
import static com.github.kilianB.TestResources.highQuality;
import static com.github.kilianB.TestResources.lenna;
import static com.github.kilianB.TestResources.lowQuality;
import static com.github.kilianB.TestResources.thumbnail;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

/**
 * @author Kilian
 *
 */
public abstract class CategorizeBaseTest {

	@Test
	void distanceIdentity() {
		CategoricalImageMatcher matcher = getInstance();

		CategorizationResult pair = matcher.categorizeImageAndAdd(ballon, "ballon");
		// Category
		assertEquals(0, (int) pair.getCategory());
		// Dostance
		assertEquals(0, (double) pair.getQuality());

	}

	@Test
	public void categorizeMultipleImages() {

		CategoricalImageMatcher matcher = getInstance();

		matcher.categorizeImageAndAdd(ballon, "ballon");
		matcher.categorizeImageAndAdd(copyright, "copyright");
		matcher.categorizeImageAndAdd(highQuality, "highQuality");
		matcher.categorizeImageAndAdd(thumbnail, "thumbnail");
		matcher.categorizeImageAndAdd(lowQuality, "lowQuality");
		matcher.categorizeImageAndAdd(lenna, "lena");
		matcher.recomputeCategories();
		assertEquals(3, matcher.getCategories().size());

		int ballonCategory = matcher.getCategory("ballon");

		int copyRightCategory = matcher.getCategory("copyright");

		int lenaCategory = matcher.getCategory("lena");

		assertNotEquals(ballonCategory, copyRightCategory);
		// TODO this rarely evaluates as true due to the random factor of kMeans.
		// but it should not due to KmeansPlusPlus. did we mess up a great than instead
		// of greater sign or
		// something?
		assertNotEquals(ballonCategory, lenaCategory);
		assertNotEquals(copyRightCategory, lenaCategory);

		assertEquals(copyRightCategory, matcher.getCategory("highQuality"));
		assertEquals(copyRightCategory, matcher.getCategory("thumbnail"));
		assertEquals(copyRightCategory, matcher.getCategory("lowQuality"));
	}

	abstract CategoricalImageMatcher getInstance();

}
