package com.github.kilianB.hashAlgorithms;

import java.awt.image.BufferedImage;
import java.math.BigInteger;
import java.util.Objects;

import com.github.kilianB.graphics.ImageUtil;
import com.github.kilianB.graphics.ImageUtil.FastPixel;

/**
 * 
 * 
 * <p>
 * Note: Unlike the other hashing algorithms this algorithm is only thread safe
 * after it successfully hashed it's first image. If concurrent usage is required
 * perform a single hash pass on an arbitrary image.
 * 
 * @author Kilian
 *
 */
public class RotAverageHash extends HashingAlgorithm {

	private static final long serialVersionUID = 1L;

	/** Unique identifier of the algorithm */
	private final int algorithmId;

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
	 * @param bitResolution
	 */
	public RotAverageHash(int bitResolution) {
		super(bitResolution);
		algorithmId = Objects.hash(getClass().getName(), this.bitResolution);

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
	protected BigInteger hash(BufferedImage image, BigInteger hash) {

		FastPixel fp = new FastPixel(ImageUtil.getScaledInstance(image, width, height));

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

		//During first pass compute the average separately 
		if (initCount) {
			for (int i = 0; i < hashArr.length; i++) {
				hashArr[i] /= count[i];
			}
		}

		// 0 bucket does not contain any value.
		for (int i = 2; i < hashArr.length; i++) {
			if (hashArr[i] >= hashArr[i - 1]) {
				hash = hash.shiftLeft(1);
			} else {
				hash = hash.shiftLeft(1).add(BigInteger.ONE);
			}
		}

		return hash;
	}

	public int computePartition(double originalX, double originalY) {
		// Compute eukledian distance to the center
		originalX -= centerX;
		originalY -= centerY;
		// Discard all pixels outside the circle
		return (int) Math.round(Math.sqrt(originalX * originalX + originalY * originalY));
	}

	@Override
	public int algorithmId() {
		return algorithmId;
	}
}
