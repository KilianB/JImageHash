package dev.brachtendorf.jimagehash.hashAlgorithms.filter;

import dev.brachtendorf.ArrayUtil;

/**
 * A maximum kernel is a non linear filter scanning the image and replacing
 * every value with the minimum value found in the neighborhood.
 * 
 * This minimum kernel allows a weight matrix to be supplied.
 * 
 * <p>
 * Example 1D kernel width 5 Kernel and no/uniform mask
 * 
 * <pre>
 * 	Values: 2 3 2 1 6
 * </pre>
 * 
 * During convolution, the kernel looks at the value 2 and replaces it with the
 * value 1 due to it being the minimum.
 * 
 * 
 * -------------------
 * 
 * <p>
 * A weight mask {@code [1 2 3 2 1]} can give more emphasis on closer pixel
 * values. An intermediary value matrix is calculated:
 * 
 * <pre>
 * Values * Mask = [2 6 6 2 6]
 * </pre>
 * 
 * and it is found that the first value is the minimum. (If two values are equal
 * the first value will be chosen). Now the unaltered value at position 1 is
 * taken. Therefore the 2 would be replaced with the value 2.
 * 
 * 
 * @author Kilian
 * @since 2.0.0
 * @see MedianKernel
 * @see MaximumKernel
 */
public class MinimumKernel extends NonAveragingKernel {

	private static final long serialVersionUID = -2271995765850760974L;

	/**
	 * Create a minimum kernel with a uniform weight mask (no weighting takes place)
	 * @param width of the kernel. has to be odd
	 * @param height of the kernel. has to be odd
	 * 
	 */
	@SuppressWarnings("deprecation")
	public MinimumKernel(int width, int height) {
		super(EdgeHandlingStrategy.EXPAND);

		if (width <= 0 || width % 2 == 0 || height <= 0 || height % 2 == 0) {
			throw new IllegalArgumentException(
					"Currently only odd dimensional kernels are supported. Width & height have to be positive");
		}
		// Create mask
		double[][] mask = new double[width][height];
		ArrayUtil.fillArrayMulti(mask, () -> {
			return 1d;
		});
		this.mask = mask;
	}

	/**
	 * Create a kernel with the given masks dimension. The masks acts as weight
	 * filter increasing or decreasing the weight of the value during convolution.
	 * For an example see the javadoc of the class.
	 * 
	 * @param mask weight matrix used to judge which value is the maximum
	 */
	public MinimumKernel(double[][] mask) {
		super(mask);
	}

	@Override
	protected double calcValue(byte[][] input, int x, int y) {
		return resolveMax(computePotentialValues(input, x, y));
	}

	@Override
	protected double calcValue(int[][] input, int x, int y) {
		return resolveMax(computePotentialValues(input, x, y));
	}

	@Override
	protected double calcValue(double[][] input, int x, int y) {
		return resolveMax(computePotentialValues(input, x, y));
	}
	
	protected double resolveMax(double[][] values) {
		if (values[1].length == 1 && values[1][0] == Double.MIN_VALUE) {
			return values[0][0];
		}
		return values[0][ArrayUtil.minimumIndex(values[1])];
	}

}
