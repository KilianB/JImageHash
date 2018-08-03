package com.github.kilianB.hashAlgorithms;

import java.awt.image.BufferedImage;
import java.math.BigInteger;
import java.util.Objects;

import org.jtransforms.dct.DoubleDCT_2D;

import com.github.kilianB.matcher.Hash;

/**
 * Calculate a hash based on the frequency of an image using the DCT T2. This
 * algorithm provides a good accuracy and is robust to several image
 * transformations.
 * 
 * @author Kilian
 *
 */
public class PerceptiveHash extends HashingAlgorithm {

	/**
	 * Unique id identifying the algorithm and it's settings
	 */
	private final int algorithmId;
	/**
	 * The height and width of the scaled instance used to compute the hash
	 */
	private final int height, width;

	/**
	 * 
	 * @param bitResolution
	 *            The bit resolution specifies the final length of the generated
	 *            hash. A higher resolution will increase computation time and space
	 *            requirement while being able to track finer detail in the image.
	 *            Be aware that a high key is not always desired.
	 */
	public PerceptiveHash(int bitResolution) {
		super(bitResolution);

		int dimension = (int) Math.round(Math.sqrt(bitResolution));
		this.width = dimension * 4;
		this.height = dimension * 4;
		// String and int hashes stays consistent throughout different JVM invocations.
		algorithmId = Objects.hash(getClass().getName(), this.bitResoluation);
	}

	@Override
	public Hash hash(BufferedImage image) {
		BufferedImage transformed = getScaledInstance(image, width, height);

		double[][] lum = new double[width][height];

		final double redFactor = 299d / 1000;
		final double greenFactor = 587d / 1000;
		final double blueFactor = 114d / 1000;

		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				int pixel = transformed.getRGB(x, y);
				// lum
				lum[x][y] = redFactor * ((pixel >> 16) & 0xFF) + greenFactor * ((pixel >> 8) & 0xFF)
						+ blueFactor * (pixel & 0xFF);
			}
		}

		DoubleDCT_2D dct = new DoubleDCT_2D(width, height);

		dct.forward(lum, false);

		// Average value of the (topmost) YxY low frequencies. Skip the first column as
		// it might be too dominant. Solid color e.g.
		// TODO DCT walk dow in a triangular motion. Skipping the entire edge neglects
		// several important frequencies. Maybe just skip
		// just the upper corner.
		double avg = 0;

		// Take a look at a forth of the pixel matrix. The lower right corner does not
		// yield much information.
		int subWidth = (int) (width / 4d);
		int count = subWidth * subWidth;

		// calculate the averge of the dct
		for (int i = 1; i < subWidth + 1; i++) {
			for (int j = 1; j < subWidth + 1; j++) {
				avg += lum[i][j] / count;
			}
		}

		BigInteger hash = BigInteger.ONE;
		for (int i = 1; i < subWidth + 1; i++) {
			for (int j = 1; j < subWidth + 1; j++) {

				if (lum[i][j] < avg) {
					hash = hash.shiftLeft(1);
				} else {
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
