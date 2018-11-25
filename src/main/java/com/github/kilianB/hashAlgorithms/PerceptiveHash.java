package com.github.kilianB.hashAlgorithms;

import java.awt.image.BufferedImage;
import java.math.BigInteger;
import java.util.Objects;

import org.jtransforms.dct.DoubleDCT_2D;

import com.github.kilianB.graphics.ImageUtil;
import com.github.kilianB.graphics.ImageUtil.FastPixel;

/**
 * Calculate a hash based on the frequency of an image using the DCT T2. This
 * algorithm provides a good accuracy and is robust to several image
 * transformations.
 * 
 * @author Kilian
 *
 */
public class PerceptiveHash extends HashingAlgorithm {

	private static final long serialVersionUID = 8409228150836051697L;

	/**
	 * The height and width of the scaled instance used to compute the hash
	 */
	private int height, width;

	/**
	 * 
	 * @param bitResolution The bit resolution specifies the final length of the
	 *                      generated hash. A higher resolution will increase
	 *                      computation time and space requirement while being able
	 *                      to track finer detail in the image. Be aware that a high
	 *                      key is not always desired.
	 */
	public PerceptiveHash(int bitResolution) {
		super(bitResolution);
		computeDimensions(bitResolution);
	}

	@Override
	protected BigInteger hash(BufferedImage image, BigInteger hash) {
		FastPixel fp = new FastPixel(ImageUtil.getScaledInstance(image, width, height));

		int[][] lum = fp.getLuma();

		// int to double conversion ...
		double[][] lumAsDouble = new double[width][height];

		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				lumAsDouble[x][y] = lum[x][y] / 255d;
			}
		}

		DoubleDCT_2D dct = new DoubleDCT_2D(width, height);

		dct.forward(lumAsDouble, false);

		// Average value of the (topmost) YxY low frequencies. Skip the first column as
		// it might be too dominant. Solid color e.g.
		// TODO DCT walk down in a triangular motion. Skipping the entire edge neglects
		// several important frequencies. Maybe just skip
		// just the upper corner.
		double avg = 0;

		// Take a look at a forth of the pixel matrix. The lower right corner does not
		// yield much information.
		int subWidth = (int) (width / 4d);
		int subHeight = (int) (height / 4d);
		int count = subWidth * subHeight;

		// calculate the average of the dct
		for (int i = 1; i < subWidth + 1; i++) {
			for (int j = 1; j < subHeight + 1; j++) {
				avg += lumAsDouble[i][j] / count;
			}
		}

		for (int i = 1; i < subWidth + 1; i++) {
			for (int j = 1; j < subHeight + 1; j++) {

				if (lumAsDouble[i][j] < avg) {
					hash = hash.shiftLeft(1);
				} else {
					hash = hash.shiftLeft(1).add(BigInteger.ONE);
				}
			}
		}
		return hash;
	}

	private void computeDimensions(int bitResolution) {

		// bitRes = (width/4)^2;
		int dimension = (int) Math.round(Math.sqrt(bitResolution)) * 4;
		// width //height
		int normalBound = ((dimension / 4) * (dimension / 4));
		int higherBound = ((dimension / 4) * (dimension / 4 + 1));

		this.width = dimension;
		this.height = dimension;

		if (higherBound < bitResolution) {
			this.width++;
			this.height++;
		} else {
			if (normalBound < bitResolution || (normalBound - bitResolution) > (higherBound - bitResolution)) {
				this.height += 4;
			}
		}
	}

	@Override
	protected int precomputeAlgoId() {
		return Objects.hash(getClass().getName(),height,width) * 31 + 1;
	}
}
