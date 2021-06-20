package dev.brachtendorf.jimagehash.hashAlgorithms.filter;

import dev.brachtendorf.ArrayUtil;

/**
 * @author Kilian
 * @since 3.0.0
 */
public abstract class NonAveragingKernel extends Kernel {

	private static final long serialVersionUID = -6587002283239284704L;

	/**
	 * Empty constructor used by inheriting classes which are not able to provide a
	 * mask during first constructor call. The inheriting class promises to provide
	 * a mask and all checks by itself
	 * 
	 * @param strat EdgeHandlingStrategy to use
	 */
	@Deprecated
	protected NonAveragingKernel(EdgeHandlingStrategy strat) {
		super(strat);
	};
	
	@SuppressWarnings("deprecation")
	public NonAveragingKernel(int width, int height) {
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
	public NonAveragingKernel(double[][] mask) {
		super(mask);
	}

	protected double[][] computePotentialValues(byte[][] input, int x, int y) {
		int maskW = mask[0].length / 2;
		int maskH = mask.length / 2;
		int width = input[0].length;
		int height = input.length;

		//double[] wValues = new double[mask.length * mask[0].length];
		double[][] values = new double[2][mask.length * mask[0].length];

		int index = 0;
		for (int yMask = -maskH; yMask <= maskH; yMask++) {
			for (int xMask = -maskW; xMask <= maskW; xMask++) {

				int xPixelIndex;
				int yPixelIndex;

				if (edgeHandling.equals(EdgeHandlingStrategy.NO_OP)) {
					xPixelIndex = x + xMask;
					yPixelIndex = y + yMask;

					if (xPixelIndex < 0 || xPixelIndex >= width || yPixelIndex < 0 || yPixelIndex >= height) {
						return new double[][] { {input[y][x]},{Double.MIN_VALUE}};
					}
				} else {
					xPixelIndex = edgeHandling.correctPixel(x + xMask, width);
					yPixelIndex = edgeHandling.correctPixel(y + yMask, height);
				}

				values[0][index] = input[yPixelIndex][xPixelIndex];
				values[1][index++] = mask[yMask + maskH][xMask + maskW] * input[yPixelIndex][xPixelIndex];
			}
		}
		return values;
	}

	protected double[][] computePotentialValues(int[][] input, int x, int y) {
		int maskW = mask[0].length / 2;
		int maskH = mask.length / 2;
		int width = input[0].length;
		int height = input.length;

		//double[] wValues = new double[mask.length * mask[0].length];
		double[][] values = new double[2][mask.length * mask[0].length];

		int index = 0;
		for (int yMask = -maskH; yMask <= maskH; yMask++) {
			for (int xMask = -maskW; xMask <= maskW; xMask++) {

				int xPixelIndex;
				int yPixelIndex;

				if (edgeHandling.equals(EdgeHandlingStrategy.NO_OP)) {
					xPixelIndex = x + xMask;
					yPixelIndex = y + yMask;

					if (xPixelIndex < 0 || xPixelIndex >= width || yPixelIndex < 0 || yPixelIndex >= height) {
						return new double[][] { {input[y][x]},{0}};
					}
				} else {
					xPixelIndex = edgeHandling.correctPixel(x + xMask, width);
					yPixelIndex = edgeHandling.correctPixel(y + yMask, height);
				}

				values[0][index] = input[yPixelIndex][xPixelIndex];
				values[1][index++] = mask[yMask + maskH][xMask + maskW] * input[yPixelIndex][xPixelIndex];
			}
		}
		return values;
	}

	protected double[][] computePotentialValues(double[][] input, int x, int y) {
		int maskW = mask[0].length / 2;
		int maskH = mask.length / 2;
		int width = input[0].length;
		int height = input.length;

		//double[] wValues = new double[mask.length * mask[0].length];
		double[][] values = new double[2][mask.length * mask[0].length];

		int index = 0;
		for (int yMask = -maskH; yMask <= maskH; yMask++) {
			for (int xMask = -maskW; xMask <= maskW; xMask++) {

				int xPixelIndex;
				int yPixelIndex;

				if (edgeHandling.equals(EdgeHandlingStrategy.NO_OP)) {
					xPixelIndex = x + xMask;
					yPixelIndex = y + yMask;

					if (xPixelIndex < 0 || xPixelIndex >= width || yPixelIndex < 0 || yPixelIndex >= height) {
						return new double[][] { {input[y][x]},{0}};
					}
				} else {
					xPixelIndex = edgeHandling.correctPixel(x + xMask, width);
					yPixelIndex = edgeHandling.correctPixel(y + yMask, height);
				}

				values[0][index] = input[yPixelIndex][xPixelIndex];
				values[1][index++] = mask[yMask + maskH][xMask + maskW] * input[yPixelIndex][xPixelIndex];
				
			}
		}
		return values;
	}

}
