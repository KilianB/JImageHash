package com.github.kilianB.hashAlgorithms.filter;

import java.io.Serializable;
import java.util.Arrays;
import java.util.function.BiFunction;

import com.github.kilianB.ArrayUtil;
import com.github.kilianB.Require;

/**
 * Kernel operations are shifting window masks applied to data point of an array
 * to recompute it's value. This class supports 2D kernels which are mainly used
 * on images pixel data.
 * 
 * e.g. the bellow mask will calculate the value of the pixel by multiplying
 * it's original value by 5 and subtracting surrounding pixels values.
 * 
 * <pre>
 * [  0 -1  0 
 *   -1  5 -1 
 *    0 -1  0 ]
 * </pre>
 * 
 * In image processing sharpening or blurring is realized by applying kernels to
 * pixel data.
 * 
 * <img src=
 * "http://machinelearninguru.com/_images/topics/computer_vision/basics/convolution/3.JPG"/>
 * 
 * @author Kilian
 * @since 2.0.0
 */
public class Kernel implements Serializable {

	private static final long serialVersionUID = -3490082941059458531L;

	/**
	 * Return an identity kernel. This kernel is a 1x1 kernel and copies the
	 * original value to the new array
	 * 
	 * @return a identity kernel
	 */
	public static Kernel identityFilter() {
		double[][] mask = { { 1d } };
		return new Kernel(mask);
	}

	/**
	 * Return an zero kernel. This kernel is a 1x1 kernel and zeroes out all values
	 * 
	 * @return a zero kernel
	 */
	public static Kernel zeroFilter() {
		double[][] mask = { { 0d } };
		return new Kernel(mask);
	}

	/**
	 * A box filter is a filter which applies the same factor to squared region of
	 * pixels and can be counted to the blurring filters.
	 * 
	 * A normalized box filter is available via
	 * {@link #boxFilterNormalized(int, int)}.
	 * 
	 * @param width  of the kernel has to be odd and > 1
	 * @param height if the kernel has to be odd and > 1
	 * @param factor the factor applied to each pixel
	 * @return the box filter kernel
	 */
	public static Kernel boxFilter(int width, int height, double factor) {
		// Construct mask
		double[][] mask = new double[width][height];
		ArrayUtil.fillArrayMulti(mask, () -> {
			return factor;
		});
		return new Kernel(mask);
	}

	/**
	 * A box filter is a filter which applies the same factor to squared region of
	 * pixels and can be counted to the blurring filters.
	 * 
	 * <p>
	 * This filter is normalized ensuring the same magnitude of the values.
	 * 
	 * @param width  of the kernel has to be odd and > 1
	 * @param height of the kernel has to be odd and > 1
	 * @return the box filter kernel
	 */
	public static Kernel boxFilterNormalized(int width, int height) {
		// Construct mask
		double factor = 1d / (width * height);
		double[][] mask = new double[width][height];
		ArrayUtil.fillArrayMulti(mask, () -> {
			return factor;
		});
		return new Kernel(mask);
	}

	/**
	 * Creates a gaussian blur.
	 * 
	 * <p>
	 * A gaussian filter blurs the kernel with decreasing amount depending on the
	 * distance to the pixel and produces a more natural blurring effect than a box
	 * filter.
	 * 
	 * <p>
	 * The gaussian filter is normalized.
	 * 
	 * @param width  of the kernel has to be odd and > 1
	 * @param height of the kernel has to be odd and > 1
	 * @param std    the standard deviation of the kernel. The higher the stronger
	 *               the blur effect
	 * @return the gaussian kernel
	 */
	public static Kernel gaussianFilter(int width, int height, double std) {

		if (width % 2 == 0 || height % 2 == 0 || width < 1 || height < 1) {
			throw new IllegalArgumentException(
					"Currently only odd sized kernels are suppoted. Width and height have to be positive");
		}
		Require.positiveValue(std, "Std has to be positive");

		// Construct mask
		double[][] mask = new double[width][height];

		double stdSquared = Math.pow(std, 2);

		int wHalf = width / 2;
		int hHalf = height / 2;

		for (int x = -wHalf; x <= wHalf; x++) {
			for (int y = -hHalf; y <= hHalf; y++) {
				double exponent = -(x * x + y * y) / (2 * stdSquared);
				mask[x + wHalf][y + hHalf] = (1 / (2 * Math.PI * stdSquared)) * Math.pow(Math.E, exponent);
			}
		}
		return new Kernel(mask, true);
	}

