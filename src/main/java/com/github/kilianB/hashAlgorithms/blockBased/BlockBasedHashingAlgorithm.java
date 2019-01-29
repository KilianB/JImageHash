package com.github.kilianB.hashAlgorithms.blockBased;

import java.awt.image.BufferedImage;
import java.math.BigInteger;

import com.github.kilianB.ArrayUtil;
import com.github.kilianB.hashAlgorithms.HashBuilder;
import com.github.kilianB.hashAlgorithms.HashingAlgorithm;

/**
 * @author Kilian
 *
 */
public abstract class BlockBasedHashingAlgorithm extends HashingAlgorithm{

	
	protected int xBlocks;
	protected int yBlocks;
	
	protected int width;
	protected int height;
	
	protected int blockWidth;
	
	/**
	 * @param bitResolution
	 */
	public BlockBasedHashingAlgorithm(int bitResolution, int blockWidth) {
		super(bitResolution);
		computeDimension(bitResolution);
		this.blockWidth = blockWidth;
		width = xBlocks * blockWidth;
		height = yBlocks * blockWidth;
	}

	@Override
	protected BigInteger hash(BufferedImage image, HashBuilder hashBuilder) {
		double[] blockValues = computeBlocks(image);
		
		//Check the median
		double medianOfMedians = ArrayUtil.median(blockValues);
		
		for(double blockMedian : blockValues) {
			if(blockMedian < medianOfMedians) {
				hashBuilder.prependOne();
			}else {
				hashBuilder.prependZero();
			}
		}
		return hashBuilder.toBigInteger();
	}

	/**
	 * @param image
	 * @return
	 */
	protected abstract double[] computeBlocks(BufferedImage image);

	@Override
	protected int precomputeAlgoId() {
		final int prime = 31;
		int result =  blockWidth;
		result = prime * result + xBlocks;
		result = prime * result + yBlocks;
		result = prime *result + getClass().getSimpleName().hashCode();
		return result;
	}
	
	/**
	 * Compute the dimension for the resize operation. We want to get to close to a
	 * quadratic images as possible to counteract scaling bias.
	 * 
	 * @param bitResolution the desired resolution
	 */
	private void computeDimension(int bitResolution) {

		// Allow for slightly non symmetry to get closer to the true bit resolution
		int dimension = (int) Math.round(Math.sqrt(bitResolution));

		// Lets allow for a +1 or -1 asymmetry and find the most fitting value
		int normalBound = (dimension * dimension);
		int higherBound = (dimension * (dimension + 1));

		this.xBlocks = dimension;
		this.yBlocks = dimension;
		if (normalBound < bitResolution || (normalBound - bitResolution) > (higherBound - bitResolution)) {
			this.yBlocks++;
		}
	}
	
}
