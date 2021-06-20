package dev.brachtendorf.jimagehash.hashAlgorithms;

import java.awt.image.BufferedImage;
import java.math.BigInteger;
import java.util.Objects;


import dev.brachtendorf.graphics.FastPixel;
import dev.brachtendorf.jimagehash.hash.Hash;
import javafx.scene.paint.Color;

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
	 * <p>
	 * <b>implnote:</b> Be aware that changing the enum names will alter the
	 * algorithm id rendering generated keys unusable
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
	private int height;
	/**
	 * The height and width of the scaled instance used to compute the hash
	 */
	private int width;

	/**
	 * Precision used to calculate the hash
	 */
	private final Precision precision;

	/**
	 * 
	 * Create a difference hasher with the given settings. The bit resolution always
	 * corresponds to the simple precision value and will increase accordingly
	 * depending on the precision chosen.
	 * 
	 * <p>
	 * Tests have shown that a 64 bit simple precision hash usually performs better
	 * than a 32 bit double precision hash.
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
	protected BigInteger hash(BufferedImage image, HashBuilder hash) {
		FastPixel fp = createPixelAccessor(image, width, height);

		// Use data buffer for faster access

		int[][] lum = fp.getLuma();

		// Calculate the left to right gradient
		for (int x = 1; x < width; x++) {
			for (int y = 0; y < height; y++) {
				if (lum[x][y] >= lum[x - 1][y]) {
					hash.prependZero();
				} else {
					hash.prependOne();
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
						hash.prependZero();
					} else {
						hash.prependOne();
					}
				}
			}
		}

		// Diagonally hash
		if (precision.equals(Precision.Triple)) {
			for (int x = 1; x < width; x++) {
				for (int y = 1; y < height; y++) {
					if (lum[x][y] < lum[x - 1][y - 1]) {
						hash.prependZero();
					} else {
						hash.prependOne();
					}
				}
			}
		}
		return hash.toBigInteger();
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
		return Objects.hash("com.github.kilianB.hashAlgorithms."+getClass().getSimpleName(), height, width, this.precision.name()) * 31 + 1;
	}

	/*
	 * Difference hash requires a little bit different handling when converting the
	 * hash to an image.
	 */
	@Override
	public Hash hash(BufferedImage image) {
		return new DHash(super.hash(image), this.precision, width, height);
	}

	@Override
	public Hash createAlgorithmSpecificHash(Hash original) {
		return new DHash(original, this.precision, width, height);
	}

	/**
	 * An extended hash class allowing dhashes to be visually represented.
	 * 
	 * @author Kilian
	 * @since 3.0.0
	 */
	public static class DHash extends Hash {

		private Precision precision;
		private int width;
		private int height;

		public DHash(Hash h, Precision precision, int width, int height) {
			super(h.getHashValue(), h.getBitResolution(), h.getAlgorithmId());
			this.precision = precision;
			this.width = width;
			this.height = height;
		}

		public BufferedImage toImage(int blockSize) {

			Color[] colorArr = new Color[] { Color.WHITE, Color.BLACK };
			int[] colorIndex = new int[hashLength];

			for (int i = 0; i < hashLength; i++) {
				colorIndex[i] = hashValue.testBit(i) ? 1 : 0;
			}
			return toImage(colorIndex, colorArr, blockSize);
		}

		public BufferedImage toImage(int[] bitColorIndex, Color[] colors, int blockSize) {

			if (precision.equals(Precision.Simple)) {

				BufferedImage bi = new BufferedImage(blockSize * width, blockSize * height,
						BufferedImage.TYPE_3BYTE_BGR);

				FastPixel fp = FastPixel.create(bi);
				drawDoublePrecision(fp, width, 1, height, 0, blockSize, 0, 0, bitColorIndex, colors);
				return bi;
			} else if (precision.equals(Precision.Double)) {

				BufferedImage bi = new BufferedImage(blockSize * width, blockSize * height * 2,
						BufferedImage.TYPE_3BYTE_BGR);

				FastPixel fp = FastPixel.create(bi);
				drawDoublePrecision(fp, width, 1, height, 0, blockSize, 0, 0, bitColorIndex, colors);
				drawDoublePrecision(fp, width, 0, height, 1, blockSize, hashLength / 2, height, bitColorIndex, colors);
				return bi;
			} else {

				BufferedImage bi = new BufferedImage(blockSize * width, blockSize * height * 3,
						BufferedImage.TYPE_3BYTE_BGR);

				FastPixel fp = FastPixel.create(bi);
				int hashOffset = 0;
				hashOffset += drawDoublePrecision(fp, width, 1, height, 0, blockSize, hashOffset, 0, bitColorIndex,
						colors);
				hashOffset += drawDoublePrecision(fp, width, 0, height, 1, blockSize, hashOffset, height, bitColorIndex,
						colors);
				drawDoublePrecision(fp, width, 1, height, 1, blockSize, hashOffset, 2 * height, bitColorIndex, colors);
				return bi;
			}
		}

		private int drawDoublePrecision(FastPixel writer, int width, int wOffset, int height, int hOffset,
				int blockSize, int offset, int yOffset, int[] bitColorIndex, Color[] colors) {
			int i = offset;
			for (int w = 0; w < (width - wOffset) * blockSize; w = w + blockSize) {
				for (int h = 0; h < (height - hOffset) * blockSize; h = h + blockSize) {
					Color c = colors[bitColorIndex[i++]];
					int red = (int) (c.getRed() * 255);
					int green = (int) (c.getGreen() * 255);
					int blue = (int) (c.getBlue() * 255);

					for (int m = 0; m < blockSize; m++) {
						for (int n = 0; n < blockSize; n++) {
							int x = w + m;
							int y = h + n + yOffset * blockSize;
							// bi.setRGB(y, x, bit ? black : white);
							writer.setRed(x, y, red);
							writer.setGreen(x, y, green);
							writer.setBlue(x, y, blue);
						}
					}
				}
			}
			return i-offset;
		}
	}

	public Precision getPrecision() {
		return precision;
	}
}
