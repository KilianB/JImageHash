package dev.brachtendorf.jimagehash.hashAlgorithms.filter;

import java.awt.image.BufferedImage;

import dev.brachtendorf.Require;
import dev.brachtendorf.graphics.FastPixel;

/**
 * 
 * Edge detection filter
 * 
 * <pre>
 * 
 * 		 1     0   -1
 * Gx :	 2     0   -2
 *  	 1     0   -1
 * 		
 * 		 1    2    1
 * Gy:	 0    0    0
 *		-1   -2   -1
 *
 * G : sqrt(Gx^2+ Gy^2)
 *
 * </pre>
 * 
 * @author Kilian
 * @since 2.0.0
 * @see <a href="https://en.wikipedia.org/wiki/Sobel_operator">Sobel Operator</a>
 */
public class SobelFilter implements Filter {

	private static final long serialVersionUID = -794573990315594591L;
	
	/** Separated Gx Kernel */
	private MultiKernel xKernel;
	/** Separated Gy Kernel */
	private MultiKernel yKernel;

	/** Gray cutoff value */
	private double threshold;

	/**
	 * Create a sobel filter
	 * 
	 * @param threshold the cutoff beneath which gray values will be set to 0. [0 - 1].
	 */
	public SobelFilter(double threshold) {

		this.threshold = (double) Require.inRange(threshold, 0, 1, "Threshold must be in range of [0-1]");

		// x Kernel
		/* @formatter:off
		 * 
		 * xKernel Separated 
		 *  1  0  -1    1
		 *  2  0  -2 => 2  1 0 -1 
		 *  1  0  -1    1
		 *  
		 * @formatter:on
		 */

		double[][] x0Mask = { { 1 }, { 2 }, { 1 } };
		double[][] x1Mask = { { 1, 0, -1 } };

		/* @formatter:off
		 * 
		 * yKernel Separated 
		 *  1  2  1     1 
		 *  0  0  0 =>  0 x 1 2 1 
		 * -1 -2 -1    -1
		 * 
		 * @formatter:on
		 */

		double[][] y0Mask = { { 1 }, { 0 }, { -1 } };
		double[][] y1Mask = { { 1, 2, 1 } };
		
		xKernel = new MultiKernel(x1Mask, x0Mask);
		yKernel = new MultiKernel(y0Mask, y1Mask);
	}

	@Override
	public BufferedImage filter(BufferedImage bi) {

		FastPixel fp = FastPixel.create(bi);

		int[][] grayscale = fp.getAverageGrayscale();

		int[][] xGradient = xKernel.applyInt(grayscale);
		int[][] yGradient = yKernel.applyInt(grayscale);

		int[][] result = new int[xGradient.length][xGradient[0].length];

		int cutOffValue = (int) (threshold * 255);

		for (int x = 0; x < xGradient.length; x++) {
			for (int y = 0; y < xGradient[x].length; y++) {
				result[x][y] = (int) Math.sqrt(xGradient[x][y] * xGradient[x][y] + yGradient[x][y] * yGradient[x][y]);
				
				if(result[x][y] < 0) {
					result[x][y] = -result[x][y];
				}
				
				if (result[x][y] < cutOffValue) {
					result[x][y] = 0;
				}
			}
		}
		
		
		BufferedImage returnBi = new BufferedImage(bi.getWidth(), bi.getHeight(), bi.getType());
		FastPixel fpSet = FastPixel.create(returnBi);

		fpSet.setAverageGrayscale(result);

		if(fpSet.hasAlpha()) {
			fpSet.setAlpha(fp.getAlpha());
		}
		
		return returnBi;
	}

}
