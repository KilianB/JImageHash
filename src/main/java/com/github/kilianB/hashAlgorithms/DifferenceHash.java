package com.github.kilianB.hashAlgorithms;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Objects;

import javax.imageio.ImageIO;

import com.github.kilianB.graphics.ImageUtil;
import com.github.kilianB.graphics.ImageUtil.FastPixel;
import com.github.kilianB.matcher.Hash;

/**
 * Calculates a hash based on gradient tracking. This hash is cheap to compute
 * and provides a high degree of accuracy. Robust to a huge range of color
 * transformation
 * 
 * @author Kilian
 *
 */
public class DifferenceHash extends HashingAlgorithm {

	/**
	 * Algorithm precision.
	 * 
	 * Be aware that changing the enum names will alter the algorithm id rendering
	 * generated keys unable to
	 * 
	 * @author Kilian
	 *
	 */
	public enum Precision {
		/** Top to bottom gradient only */
		Simple,
		/** Additionally left to right gradient */
		Double,
		/** Tripple precision (top-bottom, left-right, diagonally) */
		Triple
	}

	/**
	 * Unique id identifying the algorithm and it's settings
	 */
	private final int algorithmId;
	/**
	 * The height and width of the scaled instance used to compute the hash
	 */
	private final int height, width;

	/**
	 * Precision used to calculate the hash
	 */
	private final Precision precision;

	/**
	 * 
	 * @param bitResolution The bit resolution specifies the final length of the
	 *                      generated hash. A higher resolution will increase
	 *                      computation time and space requirement while being able
	 *                      to track finer detail in the image. <b>Be aware that a
	 *                      high resolution is not always desired.</b> The bit
	 *                      resolution is only an <b>approximation</b> of the final
	 *                      hash length.
	 * @param precision     Algorithm precision. Allowed Values:
	 *                      <dl>
	 *                      <dt>Simple:</dt>
	 *                      <dd>Calculates top - bottom gradient</dd>
	 *                      <dt>Double:</dt>
	 *                      <dd>Additionally computes left - right gradient (doubles
	 *                      key length)</dd>
	 *                      <dt>Tripple:</dt>
	 *                      <dd>Additionally computes diagonal gradient (triples key
	 *                      length)</dd>
	 *                      </dl>
	 */
	public DifferenceHash(int bitResolution, Precision precision) {
		super(bitResolution);

		// width * height = bitResolution -> (height + 1) * height =
		// bitResolution

		// Substitute
		// (x * (x+1)) = y
		// x^2 + x = y -> 1/2 * ( +- sqrt(4y + 1) - 1)

		// only look at the positive part and truncate
		int x = (int) Math.round(0.5 * (Math.sqrt(4 * bitResolution + 1) - 1));

		height = x;
		// Width +1 for padding
		width = x + 1;

		this.precision = precision;
		// String and int hashes stays consistent throughout different JVM invocations.
		// Algorithm changed between version 1.x.x and 2.x.x ensure algorithms are
		// flagged as incompatible
		algorithmId = Objects.hash(getClass().getName(), this.bitResolution, this.precision.name()) * 31 + 1;
		;
	}

	@Override
	public Hash hash(BufferedImage image) {
		FastPixel fp = new FastPixel(ImageUtil.getScaledInstance(image, width, height));
		// Use data buffer for faster access

		int[][] lum = fp.getLuma();

		// Calculate the left to right gradient
		BigInteger hash = BigInteger.ONE;
		for (int x = 1; x < width; x++) {
			for (int y = 0; y < height; y++) {
				if (lum[x][y] >= lum[x - 1][y]) {
					hash = hash.shiftLeft(1);
				} else {
					hash = hash.shiftLeft(1).add(BigInteger.ONE);
				}
			}
		}

		// Top to bottom gradient
		if (!precision.equals(Precision.Simple)) {
			// We need a padding row at the top now.
			// Caution width and height are swapped

			for (int x = 0; x < width; x++) {
				for (int y = 1; y < height; y++) {
					if (lum[x][y] < lum[x][y - 1]) {
						hash = hash.shiftLeft(1);
					} else {
						hash = hash.shiftLeft(1).add(BigInteger.ONE);
					}
				}
			}
		}
		// Diagonally hash
		if (precision.equals(Precision.Triple)) {
			for (int x = 1; x < width; x++) {
				for (int y = 1; y < height; y++) {
					if (lum[x][y] < lum[x - 1][y - 1]) {
						hash = hash.shiftLeft(1);
					} else {
						hash = hash.shiftLeft(1).add(BigInteger.ONE);
					}
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
