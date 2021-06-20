package dev.brachtendorf.jimagehash.matcher.cached;

import java.awt.image.BufferedImage;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.PriorityQueue;

import dev.brachtendorf.MathUtil;
import dev.brachtendorf.jimagehash.datastructures.tree.Result;
import dev.brachtendorf.jimagehash.datastructures.tree.binaryTree.BinaryTree;
import dev.brachtendorf.jimagehash.hash.Hash;
import dev.brachtendorf.jimagehash.hashAlgorithms.HashingAlgorithm;


/**
 * Convenience class allowing to chain multiple hashing algorithms to find
 * similar images. The CumulativeMatcher keeps the hashes and buffered images in
 * cache, allowing to add and remove hashing algorithms on the fly as well as
 * retrieving the bufferedimage object of matches. On the flip side this
 * approach requires much more memory and is unsuited for large collection of
 * images.
 * 
 * Contrary to the {@link dev.brachtendorf.jimagehash.matcher.cached.ConsecutiveMatcher
 * ConsecutiveMatcher}, instead of early aborting if one algorithm fails, this
 * class looks at the summed distance and decides if images match.
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
public class CumulativeMatcher extends ConsecutiveMatcher {

	/**
	 * Settings of the in memory matcher
	 */
	private AlgoSettings overallSetting;

	/**
	 * Create a cumulative in memory image matcher with the specified threshold
	 * 
	 * @param threshold The cutoff threshold of the summed and scaled normalizted
	 *                  distances of all hashing algorithm until an image is
	 *                  considered a match.
	 *                  <p>
	 *                  If a negative threshold is supplied no images will ever be
	 *                  returned.
	 *                  <p>
	 *                  If additional algorithms are added an increase in the
	 *                  threshold parameter is justified.
	 */
	public CumulativeMatcher(double threshold) {
		this(threshold, true);
	}

	public CumulativeMatcher(double threshold, boolean normalized) {
		this(new AlgoSettings(threshold, normalized));
	}

	public CumulativeMatcher(AlgoSettings setting) {
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
	public PriorityQueue<Result<BufferedImage>> getMatchingImages(BufferedImage image) {

		if (steps.isEmpty())
			throw new IllegalStateException(
					"Please supply at least one hashing algorithm prior to invoking the match method");

		// The maximum distance we have to search in our tree until we can't find any
		// more images
		double maxDistanceUntilTermination = overallSetting.getThreshold();

		// [Result,Summed distance of the image]
		HashMap<Result<BufferedImage>, Double> distanceMap = new HashMap<>();

		// During first iteration we need to do some extra hoops
		boolean first = true;

		// https://stackoverflow.com/a/31401836/3244464 TODO jmh benchmark
		float optimalLoadFactor = (float) Math.log(2);

		// For each hashing algorithm
		for (Entry<HashingAlgorithm, AlgoSettings> entry : steps.entrySet()) {
			HashingAlgorithm algo = entry.getKey();

			HashMap<Result<BufferedImage>, Double> temporaryMap;

			BinaryTree<BufferedImage> binTree = binTreeMap.get(algo);

			// Init temporary hashmap
			int optimalCapacity = (int) (Math
					.ceil((first ? binTree.getHashCount() : distanceMap.size()) / optimalLoadFactor) + 1);
			temporaryMap = new HashMap<>(optimalCapacity, optimalLoadFactor);

			Hash needleHash = algo.hash(image);

			int bitRes = algo.getKeyResolution();

			int threshold = 0;

			if (overallSetting.isNormalized()) {
				// Normalized threshold
				threshold = (int) (maxDistanceUntilTermination * bitRes);
			} else {
				threshold = (int) maxDistanceUntilTermination;
			}

			PriorityQueue<Result<BufferedImage>> temp = binTree.getElementsWithinHammingDistance(needleHash, threshold);

			// Find the min total distance for the next generation to specify our cutoff
			// parameter
			double minDistance = Double.MAX_VALUE;

			// filter manually
			for (Result<BufferedImage> res : temp) {

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
		PriorityQueue<Result<BufferedImage>> returnValues = new PriorityQueue<>(
				new Comparator<Result<BufferedImage>>() {
					@Override
					public int compare(Result<BufferedImage> o1, Result<BufferedImage> o2) {
						return Double.compare(o1.normalizedHammingDistance, o2.normalizedHammingDistance);
					}
				});

		for (Entry<Result<BufferedImage>, Double> e : distanceMap.entrySet()) {
			Result<BufferedImage> matchedImage = e.getKey();
			matchedImage.normalizedHammingDistance = e.getValue();
			returnValues.add(matchedImage);
		}
		return returnValues;
	}

}
