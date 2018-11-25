package com.github.kilianB.hashAlgorithms.experimental;

import java.awt.image.BufferedImage;
import java.math.BigInteger;
import java.util.Objects;

import com.github.kilianB.ArrayUtil;
import com.github.kilianB.graphics.ImageUtil;
import com.github.kilianB.graphics.ImageUtil.FastPixel;
import com.github.kilianB.hashAlgorithms.HashingAlgorithm;

/**
 * @author Kilian
 *
 */
public class MedianHash extends HashingAlgorithm {

	private static final long serialVersionUID = -5234612717498362659L;

	/**
	 * The height and width of the scaled instance used to compute the hash
	 */
	private int height, width;


	/**
	 * @param bitResolution
	 */
	public MedianHash(int bitResolution) {
		super(bitResolution);
		/*
		 * Figure out how big our resized image has to be in order to create a hash with
		 * approximately bit resolution bits while trying to stay as squared as possible
		 * to not introduce bias via stretching or shrinking the image asymmetrically.
		 */
		computeDimension(bitResolution);
	}

	@Override
	protected BigInteger hash(BufferedImage image, BigInteger hash) {
		FastPixel fp = new FastPixel(ImageUtil.getScaledInstance(image, width, height));

		int[] lum = fp.getLuma1D();
		
		int[][] luminocity = fp.getLuma();

		//Compute median.
		double medianValue;
		int[] sortedIndices = ArrayUtil.getSortedIndices(lum,true);
		int midPoint = sortedIndices.length/2;
		if(sortedIndices.length % 2 == 0) {
			medianValue = (lum[sortedIndices[midPoint]] + lum[sortedIndices[midPoint-1]])/2;
		}else {
			medianValue= lum[sortedIndices[midPoint]];
		}
		
		
		// Create hash
		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				if (luminocity[x][y] < medianValue) {
					hash = hash.shiftLeft(1);
				} else {
					hash = hash.shiftLeft(1).add(BigInteger.ONE);
				}
			}
		}
		return hash;
	}


	private void computeDimension(int bitResolution) {

		// Allow for slightly non symmetry to get closer to the true bit resolution
		int dimension = (int) Math.round(Math.sqrt(bitResolution));

		// Lets allow for a +1 or -1 asymmetry and find the most fitting value
		int normalBound = (dimension * dimension);
		int higherBound = (dimension * (dimension + 1));

		this.height = dimension;
		this.width = dimension;
		if (normalBound < bitResolution || (normalBound - bitResolution) > (higherBound - bitResolution)) {
			this.width++;
		}
	}

	@Override
	protected int precomputeAlgoId() {
		return  Objects.hash(getClass().getName(), height, width);
	}

}
