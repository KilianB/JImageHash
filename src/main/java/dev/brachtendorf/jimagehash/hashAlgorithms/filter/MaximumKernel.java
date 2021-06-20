package dev.brachtendorf.jimagehash.hashAlgorithms.filter;

import dev.brachtendorf.ArrayUtil;

/**
 * A maximum kernel is a non linear filter scanning the image and replacing
 * every value with the maximum value found in the neighborhood.
 * 
 * This maximum kernel allows a weight matrix to be supplied.
 * 
 * <p>
 * Example 1D kernel width 5 Kernel and no/uniform mask
 * 
 * <pre>
 * 	Values: 5 4 1 3 6
 * </pre>
 * 
 * During convolution, the kernel looks at the value 1 and replaces it with the
 * value 6 due to it being the maximum.
 * 
 * <p>
 * A weight mask {@code [1 2 3 2 1]} can give more emphasis on closer pixel
 * values. An intermediary value matrix is calculated:
 * 
 * <pre>
 * Values * Mask = [5 8 3 6 6]
 * </pre>
 * 
 * and it is found that the second value is the maximum. Now the unaltered vlaue
 * at position 2 is taken. Therefore the 1 is replaced with the value 4.
 * 
 * 
 * @author Kilian
 * @since 2.0.0
 * @see MedianKernel
 * @see MinimumKernel
 */
public class MaximumKernel extends NonAveragingKernel {

	private static final long serialVersionUID = 4302400514104308983L;

	/**
	 * Create a maximum kernel with no weight matrix
	 * 
	 * @param width  of the kernel
	 * @param height height of the kernel
	 */
	@SuppressWarnings("deprecation")
	public MaximumKernel(int width, int height) {
		super(EdgeHandlingStrategy.EXPAND);

		if (width <= 0 || width % 2 == 0 || height <= 0 || height % 2 == 0) {
			throw new IllegalArgumentException(
					"Currently only odd dimensional kernels are supported. Width & height have to be positive");
		}
		// Create mask
		double[][] mask = new double[height][width];
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
	public MaximumKernel(double[][] mask) {
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
		return values[0][ArrayUtil.maximumIndex(values[1])];
	}

}
