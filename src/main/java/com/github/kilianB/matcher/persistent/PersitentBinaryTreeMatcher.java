package com.github.kilianB.matcher.persistent;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.logging.Logger;

import com.github.kilianB.datastructures.tree.binaryTree.BinaryTree;
import com.github.kilianB.hashAlgorithms.HashingAlgorithm;

/**
 * * Persistent image matchers are a subset of
 * {@link com.github.kilianB.matcher.ImageMatcher ImageMatcher} which can be
 * saved to disk to be later reconstructed. They expose the method
 * {@link #serializeState(File)} and {@link #reconstructState(File, boolean)}.
 * 
 * <p>
 * The {@link com.github.kilianB.matcher.persistent.PersitentBinaryTreeMatcher}
 * implementation utilizes a binary tree as it's core to save hashes storing a
 * reference to a unique identifier describing the hashed image. No data
 * reference to the actual object is saved which allows to keep the data
 * requirement small.
 * 
 * <p>
 * Since serialized hashes can not be updated or recreated due to the source of
 * the hash not being available anymore the persistent matcher needs to ensure
 * that old hashes stay valid. This means that the matcher forbids changing the
 * hashing algorithms used to created hashes as soon as a single hash was
 * created.
 * 
 * @author Kilian
 * @since 3.0.0
 */
public abstract class PersitentBinaryTreeMatcher extends PersistentImageMatcher {

	private static final long serialVersionUID = -4650598803470549478L;

	private static final Logger LOGGER = Logger.getLogger(ConsecutiveImageMatcher.class.getSimpleName());

	/** keep track of images already added. No reason to rehash */
	protected HashSet<String> addedImages = new HashSet<>();

	/** Binary Tree holding results for each individual hashing algorithm */
	protected HashMap<HashingAlgorithm, BinaryTree<String>> binTreeMap = new HashMap<>();

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
		BinaryTree<String> binTree = new BinaryTree<>(true);
		binTreeMap.put(algo, binTree);
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

	@Override
	protected void addImageInternal(String uniqueId, BufferedImage image) {
		if (addedImages.contains(uniqueId)) {
			LOGGER.info("An image with uniqueId already exists. Skip request");
		}
		for (Entry<HashingAlgorithm, AlgoSettings> entry : steps.entrySet()) {
			HashingAlgorithm algo = entry.getKey();
			BinaryTree<String> binTree = binTreeMap.get(algo);
			binTree.addHash(algo.hash(image), uniqueId);
		}
		addedImages.add(uniqueId);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((addedImages == null) ? 0 : addedImages.hashCode());
		result = prime * result + ((binTreeMap == null) ? 0 : binTreeMap.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!super.equals(obj)) {
			return false;
		}
		if (!(obj instanceof ConsecutiveImageMatcher)) {
			return false;
		}
		PersitentBinaryTreeMatcher other = (PersitentBinaryTreeMatcher) obj;
		if (addedImages == null) {
			if (other.addedImages != null) {
				return false;
			}
		} else if (!addedImages.equals(other.addedImages)) {
			return false;
		}
		if (binTreeMap == null) {
			if (other.binTreeMap != null) {
				return false;
			}
		} else if (!binTreeMap.equals(other.binTreeMap)) {
			return false;
		}
		return true;
	}

	/**
	 * Print all binary trees currently in use by this image matcher. This gives an
	 * internal view of the saved images
	 */
	public void printAllTrees() {
		binTreeMap.entrySet().forEach(c -> c.getValue().printTree());
	}

}
