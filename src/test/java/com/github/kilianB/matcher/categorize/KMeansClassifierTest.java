package com.github.kilianB.matcher.categorize;

import static com.github.kilianB.TestResources.ballon;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.github.kilianB.hashAlgorithms.AverageHash;

/**
 * @author Kilian
 *
 */
class KMeansClassifierTest extends CategorizeBaseTest {

	@Override
	CategoricalImageMatcher getInstance() {
		return new KMeansClassifier(3, new AverageHash(32));
	}

	@Test 
	void distanceIdentity() {
		CategoricalImageMatcher matcher = getInstance();
		
		CategorizationResult pair = matcher.categorizeImageAndAdd(ballon,"ballon");
		//Category
		assertEquals(0,(int)pair.getCategory());
		//Dostance
		assertEquals(Double.NaN,(double)pair.getQuality());
		
	}
}
