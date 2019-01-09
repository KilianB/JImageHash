package com.github.kilianB.matcher.categorize;

import static com.github.kilianB.TestResources.ballon;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.github.kilianB.hashAlgorithms.AverageHash;
import com.github.kilianB.matcher.categorize.WeightedCategoricalMatcher.DimReduction;

/**
 * @author Kilian
 *
 */
class WeightedCategoricalMatcherTest extends CategorizeBaseTest {
	
	@ParameterizedTest
	@MethodSource(value = "com.github.kilianB.matcher.categorize.WeightedCategoricalMatcherTest#getMatcher")
	void sameDistanceAfterClusterRecomputation(WeightedCategoricalMatcher wMatcher) {
		wMatcher.addHashingAlgorithm(new AverageHash(32), 0);
		wMatcher.categorizeImageAndAdd(ballon, 0.2, "ballon");
		wMatcher.recomputeClusters(10);
		assertEquals(0, (double) wMatcher.categorizeImage(ballon).getSecond());
	}

	public static Stream<WeightedCategoricalMatcher> getMatcher() {
		return Stream.of(new WeightedCategoricalMatcher(DimReduction.NONE),
				new WeightedCategoricalMatcher(DimReduction.K_MEANS_APPROXIMATION),
				new WeightedCategoricalMatcher(DimReduction.BINARY_TREE));
	}

	@Override
	CategoricalMatcher getInstance() {
		CategoricalMatcher matcher = new WeightedCategoricalMatcher(DimReduction.NONE);
		matcher.addHashingAlgorithm(new AverageHash(32), 0);
		return matcher;
	}

}
