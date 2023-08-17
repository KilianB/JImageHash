package interpolator.utils.sorter;

import dev.brachtendorf.graphics.FastPixel;
import dev.brachtendorf.jimagehash.hash.ColorMoments;
import dev.brachtendorf.jimagehash.hashAlgorithms.HashBuilder;
import dev.brachtendorf.jimagehash.hashAlgorithms.HashingAlgorithm;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Objects;

public class ColorMomentsHash extends HashingAlgorithm {

	private static final long serialVersionUID = -5234612717498362659L;

	/**
	 * The height and width of the scaled instance used to compute the hash
	 */
	protected int height, width, area;

	public ColorMomentsHash(int bitResolution) {
		super(bitResolution);
		/*
		 * Figure out how big our resized image has to be in order to create a hash with
		 * approximately bitResolution bits while trying to stay as squared as possible
		 * to not introduce bias via stretching or shrinking the image asymmetrically.
		 */
		computeDimension(bitResolution);
	}


	public ColorMoments hash2(File imageFile) throws IOException {
		return hash2(imageFile, new int[]{1, 2, 3});
	}

	public ColorMoments hash2(File imageFile, int[] weights) throws IOException {
		BufferedImage image = ImageIO.read(imageFile);
		FastPixel fp = createPixelAccessor(image, width, height);

		int[][]    hue = getHue(fp);
		double[][] sat = getSaturation(fp);
		int[][]    val = getValue(fp);

		double[] meanMoments = new double[]{
			weights[0] * mean(hue),
			weights[0] * mean(sat),
			weights[0] * mean(val)
		};
		double[] stdDevMoments = new double[]{
			weights[1] * standardDeviation(hue, meanMoments[0]),
			weights[1] * standardDeviation(sat, meanMoments[1]),
			weights[1] * standardDeviation(val, meanMoments[2])
		};
		double[] skewnessMoments = new double[]{
			weights[2] * skewness(hue, meanMoments[0]),
			weights[2] * skewness(sat, meanMoments[1]),
			weights[2] * skewness(val, meanMoments[2])
		};

		return new ColorMoments(meanMoments, stdDevMoments, skewnessMoments);
	}

	@Override
	protected BigInteger hash(BufferedImage image, HashBuilder hash) {
		FastPixel fp = createPixelAccessor(image, width, height);

		int[][]    hue = getHue(fp);
		double[][] sat = getSaturation(fp);
		int[][]    val = getValue(fp);

		double[] meanMoments = new double[]{
			mean(hue),
			mean(sat),
			mean(val)
		};
		double[] stdDevMoments = new double[]{
			standardDeviation(hue, meanMoments[0]),
			standardDeviation(sat, meanMoments[1]),
			standardDeviation(val, meanMoments[2])
		};
		double[] skewnessMoments = new double[]{
			skewness(hue, meanMoments[0]),
			skewness(sat, meanMoments[1]),
			skewness(val, meanMoments[2])
		};

		computeHash(hash, 3, meanMoments);
		computeHash(hash, 1, stdDevMoments);
		computeHash(hash, 0, skewnessMoments);

		return hash.toBigInteger();
	}

	public void computeHash(HashBuilder hash, int weight, double[] moments) {
		for (double moment: moments) {
			String binary = Long.toBinaryString(Double.doubleToRawLongBits(moment))
				.substring(0, 32)
				.repeat(weight);
			for (char c : binary.toCharArray()) {
				if (c == '0')
					hash.prependZero();
				else
					hash.prependOne();
			}
		}
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
		this.area = this.height * this.width;
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

	public int[][] getHue(FastPixel fp) {
		int[][] blueArr = fp.getBlue();
		int[][] greenArr = fp.getGreen();
		int[][] redArr = fp.getRed();

		int[][] hueArr = new int[width][height];

		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				int blue = blueArr[x][y];
				int green = greenArr[x][y];
				int red = redArr[x][y];

				int min = Math.min(blue, Math.min(green, red));
				int max = Math.max(blue, Math.max(green, red));

				if (max == min) {
					hueArr[x][y] = 0;
					continue;
				}

				double range = max - min;

				double h;
				if (red == max) {
					h = 60 * ((green - blue) / range);
				} else if (green == max) {
					h = 60 * (2 + (blue - red) / range);
				} else {
					h = 60 * (4 + (red - green) / range);
				}

				int hue = (int) Math.round(h);

				if (hue < 0)
					hue += 360;

				hueArr[x][y] = hue;
			}
		}

