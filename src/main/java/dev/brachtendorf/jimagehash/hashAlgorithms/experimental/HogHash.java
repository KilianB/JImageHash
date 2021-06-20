package dev.brachtendorf.jimagehash.hashAlgorithms.experimental;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Objects;

import javax.imageio.ImageIO;

import dev.brachtendorf.ArrayUtil;
import dev.brachtendorf.MathUtil;
import dev.brachtendorf.Require;
import dev.brachtendorf.graphics.ColorUtil;
import dev.brachtendorf.graphics.FastPixel;
import dev.brachtendorf.jimagehash.Experimental;
import dev.brachtendorf.jimagehash.hashAlgorithms.HashBuilder;
import dev.brachtendorf.jimagehash.hashAlgorithms.HashingAlgorithm;

/**
 * Image Hash on HOG feature descriptor. Not ready yet. Most likely use a very
 * high bit resolution similar to how hog feature descriptors are actually used
 * 
 * Requires at least 64 bit to produce reasonable results
 * 
 * @deprecated not ready to use yet
 * @author Kilian
 * @since 2.0.0
 */
@Experimental("Test")
public class HogHash extends HashingAlgorithm {

	/*
	 * Hog is calculated following the approach outlined in "Histograms of Oriented
	 * Gradients for Human Detection" by NavneetDalal and Bill Triggs
	 * http://lear.inrialpes.fr/people/triggs/pubs/Dalal-cvpr05.pdf
	 */

	protected static final long serialVersionUID = 5353878339786219609L;

	/** The width of the rescaled image */
	protected int width;
	/** The height of the rescaled image */
	protected int height;
	/** The cell width/height used to compute the features */
	protected int cellWidth;

	/** The number of cells in x dimension */
	protected int xCells;
	/** The number of cells in y dimension */
	protected int yCells;
	/** The number of angle bins per cell */
	protected int numBins;

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
	public HogHash(int width, int height, int cellWidth, int numBins) {
		super(numBins);

		assert width % cellWidth == 0;
		assert height % cellWidth == 0;

		if (width % cellWidth != 0 || height % cellWidth != 0) {
			throw new IllegalArgumentException("Height and width have to be a multiple of cellWidth");
		}

		this.width = Require.positiveValue(width);
		this.height = Require.positiveValue(height);
		this.cellWidth = Require.positiveValue(cellWidth);
		this.numBins = Require.positiveValue(numBins);

		this.xCells = width / cellWidth;
		this.yCells = height / cellWidth;
	}

	/**
	 * Create a hog hasher with the target bit resolution.
	 * 
	 * Default values of 4 bins per cell (0°,45°,90°,135°) and a cell width of 2
	 * pixels per cell are assumed.
	 * 
	 * @param bitResolution the bit resolution of the final hash. The hash will be
	 *                      at least the specified bits but may be bigger due to
	 *                      algorithmic constraints. The best attempt is made to
	 *                      return a hash with the given number of bits.
	 */
	public HogHash(int bitResolution) {
		super(bitResolution);

		if (bitResolution <= 8) {
			throw new IllegalArgumentException("HogHash is only defined for bit resolution > 8");
		}

		/*
		 * actualBitResolution = width/cellWidth * height/cellHeight * bins >=
		 * bitResolution
		 * 
		 * We assume symmetry and bitRes shall be at least as big as given by the
		 * contract
		 * 
		 * bitRes <= width/cellWidth^2 * bins Additional Constraint : width is divisible
		 * by cellWidth
		 */

		// Directional bins
		this.numBins = 4;

		/*
		 * Solve Math.sqrt(bitResolution/bins) >= width/cellWidth;
		 * Math.sqrt(bitResolution/bins) * cellWidth >= width
		 */
		// Lets start with a cellWidth of 2 therefore width needs to be even

		this.cellWidth = 2;
		int width = (int) Math.round((Math.sqrt(bitResolution / numBins) * cellWidth));

		if (width % 2 != 0) {
			width--;
		}

		int height = width;

		double estimatedLength = width / cellWidth * height / cellWidth * numBins;

		// Allow for small deviation in width/height

		if (estimatedLength < bitResolution) {
			width += 2;
		}
		estimatedLength = width / cellWidth * height / cellWidth * numBins;
		if (estimatedLength < bitResolution) {
			height += 2;
		}
		this.width = width;
		this.height = height;

		this.xCells = width / cellWidth;
		this.yCells = height / cellWidth;
	}

