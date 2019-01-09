package com.github.kilianB.matcher.categorize;

import static com.github.kilianB.TestResources.ballon;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

import com.github.kilianB.TestResources;
import com.github.kilianB.datastructures.Pair;

/**
 * @author Kilian
 *
 */
public abstract class CategorizeBaseTest {

	@Test
	void distanceIdentity() {
		CategoricalImageMatcher matcher = getInstance();

		Pair<Integer, Double> pair = matcher.categorizeImageAndAdd(ballon, 0.2, "ballon");
		// Category
		assertEquals(0, (int) pair.getFirst());
		// Dostance
		assertEquals(0, (double) pair.getSecond());

	}

	@Test
	public void categorizeMultipleImages() {

		CategoricalImageMatcher matcher = getInstance();

		matcher.categorizeImageAndAdd(TestResources.ballon, 0.2, "ballon");
		matcher.categorizeImageAndAdd(TestResources.copyright, 0.2, "copyright");
		matcher.categorizeImageAndAdd(TestResources.highQuality, 0.2, "highQuality");
		matcher.categorizeImageAndAdd(TestResources.thumbnail, 0.2, "thumbnail");
		matcher.categorizeImageAndAdd(TestResources.lowQuality, 0.2, "lowQuality");
		matcher.categorizeImageAndAdd(TestResources.lenna, 0.2, "lena");
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
