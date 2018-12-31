package com.github.kilianB.hashAlgorithms.filter;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.github.kilianB.ArrayUtil;
import com.github.kilianB.hashAlgorithms.filter.Kernel.EdgeHandlingStrategy;

/**
 * @author Kilian
 *
 */
class KernelTest {

	/** 3x3 Kernel */
	private static Kernel squareKernel;
	/** 1x3 Kernel */
	private static Kernel widthKernel;
	/** 3x1 kernel */
	private static Kernel heightKernel;

	private static double[][] dummyInputDouble = new double[10][15];
	private static int[][] dummyInputInt = new int[10][15];
	private static byte[][] dummyInputByte = new byte[10][15];

	@BeforeAll
	public static void prepareDefaultKernels() {

		double[][] square = { { 1, 1, 1 }, { 1, 1, 1 }, { 1, 1, 1 } };
		double[][] width = { { 1, 1, 1 } };
		double[][] height = { { 1 }, { 1 }, { 1 } };

		squareKernel = new Kernel(square);
		widthKernel = new Kernel(width);
		heightKernel = new Kernel(height);

		ArrayUtil.fillArrayMulti(dummyInputDouble, (index) -> {
			return (double) index;
		});

		ArrayUtil.fillArrayMulti(dummyInputInt, (index) -> {
			return (int) index;
		});

		ArrayUtil.fillArrayMulti(dummyInputByte, (index) -> {
			return index.byteValue();
		});
	}

	@Test
	public void copyConstructorEdge() {
		Kernel copy = new Kernel(widthKernel);
		assertEquals(widthKernel.edgeHandling, copy.edgeHandling);
	}

	@Test
	public void copyConstructorMask() {
		Kernel copy = new Kernel(widthKernel);
		assertAll(() -> assertEquals(widthKernel.mask.length, copy.mask.length),
				() -> assertEquals(widthKernel.mask[0].length, copy.mask[0].length));
	}

	@Test
	public void identity() {

		double[][] mask = { { 1d } };
		double[][] arr = new double[10][10];
		ArrayUtil.fillArrayMulti(arr, (index) -> {
			return (double) index;
		});
		Kernel kernel = new Kernel(mask);
		double[][] result = kernel.apply(arr);
		assertArrayEquals(arr, result);
	}

	@Test
	public void sameDimension1D() {
		double[][] input = { { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 } };
		double[][] mask = { { 1d, 1d, 1d } };
		Kernel k = new Kernel(mask);
		double[][] result = k.apply(input);

		assertAll(() -> assertEquals(input.length, result.length),
				() -> assertEquals(input[0].length, result[0].length));

	}

	@Test
	public void sameDimension2DSquared() {
		double[][] input = { { 0, 1, 2 }, { 0, 1, 2 }, { 0, 1, 2 } };
		double[][] mask = { { 1d, 1d, 1d }, { 1d, 1d, 1d }, { 1d, 1d, 1d } };
		Kernel k = new Kernel(mask);
		double[][] result = k.apply(input);
		assertAll(() -> assertEquals(input.length, result.length),
				() -> assertEquals(input[0].length, result[0].length));
	}

	@Test
	public void sameDimension2D() {
		double[][] input = { { 0, 1, 2 }, { 0, 1, 2 } };
		double[][] mask = { { 1d, 1d, 1d } };

		Kernel k = new Kernel(mask);
		double[][] result = k.apply(input);
		assertAll(() -> assertEquals(input.length, result.length),
				() -> assertEquals(input[0].length, result[0].length));
	}

	@Test
	public void sameDimension2D1() {
		double[][] input = { { 0, 1, 2 }, { 0, 1, 2 } };
		double[][] mask = { { 1d }, { 1d }, { 1d } };

		Kernel k = new Kernel(mask);
		double[][] result = k.apply(input);
		assertAll(() -> assertEquals(input.length, result.length),
				() -> assertEquals(input[0].length, result[0].length));
	}

	@Nested
	class BoxFilter {

		@Nested
		class Original {
			@Disabled
			@Test
			public void widthHeightConstructor() {
				int width = 21;
				int height = 1;
				Kernel k = Kernel.boxFilter(width, height, 1);
				assertAll(() -> assertEquals(width, k.mask[0].length), () -> assertEquals(height, k.mask.length));
			}
		}

		@Nested
		class Normalized {

			// Works on MultiKernels
			@Test
			public void widthHeightConstructor() {
				int width = 3;
				int height = 51;
				Kernel k = Kernel.boxFilterNormalized(width, height);
				k.edgeHandling = EdgeHandlingStrategy.NO_OP;
				assertAll(() -> assertEquals(width, k.mask[0].length), () -> assertEquals(height, k.mask.length));
			}
		}

	}