	/**
	 * A 3 x 3 edge detection kernel putting emphasis on edges in the image.
	 * 
	 * <p>
	 * This kernel is normalized.
	 * 
	 * @param strength feature strength of the detector
	 * @return the edge detection kernel
	 */
	@Deprecated
	public static Kernel edgeDetectionFilter() {
		// Sobel?
		double[][] mask = { { 2, 2, 0 }, { 2, 0, -2 }, { 0, -2, -2 } };
		return new Kernel(mask, true);
	}

	@Deprecated
	public static Kernel edgeDetectionFilter(int strength) {
		// Sobel?
		double[][] mask = { { 6, 10, 0 }, { 10, 0, -10 }, { 0, -10, -6 } };
		return new Kernel(mask, true);
	}

	/*
	 * Emboss
	 */
	public static Kernel embossHorizontontalFilter(int depth) {

		depth = Require.positiveValue(depth, "Depth has to be positive");

		double[][] mask = new double[1 + depth * 2][1 + depth * 2];

		int xMatch = mask.length / 2;

		for (int y = 0; y < mask.length; y++) {
			if (y < xMatch) {
				mask[y][xMatch] = 1;
			} else if (y > xMatch) {
				mask[y][xMatch] = -1;
			}
		}
		return new Kernel(mask, false);
	}

	/**
	 * 
	 * <pre>
	 * 		 1  0  0 
	 * 		 0  0  0 
	 *		 0  0 -1
	 * </pre>
	 * 
	 * @param depth
	 * @return
	 */
	public static Kernel embossLeftDiagonalFilter(int depth) {

		depth = Require.positiveValue(depth, "Depth has to be positive");

		double[][] mask = new double[1 + depth * 2][1 + depth * 2];

		int xMatch = mask.length / 2;

		for (int y = 0; y < mask.length; y++) {
			for (int x = 0; x < mask.length; x++) {
				if (x == y) {
					if (y < xMatch) {
						mask[y][x] = 1;
					} else if (y > xMatch) {
						mask[y][x] = -1;
					}
				}
			}
		}
		return new Kernel(mask, false);
	}

	/**
	 * <pre>
	 * 		 0 0 1 
	 * 		 0 0 0 
	 *		-1 0 0
	 * </pre>
	 * 
	 * @param depth
	 * @return
	 */
	public static Kernel embossRightDiagonalFilter(int depth) {

		depth = Require.positiveValue(depth, "Depth has to be positive");

		double[][] mask = new double[1 + depth * 2][1 + depth * 2];

		int xMatch = mask.length / 2;

		for (int y = 0; y < mask.length; y++) {
			for (int x = 0; x < mask.length; x++) {
				if (x + y == mask.length - 1) {
					if (y < xMatch) {
						mask[y][x] = 1;
					} else if (y > xMatch) {
						mask[y][x] = -1;
					}
				}
			}
		}
		return new Kernel(mask, false);
	}

	/**
	 * <pre>
	 * 		0  0  0 
	 * 		1  0 -1 
	 *		0  0  0
	 * </pre>
	 * 
	 * @param depth
	 * @return
	 */
	public static Kernel embossleftRightFilter(int depth) {
		depth = Require.positiveValue(depth, "Depth has to be positive");

		double[][] mask = new double[1 + depth * 2][1 + depth * 2];

		int xMatch = mask.length / 2;

		for (int y = 0; y < mask.length; y++) {
			for (int x = 0; x < mask.length; x++) {
				if (y == xMatch) {
					if (x < xMatch) {
						mask[y][x] = 1;
					} else if (x > xMatch) {
						mask[y][x] = -1;
					}
				}
			}
		}
		return new Kernel(mask, false);
	}

