package com.github.kilianB.hashAlgorithms;

import java.awt.image.BufferedImage;
import java.math.BigInteger;
import java.util.Objects;

import com.github.kilianB.ArrayUtil;
import com.github.kilianB.graphics.FastPixel;
import com.github.kilianB.graphics.ImageUtil;

/**
 * Calculate a hash value based on the median luminosity in an image.
 * 
 * <p>
 * Really good performance almost comparable to average hash. So far does a
 * little bit better if watermarks are added to the image but trades this off
 * for a little bit worse if rescaled.
 * 
 * <p>
 * - Slower to compute
 * 
 * @author Kilian
 * @since 2.0.0
 */
public class MedianHash extends HashingAlgorithm {

	private static final long serialVersionUID = -5234612717498362659L;

	/**
	 * The height and width of the scaled instance used to compute the hash
	 */
	private int height, width;

	/**
	 * @param bitResolution The bit resolution specifies the final length of the
	 *                      generated hash. A higher resolution will increase
	 *                      computation time and space requirement while being able
	 *                      to track finer detail in the image. Be aware that a high
	 *                      key is not always desired.
	 *                      <p>
	 * 
	 *                      The median hash will produce a hash with at least the
	 *                      number of bits defined by this argument. In turn this
	 *                      also means that different bit resolutions may be mapped
	 *                      to the same final key length.
	 * 
	 *                      <pre>
	 *  64 = 8x8 = 65 bit key
	 *  128 = 11.3 -&gt; 12 -&gt; 144 bit key
	 *  256 = 16 x 16 = 256 bit key
	 *                      </pre>
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
		FastPixel fp = FastPixel.create(ImageUtil.getScaledInstance(image, width, height));

		int[] lum = fp.getLuma1D();

		int[][] luminocity = fp.getLuma();

		// Compute median.
		double medianValue;
		int[] sortedIndices = ArrayUtil.getSortedIndices(lum, true);
		int midPoint = sortedIndices.length / 2;
		if (sortedIndices.length % 2 == 0) {
			medianValue = (lum[sortedIndices[midPoint]] + lum[sortedIndices[midPoint - 1]]) / 2;
		} else {
			medianValue = lum[sortedIndices[midPoint]];
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
		return Objects.hash(getClass().getName(), height, width);
	}

}