	@Override
	protected BigInteger hash(BufferedImage image, HashBuilder hash) {
		FastPixel fp = createPixelAccessor(image, width, height);

		int[][] lum = fp.getLuma();

		// 1 Compute hisogramm
		// Vertical Gradient
		// Point 2D represents the cell position, Key: Map represents the bins
		int[][][] hog = computeHogFeatures(lum);

		// Block normalization 2 x 2 hist kernel "shifting window"
		//double[][][] normalizedHog = blockNormalization(hog);

		// toImage(new File("HogNormalized" + image.hashCode() + ".png"), image,
		// Color.RED, normalizedHog);
		// toImage(new File("Hog" + image.hashCode() + ".png"), image, Color.RED, hog);

		for (int xCell = 0; xCell < xCells; xCell++) {
			// Construct intermediary vector
			for (int yCell = 0; yCell < yCells; yCell++) {
				int lastMax = Integer.MIN_VALUE;

				int maxIndex = -1;
				// Same bin
				for (int bin = 0; bin < numBins; bin++) {
					if (hog[xCell][yCell][bin] > lastMax) {
						lastMax = hog[xCell][yCell][bin];
						maxIndex = bin;
					}
				}
				for (int bin = 0; bin < numBins; bin++) {
					if (bin == maxIndex) {
						hash.prependZero();
					} else {
						hash.prependOne();
					}
				}
			}
		}
		return hash.toBigInteger();
	}

	protected int[][][] computeHogFeatures(int[][] lum) {

		double binFac = (180 - 180d / (numBins)) / (numBins - 1);
		// 0 - 180 (180 wrap around to 0 again)

		double magnitude[][] = new double[width][height];
		double direction[][] = new double[width][height];

		// 0 Compute magnitude and direction from horizontal and vertical gradients.
		// with 101 kernels
		for (int x = 1; x < width - 1; x++) {
			for (int y = 1; y < height - 1; y++) {
				int hGradient = (lum[x + 1][y] - lum[x - 1][y]);
				int vGradient = (lum[x][y - 1] - lum[x][y + 1]);

				magnitude[x][y] = Math.sqrt(hGradient * hGradient + vGradient * vGradient);

				if (hGradient == 0) {
					// Arctan lim infinity = 90°
					direction[x][y] = 180;// °
				} else {
					direction[x][y] = Math.toDegrees(Math.atan(vGradient / (double) hGradient)) + 90;
				}
			}
		}

		int[][][] hog = new int[xCells][yCells][numBins];

		for (int x = 0; x < width; x++) {

			int xCellIndex = x / cellWidth;

			for (int y = 0; y < height; y++) {

				int yCellIndex = y / cellWidth;

				double binF = direction[x][y] / binFac;
				int bin = (int) binF;
				double nextBucketShare = MathUtil.getFractionalPart(binF);
				double currentShare = 1 - nextBucketShare;

				if (bin != numBins) {
					hog[xCellIndex][yCellIndex][bin] += (currentShare * magnitude[x][y]);
					if (bin == (numBins - 1)) {
						// Wrap around
						hog[xCellIndex][yCellIndex][0] += (nextBucketShare * magnitude[x][y]);
					} else {
						// Put proportionally in next bucket
						hog[xCellIndex][yCellIndex][bin + 1] += (nextBucketShare * magnitude[x][y]);
					}
				} else {
					// Wrap around. 180 ° == 0 °
					hog[xCellIndex][yCellIndex][0] += (currentShare * magnitude[x][y]);
				}
			}
		}
		return hog;
	}

