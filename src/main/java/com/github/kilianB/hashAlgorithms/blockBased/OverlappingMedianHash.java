package com.github.kilianB.hashAlgorithms.blockBased;

import java.awt.image.BufferedImage;
import java.util.Arrays;

import com.github.kilianB.ArrayUtil;
import com.github.kilianB.graphics.FastPixel;
import com.github.kilianB.graphics.ImageUtil;

/**
 * @author Kilian
 *
 */
public class OverlappingMedianHash extends BlockBasedHashingAlgorithm {

	/**
	 * @param bitResolution
	 * @param blockWidth
	 */
	public OverlappingMedianHash(int bitResolution, int blockWidth) {
		super(bitResolution, blockWidth);
		width = xBlocks * blockWidth;
		height = yBlocks * blockWidth;
	}

	@Override
	protected double[] computeBlocks(BufferedImage image) {
		int[][] luma = FastPixel.create(ImageUtil.getScaledInstance(image, width, height)).getLuma();

		double[] blockMedians = new double[xBlocks * yBlocks];

		int blockW = (int) Math.ceil(blockWidth * 1.5);

//		System.out.println(ArrayUtil.deepToStringFormatted(luma));
		
		
		// Blocks
		for (int x = 0, blockIndex = 0; x < xBlocks; x++) {
			for (int y = 0; y < yBlocks; y++, blockIndex++) {

				// Pixels inside blocks
				double[] blockValues = new double[blockW * blockW];
				for (int xi = 0, index = 0; xi < blockW; xi++) {
					for (int yi = 0; yi < blockW; yi++, index++) {

						int tempX = x * blockWidth + xi;
						int tempY = y * blockWidth + yi;
						if (tempX >= luma.length) {
							tempX = luma.length - (tempX - luma.length + 1);
						}
						if (tempY >= luma[0].length) {
							tempY = luma[0].length - (tempY - luma[0].length + 1);
						}
						blockValues[index] = luma[tempX][tempY];
					}
				}
				blockMedians[blockIndex] = ArrayUtil.median(blockValues);
			}
		}
		// TODO this can be speed up quite a bit
		return blockMedians;
	}

}
