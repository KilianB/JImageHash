package dev.brachtendorf.jimagehash.matcher.cached;

import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.PriorityQueue;

import dev.brachtendorf.jimagehash.datastructures.tree.Result;
import dev.brachtendorf.jimagehash.datastructures.tree.binaryTree.BinaryTree;
import dev.brachtendorf.jimagehash.hash.Hash;
import dev.brachtendorf.jimagehash.hashAlgorithms.HashingAlgorithm;
import dev.brachtendorf.jimagehash.matcher.TypedImageMatcher;

/**
 * Convenience class allowing to chain multiple hashing algorithms to find
 * similar images. The ConsecutiveMatcher keeps the hashes and buffered
 * images in cache, allowing to add and remove hashing algorithms on the fly as
 * well as retrieving the bufferedimage object of matches. On the flip side this
 * approach requires much more memory and is unsuited for large collection of
 * images.
 * 
 * <p>
 * InMemoryImage matchers apply the added hashing algorithms successively,
 * requiring all algorithms added to agree to produce a match, discarding the
 * result fast if one of the algorithms does not consider the images similar.
 * <p>
 * This means that the order of the hashing algorithms supplied hash has an
 * impact on the performance, the fastest algorithm should be added first.
 * 
 * @author Kilian
 */
public class ConsecutiveMatcher extends TypedImageMatcher {

	/** keep track of images already added. No reason to rehash */
	protected HashSet<BufferedImage> addedImages = new HashSet<>();

	/** Binary Tree holding results for each individual hashing algorithm */
	protected HashMap<HashingAlgorithm, BinaryTree<BufferedImage>> binTreeMap = new HashMap<>();

	/**
	 * Append a new hashing algorithm which will be executed after all hash
	 * algorithms passed the test.
	 * 
	 * @param algo       The algorithms to be added
	 * @param threshold  the threshold the hamming distance may be in order to pass
	 *                   as identical image.
	 * @param normalized Weather the normalized or default hamming distance shall be
	 *                   used. The normalized hamming distance will be in range of
	 *                   [0-1] while the hamming distance depends on the length of
	 *                   the hash
	 */
	public void addHashingAlgorithm(HashingAlgorithm algo, double threshold, boolean normalized) {
		super.addHashingAlgorithm(algo, threshold, normalized);

		BinaryTree<BufferedImage> binTree = new BinaryTree<>(true);
		binTreeMap.put(algo, binTree);

		// Also add all images which were added to the image matcher earlier
		if (!addedImages.isEmpty()) {
			addedImages.forEach(image -> binTree.addHash(algo.hash(image), image));
		}
	}

	/**
	 * Removes the hashing algorithms from the image matcher.
	 * 
	 * @param algo the algorithm to be removed
	 * @return true if the algorithms was removed, false otherwise
	 */
	public boolean removeHashingAlgo(HashingAlgorithm algo) {
		binTreeMap.remove(algo);
		return super.removeHashingAlgo(algo);
	}

	/**
	 * Remove all hashing algorithms used by this image matcher instance. At least
	 * one algorithm has to be supplied before imaages can be checked for similarity
	 */
	public void clearHashingAlgorithms() {
		binTreeMap.clear();
		super.clearHashingAlgorithms();
	}

	/**
	 * Return an immutable map copy of the algorithms currently used in the matcher.
	 * This map is a hard copy of the hashmap and does not updated if the underlying
	 * collection gets altered
	 * 
	 * @return A map containing all algorithms used in this matcher
	 */
	public Map<HashingAlgorithm, AlgoSettings> getAlgorithms() {
		return Collections.unmodifiableMap(new LinkedHashMap<HashingAlgorithm, AlgoSettings>(steps));
	}

	/**
	 * Add the image to the matcher allowing the image to be found in future
	 * searches.
	 * 
	 * @param image The image whose hash will be added to the matcher
	 */
	public void addImage(BufferedImage image) {
		if (steps.isEmpty())
			throw new IllegalStateException(
					"Please supply at least one hashing algorithm prior to invoking the match method");

		if (addedImages.contains(image)) {
			return;
		}

		for (Entry<HashingAlgorithm, AlgoSettings> entry : steps.entrySet()) {
			HashingAlgorithm algo = entry.getKey();
			BinaryTree<BufferedImage> binTree = binTreeMap.get(algo);
			binTree.addHash(algo.hash(image), image);
		}
		addedImages.add(image);
	}

	/**
	 * Add the images to the matcher allowing the image to be found in future
	 * searches.
	 * 
	 * @param imagesToAdd The images whose hash will be added to the matcher
	 */
	public void addImages(BufferedImage... imagesToAdd) {
		for (BufferedImage img : imagesToAdd) {
			this.addImage(img);
		}
	}

	/**
	 * Search for all similar images passing the algorithm filters supplied to this
	 * matcher. If the image itself was added to the tree it will be returned with a
	 * distance of 0
	 * 
	 * @param image The image other images will be matched against
	 * @return Similar images Return all images sorted by the
	 *         <a href="https://en.wikipedia.org/wiki/Hamming_distance">hamming
	 *         distance</a> of the last applied algorithms
	 */
	public PriorityQueue<Result<BufferedImage>> getMatchingImages(BufferedImage image) {

		if (steps.isEmpty())
			throw new IllegalStateException(
					"Please supply at least one hashing algorithm prior to invoking the match method");

		PriorityQueue<Result<BufferedImage>> returnValues = null;

		for (Entry<HashingAlgorithm, AlgoSettings> entry : steps.entrySet()) {
			HashingAlgorithm algo = entry.getKey();

			BinaryTree<BufferedImage> binTree = binTreeMap.get(algo);
			AlgoSettings settings = entry.getValue();

			Hash needleHash = algo.hash(image);

			int threshold = 0;
			if (settings.isNormalized()) {
				int hashLength = needleHash.getBitResolution();
				threshold = (int) Math.round(settings.getThreshold() * hashLength);
			} else {
				threshold = (int) settings.getThreshold();
			}

			PriorityQueue<Result<BufferedImage>> temp = binTree.getElementsWithinHammingDistance(needleHash, threshold);

			if (returnValues == null) {
				returnValues = temp;
			} else {
				temp.retainAll(returnValues);
				returnValues = temp;
			}
		}
		return returnValues;
	}

	/**
	 * Print all binary trees currently in use by this image matcher. This gives an
	 * internal view of the saved images
	 */
	public void printAllTrees() {
		binTreeMap.entrySet().forEach(c -> c.getValue().printTree());
	}

	// Don't keep a reference to the image so the garbage collector can release it
}
