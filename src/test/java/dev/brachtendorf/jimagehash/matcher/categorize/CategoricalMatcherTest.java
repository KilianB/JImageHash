package dev.brachtendorf.jimagehash.matcher.categorize;

import dev.brachtendorf.jimagehash.hashAlgorithms.AverageHash;
import dev.brachtendorf.jimagehash.hashAlgorithms.HashingAlgorithm;
import dev.brachtendorf.jimagehash.matcher.categorize.CategoricalMatcher;

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