	/**
	 * A 3 x 3 edge detection kernel putting emphasis on edges in the image.
	 * 
	 * <p>
	 * This kernel is normalized.
	 * 
	 * @param strength feature strength of the detector
	 * @return the edge detection kernel
	 */
	public static Kernel artFilter(int strength) {
		// TODO
		double[][] mask = new double[3][3];
		double factor = 1 - strength / 8d;
		ArrayUtil.fillArrayMulti(mask, () -> {
			return factor;
		});
		mask[1][1] = strength;

		double sum = 0;
		for (int i = 0; i < 3; i++) {
			for (int j = 0; j < 3; j++) {
				sum += mask[i][j];
			}
		}
		return new Kernel(mask);
	}

//	
//	/**
//	 * Laplace sharpeen
//	 * @return
//	 */
//	public static Kernel laplaceFilter(int width, int height, double std) {
	// Log kernel as given by
//	https://cecas.clemson.edu/~stb/ece847/internal/cvbook/ch03_filtering.pdf
//	}

	// Many many more sobel laplacian etc ...
	// https://web.eecs.umich.edu/~jjcorso/t/598F14/files/lecture_0924_filtering.pdf

	/** Kernel mask applied to the pixels */
	protected double[][] mask;

	/** Seperable convolution to speed up masking if applicable */
	// https://en.wikipedia.org/wiki/Singular_value_decomposition
	// https://blogs.mathworks.com/steve/2006/11/28/separable-convolution-part-2/
	// private double[] seperableMaskX;
	// private double[] seperableMaskY;

	/** How are edged of the images handled */
	protected EdgeHandlingStrategy edgeHandling;

	/**
	 * Empty constructor used by inheriting classes which are not able to provide a
	 * mask during first constructor call. The inheriting class promises to provide
	 * a mask and all checks by itself
	 */
	@Deprecated
	protected Kernel(EdgeHandlingStrategy strat) {
		this.edgeHandling = strat;
	};

	/**
	 * Construct a non normalized kernel with the given pixel mask and the default
	 * edge handling strategy of {@link EdgeHandlingStrategy#EXPAND}.
	 * 
	 * @param mask used to filter pixel. So far only oddly shaped masks are allowed
	 * @since 2.0.0
	 * @throws IllegalArgumentException if mask's width or height is even
	 */
	public Kernel(double[][] mask) {
		this(mask, EdgeHandlingStrategy.EXPAND, false);
	}

	/**
	 * Construct a kernel with the given pixel mask and the default edge handling
	 * strategy of {@link EdgeHandlingStrategy#EXPAND}.
	 * 
	 * @param mask       used to filter pixel. So far only oddly shaped masks are
	 *                   allowed
	 * @param normalized If true the mask is normalized resulting in the sum of the
	 *                   mask being 1. This will preserve the magnitude of the
	 *                   original range. If the mask will be blindly copied without
	 *                   adjustment
	 * @since 2.0.0
	 * @throws IllegalArgumentException if mask's width or height is even
	 */
	public Kernel(double[][] mask, boolean normalize) {
		this(mask, EdgeHandlingStrategy.EXPAND, normalize);
	}

	/**
	 * Construct a non normalized kernel with the given pixel mask.
	 * 
	 * @param mask         used to filter pixel. So far only oddly shaped masks are
	 *                     allowed
	 * @param edgeHandling the edge handling strategy used at the corners of the
	 *                     image
	 * @since 2.0.0
	 * @throws IllegalArgumentException if mask's width or height is even
	 */
	public Kernel(double[][] mask, EdgeHandlingStrategy edgeHandling) {
		this(mask, edgeHandling, false);
	}

	/**
	 * Construct a kernel with the given pixel mask.
	 * 
	 * @param mask         used to filter pixel. So far only oddly shaped masks are
	 *                     allowed
	 * @param edgeHandling the edge handling strategy used at the corners of the
	 *                     image
	 * @param normalized   If true the mask is normalized resulting in the sum of
	 *                     the mask being 1. This will preserve the magnitude of the
	 *                     original range. If the mask will be blindly copied
	 *                     without adjustment
	 * @since 2.0.0
	 * @throws IllegalArgumentException if mask's width or height is even
	 */

