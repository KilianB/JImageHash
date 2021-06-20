package dev.brachtendorf.jimagehash.matcher.exotic;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.logging.Logger;

import javax.imageio.ImageIO;

import dev.brachtendorf.ArrayUtil;
import dev.brachtendorf.jimagehash.Experimental;
import dev.brachtendorf.jimagehash.hash.Hash;
import dev.brachtendorf.jimagehash.hashAlgorithms.HashingAlgorithm;
import dev.brachtendorf.jimagehash.matcher.PlainImageMatcher;

import com.github.kilianB.pcg.fast.PcgRSFast;

/**
 * A bloom filter is an approximative data structure with constant space and
 * time guarantee to check if an image is likely in the set or definitely not
 * present.
 * <p>
 * False positives are possible, false negatives are not. In other words, if the
 * bloom filter decides that an element is not present it wasn't added
 * beforehand with absolute certainty.
 * <p>
 * Images are mapped by multiple hash functions. Contrary to other matchers the
 * bloom filter checks against images with the <b>exact</b> same hash in
 * constant time making it a good candidate to filter before initiating more
 * expensive operations.
 * 
 * <p>
 * If image duplicates shall be avoided different hash functions with high bit
 * resolutions are to be chosen.
 * 
 * <p>
 * While a check on image similarity isn't possible by traditional bloom filter
 * using a very low bit hash function, will result in images with small
 * deviation being hashed to the same bucket with higher possibility allowing
 * for further investigation.
 * 
 * <p>
 * The supplied hashing algorithms are only used to seed a
 * {@link com.github.kilianB.pcg.Pcg Pcg} Random Number Generator and are
 * chained back to back to each other if more hash functions are required than
 * got provided by the user to ensure the target false positive probability.
 * <p>
 * If more algorithms are provided as can be used these algorithms will be
 * discarded with a notice in the console/logger.
 * 
 * @author Kilian
 * @since 3.0.0
 */
@Experimental("Not well tested yet")
public class BloomFilter extends PlainImageMatcher {

	private static final Logger LOGGER = Logger.getLogger(BloomFilter.class.getSimpleName());

	/**
	 * Fast random number generator used as a hashing function.
	 */
	private PcgRSFast rng = new PcgRSFast();

	/**
	 * hash buckets
	 */
	private boolean[] buckets;

	/**
	 * number of buckets this filter hash (bucket.length)
	 */
	private int bits = -1;

	/**
	 * How often each user supplied hashing algorithm will be used in case not
	 * enough distinct hashing algorithms are present.
	 */
	private int[] multiplier;

	/**
	 * The number of hashing algorithms used by this bloom filter
	 */
	private int k;

	/**
	 * The number of user supplied hashing algorithms used
	 */
	private int numOfHashesUsed = 0;

	/**
	 * The number of maximum expected elements to be added to the filter.
	 */
	private int n;

	/**
	 * Is the filter in setup phase or were hashes already added
	 */
	private boolean locked = false;

	/**
	 * Create a bloom filter with expected elements and bit size. Exceeding the
	 * number of expected elements will quickly result in degrading false positive
	 * probability.
	 * 
	 * @param expectedElements the maximum number of elements to be added to this
	 *                         set
	 * @param bits             the number of bits used to store hashes
	 */
	public BloomFilter(int expectedElements, int bits) {
		this.bits = bits;
		this.n = expectedElements;
		this.buckets = new boolean[bits];
	}

	/**
	 * 
	 * @param expectedElements                the maximum number of elements to be
	 *                                        added to this set
	 * @param desiredFalsePositiveProbability the probability of an element being
	 *                                        considered a false positive once the
	 *                                        number of distinct elements added to
	 *                                        the filter reaches the
	 *                                        expectedElements count. Range (0-1]
	 */
	public BloomFilter(int expectedElements, double desiredFalsePositiveProbability) {
		this(expectedElements, getOptimalBitSizeOfFilter(desiredFalsePositiveProbability, expectedElements));
	}

	/**
	 * Added hashing algorithms will be queried to seed the internal rng. If not
	 * enough hashing algorithms are provided by the user exists algorithms will be
	 * chained back to back.
	 * <p>
	 * Hashing algorithms may only be added and removed as long as no image has been
	 * added to this set.
	 * 
	 * <p>
	 * If too many algorithms are provided to comply with the false positive target
	 * the later added algorithms may be discarded.
	 * <p>
	 * {@inheritDoc}
	 */
	@Override
	public boolean addHashingAlgorithm(HashingAlgorithm hashingAlgorithm) {
		checkLockState();
		boolean added = super.addHashingAlgorithm(hashingAlgorithm);
		return added;
	}

