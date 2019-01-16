package com.github.kilianB.matcher.categorize;

import static com.github.kilianB.TestResources.ballon;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

import com.github.kilianB.TestResources;

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

		matcher.categorizeImageAndAdd(TestResources.ballon, "ballon");
		matcher.categorizeImageAndAdd(TestResources.copyright, "copyright");
		matcher.categorizeImageAndAdd(TestResources.highQuality, "highQuality");
		matcher.categorizeImageAndAdd(TestResources.thumbnail, "thumbnail");
		matcher.categorizeImageAndAdd(TestResources.lowQuality, "lowQuality");
		matcher.categorizeImageAndAdd(TestResources.lenna, "lena");
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
