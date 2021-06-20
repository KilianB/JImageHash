package dev.brachtendorf.jimagehash.hashAlgorithms;

import java.awt.image.BufferedImage;
import java.math.BigInteger;
import java.util.Objects;

import dev.brachtendorf.graphics.FastPixel;

/**
 * Calculate a hash based on iterative application of a haar wavelet on the
 * luminosity values of the input image.
 * <p>
 * The wavelet hashs detection performance usually resides in between the
 * AverageHash and perceptive hash
 * 
 * @author Kilian
 * @since 3.0.0
 */
public class WaveletHash extends HashingAlgorithm {

	private static final long serialVersionUID = -2259243765925949874L;

	/**
	 * The width of the rescaled image
	 */
	private int width;
	/**
	 * The height of the rescale image
	 */
	private int height;

	/**
	 * The number of times to feed the data back into the wavlet function
	 */
	private int cycles;

	/**
	 * @param bitResolution The bit resolution specifies the final length of the
	 *                      generated hash. A higher resolution will increase
	 *                      computation time and space requirement while being able
	 *                      to track finer detail in the image. Be aware that a high
	 *                      key is not always desired.
	 * @param cycles        the number of times to execute the wavelet on the data
	 *                      before calculating the hash. Increasing this number
	 *                      usually leads to more distinct hash values with
	 *                      diminishing returns for higher cycle counts. 3 or 4 is a
	 *                      good starting value.
	 *                      <p>
	 *                      This parameter heavily influences the computational
	 *                      complexity of this algorithm as each additional cycles
	 *                      requires a 4 times increase in the matrix input size.
	 */
	public WaveletHash(int bitResolution, int cycles) {
		super(bitResolution);

		this.cycles = cycles;
		width = (int) Math.ceil(Math.sqrt(bitResolution * Math.pow(4, cycles)));
		height = width;
	}

	@Override
	protected BigInteger hash(BufferedImage image, HashBuilder hashBuilder) {

		// Rescale
		FastPixel fp = createPixelAccessor(image, width, height);

		int[][] luma = fp.getLuma();

		// Compute wavelet

		double[][] transformed = doHaar2DFWTransform(luma, cycles);

		// System.out.println(ArrayUtil.deepToStringFormatted(transformed));

		double avg = 0;

		int subSpace = (int) Math.sqrt(this.bitResolution);
		int subSquared = subSpace * subSpace;

		for (int x = 0; x < subSpace; x++) {
			for (int y = 0; y < subSpace; y++) {
				// Gradient of wavle
				avg += transformed[x][y] / subSquared;
			}
		}

		for (int x = 0; x < subSpace; x++) {
			for (int y = 0; y < subSpace; y++) {
				if (transformed[x][y] > avg) {
					hashBuilder.prependOne();
				} else {
					hashBuilder.prependZero();
				}
			}
		}

		// Lets do only 1 cycle for now
		return hashBuilder.toBigInteger();
	}

	// Code taken and modified from
	// https://www.jeejava.com/haar-wavelet-transform-using-java/
	public static double[][] doHaar2DFWTransform(int[][] pixels, int cycles) {
		int w = pixels[0].length;
		int h = pixels.length;

		double[][] ds = new double[h][w];
		double[][] tempds = new double[h][w];
		for (int i = 0; i < pixels.length; i++) {
			for (int j = 0; j < pixels[0].length; j++) {
				ds[i][j] = pixels[i][j];
			}
		}
		for (int i = 0; i < cycles; i++) {
			w /= 2;
			for (int j = 0; j < h; j++) {
				for (int k = 0; k < w; k++) {
					double a = ds[j][2 * k];
					double b = ds[j][2 * k + 1];
					double add = a + b;
					double sub = a - b;
					double avgAdd = add / 2;
					double avgSub = sub / 2;
					tempds[j][k] = avgAdd;
					tempds[j][k + w] = avgSub;
				}
			}
			for (int j = 0; j < h; j++) {
				for (int k = 0; k < w; k++) {
					ds[j][k] = tempds[j][k];
					ds[j][k + w] = tempds[j][k + w];
				}
			}
			h /= 2;
			for (int j = 0; j < w; j++) {
				for (int k = 0; k < h; k++) {
					double a = ds[2 * k][j];
					double b = ds[2 * k + 1][j];
					double add = a + b;
					double sub = a - b;
					double avgAdd = add / 2;
					double avgSub = sub / 2;
					tempds[k][j] = avgAdd;
					tempds[k + h][j] = avgSub;
				}
			}
			for (int j = 0; j < w; j++) {
				for (int k = 0; k < h; k++) {
					ds[k][j] = tempds[k][j];
					ds[k + h][j] = tempds[k + h][j];
				}
			}
		}
		return ds;

	}

//	private static int getHaarMaxCycles(int hw) {
//		if (hw == 0) {
//			return 0;
//		}
//		return (int) MathUtil.log(hw, 2);
//	}

	@Override
	protected int precomputeAlgoId() {
		return Objects.hash(width, height, cycles);
	}

	@Override
	public String toString() {
		return "WaveletHash [" + bitResolution + "," + cycles + "]";
	}

}