	/**
	 * 
	 * Hashing algorithms may only be added and removed as long as no image has been
	 * added to this set.
	 * <p>
	 * {@inheritDoc}
	 */
	@Override
	public boolean removeHashingAlgorithm(HashingAlgorithm hashingAlgorithm) {
		checkLockState();
		return super.removeHashingAlgorithm(hashingAlgorithm);
	}

	/**
	 * 
	 * Hashing algorithms may only be added and removed as long as no image has been
	 * added to this set.
	 * <p>
	 * {@inheritDoc}
	 */
	@Override
	public void clearHashingAlgorithms() {
		checkLockState();
		super.clearHashingAlgorithms();
	}

	/**
	 * Checks if the hash created by this image utilizing the added hashing
	 * algorithms was already added to the bloom filter with the curren false
	 * positive probability as returned by {@link #getFalsePositiveProbability()}
	 * 
	 * @param file of the image to check
	 * @return false if the image is definitely not in the filter, true if the image
	 *         might be in the set.
	 * @throws IOException if an error occurs during file reading
	 */
	public boolean isPresent(File file) throws IOException {
		return isPresent(ImageIO.read(file));
	}

	/**
	 * Checks if the hash created by this image utilizing the added hashing
	 * algorithms was already added to the bloom filter with the curren false
	 * positive probability as returned by {@link #getFalsePositiveProbability()}
	 * 
	 * @param image to check
	 * @return false if the image is definitely not in the filter, true if the image
	 *         might be in the set.
	 */
	public boolean isPresent(BufferedImage image) {

		Iterator<HashingAlgorithm> iter = this.steps.iterator();
		for (int j = 0; j < this.steps.size(); j++) {
			if (j >= numOfHashesUsed) {
				break;
			}
			int hashCode = imageToHashCode(iter.next(), image);
			for (int i = 0; i < multiplier[j]; i++) {
				if (!buckets[getBucket(hashCode, i)]) {
					return false;
				}
			}
		}
		return true;
	}

	/**
	 * Adds the hashes produced by the added hashing algorithm of this image to the
	 * filter. Future calls to {@link #isPresent(File)} will return true for the
	 * image.
	 * <p>
	 * Adding an image to the filter will prevent further modifications of the
	 * filter in terms of calling {@link #addHashingAlgorithm(HashingAlgorithm)},
	 * {@link #removeHashingAlgorithm(HashingAlgorithm)} or
	 * {@link #clearHashingAlgorithms()}.
	 * 
	 * @param image The image to add
	 * @throws IOException if an error occurs during file reading.
	 */
	public void addImage(File image) throws IOException {
		addImage(ImageIO.read(image));
	}

	/**
	 * Adds the hashes produced by the added hashing algorithm of this image to the
	 * filter. Future calls to {@link #isPresent(File)} will return true for the
	 * image.
	 * <p>
	 * Adding an image to the filter will prevent further modifications of the
	 * filter in terms of calling {@link #addHashingAlgorithm(HashingAlgorithm)},
	 * {@link #removeHashingAlgorithm(HashingAlgorithm)} or
	 * {@link #clearHashingAlgorithms()}.
	 * 
	 * @param image The image to add
	 */
	public void addImage(BufferedImage image) {
		lock();

		Iterator<HashingAlgorithm> iter = this.steps.iterator();
		for (int j = 0; j < this.steps.size(); j++) {
			if (j >= numOfHashesUsed) {
				break;
			}
			int hashCode = imageToHashCode(iter.next(), image);
			for (int i = 0; i < multiplier[j]; i++) {
				buckets[getBucket(hashCode, i)] = true;
			}
		}
	}

	/**
	 * Convert the image to a numerical hashcode
	 * 
	 * @param hasher the hashing algorithm to use
	 * @param image  the image to hash
	 * @return an integer hashcode
	 */
	protected int imageToHashCode(HashingAlgorithm hasher, BufferedImage image) {
		Hash hash = hasher.hash(image);

		int hashCode = hash.getHashValue().hashCode();
		hashCode = hashCode * 31 + hasher.algorithmId();
		return hashCode;
	}

	/**
	 * Map an integer to a bucket of the bloom filter
	 * 
	 * @param hashCode the integer used as seed
	 * @param i        secondary seed value
	 * @return index of a bucket
	 */
	protected int getBucket(int hashCode, int i) {
		rng.setSeed(hashCode, i);
		return rng.nextInt(bits);
	}

