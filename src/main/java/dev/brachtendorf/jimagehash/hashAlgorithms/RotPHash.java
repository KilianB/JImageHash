package dev.brachtendorf.jimagehash.hashAlgorithms;

import java.awt.image.BufferedImage;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.jtransforms.dct.DoubleDCT_1D;

import dev.brachtendorf.graphics.FastPixel;

/**
 * A rotational invariant hashing algorithm which is mostly immune to rotation
 * attacks. The hash wraps the pixels around a circle and computes a discrete
 * cosine transformation on each subsection.
 * 
 * <img src=
 * "https://user-images.githubusercontent.com/9025925/47964206-6f99b400-e036-11e8-8843-471242f9943a.png"
 *  alt="Ring partition. Pixels are mapped to buckets according to their distance to the center">
 * 
 * @author Kilian
 * @since 2.0.0
 */
public class RotPHash extends HashingAlgorithm {

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
	public RotPHash(int bitResolution) {
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
	public RotPHash(int bitResolution, boolean truncateKey) {
		super(bitResolution);
		this.truncateKey = truncateKey;

		// A rough approximation to get to the desired key length.
		buckets = (int) (Math.sqrt(this.bitResolution * 1.27)) + 3;
		/*
		 * TODO this can be calculated more accurately by computing the bucket bounds
		 * (circumference of each bucket and computing the number of pixels mapped to
		 * the bucket beforehand. This would also allow us to more accurately specify
		 * the percent share of data we throw away when calculating the dct transform.
		 */

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
	protected BigInteger hash(BufferedImage image, HashBuilder hash) {

		// 0. Preprocessing. Extract Luminosity
		// Fast pixel access. Order 10x faster than jdk internal
		FastPixel fp = createPixelAccessor(image, width, height);

		@SuppressWarnings("unchecked")
		List<Integer>[] values = new List[buckets];
		for (int i = 0; i < buckets; i++) {
			values[i] = new ArrayList<Integer>();
		}

		// 1. Map each pixel into a circle bucket. (Currently we ignore parts of the
		// image if they do not fit inside a cropped circle)
		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				// Wrap pixel around center. Return the bucket whose center is the closest to
				// this pixel
				int bucket = computePartition(x, y);
				if (bucket >= buckets) {
					continue;
				}
				values[bucket].add(fp.getLuma(x, y));
			}
		}

		// 2. Construct the final hash

//		int charNeeded = StringUtil.charsNeeded(buckets);
//		String debugFormat = "Count %"+charNeeded+"d Bucket %"+charNeeded+"d Avg: %.2f %n";

		int length = 0;
		for (int i = 0; i < buckets; i++) {
			// Sort lum values to get a dct independent of initial rotation
			Collections.sort(values[i]);

			double[] arr = new double[values[i].size()];
			for (int j = 0; j < arr.length; j++) {
				arr[j] = values[i].get(j);
			}

			// Compute dct of each bucket and calculate the average
			DoubleDCT_1D dct = new DoubleDCT_1D(arr.length);
			dct.forward(arr, false);

			double avg = 0;
			int count = arr.length / 4 - 1;
			for (int j = 2; j < count; j++) {
				avg += (arr[j] / (count - 2));
			}

			/*
			 * The first two fields should always be ignored. Their values are way out of
			 * magnitude in order to add any kind of distinguishing capabilities of the hash
			 */
			for (int j = 2; j < count; j++) {

				// We discard parts of the information of the last layer if we need a specific
				// length key
				if (this.truncateKey && length == bitResolution)
					break;

				if (arr[j] >= avg) {
					hash.prependZero();
				} else {
					hash.prependOne();
				}
				length++;
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
	protected int computePartition(double originalX, double originalY) {
		// Compute euclidean distance to the center
		originalX -= centerX;
		originalY -= centerY;
		double distance = Math.sqrt(originalX * originalX + originalY * originalY);
		return (int) (distance / widthPerSection);
	}

//	@Override
//	public int getKeyResolution() {
//		// We can compute this more quickly than the super class. so we might as well do
//		// it
//		if (keyResolution < 0) {
//			if (truncateKey) {
//				keyResolution = this.bitResolution;
//			} else {
//				BufferedImage bi = new BufferedImage(1, 1, BufferedImage.TYPE_3BYTE_BGR);
//				keyResolution = this.hash(bi, BigInteger.ONE).bitLength() - 1;
//			}
//
//		}
//		return keyResolution;
//	}

	@Override
	protected int precomputeAlgoId() {
		return Objects.hash("com.github.kilianB.hashAlgorithms."+getClass().getSimpleName(), this.width, this.height, this.truncateKey);

	}

}