		return hueArr;
	}

	public double[][] getSaturation(FastPixel fp) {
		int[][] blueArr = fp.getBlue();
		int[][] greenArr = fp.getGreen();
		int[][] redArr = fp.getRed();

		double[][] satArr = new double[width][height];

		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				int blue = blueArr[x][y];
				int green = greenArr[x][y];
				int red = redArr[x][y];

				int max = Math.max(blue, Math.max(green, red));
				if (max == 0) {
					satArr[x][y] = 0;
					continue;
				}
				int min = Math.min(blue, Math.min(green, red));

				satArr[x][y] = ((max - min) / (double) max);
			}
		}

		return satArr;
	}

	public int[][] getValue(FastPixel fp) {
		int[][] blueArr = fp.getBlue();
		int[][] greenArr = fp.getGreen();
		int[][] redArr = fp.getRed();

		int[][] valArr = new int[width][height];

		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				int blue = blueArr[x][y];
				int green = greenArr[x][y];
				int red = redArr[x][y];

				int max = Math.max(blue, Math.max(green, red));

				valArr[x][y] = max;
			}
		}

		return valArr;
	}

	public double skewness(final double[][] arr, final double mean) {
		double accum1 = 0.0;
		double accum2 = 0.0;
		double accum3 = 0.0;
		for (int i = 0; i < width; i++) {
			for (int j = 0; j < height; j++) {
				final double d = arr[i][j] - mean;
				accum1 += d;
				accum2 += d * d;
				accum3 += d * d * d;
			}
		}
		final double variance = (accum2 - (accum1 * accum1 / area)) / (area - 1);
		accum3 /= variance * Math.sqrt(variance);

		// Get N
		double n0 = area;

		// Calculate skewness
		return (n0 / ((n0 - 1) * (n0 - 2))) * accum3;
	}

	public double skewness(final int[][] arr, final double mean) {
		double accum1 = 0.0;
		double accum2 = 0.0;
		double accum3 = 0.0;
		for (int i = 0; i < width; i++) {
			for (int j = 0; j < height; j++) {
				final double d = arr[i][j] - mean;
				accum1 += d;
				accum2 += d * d;
				accum3 += d * d * d;
			}
		}
		final double variance = (accum2 - (accum1 * accum1 / area)) / (area - 1);
		accum3 /= variance * Math.sqrt(variance);

		// Get N
		double n0 = area;

		// Calculate skewness
		return (n0 / ((n0 - 1) * (n0 - 2))) * accum3;
	}

	public double standardDeviation(final double[][] arr, final double mean) {
		double stdDev = 0;
		for (int i = 0; i < width; i++) {
			for (int j = 0; j < height; j++) {
				stdDev += Math.pow(arr[i][j] - mean, 2);
			}
		}
		return Math.sqrt(stdDev / area);
	}

	public double standardDeviation(final int[][] arr, final double mean) {
		double stdDev = 0;
		for (int i = 0; i < width; i++) {
			for (int j = 0; j < height; j++) {
				stdDev += Math.pow(arr[i][j] - mean, 2);
			}
		}
		return Math.sqrt(stdDev / area);
	}

	public double mean(final double[][] arr) {
		double sum = 0;
		for (int i = 0; i < width; i++) {
			for (int j = 0; j < height; j++) {
				sum += arr[i][j];
			}
		}
		return sum / area;
	}

	public double mean(final int[][] arr) {
		int sum = 0;
		for (int i = 0; i < width; i++) {
			for (int j = 0; j < height; j++) {
				sum += arr[i][j];
			}
		}
		return sum / area;
	}
}