	/**
	 * Currently only tests index out of boundes exceptions
	 * 
	 * @author Kilian
	 *
	 */
	@Nested
	class EdgeHandling {

		@Nested
		class Double {
			@Test
			public void testNoOpSquare() {
				Kernel k = new Kernel(squareKernel);
				k.edgeHandling = EdgeHandlingStrategy.NO_OP;
				double[][] expected = {
						{ 0.0, 1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0, 11.0, 12.0, 13.0, 14.0 },
						{ 0.0, 9.0, 18.0, 27.0, 36.0, 45.0, 54.0, 63.0, 72.0, 81.0, 90.0, 99.0, 108.0, 117.0, 14.0 },
						{ 0.0, 9.0, 18.0, 27.0, 36.0, 45.0, 54.0, 63.0, 72.0, 81.0, 90.0, 99.0, 108.0, 117.0, 14.0 },
						{ 0.0, 9.0, 18.0, 27.0, 36.0, 45.0, 54.0, 63.0, 72.0, 81.0, 90.0, 99.0, 108.0, 117.0, 14.0 },
						{ 0.0, 9.0, 18.0, 27.0, 36.0, 45.0, 54.0, 63.0, 72.0, 81.0, 90.0, 99.0, 108.0, 117.0, 14.0 },
						{ 0.0, 9.0, 18.0, 27.0, 36.0, 45.0, 54.0, 63.0, 72.0, 81.0, 90.0, 99.0, 108.0, 117.0, 14.0 },
						{ 0.0, 9.0, 18.0, 27.0, 36.0, 45.0, 54.0, 63.0, 72.0, 81.0, 90.0, 99.0, 108.0, 117.0, 14.0 },
						{ 0.0, 9.0, 18.0, 27.0, 36.0, 45.0, 54.0, 63.0, 72.0, 81.0, 90.0, 99.0, 108.0, 117.0, 14.0 },
						{ 0.0, 9.0, 18.0, 27.0, 36.0, 45.0, 54.0, 63.0, 72.0, 81.0, 90.0, 99.0, 108.0, 117.0, 14.0 },
						{ 0.0, 1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0, 11.0, 12.0, 13.0, 14.0 } };
				assertArrayEquals(expected, k.apply(dummyInputDouble));
			}

			@Test
			public void testNoOpWidth() {
				Kernel k = new Kernel(widthKernel);
				k.edgeHandling = EdgeHandlingStrategy.NO_OP;
				double[][] expected = {
						{ 0.0, 3.0, 6.0, 9.0, 12.0, 15.0, 18.0, 21.0, 24.0, 27.0, 30.0, 33.0, 36.0, 39.0, 14.0 },
						{ 0.0, 3.0, 6.0, 9.0, 12.0, 15.0, 18.0, 21.0, 24.0, 27.0, 30.0, 33.0, 36.0, 39.0, 14.0 },
						{ 0.0, 3.0, 6.0, 9.0, 12.0, 15.0, 18.0, 21.0, 24.0, 27.0, 30.0, 33.0, 36.0, 39.0, 14.0 },
						{ 0.0, 3.0, 6.0, 9.0, 12.0, 15.0, 18.0, 21.0, 24.0, 27.0, 30.0, 33.0, 36.0, 39.0, 14.0 },
						{ 0.0, 3.0, 6.0, 9.0, 12.0, 15.0, 18.0, 21.0, 24.0, 27.0, 30.0, 33.0, 36.0, 39.0, 14.0 },
						{ 0.0, 3.0, 6.0, 9.0, 12.0, 15.0, 18.0, 21.0, 24.0, 27.0, 30.0, 33.0, 36.0, 39.0, 14.0 },
						{ 0.0, 3.0, 6.0, 9.0, 12.0, 15.0, 18.0, 21.0, 24.0, 27.0, 30.0, 33.0, 36.0, 39.0, 14.0 },
						{ 0.0, 3.0, 6.0, 9.0, 12.0, 15.0, 18.0, 21.0, 24.0, 27.0, 30.0, 33.0, 36.0, 39.0, 14.0 },
						{ 0.0, 3.0, 6.0, 9.0, 12.0, 15.0, 18.0, 21.0, 24.0, 27.0, 30.0, 33.0, 36.0, 39.0, 14.0 },
						{ 0.0, 3.0, 6.0, 9.0, 12.0, 15.0, 18.0, 21.0, 24.0, 27.0, 30.0, 33.0, 36.0, 39.0, 14.0 } };
				assertArrayEquals(expected, k.apply(dummyInputDouble));
			}

