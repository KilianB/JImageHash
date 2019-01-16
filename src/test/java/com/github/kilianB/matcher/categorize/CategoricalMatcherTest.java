package com.github.kilianB.matcher.categorize;

import com.github.kilianB.hashAlgorithms.AverageHash;
import com.github.kilianB.hashAlgorithms.HashingAlgorithm;

/**
 * @author Kilian
 *
 */
class CategoricalMatcherTest extends CategorizeBaseTest{

	@Override
	CategoricalMatcher getInstance() {
		CategoricalMatcher matcher = new CategoricalMatcher(.2);
		HashingAlgorithm hasher = new AverageHash(32);
		matcher.addHashingAlgorithm(hasher);
		return matcher;
	}
	

}
