package com.github.kilianB.hashAlgorithms;

import java.awt.image.BufferedImage;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.jtransforms.dct.DoubleDCT_1D;

import com.github.kilianB.graphics.ImageUtil;
import com.github.kilianB.graphics.ImageUtil.FastPixel;

/**
 * 
 * A rotational invariant hashing algorithm which is mostly immune to rotation
 * attacks. The hash wraps the pixels around a circle and computes a discrete
 * cosine transformation on each subsection.
 * 
 * @author Kilian
 * @since 2.0.0
 */
public class RotPHash2 extends HashingAlgorithm {

	private static final long serialVersionUID = -7498910506857652806L;

	/** If parts of the key shall be truncated */
	private final boolean truncateKey;

	/** Width of the rescaled image */
	private int width;

	/** Height of the rescaled image */
	private int height;

	/** X Origin the pixels will be rotated around */
	private double centerX;

	/** Y Origin the pixels will be rotated around */
	private double centerY;

	/** The width of each circle */
	private double widthPerSection;

	/** The number of circles the pixels will be mapped to */
	private int buckets;

	/**
	 * Create a Rotational Invariant Perceptive Hasher
	 * 
	 * @param bitResolution The desired bit resolution of the created hash
	 */
	public RotPHash2(int bitResolution) {
		this(bitResolution, false);
	}

	/**
	 * Create a Rotational Invariant Perceptive Hasher
	 * 
	 * @param bitResolution The desired bit resolution of the created hash
	 * @param truncateKey   if true the resulting key will exactly have
	 *                      bitResolution length at the cost of truncating some
	 *                      information in the last layer. This may result in tiny
	 *                      bit of accuracy loss.
	 *                      <p>
	 *                      If false the keys length will at least be bitResolution
	 *                      bits long, but most likely longer. All keys produced
	 *                      with the same settings will have the same length.
	 */
	public RotPHash2(int bitResolution, boolean truncateKey) {
		super(bitResolution);
		this.truncateKey = truncateKey;

		// A rough approximation to get to the desired key length.
		buckets = (int) (Math.sqrt(this.bitResolution * 1.27)) + 3;

		// To fill all buckets reliable we need at least 2 pixels due to rotation on
		// each side as well as an even number to comply with symmetry constraints.
		width = buckets * 2;
		height = width;
		widthPerSection = (width / 2d) / buckets;

		// The center of the image used to wrap pixels around.
		centerX = (width - 1) / 2d; // This will be even
		centerY = centerX;
	}

	@Override
	protected BigInteger hash(BufferedImage image, BigInteger hash) {

		// 0. Preprocessing. Extract Luminosity
		BufferedImage transformed = ImageUtil.getScaledInstance(image, width, height);
		// Fast pixel access. Order 10x faster than jdk internal
		FastPixel fp = new FastPixel(transformed);

		@SuppressWarnings("unchecked")
		List<Integer>[] values = new List[buckets];
		for (int i = 0; i < buckets; i++) {
			values[i] = new ArrayList<Integer>();
		}

		int count = 0;

		// 1. Map each pixel into a circle bucket. (Currently we ignore parts of the
		// image if they do not fit inside a cropped circle)
		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				// Wrap pixel around center. Return the bucket whose center is the closest to
				// this pixel
				int bucket = computePartition(x, y);
				if (bucket >= buckets) {
					// Everything beyond this column will be outside as well.
					continue;
				}
				values[bucket].add(fp.getLuma(x, y));
				count++;
			}
		}

		// 2. Construct the final hash

		double[] featureArray = new double[count];

		// flatten 2 d list to 1d array
		int curIndex = 0;
		for (int bucket = 0; bucket < buckets; bucket++) {
			// Sort lum values to get a dct independent of initial rotation
			Collections.sort(values[bucket]);
			for (int feature : values[bucket]) {
				featureArray[curIndex++] = feature;
			}
		}

		// Compute dct
		DoubleDCT_1D dct = new DoubleDCT_1D(featureArray.length);
		dct.forward(featureArray, false);

		// Average value of the (topmost) YxY low frequencies. Skip the first column as
		// it might be too dominant. Solid color e.g.
		// TODO DCT walk down in a triangular motion. Skipping the entire edge neglects
		// several important frequencies. Maybe just skip
		// just the upper corner.
		double avg = 0;

		// Take a look at a forth of the pixel matrix. The lower right corner does not
		// yield much information.
		int subWidth = (int) (count / 4d);

		// calculate the average of the dct
		for (int i = 1; i < subWidth + 1; i++) {
			avg += featureArray[i] / subWidth;
		}

		for (int i = 1; i < subWidth + 1; i++) {
			if (featureArray[i] < avg) {
				hash = hash.shiftLeft(1);
			} else {
				hash = hash.shiftLeft(1).add(BigInteger.ONE);
			}
		}
		
		return hash;
	}

	protected int computePartition(double originalX, double originalY) {
		// Compute euclidean distance to the center
		originalX -= centerX;
		originalY -= centerY;
		double distance = Math.sqrt(originalX * originalX + originalY * originalY);
		return (int) (distance / widthPerSection);
	}

	@Override
	public int getKeyResolution() {
		if (keyResolution < 0) {
			if (truncateKey) {
				keyResolution = this.bitResolution;
			} else {
				BufferedImage bi = new BufferedImage(1, 1, BufferedImage.TYPE_3BYTE_BGR);
				keyResolution = this.hash(bi, BigInteger.ONE).bitLength() - 1;
			}

		}
		return keyResolution;
	}

	@Override
	protected int precomputeAlgoId() {
		return  Objects.hash(getClass().getName(), this.width,this.height, truncateKey);
	}

}
