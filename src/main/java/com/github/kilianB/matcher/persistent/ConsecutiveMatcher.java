package com.github.kilianB.matcher.persistent;

import java.awt.image.BufferedImage;
import java.util.Map.Entry;
import java.util.PriorityQueue;

import com.github.kilianB.datastructures.tree.Result;
import com.github.kilianB.datastructures.tree.binaryTree.BinaryTree;
import com.github.kilianB.hash.Hash;
import com.github.kilianB.hashAlgorithms.HashingAlgorithm;

/**
 * Convenience class allowing to chain multiple hashing algorithms to find
 * similar images. The ConsecutiveMatcher keeps the hashes and buffered images
 * in cache.
 * 
 * @author Kilian
 *
 */
public class ConsecutiveMatcher extends PersitentBinaryTreeMatcher {

	/**
	 * @param cacheAddedHashes Additionally to the binary tree, hashes of added
	 *                         images will be mapped to their uniqueId allowing to
	 *                         retrieve matches of added images without loading the
	 *                         image file from disk. This setting increases memory
	 *                         overhead in exchange for performance.
	 *                         <p>
	 *                         Use this setting if calls to
	 *                         {@link #getMatchingImages(java.io.File)} likely
	 *                         contains an image already added to the match prior.
	 */
	public ConsecutiveMatcher(boolean cacheAddedHashes) {
		super(cacheAddedHashes);
	}

	private static final long serialVersionUID = 831914616034052308L;

	/**
	 * A preconfigured image matcher chaining dHash and pHash algorithms for fast
	 * high quality results.
	 * <p>
	 * The dHash is a quick algorithms allowing to filter images which are very
	 * unlikely to be similar images. pHash is computationally more expensive and
	 * used to inspect possible candidates further
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
	 * 
	 * @return The matcher used to check if images are similar
	 */
	public static PersitentBinaryTreeMatcher createDefaultMatcher(boolean cacheAddedHashes) {
		return createDefaultMatcher(cacheAddedHashes, Setting.Quality);
	}

	/**
	 * A preconfigured image matcher chaining dHash and pHash algorithms for fast
	 * high quality results.
	 * <p>
	 * The dHash is a quick algorithms allowing to filter images which are very
	 * unlikely to be similar images. pHash is computationally more expensive and
	 * used to inspect possible candidates further
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
	public static ConsecutiveMatcher createDefaultMatcher(boolean cacheAddedHashes, Setting algorithmSetting) {
		ConsecutiveMatcher matcher = new ConsecutiveMatcher(cacheAddedHashes);
		matcher.addDefaultHashingAlgorithms(matcher, algorithmSetting);
		return matcher;
	}

	protected PriorityQueue<Result<String>> getMatchingImagesInternal(BufferedImage image, String uniqueId) {

		if (steps.isEmpty())
			throw new IllegalStateException(
					"Please supply at least one hashing algorithm prior to invoking the match method");

		PriorityQueue<Result<String>> returnValues = null;

		for (Entry<HashingAlgorithm, AlgoSettings> entry : steps.entrySet()) {
			HashingAlgorithm algo = entry.getKey();

			BinaryTree<String> binTree = binTreeMap.get(algo);
			AlgoSettings settings = entry.getValue();

			Hash needleHash = getHash(algo, uniqueId, image);

			int threshold = 0;
			if (settings.isNormalized()) {
				int hashLength = needleHash.getBitResolution();
				threshold = (int) Math.round(settings.getThreshold() * hashLength);
			} else {
				threshold = (int) settings.getThreshold();
			}

			PriorityQueue<Result<String>> temp = binTree.getElementsWithinHammingDistance(needleHash, threshold);

			if (returnValues == null) {
				returnValues = temp;
			} else {
				temp.retainAll(returnValues);
				returnValues = temp;
			}
		}
		return returnValues;
	}

	// Don't keep a reference to the image so the garbage collector can release it
}
