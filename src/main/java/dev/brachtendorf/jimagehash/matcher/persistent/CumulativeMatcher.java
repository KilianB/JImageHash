package dev.brachtendorf.jimagehash.matcher.persistent;

import java.awt.image.BufferedImage;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.PriorityQueue;

import dev.brachtendorf.MathUtil;
import dev.brachtendorf.jimagehash.hashAlgorithms.HashingAlgorithm;
import dev.brachtendorf.jimagehash.datastructures.tree.Result;
import dev.brachtendorf.jimagehash.datastructures.tree.binaryTree.BinaryTree;
import dev.brachtendorf.jimagehash.hash.Hash;

/**
 * Instead of early aborting if one algorithm fails like the
 * {@link dev.brachtendorf.jimagehash.matcher.cached.ConsecutiveMatcher}, this class
 * looks at the summed distance and decides if images match.
 * 
 * <pre>
 * Example:
 * A CumulativeInMemoryMatcher with a threshold of 0.6 (may be any value desired)
 * 
 * Pass:
 * Hasher  computes a distance of 0.1
 * Hasher1 computes a distance of 0.4
 * -------
 * Since the summed distance of 0.5 is smaller than 0.6, the two images are considered a match
 * 
 * Fail:
 * Hasher computes a distance of 0.7
 * Hasher1 not executed since distance already greater than threshold
 * -----------------------------
 * Not considered a match
 * </pre>
 * 
 * Additionally each hashing algorithm may be appointed a weight value which
 * transforms the returned hash value accordingly, increasing or decreasing the
 * importance of this algorithm.
 * 
 * @author Kilian
 * @since 2.0.0
 */
public class CumulativeMatcher extends PersitentBinaryTreeMatcher {

	private static final long serialVersionUID = 4124905869048848068L;
	/**
	 * Settings of the in memory matcher
	 */
	private AlgoSettings overallSetting;

	/**
	 * Create a cumulative in memory image matcher with the specified normalized
	 * threshold
	 * 
	 * @param cacheAddedHashes Additionally to the binary tree, hashes of added
	 *                         images will be mapped to their uniqueId allowing to
	 *                         retrieve matches of added images without loading the
	 *                         image file from disk. This setting increases memory
	 *                         overhead in exchange for performance.
	 *                         <p>
	 *                         Use this setting if calls to
	 *                         {@link #getMatchingImages(java.io.File)} likely
	 *                         contains an image already added to the match prior.
	 * @param threshold        The cutoff threshold of the summed and scaled
	 *                         distances of all hashing algorithm until an image is
	 *                         considered a match.
	 *                         <p>
	 *                         If a negative threshold is supplied no images will
	 *                         ever be returned.
	 *                         <p>
	 *                         If additional algorithms are added an increase in the
	 *                         threshold parameter is justified.
	 */
	public CumulativeMatcher(boolean cacheAddedHashes, double threshold) {
		this(cacheAddedHashes, threshold, true);
	}

	/**
	 * Create a cumulative in memory image matcher with the specified threshold
	 * 
	 * @param cacheAddedHashes Additionally to the binary tree, hashes of added
	 *                         images will be mapped to their uniqueId allowing to
	 *                         retrieve matches of added images without loading the
	 *                         image file from disk. This setting increases memory
	 *                         overhead in exchange for performance.
	 *                         <p>
	 *                         Use this setting if calls to
	 *                         {@link #getMatchingImages(java.io.File)} likely
	 *                         contains an image already added to the match prior.
	 * @param threshold        The cutoff threshold of the summed and scaled
	 *                         distances of all hashing algorithm until an image is
	 *                         considered a match.
	 *                         <p>
	 *                         If a negative threshold is supplied no images will
	 *                         ever be returned.
	 *                         <p>
	 *                         If additional algorithms are added an increase in the
	 *                         threshold parameter is justified.
	 * @param normalized       If true the threshold is considered the normalized
	 *                         distance created by each added Hashing algorithm.
	 */
	public CumulativeMatcher(boolean cacheAddedHashes, double threshold, boolean normalized) {
		this(cacheAddedHashes, new AlgoSettings(threshold, normalized));
	}

	protected CumulativeMatcher(boolean cacheAddedHashes, AlgoSettings setting) {
		super(cacheAddedHashes);
		overallSetting = Objects.requireNonNull(setting, "Setting may not be null");
	}

	/**
	 * Add a hashing algorithm to the matcher with a weight multiplier of 1. In
	 * order for images to match they have to be beneath the threshold of the summed
	 * distances of all added hashing algorithms.
	 * 
	 * @param algo The algorithm to add to the matcher.
	 */
	public void addHashingAlgorithm(HashingAlgorithm algo) {
		super.addHashingAlgorithm(algo, 1);
	}

	/**
	 * Add a hashing algorithm to the matcher with the given weight multiplier. In
	 * order for images to match they have to be beneath the threshold of the summed
	 * distances of all added hashing algorithms.
	 * 
	 * <p>
	 * The weight parameter scales the returned hash distance. For example a weight
	 * multiplier of 2 means that the returned distance is multiplied by 2. If the
	 * total allowed distance of this matcher is 0.7 and the returned hash is 0.3 *
	 * 2 the next algorithm only may return a distance of 0.1 or smaller in order
	 * for the image to pass.
	 * 
	 * @param algo   The algorithms to be added
	 * @param weight The weight multiplier of this algorithm.
	 */
	public void addHashingAlgorithm(HashingAlgorithm algo, double weight) {
		/*
		 * only used to redefine javadocs Add false to circumvent the range check. We do
		 * not check for normalized in this case either way
		 */
		super.addHashingAlgorithm(algo, weight, false);
	}

