package com.github.kilianB.hashAlgorithms.filter;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Arrays;

import javax.imageio.ImageIO;

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
	static Kernel squareKernel;
	/** 1x3 Kernel */
	static Kernel widthKernel;
	/** 3x1 kernel */
	static Kernel heightKernel;

	static double[][] dummyInputDouble = new double[10][15];
	static int[][] dummyInputInt = new int[10][15];
	static byte[][] dummyInputByte = new byte[10][15];

	@BeforeAll
	static void prepareDefaultKernels() {

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
	void copyConstructorEdge() {
		Kernel copy = new Kernel(widthKernel);
		assertEquals(widthKernel.edgeHandling, copy.edgeHandling);
	}

	@Test
	void copyConstructorMask() {
		Kernel copy = new Kernel(widthKernel);
		assertAll(() -> assertEquals(widthKernel.mask.length, copy.mask.length),
				() -> assertEquals(widthKernel.mask[0].length, copy.mask[0].length));
	}

	@Test
	void identity() {

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
	void sameDimension1D() {
		double[][] input = { { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 } };
		double[][] mask = { { 1d, 1d, 1d } };
		Kernel k = new Kernel(mask);
		double[][] result = k.apply(input);

		assertAll(() -> assertEquals(input.length, result.length),
				() -> assertEquals(input[0].length, result[0].length));

	}

	@Test
	void sameDimension2DSquared() {
		double[][] input = { { 0, 1, 2 }, { 0, 1, 2 }, { 0, 1, 2 } };
		double[][] mask = { { 1d, 1d, 1d }, { 1d, 1d, 1d }, { 1d, 1d, 1d } };
		Kernel k = new Kernel(mask);
		double[][] result = k.apply(input);
		assertAll(() -> assertEquals(input.length, result.length),
				() -> assertEquals(input[0].length, result[0].length));
	}

	@Test
	void sameDimension2D() {
		double[][] input = { { 0, 1, 2 }, { 0, 1, 2 } };
		double[][] mask = { { 1d, 1d, 1d } };

		Kernel k = new Kernel(mask);
		double[][] result = k.apply(input);
		assertAll(() -> assertEquals(input.length, result.length),
				() -> assertEquals(input[0].length, result[0].length));
	}

	@Test
	void sameDimension2D1() {
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
			void widthHeightConstructor() {
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
			void widthHeightConstructor() {
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
			void testNoOpSquare() {
				Kernel k = new Kernel(squareKernel);
				k.edgeHandling = EdgeHandlingStrategy.NO_OP;
				k.apply(dummyInputDouble);
			}

			@Test
			void testNoOpWidth() {
				Kernel k = new Kernel(widthKernel);
				k.edgeHandling = EdgeHandlingStrategy.NO_OP;
				k.apply(dummyInputDouble);
			}

			@Test
			void testNoOpHeight() {
				Kernel k = new Kernel(heightKernel);
				k.edgeHandling = EdgeHandlingStrategy.NO_OP;
				k.apply(dummyInputDouble);
			}

			@Test
			void testExpandSquare() {
				Kernel k = new Kernel(squareKernel);
				k.edgeHandling = EdgeHandlingStrategy.EXPAND;
				k.apply(dummyInputDouble);
			}

			@Test
			void testExpandWidth() {
				Kernel k = new Kernel(widthKernel);
				k.edgeHandling = EdgeHandlingStrategy.EXPAND;
				k.apply(dummyInputDouble);
			}

			@Test
			void testExpandHeight() {
				Kernel k = new Kernel(heightKernel);
				k.edgeHandling = EdgeHandlingStrategy.EXPAND;
				k.apply(dummyInputDouble);
			}

			@Test
			void testWrapSquare() {
				Kernel k = new Kernel(squareKernel);
				k.edgeHandling = EdgeHandlingStrategy.WRAP;
				k.apply(dummyInputDouble);
			}

			@Test
			void testWrapWidth() {
				Kernel k = new Kernel(widthKernel);
				k.edgeHandling = EdgeHandlingStrategy.WRAP;
				k.apply(dummyInputDouble);
			}

			@Test
			void testWrapHeight() {
				Kernel k = new Kernel(heightKernel);
				k.edgeHandling = EdgeHandlingStrategy.WRAP;
				k.apply(dummyInputDouble);
			}

			@Test
			void testMirrorSquare() {
				Kernel k = new Kernel(squareKernel);
				k.edgeHandling = EdgeHandlingStrategy.MIRROR;
				k.apply(dummyInputDouble);
			}

			@Test
			void testMirrorWidth() {
				Kernel k = new Kernel(widthKernel);
				k.edgeHandling = EdgeHandlingStrategy.MIRROR;
				k.apply(dummyInputDouble);
			}

			@Test
			void testMirrorHeight() {
				Kernel k = new Kernel(heightKernel);
				k.edgeHandling = EdgeHandlingStrategy.MIRROR;
				k.apply(dummyInputDouble);
			}
		}

		@Nested
		class Integer {
			@Test
			void testNoOpSquare() {
				Kernel k = new Kernel(squareKernel);
				k.edgeHandling = EdgeHandlingStrategy.NO_OP;
				k.apply(dummyInputInt);
			}

			@Test
			void testNoOpWidth() {
				Kernel k = new Kernel(widthKernel);
				k.edgeHandling = EdgeHandlingStrategy.NO_OP;
				k.apply(dummyInputInt);
			}

			@Test
			void testNoOpHeight() {
				Kernel k = new Kernel(heightKernel);
				k.edgeHandling = EdgeHandlingStrategy.NO_OP;
				k.apply(dummyInputInt);
			}

			@Test
			void testExpandSquare() {
				Kernel k = new Kernel(squareKernel);
				k.edgeHandling = EdgeHandlingStrategy.EXPAND;
				k.apply(dummyInputInt);
			}

			@Test
			void testExpandWidth() {
				Kernel k = new Kernel(widthKernel);
				k.edgeHandling = EdgeHandlingStrategy.EXPAND;
				k.apply(dummyInputInt);
			}

			@Test
			void testExpandHeight() {
				Kernel k = new Kernel(heightKernel);
				k.edgeHandling = EdgeHandlingStrategy.EXPAND;
				k.apply(dummyInputInt);
			}

			@Test
			void testWrapSquare() {
				Kernel k = new Kernel(squareKernel);
				k.edgeHandling = EdgeHandlingStrategy.WRAP;
				k.apply(dummyInputInt);
			}

			@Test
			void testWrapWidth() {
				Kernel k = new Kernel(widthKernel);
				k.edgeHandling = EdgeHandlingStrategy.WRAP;
				k.apply(dummyInputInt);
			}

			@Test
			void testWrapHeight() {
				Kernel k = new Kernel(heightKernel);
				k.edgeHandling = EdgeHandlingStrategy.WRAP;
				k.apply(dummyInputInt);
			}

			@Test
			void testMirrorSquare() {
				Kernel k = new Kernel(squareKernel);
				k.edgeHandling = EdgeHandlingStrategy.MIRROR;
				k.apply(dummyInputInt);
			}

			@Test
			void testMirrorWidth() {
				Kernel k = new Kernel(widthKernel);
				k.edgeHandling = EdgeHandlingStrategy.MIRROR;
				k.apply(dummyInputInt);
			}

			@Test
			void testMirrorHeight() {
				Kernel k = new Kernel(heightKernel);
				k.edgeHandling = EdgeHandlingStrategy.MIRROR;
				k.apply(dummyInputInt);
			}
		}

		@Nested
		class Byte {
			@Test
			void testNoOpSquare() {
				Kernel k = new Kernel(squareKernel);
				k.edgeHandling = EdgeHandlingStrategy.NO_OP;
				k.apply(dummyInputByte);
			}

			@Test
			void testNoOpWidth() {
				Kernel k = new Kernel(widthKernel);
				k.edgeHandling = EdgeHandlingStrategy.NO_OP;
				k.apply(dummyInputByte);
			}

			@Test
			void testNoOpHeight() {
				Kernel k = new Kernel(heightKernel);
				k.edgeHandling = EdgeHandlingStrategy.NO_OP;
				k.apply(dummyInputByte);
			}

			@Test
			void testExpandSquare() {
				Kernel k = new Kernel(squareKernel);
				k.edgeHandling = EdgeHandlingStrategy.EXPAND;
				k.apply(dummyInputByte);
			}

			@Test
			void testExpandWidth() {
				Kernel k = new Kernel(widthKernel);
				k.edgeHandling = EdgeHandlingStrategy.EXPAND;
				k.apply(dummyInputByte);
			}

			@Test
			void testExpandHeight() {
				Kernel k = new Kernel(heightKernel);
				k.edgeHandling = EdgeHandlingStrategy.EXPAND;
				k.apply(dummyInputByte);
			}

			@Test
			void testWrapSquare() {
				Kernel k = new Kernel(squareKernel);
				k.edgeHandling = EdgeHandlingStrategy.WRAP;
				k.apply(dummyInputByte);
			}

			@Test
			void testWrapWidth() {
				Kernel k = new Kernel(widthKernel);
				k.edgeHandling = EdgeHandlingStrategy.WRAP;
				k.apply(dummyInputByte);
			}

			@Test
			void testWrapHeight() {
				Kernel k = new Kernel(heightKernel);
				k.edgeHandling = EdgeHandlingStrategy.WRAP;
				k.apply(dummyInputByte);
			}

			@Test
			void testMirrorSquare() {
				Kernel k = new Kernel(squareKernel);
				k.edgeHandling = EdgeHandlingStrategy.MIRROR;
				k.apply(dummyInputByte);
			}

			@Test
			void testMirrorWidth() {
				Kernel k = new Kernel(widthKernel);
				k.edgeHandling = EdgeHandlingStrategy.MIRROR;
				k.apply(dummyInputByte);
			}

			@Test
			void testMirrorHeight() {
				Kernel k = new Kernel(heightKernel);
				k.edgeHandling = EdgeHandlingStrategy.MIRROR;
				k.apply(dummyInputByte);
			}
		}

	}

}
