package dev.brachtendorf.jimagehash.hashAlgorithms.filter;

import java.awt.image.BufferedImage;
import java.util.Arrays;

import dev.brachtendorf.ArrayUtil;
import dev.brachtendorf.graphics.FastPixel;

/**
 * A median kernel is a non linear filter scanning the image and replacing
 * every value with the median value found in the neighborhood.
 * 
 * This median kernel allows a weight matrix to be supplied.
 * 
 * <p>
 * Example 1D kernel width 5 Kernel and no/uniform mask
 * 
 * <pre>
 * 	Values: 5 4 1 3 6
 * </pre>
 * 
 * 1 3 4 5 6
 * 
 * During convolution, the kernel looks at the value 1 and replaces it with the
 * value 4 due to it being the median.
 * 
 * <p>
 * A weight mask {@code [1 2 3 2 1]} can give more emphasis on closer pixel
 * values. An intermediary value matrix is calculated:
 * 
 * <pre>
 * Values * Mask = [5 8 3 6 6]
 * </pre>
 * 
 * 3 5 6 6 8 
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
public class MedianKernel extends NonAveragingKernel {

	private static final long serialVersionUID = 5756361510407136992L;

	/**
	 * Create a median kernel with a uniform weight mask (no weighting takes place)
	 * @param width of the kernel. has to be odd
	 * @param height of the kernel. has to be odd
	 * 
	 */
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
	 * Create a kernel with the given masks dimension. The masks acts as weight
	 * filter increasing or decreasing the weight of the value during convolution.
	 * For an example see the javadoc of the class.
	 * 
	 * @param mask weight matrix used to judge which value is the maximum
	 */
	public MedianKernel(double[][] mask) {
		super(mask);
	}
	
	@Override
	protected double calcValue(byte[][] input, int x, int y) {
		return resolveMedian(computePotentialValues(input, x, y));
	}

	@Override
	protected double calcValue(int[][] input, int x, int y) {
		return resolveMedian(computePotentialValues(input, x, y));
	}

	@Override
	protected double calcValue(double[][] input, int x, int y) {
		return resolveMedian(computePotentialValues(input, x, y));
	}
	
	protected double resolveMedian(double[][] values) {
		if (values[1].length == 1 && values[1][0] == Double.MIN_VALUE) {
			return values[0][0];
		}
		Arrays.sort(values[0]);
		
		//TODO currently not using the weighed mask
		//halfIndex = ArrayUtil.getSortedIndices(values[1])[values.length/half];

		
		// Find the median value
		int halfIndex = values.length / 2;
		if (values.length % 2 == 0) {
			return (values[0][halfIndex] + values[0][halfIndex + 1]) / 2;
		} else {
			return values[0][halfIndex];
		}
	}
	
	
	@Override
	public BufferedImage filter(BufferedImage input) {
		BufferedImage bi = new BufferedImage(input.getWidth(), input.getHeight(), input.getType());
		FastPixel fp = FastPixel.create(input);
		FastPixel fpSet = FastPixel.create(bi);
		int[][] gray = fp.getAverageGrayscale();
		
		gray = applyInt(gray);
		
		if(fpSet.hasAlpha()) {
			fpSet.setAlpha(fp.getAlpha());
		}
		
		fpSet.setAverageGrayscale(gray);
		return bi;
	}

}
