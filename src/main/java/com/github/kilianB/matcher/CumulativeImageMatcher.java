package com.github.kilianB.matcher;

import java.awt.image.BufferedImage;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.PriorityQueue;

import com.github.kilianB.MathUtil;
import com.github.kilianB.dataStrorage.tree.BinaryTree;
import com.github.kilianB.dataStrorage.tree.Result;
import com.github.kilianB.hashAlgorithms.AverageHash;
import com.github.kilianB.hashAlgorithms.DifferenceHash;
import com.github.kilianB.hashAlgorithms.HashingAlgorithm;
import com.github.kilianB.hashAlgorithms.PerceptiveHash;
import com.github.kilianB.hashAlgorithms.RotPHash;
import com.github.kilianB.hashAlgorithms.DifferenceHash.Precision;
import com.github.kilianB.matcher.ImageMatcher.Setting;

/**
 * Instead of early aborting if one algorithm fails like the
 * {@link com.github.kilianB.matcher.InMemoryImageMatcher}, this class looks at
 * the summed distance and decides if images match.
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
public class CumulativeImageMatcher extends InMemoryImageMatcher {

	/**
	 * Settings of the in memory matcher
	 */
	private AlgoSettings overallSetting;

	/**
	 * A preconfigured image matcher chaining dHash and pHash algorithms for fast
	 * high quality results.
	 * <p>
	 * The dHash is a quick algorithms allowing to filter images which are very
	 * unlikely to be similar images. pHash is computationally more expensive and
	 * used to inspect possible candidates further
	 * 
	 * @return The matcher used to check if images are similar
	 */
	public static CumulativeImageMatcher createDefaultMatcher() {
		return createDefaultMatcher(Setting.Quality);
	}

	/**
	 * A preconfigured image matcher chaining dHash and pHash algorithms for fast
	 * high quality results.
	 * <p>
	 * The dHash is a quick algorithms allowing to filter images which are very
	 * unlikely to be similar images. pHash is computationally more expensive and
	 * used to inspect possible candidates further
	 * 
	 * @param algorithmSetting
	 *                         <p>
	 *                         How aggressive the algorithm advances while comparing
	 *                         images
	 *                         </p>
	 *                         <ul>
	 *                         <li><b>Forgiving:</b> Matches a bigger range of
	 *                         images</li>
	 *                         <li><b>Fair:</b> Matches all sample images</li>
	 *                         <li><b>Quality:</b> Recommended: Does not initially
	 *                         filter as aggressively as Fair but returns usable
	 *                         results</li>
	 *                         <li><b>Strict:</b> Only matches images which are
	 *                         closely related to each other</li>
	 *                         </ul>
	 * 
	 * @return The matcher used to check if images are similar
	 */
	public static CumulativeImageMatcher createDefaultMatcher(Setting algorithmSetting) {
		CumulativeImageMatcher matcher = null;

		switch (algorithmSetting) {
		case Speed:
			matcher = new CumulativeImageMatcher(0.6f);
			// Chain in the order of execution speed
			matcher.addHashingAlgorithm(new AverageHash(16), 1f);
			matcher.addHashingAlgorithm(new DifferenceHash(64, Precision.Simple), 1f);
			break;
		case Rotational:
			// PHash scales better for higher resolutions. Average hash is good as well but
			// do we need to add it here?
			matcher = new CumulativeImageMatcher(0.2f);
			matcher.addHashingAlgorithm(new RotPHash(64), 1f);
			// matcherToConfigure.addHashingAlgorithm(new RotAverageHash (32),0.21f);
		case Forgiving:

			matcher = new CumulativeImageMatcher(0.8f);
			matcher.addHashingAlgorithm(new AverageHash(64), 1);
			// Add some mroe weight to the perceptive hash since it usually is more accurate
			matcher.addHashingAlgorithm(new PerceptiveHash(32), 2f);
			break;
		case Quality:
		case Fair:
			matcher = new CumulativeImageMatcher(0.5f);
			matcher.addHashingAlgorithm(new AverageHash(64), 1);
			// Add some more weight to the perceptive hash since it usually is more accurate
			matcher.addHashingAlgorithm(new PerceptiveHash(32), 2f);
			break;
		case Strict:
			matcher = new CumulativeImageMatcher(0.3f);
			matcher.addHashingAlgorithm(new AverageHash(8), 1f);
			matcher.addHashingAlgorithm(new PerceptiveHash(32), 1f);
			matcher.addHashingAlgorithm(new PerceptiveHash(64), 1f);
			break;
		default:
			throw new IllegalArgumentException("Setting not handled");
		}
		return matcher;
	}

	/**
	 * Create a cumulative in memory image matcher with the specified threshold
	 * 
	 * @param threshold The cutoff threshold of the summed and scaled distances of
	 *                  all hashing algorithm until an image is considered a match.
	 *                  <p>
	 *                  If a negative threshold is supplied no images will ever be
	 *                  returned.
	 *                  <p>
	 *                  If additional algorithms are added an increase in the
	 *                  threshold parameter is justified.
	 */
	public CumulativeImageMatcher(float threshold) {
		this(threshold, true);
	}

	public CumulativeImageMatcher(float threshold, boolean normalized) {
		this(new AlgoSettings(threshold, normalized));
	}

	public CumulativeImageMatcher(AlgoSettings setting) {
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
	 * The weight parameter scales the returned hash distance. For example a weight multiplier
	 * of 2 means that the returned distance is multiplied by 2. If the total allowed
	 * distance of this matcher is 0.7 and the returned hash is 0.3 * 2 the next algorithm
	 * only may return a distance of 0.1 or smaller in order for the image to pass. 
	 * 
	 * @param algo   The algorithms to be added
	 * @param weight The weight multiplier of this algorithm.
	 */
	public void addHashingAlgorithm(HashingAlgorithm algo, float weight) {
		/*
		 * only used to redefine javadocs Add false to cirumvent the range check. We do
		 * not check for normalized in this case either way
		 */
		super.addHashingAlgorithm(algo, weight, false);
	}

	/**
	 * Add a hashing algorithm to the matcher with the given weight multiplier. In
	 * order for images to match they have to be beneath the threshold of the summed
	 * distances of all added hashing algorithms.
	 * 
	 * * <p>
	 * The weight parameter scales the returned hash distance. For example a weight multiplier
	 * of 2 means that the returned distance is multiplied by 2. If the total allowed
	 * distance of this matcher is 0.7 and the returned hash is 0.3 * 2 the next algorithm
	 * only may return a distance of 0.1 or smaller in order for the image to pass. 
	 * 
	 * @param algo   The algorithms to be added
	 * @param weight The weight multiplier of this algorithm.
	 * @param dummy  not used by this type of image matcher. This method signature
	 *               is only available due to inheritance.
	 */
	public void addHashingAlgorithm(HashingAlgorithm algo, float weight, boolean dummy) {
		// only used to redefine javadocs
		super.addHashingAlgorithm(algo, weight, false);
	}

	public PriorityQueue<Result<BufferedImage>> getMatchingImages(BufferedImage image) {

		if (steps.isEmpty())
			throw new IllegalStateException(
					"Please supply at least one hashing algorithm prior to invoking the match method");

		// The maximum distance we have to search in our tree until we can't find any
		// more images
		double maxDistanceUntilTermination = overallSetting.threshold;

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

			if (overallSetting.normalized) {
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
						double distanceLeft = overallSetting.threshold - distanceSoFar;

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
