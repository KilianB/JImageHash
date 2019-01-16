package com.github.kilianB.hashAlgorithms;

import static com.github.kilianB.TestResources.lenna;
import static com.github.kilianB.TestResources.lenna180;
import static com.github.kilianB.TestResources.lenna270;
import static com.github.kilianB.TestResources.lenna90;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.github.kilianB.ArrayUtil;
import com.github.kilianB.hash.Hash;

/**
 * @author Kilian
 *
 */
public abstract class RotationalTestBase extends HashTestBase{

	@ParameterizedTest
	@MethodSource(value = "com.github.kilianB.hashAlgorithms.RotationalTestBase#bitResolution")
	public void rotatedImages(Integer bitResolution) {

		HashingAlgorithm h = getInstance(bitResolution+this.offsetBitResolution());
		Hash rot0 = h.hash(lenna);
		Hash rot90 = h.hash(lenna90);
		Hash rot180 = h.hash(lenna180);
		Hash rot270 = h.hash(lenna270);

		assertAll(() -> {
			assertTrue(rot0.normalizedHammingDistance(rot90) < 0.2);
		},() -> {
			assertTrue(rot0.normalizedHammingDistance(rot180) < 0.2);
		},() -> {
			assertTrue(rot0.normalizedHammingDistance(rot270) < 0.2);
		},() -> {
			assertTrue(rot90.normalizedHammingDistance(rot180) < 0.2);
		},() -> {
			assertTrue(rot90.normalizedHammingDistance(rot270) < 0.2);
		}, () -> {
			assertTrue(rot180.normalizedHammingDistance(rot270) < 0.2);
		});
	}
	
	public static Stream<Integer> bitResolution() {
		Integer[] ints = new Integer[10];
		ArrayUtil.fillArray(ints, (index) -> {
			return index*2+20;
		});
		return Stream.of(ints);
	}
	
}
