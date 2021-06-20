package dev.brachtendorf.jimagehash.hashAlgorithms.experimental;

import java.awt.image.BufferedImage;
import java.math.BigInteger;

import dev.brachtendorf.graphics.FastPixel;
import dev.brachtendorf.jimagehash.hashAlgorithms.HashBuilder;


/**
 * 
 * 
 * @author Kilian
 * @deprecated not ready to use yet
 * @since 2.0.0
 */
public class HogHashAngularEncoded extends HogHash {

	private static final long serialVersionUID = 8174228394380201299L;

	/**
	 * * Create a hog hasher with the target bit resolution.
	 * 
	 * Default values of 4 bins per cell (0°,45°,90°,135°) and a cell width of 2
	 * pixels per cell are assumed.
	 * 
	 * @param bitResolution the bit resolution of the final hash. The hash will be
	 *                      at least the specified bits but may be bigger due to
	 *                      algorithmic constraints. The best attempt is made to
	 *                      return a hash with the given number of bits.
	 */
	public HogHashAngularEncoded(int bitResolution) {
		super(bitResolution);
	}

	/**
	 * Create a hog hasher with parameters specific to the hog feature detection
	 * algorithm.
	 * 
	 * The actual hash will have a key length of
	 * <code>(width / cellWidth) * (height / cellWidth) * numBins</code>
	 * 
	 * @param width     of the rescaled image
	 * @param height    of the rescaled image
	 * @param cellWidth the width and height of sub cell. For each cell a gradient
	 *                  will be computed. The cell width has to be a divisor of
	 *                  width AND height!
	 * @param numBins   the number of bins per cell. The number of bins represent
	 *                  the angular granularity the gradients will be sorted into.
	 *                  The gradients will be sorted into buckets equivalent of the
	 *                  size of 180°/numBins
	 * @throws IllegalArgumentException if width or height can't be divided by
	 *                                  cellWidth or if any of the arguments is smaller or equal 0
	 */
	public HogHashAngularEncoded(int width, int height, int cellWidth, int numBins) {
		super(width, height, cellWidth, numBins);
	}

	@Override
	protected BigInteger hash(BufferedImage image, HashBuilder hash) {
		FastPixel fp = createPixelAccessor(image, width, height);

		int[][] lum = fp.getLuma();

		// 1 Compute hisogramm

		int[][][] hog = computeHogFeatures(lum);

		double binAverage[] = new double[numBins];
		// Compute average of each bin

		int cells = xCells * yCells;

		for (int xCell = 0; xCell < xCells; xCell++) {
			// Construct intermediary vector
			for (int yCell = 0; yCell < yCells; yCell++) {
				for (int bin = 0; bin < numBins; bin++) {
					binAverage[bin] += hog[xCell][yCell][bin] / cells;
				}
			}
		}

		for (int xCell = 0; xCell < xCells; xCell++) {
			// Construct intermediary vector
			for (int yCell = 0; yCell < yCells; yCell++) {
				for (int bin = 0; bin < numBins; bin++) {
					if (hog[xCell][yCell][bin] > binAverage[bin]) {
						hash.prependZero();
					} else {
						hash.prependOne();
					}
				}
			}
		}

		return hash.toBigInteger();
	}

}