	protected double[][][] blockNormalization(int[][][] hog) {

		// Here we don't use an overlapping window but rather a kernel again

		double[][][] normalizedHog = new double[xCells][yCells][numBins];

		for (int xCell = 0; xCell < xCells; xCell++) {
			// Construct intermediary vector
			for (int yCell = 0; yCell < yCells; yCell++) {
				int vectorLength = 0;

				// Same bin
				for (int bin = 0; bin < numBins; bin++) {
					vectorLength += (hog[xCell][yCell][bin] * hog[xCell][yCell][bin]);
				}

				// Lower bin
				if (yCell < yCells - 1) {
					for (int bin = 0; bin < numBins; bin++) {
						vectorLength += (hog[xCell][yCell + 1][bin] * hog[xCell][yCell + 1][bin]);
					}
				} else { // Corner case. If we are at the bottom take a look at the higher bin
					if (yCell - 1 > 0) {
						for (int bin = 0; bin < numBins; bin++) {
							vectorLength += (hog[xCell][yCell - 1][bin] * hog[xCell][yCell - 1][bin]);
						}
					}

				}

				// Right bin
				if (xCell < xCells - 1) {
					for (int bin = 0; bin < numBins; bin++) {
						vectorLength += (hog[xCell + 1][yCell][bin] * hog[xCell + 1][yCell][bin]);
					}
					// Lower right
					if (yCell < yCells - 1) {
						for (int bin = 0; bin < numBins; bin++) {
							vectorLength += (hog[xCell + 1][yCell + 1][bin] * hog[xCell + 1][yCell + 1][bin]);
						}
					}
				} else {
					// Corner case
					if (xCell - 1 > 0) {
						for (int bin = 0; bin < numBins; bin++) {
							vectorLength += (hog[xCell - 1][yCell][bin] * hog[xCell - 1][yCell][bin]);
						}
						// Lower right
						if (yCell - 1 > 0) {
							for (int bin = 0; bin < numBins; bin++) {
								vectorLength += (hog[xCell - 1][yCell - 1][bin] * hog[xCell - 1][yCell - 1][bin]);
							}
						}
					}

				}

				double normFactor = Math.sqrt(vectorLength);
				// System.out.println(vectorLength + " " + normFactor);
				// Here we deviate a little bit from the traditional hog. We don't need the 3k++
				// features descriptors.
				for (int bin = 0; bin < numBins; bin++) {
					normalizedHog[xCell][yCell][bin] = ((hog[xCell][yCell][bin] / normFactor));
				}
			}
		}

		return normalizedHog;

	}

