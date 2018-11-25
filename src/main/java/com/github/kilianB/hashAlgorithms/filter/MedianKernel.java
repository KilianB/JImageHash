package com.github.kilianB.hashAlgorithms.filter;

import java.util.Arrays;

import com.github.kilianB.ArrayUtil;

/**
 * @author Kilian
 *
 */
public class MedianKernel extends Kernel {

	// TODO weighted median usually is something else in statistics

	// Median kernel
	@SuppressWarnings("deprecation")
	public MedianKernel(int width, int height) {
		super(EdgeHandlingStrategy.EXPAND);

		if (width <= 0 || width % 2 == 0 || height <= 0 || height % 2 == 0) {
			throw new IllegalArgumentException(
					"Currently only odd dimensional kernels are supported. Width & height have to be positive");
		}
		// Create mask
		double[][] mask = new double[width][height];
		ArrayUtil.fillArrayMulti(mask,()->{return 1d;});
		this.mask = mask;
	}
	
	/**
	 * 
	 * MASK are multiplicate values for determening the median value!!
	 * TODO
	 * weights or actual values?
	 * 
	 * 
	 * @param mask
	 */
	public MedianKernel(double[][] mask) {
		super(mask);
	}
	
	protected double calcValue(byte[][] input, int x, int y, int width, int height) {
		int maskW = mask[0].length / 2;
		int maskH = mask.length / 2;

		double[] wValues = new double[mask.length * mask.length];
		double[] values = new double[mask.length * mask.length];

		int index = 0;
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

				values[index] = mask[yMask + maskH][xMask + maskW] * input[xPixelIndex][yPixelIndex];
				wValues[index++] = mask[yMask + maskH][xMask + maskW] * input[xPixelIndex][yPixelIndex];
			}
		}
		
		//TODO how do we want to do this?

		Arrays.sort(values);

		// Find the median value

		int halfIndex = values.length / 2;

		if (values.length % 2 == 0) {
			return (values[halfIndex] + values[halfIndex + 1]) / 2;
		} else {
			return values[halfIndex];
		}
	}

	protected double calcValue(int[][] input, int x, int y, int width, int height) {
		int maskW = mask[0].length / 2;
		int maskH = mask.length / 2;

		double[] values = new double[mask.length * mask.length];

		int index = 0;
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

				values[index++] = mask[yMask + maskH][xMask + maskW] * input[xPixelIndex][yPixelIndex];
			}
		}

		Arrays.sort(values);

		// Find the median value

		int halfIndex = values.length / 2;

		if (values.length % 2 == 0) {
			return (values[halfIndex] + values[halfIndex + 1]) / 2;
		} else {
			return values[halfIndex];
		}
	}

	protected double calcValue(double[][] input, int x, int y, int width, int height) {
		int maskW = mask[0].length / 2;
		int maskH = mask.length / 2;

		double[] values = new double[mask.length * mask.length];

		int index = 0;
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

				values[index++] = mask[yMask + maskH][xMask + maskW] * input[xPixelIndex][yPixelIndex];
			}
		}

		Arrays.sort(values);

		// Find the median value

		int halfIndex = values.length / 2;

		if (values.length % 2 == 0) {
			return (values[halfIndex] + values[halfIndex + 1]) / 2;
		} else {
			return values[halfIndex];
		}
	}

}