	/**
	 * Add a hashing algorithm to the matcher with the given weight multiplier. In
	 * order for images to match they have to be beneath the threshold of the summed
	 * distances of all added hashing algorithms.
	 * 
	 * *
	 * <p>
	 * The weight parameter scales the returned hash distance. For example a weight
	 * multiplier of 2 means that the returned distance is multiplied by 2. If the
	 * total allowed distance of this matcher is 0.7 and the returned hash is 0.3 *
	 * 2 the next algorithm only may return a distance of 0.1 or smaller in order
	 * for the image to pass.
	 * 
	 * @param algo   The algorithms to be added
	 * @param weight The weight multiplier of this algorithm.
	 * @param dummy  not used by this type of image matcher. This method signature
	 *               is only available due to inheritance.
	 */
	public void addHashingAlgorithm(HashingAlgorithm algo, double weight, boolean dummy) {
		// only used to redefine javadocs
		super.addHashingAlgorithm(algo, weight, false);
	}

	@Override
	protected PriorityQueue<Result<String>> getMatchingImagesInternal(BufferedImage image, String uniqueId) {

		if (steps.isEmpty())
			throw new IllegalStateException(
					"Please supply at least one hashing algorithm prior to invoking the match method");

		// The maximum distance we have to search in our tree until we can't find any
		// more images
		double maxDistanceUntilTermination = overallSetting.getThreshold();

		// [Result,Summed distance of the image]
		HashMap<Result<String>, Double> distanceMap = new HashMap<>();

		// During first iteration we need to do some extra hoops
		boolean first = true;

		// https://stackoverflow.com/a/31401836/3244464 TODO jmh benchmark
		float optimalLoadFactor = (float) Math.log(2);

		// For each hashing algorithm
		for (Entry<HashingAlgorithm, AlgoSettings> entry : steps.entrySet()) {
			HashingAlgorithm algo = entry.getKey();

			HashMap<Result<String>, Double> temporaryMap;

			BinaryTree<String> binTree = binTreeMap.get(algo);

			// Init temporary hashmap
			int optimalCapacity = (int) (Math
					.ceil((first ? binTree.getHashCount() : distanceMap.size()) / optimalLoadFactor) + 1);
			temporaryMap = new HashMap<>(optimalCapacity, optimalLoadFactor);

			Hash needleHash = getHash(algo, uniqueId, image);

			int bitRes = algo.getKeyResolution();

			int threshold = 0;

			if (overallSetting.isNormalized()) {
				// Normalized threshold
				threshold = (int) (maxDistanceUntilTermination * bitRes);
			} else {
				threshold = (int) maxDistanceUntilTermination;
			}

			PriorityQueue<Result<String>> temp = binTree.getElementsWithinHammingDistance(needleHash, threshold);

			// Find the min total distance for the next generation to specify our cutoff
			// parameter
			double minDistance = Double.MAX_VALUE;

			// filter manually
			for (Result<String> res : temp) {

				double normalDistance = entry.getValue().getThreshold() * (res.distance / (double) bitRes);

				// Initially seed hashmap
				if (first) {
					// Add all
					// System.out.printf("Distance: %.3f | %s %n", normalDistance, res.toString());
					temporaryMap.put(res, normalDistance);
					if (normalDistance < minDistance) {
						minDistance = normalDistance;
					}
				} else {
					// Second third hash
					if (distanceMap.containsKey(res)) {

						// update distance left until considered invalid
						double distanceSoFar = distanceMap.get(res) + normalDistance;
						double distanceLeft = overallSetting.getThreshold() - distanceSoFar;

						// System.out.printf("Distance: %.3f | dLeft: %.3f distSoFar: %.3f %s %n",
						// normalDistance,
						// distanceLeft, distanceSoFar, res.toString());

						if (distanceLeft > 0) {
							// Update distance
							temporaryMap.put(res, distanceSoFar);
							if (distanceSoFar < minDistance) {
								minDistance = distanceSoFar;
							}
						}
					}
				}
				// Cumulative distance already supplied
			}

			distanceMap = temporaryMap;

			if (first) {
				first = false;
			}

			maxDistanceUntilTermination -= minDistance;

			if (MathUtil.isDoubleEquals(maxDistanceUntilTermination, 0, -1e100)) {
				break;
			}
		}

		// TODO note that we used the normalized distance here
		PriorityQueue<Result<String>> returnValues = new PriorityQueue<>(new Comparator<Result<String>>() {
			@Override
			public int compare(Result<String> o1, Result<String> o2) {
				return Double.compare(o1.normalizedHammingDistance, o2.normalizedHammingDistance);
			}
		});

		for (Entry<Result<String>, Double> e : distanceMap.entrySet()) {
			Result<String> matchedImage = e.getKey();
			matchedImage.normalizedHammingDistance = e.getValue();
			returnValues.add(matchedImage);
		}
		return returnValues;
	}

}
