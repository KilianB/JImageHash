package com.github.kilianB.hashAlgorithms;

import java.awt.image.BufferedImage;
import java.math.BigInteger;
import java.util.Objects;

import com.github.kilianB.MathUtil;
import com.github.kilianB.graphics.ImageUtil;
import com.github.kilianB.graphics.ImageUtil.FastPixel;
import com.github.kilianB.matcher.Hash;

/**
 * Image Hash on HOG feature descriptor
 * 
 * @author Kilian
 *
 */
public class HogHash extends HashingAlgorithm {

	/*
	 * Hog is calculated following the approach outlined in "Histograms of Oriented
	 * Gradients for Human Detection" by NavneetDalal and Bill Triggs
	 * http://lear.inrialpes.fr/people/triggs/pubs/Dalal-cvpr05.pdf
	 */

	private static final long serialVersionUID = 5353878339786219609L;
	
	private final int algorithmId;

	/**
	 * @param bitResolution
	 */
	public HogHash(int bitResolution) {
		super(bitResolution);

		algorithmId = Objects.hash(getClass().getName(), bitResolution);
	}

	@Override
	public Hash hash(BufferedImage image) {

		// Fit direction to 0 - 180 instead of
		boolean unsignedGradients = true;

		int width = 64;
		int height = 64;

		// How many pixels does a cell have in each direction
		int cellWidth = 8;

		int xCells = width / cellWidth;
		int yCells = height / cellWidth;

		// How many gradient histogram binds does each cell contain?
		int numBins = 4;

		//
		double binFac = (180 - 180d / (numBins)) / (numBins - 1);

		// 0 - 180 (180 wrap around to 0 again)

		assert width % cellWidth == 0;
		assert height % cellWidth == 0;

		BufferedImage bi = ImageUtil.getScaledInstance(image, width, height);
		FastPixel fp = new FastPixel(bi);

		int[][] lum = fp.getLuma();

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

		// 1 Compute hisogramm

		// Vertical Gradient

		// Point 2D represents the cell position, Key: Map represents the bins

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

		// block normalization 2 x 2 hist shifting window

		double[][][] normalizedHog = new double[xCells][yCells][numBins];

		// Create hash
		// Padding bit
		BigInteger hash = BigInteger.ONE;

		for (int xCell = 0; xCell < xCells; xCell++) {
			// Construct intermediary vector
			for (int yCell = 0; yCell < yCells; yCell++) {
				// TODO avoid overflow
				int vectorLength = 0;
				
				int lastMax = Integer.MIN_VALUE;
				
				int bins = numBins;
				int maxIndex = -1;
				// Same bin
				for (int bin = 0; bin < numBins; bin++) {
					vectorLength += (hog[xCell][yCell][bin] * hog[xCell][yCell][bin]);
					
					if(hog[xCell][yCell][bin] > lastMax) {
						lastMax =hog[xCell][yCell][bin];
						maxIndex = bin;
					}
				}

				for (int bin = 0; bin < numBins; bin++) {
					if (bin == maxIndex) {
						hash = hash.shiftLeft(1);
					} else {
						hash = hash.shiftLeft(1).add(BigInteger.ONE);
					}
				}

//
//				// Right bin
//				if (xCell < xCells - 1) {
//					for (int bin = 0; bin < numBins; bin++) {
//						vectorLength += (hog[xCell + 1][yCell][bin] * hog[xCell + 1][yCell][bin]);
////					}
//						bins += numBins;
//					}
////
////				// Lower bin
//					if (yCell < yCells - 1) {
//						for (int bin = 0; bin < numBins; bin++) {
//							vectorLength += (hog[xCell][yCell + 1][bin] * hog[xCell][yCell + 1][bin]);
//						}
//
//						bins += numBins;
//						// Lower right
//						if (xCell < xCells - 1) {
//							for (int bin = 0; bin < numBins; bin++) {
//								vectorLength += (hog[xCell + 1][yCell + 1][bin] * hog[xCell + 1][yCell + 1][bin]);
//							}
//							bins += numBins;
//						}
//					}
//					double normFactor = Math.sqrt(vectorLength);
//
//					// System.out.println(vectorLength + " " + normFactor);
//
//					// Here we deviate a little bit from the traditional hog. We don't need the 3k++
//					// features descriptors.
//					// TODO in this case normalization is unnecessary if we simply compare
//					for (int bin = 0; bin < numBins; bin++) {
//						normalizedHog[xCell][yCell][bin] = ((hog[xCell][yCell][bin] / normFactor));
//
//						if (bin > 0) {
//							if (normalizedHog[xCell][yCell][bin] < normalizedHog[xCell][yCell][bin - 1]) {
//								hash = hash.shiftLeft(1);
//							} else {
//								hash = hash.shiftLeft(1).add(BigInteger.ONE);
//							}
//						}
//					}
//				}
			}
		}

//		BufferedImage bImage = new BufferedImage(width, height, 0x2);
//		Graphics2D g2 = (Graphics2D) bImage.getGraphics();
//
//		g2.drawImage(bi, 0, 0, width, height, null);
//
//		g2.setPaint(Color.red);
//
//		g2.setFont(new Font("TimesRoman", Font.PLAIN, 5));
//
//		g2.setStroke(new BasicStroke(1));
//
//		for (int xCell = 0; xCell < xCells; xCell++) {
//			// Construct intermediary vector
//			for (int yCell = 0; yCell < yCells; yCell++) {
//				// System.out.println(Arrays.toString(normalizedHog[xCell][yCell]));
//
//				// Pixel
//
//				for (int bin = 0; bin < numBins; bin++) {
//					int angle = (int) (bin * 180d / numBins);
//
//					// Center
//					int x = xCell * cellWidth + cellWidth / 2;
//					int y = yCell * cellWidth + cellWidth / 2;
//
//					g2.setPaint(ColorUtil.getContrastColor(new Color(fp.getRGB(x, y))));
//
//					// g2.setPaint(Color.WHITE);
//
//					int length = (int) (cellWidth * normalizedHog[xCell][yCell][bin]);
//
//					int startX = (int) (x - (Math.cos(Math.toRadians(angle)) * length));
//					int startY = (int) (y - (Math.sin(Math.toRadians(angle)) * length));
//
//					int endX = (int) (x + (Math.cos(Math.toRadians(angle)) * length));
//					int endY = (int) (y + (Math.sin(Math.toRadians(angle)) * length));
//
//					g2.drawLine(startX, startY, endX, endY);
//
//				}
//			}
//		}
//
//		g2.dispose();
//
//		try {
//			ImageIO.write(bImage, "png", new File("HogTest.png"));
//		} catch (IOException e) {
//			e.printStackTrace();
//		}

		return new Hash(hash, algorithmId);
	}

	@Override
	public int algorithmId() {
		return algorithmId;
	}
}
