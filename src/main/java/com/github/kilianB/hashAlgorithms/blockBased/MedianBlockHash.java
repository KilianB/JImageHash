package com.github.kilianB.hashAlgorithms.blockBased;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import com.github.kilianB.ArrayUtil;
import com.github.kilianB.graphics.FastPixel;
import com.github.kilianB.graphics.ImageUtil;
import com.github.kilianB.hash.Hash;
import com.github.kilianB.hashAlgorithms.HashingAlgorithm;

/**
 * @author Kilian
 *
 */
public class MedianBlockHash extends BlockBasedHashingAlgorithm {

	/**
	 * @param bitResolution
	 * @param blockWidth
	 */
	public MedianBlockHash(int bitResolution, int blockWidth) {
		super(bitResolution, blockWidth);
	}

	@Override
	protected double[] computeBlocks(BufferedImage image) {

		int[][] luma = FastPixel.create(ImageUtil.getScaledInstance(image, width, height)).getLuma();

		double[] blockMedians = new double[xBlocks * yBlocks];

//		System.out.println(ArrayUtil.deepToStringFormatted(luma));
		
		//Blocks
		for (int x = 0, blockIndex = 0; x < xBlocks; x++) {
			for (int y = 0; y < yBlocks; y++, blockIndex++) {

				//Pixels inside blocks
				double[] blockValues = new double[blockWidth * blockWidth];
				for (int xi = x * blockWidth, index = 0; xi < (x + 1) * blockWidth; xi++) {
					for (int yi = y * blockWidth; yi < (y + 1) * blockWidth; yi++, index++) {
						blockValues[index] = luma[xi][yi];
					}
				}
				blockMedians[blockIndex] = ArrayUtil.median(blockValues);
//				System.out.println(Arrays.toString(blockValues) + " Median: " + blockMedians[blockIndex]);
				
			}
		}
		//TODO this can be speed up quite a bit
		return blockMedians;
	}

	
	public static void main(String[] args) throws IOException {
		HashingAlgorithm blockHasher = new OverlappingMedianHash(32,3);
		
		Hash h = blockHasher.hash(new File("src/test/resources/ballon.jpg"));
		Hash h1 = blockHasher.hash(new File("src/test/resources/highQuality.jpg"));
		Hash h2 = blockHasher.hash(new File("src/test/resources/lowQuality.jpg"));

		
		System.out.println(h);
		System.out.println(h1);
		System.out.println(h2);
	
	}
}
