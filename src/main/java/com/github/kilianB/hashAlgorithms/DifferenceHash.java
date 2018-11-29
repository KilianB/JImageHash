package com.github.kilianB.hashAlgorithms;

import java.awt.image.BufferedImage;
import java.math.BigInteger;
import java.util.Objects;

import com.github.kilianB.graphics.FastPixel;
import com.github.kilianB.graphics.ImageUtil;

/**
 * Calculates a hash based on gradient tracking. This hash is cheap to compute
 * and provides a high degree of accuracy. Robust to a huge range of color
 * transformation
 * 
 * @author Kilian
 * @since 1.0.0
 */
public class DifferenceHash extends HashingAlgorithm {

	private static final long serialVersionUID = 7236596241664072005L;

	/**
	 * Algorithm precision used during calculation.
	 * 
	 * @implnote Be aware that changing the enum names will alter the algorithm id
	 *           rendering generated keys unusable
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
	 * The height and width of the scaled instance used to compute the hash
	 */
	private int height, width;

	/**
	 * Precision used to calculate the hash
	 */
	private final Precision precision;

	/**
	 * 
	 * Create a difference hasher with the given settings. The bit resolution always
	 * corresponds to the simple precision value and will increase accordingly depending
	 * on the precision chosen.
	 * 
	 * <p>
	 * Tests have shown that a 64 bit simple precision hash usually performs better than a 
	 * 32 bit double precision hash.
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

		computeDimensions(bitResolution);

		this.precision = precision;
	}

	@Override
	protected BigInteger hash(BufferedImage image, BigInteger hash) {
		FastPixel fp = FastPixel.create(ImageUtil.getScaledInstance(image, width, height));
		// Use data buffer for faster access

		int[][] lum = fp.getLuma();

		// Calculate the left to right gradient
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
		return hash;
	}

	/**
	 * Compute the dimension for the resize operation. We want to get to close to a
	 * quadratic images as possible to counteract scaling bias.
	 * 
	 * @param bitResolution the desired resolution
	 */
	private void computeDimensions(int bitResolution) {
		int dimension = (int) Math.round(Math.sqrt(bitResolution + 1));

		// width //height
		int normalBound = (dimension - 1) * (dimension);
		int higherBound = (dimension - 1) * (dimension + 1);

		this.width = dimension;
		this.height = dimension;

		if (higherBound < bitResolution) {
			this.width++;
			this.height++;
		} else {
			if (normalBound < bitResolution || (normalBound - bitResolution) > (higherBound - bitResolution)) {
				this.height++;
			}
		}

	}

	@Override
	protected int precomputeAlgoId() {
		// + 1 to ensure id is incompatible to earlier version
		return Objects.hash(getClass().getName(), height, width, this.precision.name()) * 31 + 1;
	}

}
