package dev.brachtendorf.jimagehash.hashAlgorithms.filter;

import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.function.BiFunction;

import dev.brachtendorf.ArrayUtil;
import dev.brachtendorf.MiscUtil;
import dev.brachtendorf.Require;
import dev.brachtendorf.graphics.FastPixel;

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
 * "http://machinelearninguru.com/_images/topics/computer_vision/basics/convolution/3.JPG"
 * alt="Convolution example">
 * 
 * TODO support separability for custom kernels TODO kernel indices are swapped
 * and twisted.
 * 
 * @author Kilian
 * @since 2.0.0
 */
public class Kernel implements Filter {

	private static final long serialVersionUID = -3490082941059458531L;

	/** Kernel mask applied to the pixels */
	protected double[][] mask;

	/**
	 * Seperable convolution to speed up masking if applicable for custom kernels
	 */
	// https://en.wikipedia.org/wiki/Singular_value_decomposition for custom kernels
	// https://blogs.mathworks.com/steve/2006/11/28/separable-convolution-part-2/
	// private double[] seperableMaskX;
	// private double[] seperableMaskY;

	/** How are edged of the images handled */
	protected EdgeHandlingStrategy edgeHandling;

	// TODO we could compute a pixel mapping map before convolution reducing method
	// calls and maybe increase performance instead of on the fly calculation of
	// those values?
	// int[][] pixelAccessMap
	
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
	 * <pre>
	 * 	Example factor: 0.3 width: 3 height: 3
	 * 
	 * 	  0.3  0.3  0.3
	 * 	  0.3  0.3  0.3
	 * 	  0.3  0.3  0.3
	 * </pre>
	 * 
	 * 
	 * A normalized box filter is available via
	 * {@link #boxFilterNormalized(int, int)}.
	 * 
	 * @param width  of the kernel has to be odd and positive
	 * @param height if the kernel has to be odd and positive
	 * @param factor the factor applied to each pixel
	 * @return the box filter kernel
	 */
	public static Kernel boxFilter(int width, int height, double factor) {

		// Seperable implementation. Split into multi kernel for more performance
		double xMask[][] = new double[1][width];
		double yMask[][] = new double[height][1];

		double[] xInternal = new double[width];
		for (int i = 0; i < width; i++) {
			xInternal[i] = factor;
		}

		xMask[0] = xInternal;

		ArrayUtil.fillArray(yMask, () -> {
			return new double[] { 1d };
		});

		return new MultiKernel(yMask, xMask);

		/*
		 * 2d variant // Construct mask double[][] mask = new double[width][height];
		 * ArrayUtil.fillArrayMulti(mask, () -> { return factor; }); return new
		 * Kernel(mask);
		 */
	}

	public static Kernel boxFilterNormalizedSep(int width, int height) {
		// Construct mask
		double factor = 1d / (width * height);
		// Seperable implementation. Split into multi kernel for more performance
		double xMask[][] = new double[1][width];
		double yMask[][] = new double[height][1];

		double[] xInternal = new double[width];
		for (int i = 0; i < width; i++) {
			xInternal[i] = factor;
		}

		xMask[0] = xInternal;

		ArrayUtil.fillArray(yMask, () -> {
			return new double[] { 1d };
		});

		return new MultiKernel(xMask, yMask);
	}

	/**
	 * A box filter is a filter which applies the same factor to squared region of
	 * pixels and can be counted to the blurring filters.
	 * 
	 * <p>
	 * This filter is normalized ensuring the same magnitude of the values.
	 * 
	 * @param width  of the kernel has to be odd and positive
	 * @param height of the kernel has to be odd and positive
	 * @return the box filter kernel
	 */
	public static Kernel boxFilterNormalized(int width, int height) {
		// Construct mask
		double factor = 1d / (width * height);
		double[][] mask = new double[height][width];
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
	 * @param width  of the kernel has to be odd and positive
	 * @param height of the kernel has to be odd and positive
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

		// TODO Seperability

		// http://www-edlab.cs.umass.edu/~smaji/cmpsci370/slides/hh/lec02_hh_advanced_edges.pdf

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

	/**
	 * @param depth width and height. higher values will create a stronger effect
	 * @return a kernel creating a horizontal emboss effect
	 */
	public static Kernel embossHorizontontalFilter(int depth) {

		depth = Require.positiveValue(depth, "Depth has to be positive");

		double[][] mask = new double[1 + depth * 2][1 + depth * 2];

		int xMatch = mask.length / 2;

		for (int y = 0; y < mask.length; y++) {
			if (y < xMatch) {
				mask[y][xMatch] = -1;
			} else if (y > xMatch) {
				mask[y][xMatch] = 1;
			}
		}
		return new GrayScaleFilter(mask);
	}

	/**
	 * 
	 * <pre>
	 * 		 1  0  0 
	 * 		 0  0  0 
	 *		 0  0 -1
	 * </pre>
	 * 
	 * @param depth width and height. higher values will create a stronger effect
	 * @return a kernel creating a left diagonal emboss effect
	 */
	public static Kernel embossLeftDiagonalFilter(int depth) {

		depth = Require.positiveValue(depth, "Depth has to be positive");

		double[][] mask = new double[1 + depth * 2][1 + depth * 2];

		int xMatch = mask.length / 2;

		for (int y = 0; y < mask.length; y++) {
			for (int x = 0; x < mask.length; x++) {
				if (x == y) {
					if (y < xMatch) {
						mask[y][x] = -1;
					} else if (y > xMatch) {
						mask[y][x] = 1;
					}
				}
			}
		}
		return new GrayScaleFilter(mask);
	}

	/**
	 * <pre>
	 * 		 0 0 1 
	 * 		 0 0 0 
	 *		-1 0 0
	 * </pre>
	 * 
	 * @param depth width and height. higher values will create a stronger effect
	 * @return a kernel creating a right diagonal emboss effect
	 */
	public static Kernel embossRightDiagonalFilter(int depth) {

		depth = Require.positiveValue(depth, "Depth has to be positive");

		double[][] mask = new double[1 + depth * 2][1 + depth * 2];

		int xMatch = mask.length / 2;

		for (int y = 0; y < mask.length; y++) {
			for (int x = 0; x < mask.length; x++) {
				if (x + y == mask.length - 1) {
					if (y < xMatch) {
						mask[y][x] = -1;
					} else if (y > xMatch) {
						mask[y][x] = 1;
					}
				}
			}
		}
		return new GrayScaleFilter(mask);
	}

	/**
	 * <pre>
	 * 		0  0  0 
	 * 		1  0 -1 
	 *		0  0  0
	 * </pre>
	 * 
	 * @param depth width and height. higher values will create a stronger effect
	 * @return a kernel creating a right left emboss effect
	 */
	public static Kernel embossleftRightFilter(int depth) {
		depth = Require.positiveValue(depth, "Depth has to be positive");

		double[][] mask = new double[1 + depth * 2][1 + depth * 2];

		int xMatch = mask.length / 2;

		for (int y = 0; y < mask.length; y++) {
			for (int x = 0; x < mask.length; x++) {
				if (y == xMatch) {
					if (x < xMatch) {
						mask[y][x] = -1;
					} else if (x > xMatch) {
						mask[y][x] = 1;
					}
				}
			}
		}
		return new GrayScaleFilter(mask);
	}

//	

	// Many many more sobel laplacian etc ...
	// https://web.eecs.umich.edu/~jjcorso/t/598F14/files/lecture_0924_filtering.pdf
	
	/**
	 * Create a clone of the supplied kernel
	 * 
	 * @param template the kernel to clone
	 */
	@SuppressWarnings("deprecation")
	public Kernel(Kernel template) {
		this.edgeHandling = template.edgeHandling;
		try {
			this.mask = ArrayUtil.deepArrayCopyClone(template.mask);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Empty constructor used by inheriting classes which are not able to provide a
	 * mask during first constructor call. The inheriting class promises to provide
	 * a mask and all checks by itself
	 * 
	 * @param strat EdgeHandlingStrategy to use
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
	 * @param mask      used to filter pixel. So far only oddly shaped masks are
	 *                  allowed
	 * @param normalize If true the mask is normalized resulting in the sum of the
	 *                  mask being 1. This will preserve the magnitude of the
	 *                  original range. If the mask will be blindly copied without
	 *                  adjustment
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
	 * @param normalize    If true the mask is normalized resulting in the sum of
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
		double[][] result = new double[input.length][input[0].length];

		for (int y = 0; y < input.length; y++) {
			for (int x = 0; x < input[0].length; x++) {
				result[y][x] = calcValue(input, x, y);
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

		int[][] result = new int[input.length][input[0].length];

		for (int y = 0; y < input.length; y++) {
			for (int x = 0; x < input[0].length; x++) {
				result[y][x] = (int) Math.round(calcValue(input, x, y));
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
	public int[][] applyInt(double[][] input) {

		int[][] result = new int[input.length][input[0].length];

		for (int y = 0; y < input.length; y++) {
			for (int x = 0; x < input[0].length; x++) {
				result[y][x] = (int) Math.round(calcValue(input, x, y));
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

		double[][] result = new double[input.length][input[0].length];

		for (int y = 0; y < input.length; y++) {
			for (int x = 0; x < input[0].length; x++) {
				result[y][x] = calcValue(input, x, y);
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
		double[][] result = new double[input.length][input[0].length];

		for (int y = 0; y < input.length; y++) {
			for (int x = 0; x < input[0].length; x++) {
				result[y][x] = calcValue(input, x, y);
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
		byte[][] result = new byte[input.length][input[0].length];

		for (int y = 0; y < input.length; y++) {
			for (int x = 0; x < input[0].length; x++) {
				result[y][x] = (byte) Math.round(calcValue(input, x, y));
			}
		}
		return result;
	}

	public byte[][] applyByte(double[][] input) {
		byte[][] result = new byte[input.length][input[0].length];

		for (int y = 0; y < input.length; y++) {
			for (int x = 0; x < input[0].length; x++) {
				result[y][x] = (byte) Math.round(calcValue(input, x, y));
			}
		}
		return result;
	}

	protected double calcValue(byte[][] input, int x, int y) {
		double value = 0;
		int maskW = mask[0].length / 2;
		int maskH = mask.length / 2;

		int width = input[0].length;
		int height = input.length;

		for (int yMask = -maskH; yMask <= maskH; yMask++) {
			for (int xMask = -maskW; xMask <= maskW; xMask++) {

				int xPixelIndex;
				int yPixelIndex;

				if (edgeHandling.equals(EdgeHandlingStrategy.NO_OP)) {
					xPixelIndex = x + xMask;
					yPixelIndex = y + yMask;

					if (xPixelIndex < 0 || xPixelIndex >= width || yPixelIndex < 0 || yPixelIndex >= height) {
						return input[y][x];
					}
				} else {
					xPixelIndex = edgeHandling.correctPixel(x + xMask, width);
					yPixelIndex = edgeHandling.correctPixel(y + yMask, height);
				}
				value += mask[yMask + maskH][xMask + maskW] * input[yPixelIndex][xPixelIndex];
			}
		}
		return value;
	}

	protected double calcValue(int[][] input, int x, int y) {
		double value = 0;
		int maskW = mask[0].length / 2;
		int maskH = mask.length / 2;

		int width = input[0].length;
		int height = input.length;

		for (int yMask = -maskH; yMask <= maskH; yMask++) {
			for (int xMask = -maskW; xMask <= maskW; xMask++) {

				int xPixelIndex;
				int yPixelIndex;

				if (edgeHandling.equals(EdgeHandlingStrategy.NO_OP)) {
					xPixelIndex = x + xMask;
					yPixelIndex = y + yMask;

					if (xPixelIndex < 0 || xPixelIndex >= width || yPixelIndex < 0 || yPixelIndex >= height) {
						return input[y][x];
					}
				} else {
					xPixelIndex = edgeHandling.correctPixel(x + xMask, width);
					yPixelIndex = edgeHandling.correctPixel(y + yMask, height);
				}
				value += mask[yMask + maskH][xMask + maskW] * input[yPixelIndex][xPixelIndex];
			}
		}
		return value;
	}

	/**
	 * 
	 * @param input array
	 * @param x     pixelToLookAt
	 * @param y     pixelToLookAt
	 * @return convolutedPixel fo this x and y
	 */
	protected double calcValue(double[][] input, int x, int y) {
		double value = 0;
		int maskW = mask[0].length / 2;
		int maskH = mask.length / 2;

		int width = input[0].length;
		int height = input.length;

		for (int yMask = -maskH; yMask <= maskH; yMask++) {
			for (int xMask = -maskW; xMask <= maskW; xMask++) {

				int xPixelIndex;
				int yPixelIndex;

				if (edgeHandling.equals(EdgeHandlingStrategy.NO_OP)) {
					xPixelIndex = x + xMask;
					yPixelIndex = y + yMask;

					if (xPixelIndex < 0 || xPixelIndex >= width || yPixelIndex < 0 || yPixelIndex >= height) {
						return input[y][x];
					}
				} else {
					xPixelIndex = edgeHandling.correctPixel(x + xMask, width);
					yPixelIndex = edgeHandling.correctPixel(y + yMask, height);
				}
				value += mask[yMask + maskH][xMask + maskW] * input[yPixelIndex][xPixelIndex];
			}
		}
		return value;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((edgeHandling == null) ? 0 : MiscUtil.consistentHashCode(edgeHandling));
		result = prime * result + Arrays.deepHashCode(mask);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Kernel other = (Kernel) obj;
		if (edgeHandling != other.edgeHandling)
			return false;
		if (!Arrays.deepEquals(mask, other.mask))
			return false;
		return true;
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
		 * Function accepting the pixelIndex and the width or height of the pixel and
		 * returning the index of the pixel used to compute the value
		 */
		private BiFunction<Integer, Integer, Integer> compute;

		/**
		 * @param func
		 */
		private EdgeHandlingStrategy(BiFunction<Integer, Integer, Integer> func) {
			this.compute = func;
		}

	
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
		return "Kernel [edgeHandling=" + edgeHandling + ", " + "mask=\n" + ArrayUtil.deepToStringFormatted(mask) + "]";
	}

	@Override
	public BufferedImage filter(BufferedImage input) {
		BufferedImage bi = new BufferedImage(input.getWidth(), input.getHeight(), input.getType());
		FastPixel fp = FastPixel.create(input);
		FastPixel fpSet = FastPixel.create(bi);
		int[][] red = fp.getRed();
		int[][] green = fp.getGreen();
		int[][] blue = fp.getBlue();

		red = applyInt(red);
		green = applyInt(green);
		blue = applyInt(blue);

		fpSet.setRed(red);
		fpSet.setGreen(green);
		fpSet.setBlue(blue);

		if (fpSet.hasAlpha()) {
			fpSet.setAlpha(fp.getAlpha());
		}

		return bi;
	}

	/**
	 * Kernels whose filter class work with grayscale instead of seperate color
	 * channels
	 * 
	 * @author Kilian
	 *
	 */
	public static class GrayScaleFilter extends Kernel {

		private static final long serialVersionUID = -1079407275717629013L;

		/**
		 * @param mask kernel mask
		 */
		public GrayScaleFilter(double[][] mask) {
			super(mask);
		}

		@Override
		public BufferedImage filter(BufferedImage input) {
			BufferedImage bi = new BufferedImage(input.getWidth(), input.getHeight(), input.getType());
			FastPixel fp = FastPixel.create(input);
			FastPixel fpSet = FastPixel.create(bi);
			int[][] gray = fp.getAverageGrayscale();
			gray = applyInt(gray);
			fpSet.setAverageGrayscale(gray);
			return bi;
		}

	}

}
