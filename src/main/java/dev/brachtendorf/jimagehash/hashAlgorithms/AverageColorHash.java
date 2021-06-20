package dev.brachtendorf.jimagehash.hashAlgorithms;

import java.awt.image.BufferedImage;
import java.math.BigInteger;

import dev.brachtendorf.ArrayUtil;
import dev.brachtendorf.graphics.FastPixel;

/**
 * Calculate a hash value based on the average rgb color in an image.
 * 
 * @author Kilian
 * @since 2.0.0 similar to ahash from version 1.0.0
 */
public class AverageColorHash extends AverageHash {

	private static final long serialVersionUID = -5234612717498362659L;


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
	public AverageColorHash(int bitResolution) {
		super(bitResolution);
	}

	@Override
	protected BigInteger hash(BufferedImage image, HashBuilder hash) {
		FastPixel fp = createPixelAccessor(image, width, height);

		int[][] grayscale = fp.getAverageGrayscale();

		// Calculate the average color of the entire image
		double avgPixelValue = ArrayUtil.average(grayscale);

		// Create hash
		return computeHash(hash,grayscale,avgPixelValue);
	}

}
