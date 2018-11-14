package com.github.kilianB.hashAlgorithms;

import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBufferByte;
import java.math.BigInteger;
import java.util.Objects;

import com.github.kilianB.graphics.ImageUtil;
import com.github.kilianB.graphics.ImageUtil.FastPixel;
import com.github.kilianB.matcher.Hash;

/**
 * @author Kilian
 *
 */
public class Rotational extends HashingAlgorithm {

	// Radians in a circle
	private static final double RAD_CIRCLE = 2 * Math.PI;

	private final int algorithmId;

	/*
	 * The center coordinates of rescaled images. The input image will be rotated
	 * and wrapped around this center;
	 */
	// TODO center

	private int width;
	private int height;
	private double centerX;
	private double centerY;

	private double widthPerSection;

	/**
	 * @param bitResolution
	 */
	public Rotational(int bitResolution) {
		super(bitResolution);
		algorithmId = Objects.hash(getClass().getName(), this.bitResolution);

		// To fill all buckets reliable we need 2 pixels due to rotation.
		// also an even number is required to comply with symmetry.(which is given after
		// multi by 2)
		width = bitResolution * 2;
		height = width;
		widthPerSection = (width / (double) 2) / bitResolution;
		
		centerX = (width - 1) / (double) 2; // This will be even
		centerY = centerX;

	}

	@Override
	public Hash hash(BufferedImage image) {

		FastPixel fp = new FastPixel(ImageUtil.getScaledInstance(image, width, height));

		int hash[] = new int[bitResolution];
		// TODO count is deterministic. We can calculate it once at the beginning and
		// use it. No need to recompute all the time
		int count[] = new int[bitResolution];

		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				int bucket = computePartition(x, y);

				if (bucket >= bitResolution) {
					// Everything beyond this column will be outside as well.
					continue;
				}
				hash[bucket] += fp.getLuma(x, y);
				count[bucket]++;
			}
		}

		BigInteger finalHash = BigInteger.ONE;

		for (int i = 0; i < bitResolution; i++) {
			hash[i] /= count[i];
		}

		for (int i = 1; i < bitResolution; i++) {
			if (hash[i] >= hash[i - 1]) {
				finalHash = finalHash.shiftLeft(1);
			} else {
				finalHash = finalHash.shiftLeft(1).add(BigInteger.ONE);
			}
		}

		return new Hash(finalHash, algorithmId);
	}

	public int computePartition(double originalX, double originalY) {

		// Compute eukledian distance to the center
		originalX -= centerX;
		originalY -= centerY;
		// Discard all pixels outside the circle
		double distance = Math.sqrt(originalX * originalX + originalY * originalY);
		// For now ignore pixels outside the circle...
		return (int) (distance / widthPerSection);
	}

	@Override
	public int algorithmId() {
		return algorithmId;
	}

}
