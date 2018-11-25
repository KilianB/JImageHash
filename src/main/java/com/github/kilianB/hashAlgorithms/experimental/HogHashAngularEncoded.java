package com.github.kilianB.hashAlgorithms.experimental;

import java.awt.image.BufferedImage;
import java.math.BigInteger;

import com.github.kilianB.graphics.ImageUtil;
import com.github.kilianB.graphics.ImageUtil.FastPixel;

/**
 * @author Kilian
 *
 */
public class HogHashAngularEncoded extends HogHash {

	/**
	 * @param bitResolution
	 */
	public HogHashAngularEncoded(int bitResolution) {
		super(bitResolution);
	}

	/**
	 * @param width
	 * @param height
	 * @param cellWidth
	 * @param numBins
	 */
	public HogHashAngularEncoded(int width, int height, int cellWidth, int numBins) {
		super(width, height, cellWidth, numBins);
	}

	@Override
	protected BigInteger hash(BufferedImage image, BigInteger hash) {

		BufferedImage bi = ImageUtil.getScaledInstance(image, width, height);
		FastPixel fp = new FastPixel(bi);

		int[][] lum = fp.getLuma();

		// 1 Compute hisogramm

		int[][][] hog = computeHogFeatures(lum);

		double binAverage[] = new double[numBins];
		//Compute average of each bin
		
		int cells = xCells * yCells;
		
		for (int xCell = 0; xCell < xCells; xCell++) {
			// Construct intermediary vector
			for (int yCell = 0; yCell < yCells; yCell++) {
				for(int bin = 0; bin < numBins; bin++) {
					binAverage[bin] += hog[xCell][yCell][bin]/cells;
				}
			}
		}
		
		for (int xCell = 0; xCell < xCells; xCell++) {
			// Construct intermediary vector
			for (int yCell = 0; yCell < yCells; yCell++) {
				for(int bin = 0; bin < numBins; bin++) {
					if(hog[xCell][yCell][bin] > binAverage[bin]) {
						hash = hash.shiftLeft(1);
					}else {
						hash = hash.shiftLeft(1).add(BigInteger.ONE);
					}
				}
			}
		}

		return hash;
	}

}