			@Test
			public void testNoOpHeight() {
				Kernel k = new Kernel(heightKernel);
				k.edgeHandling = EdgeHandlingStrategy.NO_OP;
				k.apply(dummyInputDouble);
				double[][] expected = {{0.0, 1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0, 11.0, 12.0, 13.0, 14.0},
					 {0.0, 3.0, 6.0, 9.0, 12.0, 15.0, 18.0, 21.0, 24.0, 27.0, 30.0, 33.0, 36.0, 39.0, 42.0},
					 {0.0, 3.0, 6.0, 9.0, 12.0, 15.0, 18.0, 21.0, 24.0, 27.0, 30.0, 33.0, 36.0, 39.0, 42.0},
					 {0.0, 3.0, 6.0, 9.0, 12.0, 15.0, 18.0, 21.0, 24.0, 27.0, 30.0, 33.0, 36.0, 39.0, 42.0},
					 {0.0, 3.0, 6.0, 9.0, 12.0, 15.0, 18.0, 21.0, 24.0, 27.0, 30.0, 33.0, 36.0, 39.0, 42.0},
					 {0.0, 3.0, 6.0, 9.0, 12.0, 15.0, 18.0, 21.0, 24.0, 27.0, 30.0, 33.0, 36.0, 39.0, 42.0},
					 {0.0, 3.0, 6.0, 9.0, 12.0, 15.0, 18.0, 21.0, 24.0, 27.0, 30.0, 33.0, 36.0, 39.0, 42.0},
					 {0.0, 3.0, 6.0, 9.0, 12.0, 15.0, 18.0, 21.0, 24.0, 27.0, 30.0, 33.0, 36.0, 39.0, 42.0},
					 {0.0, 3.0, 6.0, 9.0, 12.0, 15.0, 18.0, 21.0, 24.0, 27.0, 30.0, 33.0, 36.0, 39.0, 42.0},
					 {0.0, 1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0, 11.0, 12.0, 13.0, 14.0}};
				assertArrayEquals(expected, k.apply(dummyInputDouble));
			}

			@Test
			public void testExpandSquare() {
				Kernel k = new Kernel(squareKernel);
				k.edgeHandling = EdgeHandlingStrategy.EXPAND;
				k.apply(dummyInputDouble);
			}

			@Test
			public void testExpandWidth() {
				Kernel k = new Kernel(widthKernel);
				k.edgeHandling = EdgeHandlingStrategy.EXPAND;
				k.apply(dummyInputDouble);
			}

			@Test
			public void testExpandHeight() {
				Kernel k = new Kernel(heightKernel);
				k.edgeHandling = EdgeHandlingStrategy.EXPAND;
				k.apply(dummyInputDouble);
			}

			@Test
			public void testWrapSquare() {
				Kernel k = new Kernel(squareKernel);
				k.edgeHandling = EdgeHandlingStrategy.WRAP;
				k.apply(dummyInputDouble);
			}

			@Test
			public void testWrapWidth() {
				Kernel k = new Kernel(widthKernel);
				k.edgeHandling = EdgeHandlingStrategy.WRAP;
				k.apply(dummyInputDouble);
			}

			@Test
			public void testWrapHeight() {
				Kernel k = new Kernel(heightKernel);
				k.edgeHandling = EdgeHandlingStrategy.WRAP;
				k.apply(dummyInputDouble);
			}

			@Test
			public void testMirrorSquare() {
				Kernel k = new Kernel(squareKernel);
				k.edgeHandling = EdgeHandlingStrategy.MIRROR;
				k.apply(dummyInputDouble);
			}

			@Test
			public void testMirrorWidth() {
				Kernel k = new Kernel(widthKernel);
				k.edgeHandling = EdgeHandlingStrategy.MIRROR;
				k.apply(dummyInputDouble);
			}

			@Test
			public void testMirrorHeight() {
				Kernel k = new Kernel(heightKernel);
				k.edgeHandling = EdgeHandlingStrategy.MIRROR;
				k.apply(dummyInputDouble);
			}
		}

		@Nested
		class Integer {
			@Test
			public void testNoOpSquare() {
				Kernel k = new Kernel(squareKernel);
				k.edgeHandling = EdgeHandlingStrategy.NO_OP;
				k.apply(dummyInputInt);
			}

			@Test
			public void testNoOpWidth() {
				Kernel k = new Kernel(widthKernel);
				k.edgeHandling = EdgeHandlingStrategy.NO_OP;
				k.apply(dummyInputInt);
			}

			@Test
			public void testNoOpHeight() {
				Kernel k = new Kernel(heightKernel);
				k.edgeHandling = EdgeHandlingStrategy.NO_OP;
				k.apply(dummyInputInt);
			}

