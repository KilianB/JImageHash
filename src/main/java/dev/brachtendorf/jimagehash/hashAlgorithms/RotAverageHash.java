package dev.brachtendorf.jimagehash.hashAlgorithms;

import java.awt.image.BufferedImage;
import java.math.BigInteger;
import java.util.Objects;

import dev.brachtendorf.MathUtil;
import dev.brachtendorf.graphics.FastPixel;

/**
 * 
 * Calculate a hash value based on the average luminosity in an image after
 * mapping each pixel to a rotational bucket.
 * 
 * <p>
 * <img src=
 * "https://user-images.githubusercontent.com/9025925/47964206-6f99b400-e036-11e8-8843-471242f9943a.png"
 * alt="Ring partition. Pixels are nmapped to buckets according to their
 * distance to the center">
 * 
 * <p>
 * Each bucket corresponds to a single pixel which means that the rescaled image
 * gets rather large when higher bit resolutions are required which affects the
 * performance for high bit keys lengths.
 * 
 * <p>
 * Note: Unlike the other hashing algorithms this algorithm is only thread safe
 * after it successfully hashed it's first image. If concurrent usage is
 * required perform a single hash pass on an arbitrary image.
 * 
 * @author Kilian
 *
 */
public class RotAverageHash extends HashingAlgorithm {

	private static final long serialVersionUID = 128391293L;

	/** Width of the rescaled image */
	private int width;

	/** Height of the rescaled image */
	private int height;

	/** X Origin the pixels will be rotated around */
	private double centerX;

	/** Y Origin the pixels will be rotated around */
	private double centerY;

	/**
	 * The number of pixels in each bucket used to compute the average. Since this
	 * value stays consistent due to resizing it can be cached.
	 */
	private int[] count = null;

	/**
	 * @param bitResolution The bit resolution specifies the final length of the
	 *                      generated hash. A higher resolution will increase
	 *                      computation time and space requirement while being able
	 *                      to track finer detail in the image. Be aware that a high
	 *                      key is not always desired and this implementation
	 *                      specifically suffers performance penalties when dealing
	 *                      with huge hash sizes
	 *                      <p>
	 * 
	 *                      The average kernel hash will produce a hash with at
	 *                      least the number of bits defined by this argument. In
	 *                      turn this also means that different bit resolutions may
	 *                      be mapped to the same final key length.
	 */
	public RotAverageHash(int bitResolution) {
		super(bitResolution);

		int bucketPixelWidth = 2;

		// To fill all buckets reliable we need 2 pixels due to rotation.
		// also an even number is required to comply with symmetry.(which is given after
		// multi by 2)
		width = (bitResolution + 1) * bucketPixelWidth;
		height = width;
		centerX = (width - 1) / 2d; // This will be even
		centerY = centerX;
	}

	@Override
	protected BigInteger hash(BufferedImage image, HashBuilder hash) {
		FastPixel fp = createPixelAccessor(image, width, height);

		// We need 2 more bucket since we compare to n-1 and no values are mapped to 0
		// bucket

		// Average luminosity of the bucket
		double hashArr[] = new double[bitResolution + 2];

		boolean initCount = count == null;

		if (initCount) {
			count = new int[hashArr.length];
		}

		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				int bucket = computePartition(x, y);

				if (bucket >= hashArr.length) {
					// Everything beyond this column will be outside as well.
					continue;
				}

				if (initCount) {
					count[bucket]++;
					hashArr[bucket] += fp.getLuma(x, y);
				} else {
					hashArr[bucket] += (fp.getLuma(x, y) / (double) count[bucket]);
				}
			}
		}

		// During first pass compute the average separately
		if (initCount) {
			for (int i = 0; i < hashArr.length; i++) {
				hashArr[i] /= count[i];
			}
		}

		// 0 bucket does not contain any value.
		for (int i = 2; i < hashArr.length; i++) {
			if (hashArr[i] >= hashArr[i - 1] || MathUtil.isDoubleEquals(hashArr[i], hashArr[i - 1], 0.000001)) {
				hash.prependZero();
			} else {
				hash.prependOne();
			}
		}

		return hash.toBigInteger();
	}

	/**
	 * Compute the ring partition this specific pixel will fall into.
	 * 
	 * @param originalX the x pixel index in the picture
	 * @param originalY the y pixel index in the picture
	 * @return the bucket index
	 */
	public int computePartition(double originalX, double originalY) {
		// Compute eukledian distance to the center
		originalX -= centerX;
		originalY -= centerY;
		// Discard all pixels outside the circle
		return (int) Math.round(Math.sqrt(originalX * originalX + originalY * originalY));
	}

	@Override
	protected int precomputeAlgoId() {
		// @since 1.0.0 force incompatible hashes due to new calculation method.
		// https://github.com/KilianB/JImageHash/issues/49
		final int doubleEqualsOffset = 3;
		// These variables are enough to uniquely identify the hashing algorithm
		return Objects.hash("com.github.kilianB.hashAlgorithms."+getClass().getSimpleName(), width, height, doubleEqualsOffset);
	}
}