	/**
	 * Create a visual representation of the hog features. Debug method
	 * 
	 * @param outputFile 	The file to save the image to
	 * @param originalImage	The original image used to create the features. Used as background
	 * @param gradientColor	Color of the vectors the vectors
	 * @param hog	hog features to draw
	 */
	protected void toImage(File outputFile, BufferedImage originalImage, Color gradientColor, int[][][] hog) {

		// Normalize over the entire image

		int globalMaximum = Integer.MIN_VALUE;

		for (int xCell = 0; xCell < xCells; xCell++) {
			for (int yCell = 0; yCell < yCells; yCell++) {
				int max = ArrayUtil.maximum(hog[xCell][yCell]);
				if (max > globalMaximum) {
					globalMaximum = max;
				}
			}
		}

		// Compute dimensions for the unscaled image!
		int width = originalImage.getWidth();
		int height = originalImage.getHeight();

		// Original
		// this.xCells = width / cellWidth;
		// this.yCells = height / cellWidth;

		// Rescale

		double cellWidth = width / this.xCells;
		double cellHeight = height / this.yCells;

		BufferedImage bImage = new BufferedImage(width, height, 0x2);
		Graphics2D g2 = (Graphics2D) bImage.getGraphics();

		g2.drawImage(originalImage, 0, 0, width, height, null);

		g2.setPaint(gradientColor);

		// g2.setFont(new Font("TimesRoman", Font.PLAIN, 5));

		g2.setStroke(new BasicStroke(1));

		for (int xCell = 0; xCell < xCells; xCell++) {

			// Construct intermediary vector
			for (int yCell = 0; yCell < yCells; yCell++) {
				// System.out.println(Arrays.toString(normalizedHog[xCell][yCell]));

				// Pixel

				for (int bin = 0; bin < numBins; bin++) {
					int angle = (int) (bin * 180d / numBins);

					// Center
					int x = (int) (xCell * cellWidth + cellWidth / 2);
					int y = (int) (yCell * cellHeight + cellHeight / 2);

					g2.setPaint(ColorUtil.getContrastColor(new Color(originalImage.getRGB(x, y))));

					// Normalize to the global max
					double lengthX = (cellWidth * hog[xCell][yCell][bin]) / globalMaximum;
					double lengthY = (cellHeight * hog[xCell][yCell][bin]) / globalMaximum;

					int startX = (int) (x - (Math.cos(Math.toRadians(angle)) * lengthX));
					int startY = (int) (y - (Math.sin(Math.toRadians(angle)) * lengthY));

					int endX = (int) (x + (Math.cos(Math.toRadians(angle)) * lengthX));
					int endY = (int) (y + (Math.sin(Math.toRadians(angle)) * lengthY));

					g2.drawLine(startX, startY, endX, endY);
				}
			}
		}

		g2.dispose();

		try {
			ImageIO.write(bImage, "png", outputFile);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Create a visual representation of the normalized hog features. Debug method
	 * 
	 * @param outputFile 	The file to save the image to
	 * @param originalImage	The original image used to create the features. Used as background
	 * @param gradientColor	Color of the vectors the vectors
	 * @param normalizedHog	hog features to draw
	 */
	protected void toImage(File outputFile, BufferedImage originalImage, Color gradientColor,
			double[][][] normalizedHog) {

		// Compute dimensions for the unscaled image!
		int width = originalImage.getWidth();
		int height = originalImage.getHeight();

		// Original
		// this.xCells = width / cellWidth;
		// this.yCells = height / cellWidth;

		// Rescale

		double cellWidth = width / this.xCells;
		double cellHeight = height / this.yCells;

		BufferedImage bImage = new BufferedImage(width, height, 0x2);
		Graphics2D g2 = (Graphics2D) bImage.getGraphics();

		g2.drawImage(originalImage, 0, 0, width, height, null);

		g2.setPaint(gradientColor);

		// g2.setFont(new Font("TimesRoman", Font.PLAIN, 5));

		g2.setStroke(new BasicStroke(1));

		for (int xCell = 0; xCell < xCells; xCell++) {

			// Construct intermediary vector
			for (int yCell = 0; yCell < yCells; yCell++) {
				// Pixel

				for (int bin = 0; bin < numBins; bin++) {
					int angle = (int) (bin * 180d / numBins);

					// Center
					int x = (int) (xCell * cellWidth + cellWidth / 2);
					int y = (int) (yCell * cellHeight + cellHeight / 2);

					g2.setPaint(ColorUtil.getContrastColor(new Color(originalImage.getRGB(x, y))));

					double lengthX = (cellWidth * normalizedHog[xCell][yCell][bin]) * 0.75d;
					double lengthY = (cellHeight * normalizedHog[xCell][yCell][bin]) * 0.75d;

					int startX = (int) (x - (Math.cos(Math.toRadians(angle)) * lengthX));
					int startY = (int) (y - (Math.sin(Math.toRadians(angle)) * lengthY));

					int endX = (int) (x + (Math.cos(Math.toRadians(angle)) * lengthX));
					int endY = (int) (y + (Math.sin(Math.toRadians(angle)) * lengthY));

					g2.drawLine(startX, startY, endX, endY);
				}
			}
		}

		g2.dispose();

		try {
			ImageIO.write(bImage, "png", outputFile);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public String toString() {
		return getClass().getSimpleName()+ " [numBins=" + numBins + "]";
	}

	@Override
	protected int precomputeAlgoId() {
		return Objects.hash("com.github.kilianB.hashAlgorithms.experimental."+getClass().getSimpleName(), width,height,cellWidth,numBins);
	}
}