			@Test
			public void testExpandSquare() {
				Kernel k = new Kernel(squareKernel);
				k.edgeHandling = EdgeHandlingStrategy.EXPAND;
				k.apply(dummyInputInt);
			}

			@Test
			public void testExpandWidth() {
				Kernel k = new Kernel(widthKernel);
				k.edgeHandling = EdgeHandlingStrategy.EXPAND;
				k.apply(dummyInputInt);
			}

			@Test
			public void testExpandHeight() {
				Kernel k = new Kernel(heightKernel);
				k.edgeHandling = EdgeHandlingStrategy.EXPAND;
				k.apply(dummyInputInt);
			}

			@Test
			public void testWrapSquare() {
				Kernel k = new Kernel(squareKernel);
				k.edgeHandling = EdgeHandlingStrategy.WRAP;
				k.apply(dummyInputInt);
			}

			@Test
			public void testWrapWidth() {
				Kernel k = new Kernel(widthKernel);
				k.edgeHandling = EdgeHandlingStrategy.WRAP;
				k.apply(dummyInputInt);
			}

			@Test
			public void testWrapHeight() {
				Kernel k = new Kernel(heightKernel);
				k.edgeHandling = EdgeHandlingStrategy.WRAP;
				k.apply(dummyInputInt);
			}

			@Test
			public void testMirrorSquare() {
				Kernel k = new Kernel(squareKernel);
				k.edgeHandling = EdgeHandlingStrategy.MIRROR;
				k.apply(dummyInputInt);
			}

			@Test
			public void testMirrorWidth() {
				Kernel k = new Kernel(widthKernel);
				k.edgeHandling = EdgeHandlingStrategy.MIRROR;
				k.apply(dummyInputInt);
			}

			@Test
			public void testMirrorHeight() {
				Kernel k = new Kernel(heightKernel);
				k.edgeHandling = EdgeHandlingStrategy.MIRROR;
				k.apply(dummyInputInt);
			}
		}

		@Nested
		class Byte {
			@Test
			public void testNoOpSquare() {
				Kernel k = new Kernel(squareKernel);
				k.edgeHandling = EdgeHandlingStrategy.NO_OP;
				k.apply(dummyInputByte);
			}

			@Test
			public void testNoOpWidth() {
				Kernel k = new Kernel(widthKernel);
				k.edgeHandling = EdgeHandlingStrategy.NO_OP;
				k.apply(dummyInputByte);
			}

			@Test
			public void testNoOpHeight() {
				Kernel k = new Kernel(heightKernel);
				k.edgeHandling = EdgeHandlingStrategy.NO_OP;
				k.apply(dummyInputByte);
			}

			@Test
			public void testExpandSquare() {
				Kernel k = new Kernel(squareKernel);
				k.edgeHandling = EdgeHandlingStrategy.EXPAND;
				k.apply(dummyInputByte);
			}

			@Test
			public void testExpandWidth() {
				Kernel k = new Kernel(widthKernel);
				k.edgeHandling = EdgeHandlingStrategy.EXPAND;
				k.apply(dummyInputByte);
			}

			@Test
			public void testExpandHeight() {
				Kernel k = new Kernel(heightKernel);
				k.edgeHandling = EdgeHandlingStrategy.EXPAND;
				k.apply(dummyInputByte);
			}

			@Test
			public void testWrapSquare() {
				Kernel k = new Kernel(squareKernel);
				k.edgeHandling = EdgeHandlingStrategy.WRAP;
				k.apply(dummyInputByte);
			}

			@Test
			public void testWrapWidth() {
				Kernel k = new Kernel(widthKernel);
				k.edgeHandling = EdgeHandlingStrategy.WRAP;
				k.apply(dummyInputByte);
			}

			@Test
			public void testWrapHeight() {
				Kernel k = new Kernel(heightKernel);
				k.edgeHandling = EdgeHandlingStrategy.WRAP;
				k.apply(dummyInputByte);
			}

			@Test
			public void testMirrorSquare() {
				Kernel k = new Kernel(squareKernel);
				k.edgeHandling = EdgeHandlingStrategy.MIRROR;
				k.apply(dummyInputByte);
			}

			@Test
			public void testMirrorWidth() {
				Kernel k = new Kernel(widthKernel);
				k.edgeHandling = EdgeHandlingStrategy.MIRROR;
				k.apply(dummyInputByte);
			}

			@Test
			public void testMirrorHeight() {
				Kernel k = new Kernel(heightKernel);
				k.edgeHandling = EdgeHandlingStrategy.MIRROR;
				k.apply(dummyInputByte);
			}
		}

	}

}
