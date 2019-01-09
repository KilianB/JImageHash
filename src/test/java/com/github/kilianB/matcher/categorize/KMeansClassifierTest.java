package com.github.kilianB.matcher.categorize;

import static com.github.kilianB.TestResources.ballon;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.github.kilianB.datastructures.Pair;
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
		
		Pair<Integer, Double> pair = matcher.categorizeImageAndAdd(ballon,0.2,"ballon");
		//Category
		assertEquals(0,(int)pair.getFirst());
		//Dostance
		assertEquals(Double.NaN,(double)pair.getSecond());
		
	}
}
