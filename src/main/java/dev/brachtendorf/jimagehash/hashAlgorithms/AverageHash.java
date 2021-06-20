package dev.brachtendorf.jimagehash.hashAlgorithms;

import java.awt.image.BufferedImage;
import java.math.BigInteger;
import java.util.Objects;

import dev.brachtendorf.ArrayUtil;
import dev.brachtendorf.graphics.FastPixel;

/**
 * Calculate a hash value based on the average luminosity in an image.
 * 
 * @author Kilian
 * @since 1.0.0
 * @since 2.0.0 use luminosity instead of average pixel color
 */
public class AverageHash extends HashingAlgorithm {

	private static final long serialVersionUID = -5234612717498362659L;

	/**
	 * The height and width of the scaled instance used to compute the hash
	 */
	protected int height, width;

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
	 *                      The average hash will produce a hash with at least the
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
	public AverageHash(int bitResolution) {
		super(bitResolution);
		/*
		 * Figure out how big our resized image has to be in order to create a hash with
		 * approximately bit resolution bits while trying to stay as squared as possible
		 * to not introduce bias via stretching or shrinking the image asymmetrically.
		 */
		computeDimension(bitResolution);
	}

	@Override
	protected BigInteger hash(BufferedImage image, HashBuilder hash) {
		FastPixel fp = createPixelAccessor(image, width, height);

		int[][] luminocity = fp.getLuma();

		// Calculate the average color of the entire image
		double avgPixelValue = ArrayUtil.average(luminocity);

		// Create hash
		return computeHash(hash, luminocity, avgPixelValue);
	}

	protected BigInteger computeHash(HashBuilder hash, double[][] pixelValue, double compareAgainst) {
		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				if (pixelValue[x][y] < compareAgainst) {
					hash.prependZero();
				} else {
					hash.prependOne();
				}
			}
		}
		return hash.toBigInteger();
	}

	protected BigInteger computeHash(HashBuilder hash, int[][] pixelValue, double compareAgainst) {
		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				if (pixelValue[x][y] < compareAgainst) {
					hash.prependZero();
				} else {
					hash.prependOne();
				}
			}
		}
		return hash.toBigInteger();
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

		this.height = dimension;
		this.width = dimension;
		if (normalBound < bitResolution || (normalBound - bitResolution) > (higherBound - bitResolution)) {
			this.width++;
		}
	}

	@Override
	protected int precomputeAlgoId() {
		/*
		 * String and int hashes stays consistent throughout different JVM invocations.
		 * Algorithm changed between version 1.x.x and 2.x.x ensure algorithms are
		 * flagged as incompatible. Dimension are what makes average hashes unique
		 * therefore, even
		 */
		return Objects.hash("com.github.kilianB.hashAlgorithms."+getClass().getSimpleName(), height, width);
	}
}
