package dev.brachtendorf.jimagehash.matcher.categorize;

import static dev.brachtendorf.jimagehash.TestResources.ballon;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import dev.brachtendorf.jimagehash.hashAlgorithms.AverageHash;
import dev.brachtendorf.jimagehash.matcher.categorize.WeightedCategoricalMatcher.DimReduction;

/**
 * @author Kilian
 *
 */
class WeightedCategoricalMatcherTest extends CategorizeBaseTest {
	
	@ParameterizedTest
	@MethodSource(value = "dev.brachtendorf.jimagehash.matcher.categorize.WeightedCategoricalMatcherTest#getMatcher")
	void sameDistanceAfterClusterRecomputation(WeightedCategoricalMatcher wMatcher) {
		wMatcher.addHashingAlgorithm(new AverageHash(32));
		wMatcher.categorizeImageAndAdd(ballon, "ballon");
		wMatcher.recomputeClusters(10);
		assertEquals(0, (double) wMatcher.categorizeImage(ballon).getQuality());
	}

	public static Stream<WeightedCategoricalMatcher> getMatcher() {
		return Stream.of(new WeightedCategoricalMatcher(.2,DimReduction.NONE),
				new WeightedCategoricalMatcher(.2,DimReduction.K_MEANS_APPROXIMATION),
				new WeightedCategoricalMatcher(.2,DimReduction.BINARY_TREE));
	}

	@Override
	CategoricalMatcher getInstance() {
		CategoricalMatcher matcher = new WeightedCategoricalMatcher(.2,DimReduction.NONE);
		matcher.addHashingAlgorithm(new AverageHash(32));
		return matcher;
	}

}