	public Kernel(double[][] mask, EdgeHandlingStrategy edgeHandling, boolean normalize) {

		if (mask.length % 2 == 0 || mask[0].length % 2 == 0) {
			throw new IllegalArgumentException("Currently only odd width and height kernels are supported");
		}

		if (normalize) {
			double maskSum = 0;

			for (double[] m : mask) {
				for (double d : m) {
					maskSum += d;
				}
			}
			if (maskSum != 0) {
				for (double[] m : mask) {
					for (int i = 0; i < m.length; i++) {
						m[i] /= maskSum;
					}
				}
			}
		}

		this.mask = mask;
		this.edgeHandling = edgeHandling;
	}

	/**
	 * Apply the kernel to the 2d array. If the desired output is a int[][] array
	 * refer to {@link #applyInt(int[][])}.
	 * 
	 * @param input the input array to apply the kernel on
	 * @return a new array created by the kernel
	 */
	public double[][] apply(int[][] input) {
		int width = input.length;
		int height = input[0].length;

		double[][] result = new double[width][height];

		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				result[x][y] = calcValue(input, x, y, width, height);
			}
		}
		return result;
	}

	/**
	 * Apply the kernel to the 2d array with each value casted to a int value.
	 * 
	 * @param input the input array to apply the kernel on
	 * @return a new array created by the kernel
	 */
	public int[][] applyInt(int[][] input) {

		int width = input.length;
		int height = input[0].length;

		int[][] result = new int[width][height];

		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				result[x][y] = (int) calcValue(input, x, y, width, height);
			}
		}
		return result;
	}

	/**
	 * Apply the kernel to the 2d array.
	 * 
	 * @param input the input array to apply the kernel on
	 * @return a new array created by the kernel
	 */
	public double[][] apply(double[][] input) {

		int width = input.length;
		int height = input[0].length;

		double[][] result = new double[width][height];

		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				result[x][y] = calcValue(input, x, y, width, height);
			}
		}
		return result;
	}

	/**
	 * Apply the kernel to the 2d array. If the desired output is a byte[][] array
	 * refer to {@link #applyByte(byte[][])}.
	 * 
	 * @param input the input array to apply the kernel on
	 * @return a new array created by the kernel
	 */
	public double[][] apply(byte[][] input) {
		int width = input.length;
		int height = input[0].length;

		double[][] result = new double[width][height];

		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				result[x][y] = calcValue(input, x, y, width, height);
			}
		}
		return result;
	}

	/**
	 * Apply the kernel to the 2d array with each value casted to a byte value.
	 * 
	 * @param input the input array to apply the kernel on
	 * @return a new array created by the kernel
	 */
	public byte[][] applyByte(byte[][] input) {

		int width = input.length;
		int height = input[0].length;

		byte[][] result = new byte[width][height];

		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				result[x][y] = (byte) calcValue(input, x, y, width, height);
			}
		}
		return result;
	}

	protected double calcValue(byte[][] input, int x, int y, int width, int height) {
		double value = 0;
		int maskW = mask[0].length / 2;
		int maskH = mask.length / 2;

		for (int yMask = -maskH; yMask <= maskH; yMask++) {
			for (int xMask = -maskW; xMask <= maskW; xMask++) {

				int xPixelIndex;
				int yPixelIndex;

				if (edgeHandling.equals(EdgeHandlingStrategy.NO_OP)) {
					xPixelIndex = x + xMask;
					yPixelIndex = y + yMask;

					if (xPixelIndex < 0 || xPixelIndex >= width) {
						return input[x][y];
					}
				} else {
					xPixelIndex = edgeHandling.correctPixel(x + xMask, width);
					yPixelIndex = edgeHandling.correctPixel(y + yMask, height);
				}
				value += mask[yMask + maskH][xMask + maskW] * input[xPixelIndex][yPixelIndex];
			}
		}
		return value;
	}

	protected double calcValue(int[][] input, int x, int y, int width, int height) {
		double value = 0;
		int maskW = mask[0].length / 2;
		int maskH = mask.length / 2;

		for (int yMask = -maskH; yMask <= maskH; yMask++) {
			for (int xMask = -maskW; xMask <= maskW; xMask++) {

				int xPixelIndex;
				int yPixelIndex;

				if (edgeHandling.equals(EdgeHandlingStrategy.NO_OP)) {
					xPixelIndex = x + xMask;
					yPixelIndex = y + yMask;

					if (xPixelIndex < 0 || xPixelIndex >= width) {
						return input[x][y];
					}
				} else {
					xPixelIndex = edgeHandling.correctPixel(x + xMask, width);
					yPixelIndex = edgeHandling.correctPixel(y + yMask, height);
				}
				value += mask[yMask + maskH][xMask + maskW] * input[xPixelIndex][yPixelIndex];
			}
		}
		return value;
	}

	protected double calcValue(double[][] input, int x, int y, int width, int height) {
		double value = 0;
		int maskW = mask[0].length / 2;
		int maskH = mask.length / 2;

		for (int yMask = -maskH; yMask <= maskH; yMask++) {
			for (int xMask = -maskW; xMask <= maskW; xMask++) {

				int xPixelIndex;
				int yPixelIndex;

				if (edgeHandling.equals(EdgeHandlingStrategy.NO_OP)) {
					xPixelIndex = x + xMask;
					yPixelIndex = y + yMask;

					if (xPixelIndex < 0 || xPixelIndex >= width) {
						return input[x][y];
					}
				} else {
					xPixelIndex = edgeHandling.correctPixel(x + xMask, width);
					yPixelIndex = edgeHandling.correctPixel(y + yMask, height);
				}
				value += mask[yMask + maskH][xMask + maskW] * input[xPixelIndex][yPixelIndex];
			}
		}
		return value;
	}

	/**
	 * The edge handling strategy defines the behaviour when a kernel reaches the
	 * edge of an image and does not have enough information to compute it's value.
	 * 
	 * @author Kilian
	 * @since 2.0.0
	 */
	public enum EdgeHandlingStrategy {
		/**
		 * If not enough data is available copy the original value.
		 */
		NO_OP(null),
		/**
		 * 
		 * If not enough data is available take the last pixel at the edge and repeat it
		 */
		EXPAND((Integer pIndex, Integer wHeight) -> {
			// Expand
			if (pIndex < 0) {
				return 0;
			} else if (pIndex >= wHeight) {
				return wHeight - 1;
			}
			return pIndex;
		}),
		/**
		 * If not enough data is available mirror the pixels along the edge
		 */
		MIRROR((Integer pIndex, Integer wHeight) -> {
			if (pIndex < 0) {
				return -pIndex;
			} else if (pIndex >= wHeight) {
				// TODO Check
				return wHeight - (pIndex - wHeight) - 1;
			}
			return pIndex;
		}),
		/**
		 * If not enough data is available wrap the image around
		 */
		WRAP((Integer pIndex, Integer wHeight) -> {
			// Expand
			if (pIndex < 0) {
				return wHeight + pIndex;
			} else if (pIndex >= wHeight) {
				return 0 + wHeight - pIndex;
			}
			return pIndex;
		});
		// KERNEL_CROP(null);
		// crop the part of the kernel and normalize afterwards. Not possible with this
		// interface
		// TODO Kernel crop

		/**
		 * 
		 * @param func
		 */
		private EdgeHandlingStrategy(BiFunction<Integer, Integer, Integer> func) {
			this.compute = func;
		}

		/**
		 * Function accepting the pixelIndex and the width or height of the pixel and
		 * returning the index of the pixel used to compute the value
		 */
		private BiFunction<Integer, Integer, Integer> compute;

		/**
		 * Return the array index to compute the kernel value
		 * 
		 * @param pIndex        The pixel index of the array
		 * @param widthOrHeight the width or height of the array
		 * @return the index of the array used to compute the value
		 */
		public int correctPixel(int pIndex, int widthOrHeight) {
			return compute.apply(pIndex, widthOrHeight);
		}

	}

	@Override
	public String toString() {
		return "Kernel [mask=" + Arrays.deepToString(mask) + ", edgeHandling=" + edgeHandling + "]";
	}

//	private int computeRank(double[][] matrix, double tolerance) {
//		
//	}

}
