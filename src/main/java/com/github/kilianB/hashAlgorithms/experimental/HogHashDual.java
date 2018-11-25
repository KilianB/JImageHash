package com.github.kilianB.hashAlgorithms.experimental;

import java.awt.image.BufferedImage;
import java.math.BigInteger;
import java.util.Objects;

import com.github.kilianB.MathUtil;
import com.github.kilianB.graphics.ImageUtil;
import com.github.kilianB.graphics.ImageUtil.FastPixel;
import com.github.kilianB.hashAlgorithms.HashingAlgorithm;

/**
 * Image Hash on HOG feature descriptor. Not ready yet. Most likely use a very
 * high bit resolution similar to how hog feature descriptors are actually used
 * 
 * @deprecated not ready to use yet
 * @author Kilian
 * 
 */
@Deprecated
public class HogHashDual extends HogHash {

	/*
	 * Hog is calculated following the approach outlined in "Histograms of Oriented
	 * Gradients for Human Detection" by NavneetDalal and Bill Triggs
	 * http://lear.inrialpes.fr/people/triggs/pubs/Dalal-cvpr05.pdf
	 */
 
	private static final long serialVersionUID = 5353878339786219609L;

	public HogHashDual(int width, int height, int cellWidth, int numBins) {
		super(width,height,cellWidth,numBins);
	}
	
	public HogHashDual(int bitResolution) {
		super(bitResolution);
	}
	
	@Override
	protected BigInteger hash(BufferedImage image, BigInteger hash) {

		
		BufferedImage bi = ImageUtil.getScaledInstance(image, width, height);
		FastPixel fp = new FastPixel(bi);

		int[][] lum = fp.getLuma();

		// 1 Compute hisogramm

		int[][][] hog = computeHogFeatures(lum);

		for (int xCell = 0; xCell < xCells; xCell++) {
			// Construct intermediary vector
			for (int yCell = 0; yCell < yCells; yCell++) {

				int lastMax = Integer.MIN_VALUE;

				int maxIndex = -1;
				int secondMax = -1;
				// Same bin
				for (int bin = 0; bin < numBins; bin++) {
					if (hog[xCell][yCell][bin] > lastMax) {
						lastMax = hog[xCell][yCell][bin];
						secondMax = maxIndex;
						maxIndex = bin;
					}
				}

				boolean caryOver = false;
				for (int bin = 0; bin < numBins; bin++) {
					if (bin == maxIndex) {
						hash = hash.shiftLeft(1);
						caryOver = true;
					} else if (caryOver || secondMax == bin) {
						hash = hash.shiftLeft(1);
						caryOver = false;
					} else {
						hash = hash.shiftLeft(1).add(BigInteger.ONE);
					}
				}
			}
		}

		return hash;
	}

	@Override
	public String toString() {
		return "HogHashDual [numBins=" + numBins + "]";
	}

}
