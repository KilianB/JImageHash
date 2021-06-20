package dev.brachtendorf.jimagehash.matcher.persistent;

import java.awt.image.BufferedImage;
import java.util.Map.Entry;
import java.util.PriorityQueue;

import dev.brachtendorf.jimagehash.datastructures.tree.Result;
import dev.brachtendorf.jimagehash.datastructures.tree.binaryTree.BinaryTree;
import dev.brachtendorf.jimagehash.hash.Hash;
import dev.brachtendorf.jimagehash.hashAlgorithms.HashingAlgorithm;

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
