package com.github.kilianB.hashAlgorithms;

import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBufferByte;
import java.math.BigInteger;
import java.util.Objects;

import com.github.kilianB.graphics.ImageUtil;
import com.github.kilianB.graphics.ImageUtil.FastPixel;
import com.github.kilianB.matcher.Hash;

/**
 * Calculate a hash value based on the average color in an image. This hash
 * reacts to changes in hue and saturation and is arguably slow
 * 
 * @author Kilian
 *
 */
public class AverageHash extends HashingAlgorithm {

	/**
	 * Unique id identifying the algorithm and it's settings
	 */
	private final int algorithmId;
	/**
	 * The height and width of the scaled instance used to compute the hash
	 */
	private final int height, width;
	
	private final int pixelCount;

	/**
	 * @param bitResolution The bit resolution specifies the final length of the
	 *                      generated hash. A higher resolution will increase
	 *                      computation time and space requirement while being able
	 *                      to track finer detail in the image. Be aware that a high
	 *                      key is not always desired.
	 *                      <p>
	 * 
	 *                      The average hash requires to re scale the base image
	 *                      according to the required bit resolution. If the square
	 *                      root of the bit resolution is not a natural number the
	 *                      resolution will be rounded to the next whole number.
	 *                      </p>
	 * 
	 *                      <pre>
	 *  64 = 8x8 = 65 bit key
	 *  128 = 11.3 -&gt; 12 -&gt; 144 bit key
	 *  256 = 16 x 16 = 256 bit key
	 *                      </pre>
	 */
	public AverageHash(int bitResolution) {
		super(bitResolution);
		int dimension = (int) Math.round(Math.sqrt(bitResolution));
		this.width = dimension;
		this.height = dimension;
		this.pixelCount = width*height;
		// String and int hashes stays consistent throughout different JVM invocations.
		// Algorithm changed between version 1.x.x and 2.x.x ensure algorithms are
		// flagged as incompatible
		algorithmId = Objects.hash(getClass().getName(), this.bitResolution) * 31 + 1;
	}

	@Override
	public Hash hash(BufferedImage image) {

		FastPixel fp = new FastPixel(ImageUtil.getScaledInstance(image, width, height));

		int[][] luminocity = fp.getLuma();

		// Calculate the average color of the entire image
		
		double avgPixelValue = 0;
		
		for(int x = 0; x < width; x++) {
			for(int y = 0; y < height; y++) {
				avgPixelValue += ((double) luminocity[x][y] / pixelCount);
			}
		}
		// Create hash
		// Padding bit
		BigInteger hash = BigInteger.ONE;

		for(int x = 0; x < width; x++) {
			for(int y = 0; y < height; y++) {
				if (luminocity[x][y] < avgPixelValue) {
					hash = hash.shiftLeft(1);
				}else {
					hash = hash.shiftLeft(1).add(BigInteger.ONE);
				}
			}
		}
		return new Hash(hash, algorithmId);
	}

	@Override
	public int algorithmId() {
		return algorithmId;
	}

}