	/**
	 * Check if modification of the filter is allowed.
	 */
	protected void checkLockState() {
		if (locked) {
			throw new IllegalStateException("The filter can't be modified after images have already been added");
		}
	}

	/**
	 * Lock the bloom filter and set variables to fixed values. This will prevent
	 * hashing algorithms to be added to the filter.
	 */
	protected void lock() {

		// Finish setup
		if (!locked) {

			if (steps.isEmpty()) {
				throw new IllegalStateException("Can't add image with 0 supplied hashing algorithms");
			}

			// Calculate modifiers
			int numHashingAlgorithms = this.steps.size();

			multiplier = new int[numHashingAlgorithms];
			ArrayUtil.fillArray(multiplier, () -> {
				return 1;
			});

			// Set by user first constructor bits and n is set
			this.k = getOptimalNumberHashFunctions(bits, n);
			System.out.println("k " + k + " " + numHashingAlgorithms);
			if (this.k < numHashingAlgorithms) {
				LOGGER.warning(
						"Fewer hashing algorithms needed as supplied. Discard algos. If desired increase the bit size of the bloom filter.");
				numOfHashesUsed = this.k;
			} else {
				numOfHashesUsed = numHashingAlgorithms;
				// We need more algorithms. Chain the first used algorithms multiple timesy
				System.out.println("we need more algorithms: " + numHashingAlgorithms + " " + this.k);
				for (int i = 0; i < this.k - numHashingAlgorithms; i++) {
					multiplier[i % multiplier.length]++;
				}
			}

			locked = true;
		}

	}

	/**
	 * @return the number of buckets set
	 */
	public int bitsSet() {
		int bitsSet = 0;
		for (boolean b : buckets) {
			if (b) {
				bitsSet++;
			}
		}
		return bitsSet;
	}

	/**
	 * This method will return usable results after the first image has been added
	 * 
	 * @return the percentage of buckets set. [0-1]
	 * 
	 */
	public double bucketsSet() {
		return bitsSet() / (double) buckets.length;
	}

	/**
	 * Return the approximate number of distinct elements added to this set.
	 * <p>
	 * This method will return usable results after the first image has been added
	 * 
	 * @return the approximate number of elements added
	 */
	public double getApproximateDistinctElementsInFilter() {

		return -(bits / (double) k) * Math.log(1 - bucketsSet());
	}

	/**
	 * Get the current false positive probability of the filter using the
	 * {@link #getApproximateDistinctElementsInFilter()} guess to assume the number
	 * of elements currently in the filter.
	 * <p>
	 * This method will return usable results after the first image has been added
	 * 
	 * @return the probability of a false positive [0-1]
	 */
	public double getFalsePositiveProbability() {
		return getFalsePositiveProbability(getApproximateDistinctElementsInFilter());
	}

	/**
	 * Get the false positive probability of the filter once the filter reached the
	 * specified amount of distinct added items.
	 * <p>
	 * This method will return usable results after the first image has been added
	 * 
	 * @param numberOfElements the number of elements are currently in the filter
	 * @return the probability of a false positive [0-1]
	 */
	public double getFalsePositiveProbability(double numberOfElements) {
		return Math.pow((1 - Math.exp(-k * (double) numberOfElements / (double) bits)), k);
	}

	/**
	 * Get the optimal number of hash functions needed to reduce false positive
	 * errors.
	 * 
	 * @param m bit size of the bloom filter
	 * @param n number of maximum elements added
	 * @return k the number of hash functions that should be used to reduce false
	 *         positives.
	 */
	public static int getOptimalNumberHashFunctions(int m, int n) {
		int optimalHashFunctions = (int) Math.round((m / (double) n) * Math.log(2));
		return optimalHashFunctions > 0 ? optimalHashFunctions : 1;
	}

	/**
	 * Assuming the optimal k value used as calculated by
	 * {@link #getOptimalNumberHashFunctions(int, int)}. Get the optimal number of
	 * bits to achieve the target probability when the set is filled to maximum
	 * capacity
	 * 
	 * @param p the desired false positive probability (0-1]
	 * @param n number of maximum elements added to the set
	 * @return m the optimal bit size of the bloom filter
	 */
	public static int getOptimalBitSizeOfFilter(double p, int n) {
		return -(int) Math.round((n * Math.log(p)) / ((Math.log(2) * Math.log(2))));
	}

}
