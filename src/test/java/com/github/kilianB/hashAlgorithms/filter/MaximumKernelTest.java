package com.github.kilianB.hashAlgorithms.filter;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.github.kilianB.ArrayUtil;

/**
 * @author Kilian
 *
 */
class MaximumKernelTest {

	@Test
	public void identity() {
		double[][] arr = new double[10][10];
		ArrayUtil.fillArrayMulti(arr, (index) -> {
			return (double) index;
		});
		MaximumKernel kernel = new MaximumKernel(1, 1);
		double[][] result = kernel.apply(arr);
		assertArrayEquals(arr, result);
	}

	@Test
	public void sameDimension1D() {
		double[][] input = { { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 } };
		Kernel k = new MaximumKernel(3, 1);
		double[][] result = k.apply(input);
		assertEquals(input.length, result.length);
		assertEquals(input[0].length, result[0].length);
	}

	@Test
	public void sameDimension2DSquared() {
		double[][] input = { { 0, 1, 2 }, { 0, 1, 2 }, { 0, 1, 2 } };
		Kernel k = new MaximumKernel(3, 1);
		double[][] result = k.apply(input);
		assertEquals(input.length, result.length);
		assertEquals(input[0].length, result[0].length);
	}

	@Test
	public void sameDimension2D() {
		double[][] input = { { 0, 1, 2 }, { 0, 1, 2 }};
		Kernel k = new MaximumKernel(3, 1);
		double[][] result = k.apply(input);
		assertEquals(input.length, result.length);
		assertEquals(input[0].length, result[0].length);

	}

	@Test
	public void noMask1D() {
		double[][] arr = { { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13 } };
		double[][] res = { { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 13 } };
		MaximumKernel kernel = new MaximumKernel(3, 1);
		double[][] result = kernel.apply(arr);
		assertArrayEquals(res, result);
	}

	@Test
	public void mask1D() {
		double[][] mask = { { 1, 3, 1 } };
		double[][] arr = { { 0, 1, 4, 2, 3, 5 } };
		double[][] res = { { 1, 4, 4, 2, 3, 5 } };
		MaximumKernel kernel = new MaximumKernel(mask);
		double[][] result = kernel.apply(arr);
		assertArrayEquals(res, result);
	}

}
