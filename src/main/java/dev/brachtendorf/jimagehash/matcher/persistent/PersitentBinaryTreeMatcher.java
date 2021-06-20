package dev.brachtendorf.jimagehash.matcher.persistent;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.PriorityQueue;
import java.util.logging.Logger;

import dev.brachtendorf.jimagehash.datastructures.tree.Result;
import dev.brachtendorf.jimagehash.datastructures.tree.binaryTree.BinaryTree;
import dev.brachtendorf.jimagehash.hash.Hash;
import dev.brachtendorf.jimagehash.hashAlgorithms.HashingAlgorithm;

/**
 * * Persistent image matchers are a subset of
 * {@link dev.brachtendorf.jimagehash.matcher.TypedImageMatcher TypedImageMatcher} which
 * can be saved to disk to be later reconstructed. They expose the method
 * {@link #serializeState(File)} and {@link #reconstructState(File, boolean)}.
 * 
 * <p>
 * The {@link dev.brachtendorf.jimagehash.matcher.persistent.PersitentBinaryTreeMatcher}
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

	private static final Logger LOGGER = Logger.getLogger(ConsecutiveMatcher.class.getSimpleName());

	/** keep track of images already added. No reason to rehash */
	protected HashSet<String> addedImages = new HashSet<>();

	/** Binary Tree holding results for each individual hashing algorithm */
	protected HashMap<HashingAlgorithm, BinaryTree<String>> binTreeMap = new HashMap<>();

	protected boolean cacheAddedHashes;

	/**
	 * Save the hashes of added images mapped to their unique id for fast retrieval.
	 */
	protected Map<HashingAlgorithm, Map<String, Hash>> cachedHashes;

	/**
	 * TODO handle serialization empty constructor?
	 * 
	 * @param cacheAddedHashes Additionally to the binary tree hashes of added
	 *                         images will be mapped to their uniqueId. This allows
	 *                         to retrieve matches of added images without loading
	 *                         the image file from disk speeding up the operation
	 *                         Immensely. Saving the hash in a hashmap increases
	 *                         memory overhead.
	 *                         <p>
	 *                         Use this setting if
	 *                         {@link #getMatchingImages(java.io.File)} likely
	 *                         contains an image already added to the match prior.
	 */
	public PersitentBinaryTreeMatcher(boolean cacheAddedHashes) {
		this.cacheAddedHashes = cacheAddedHashes;
		if (cacheAddedHashes) {
			cachedHashes = new HashMap<>();
		}
	}

	@Override
	public PriorityQueue<Result<String>> getMatchingImages(File image) throws IOException {
		if (cacheAddedHashes && addedImages.contains(image.getAbsolutePath())) {
			// Quick retrieval possible. We don't need to read the file since the hashes are
			// cached
			return getMatchingImagesInternal(null, image.getAbsolutePath());
		} else {
			return super.getMatchingImages(image);
		}
	}

	@Override
	public PriorityQueue<Result<String>> getMatchingImages(BufferedImage image) {
		return getMatchingImagesInternal(image, null);
	}

	/**
	 * Return a list of images that are considered matching by the definition of
	 * this matcher.
	 * <p>
	 * This method is propagated by the super class allowing to utilize caching
	 * techniques to avoid reloading known images. Either the bufferedImage or the
	 * uniqueId argument is send depending on if the uniqueId is enough to query the
	 * hashes using the {@link #getHash(HashingAlgorithm, String, BufferedImage)}
	 * method call.
	 * 
	 * @param bi the buffered image to match or null
	 * @param uniqueId the uniqueId of a previously cached image or null
	 * @return a list of unique id's identifying the previously matched images
	 *         sorted by distance.
	 */
	protected abstract PriorityQueue<Result<String>> getMatchingImagesInternal(BufferedImage bi, String uniqueId);

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
		if (cacheAddedHashes) {
			cachedHashes.put(algo, new HashMap<>());
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
		if (cacheAddedHashes) {
			cachedHashes.remove(algo);
		}
		return super.removeHashingAlgo(algo);
	}

	/**
	 * Remove all hashing algorithms used by this image matcher instance. At least
	 * one algorithm has to be supplied before imaages can be checked for similarity
	 */
	public void clearHashingAlgorithms() {
		binTreeMap.clear();
		if (cacheAddedHashes) {
			cachedHashes.clear();
		}
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
			Hash hash = algo.hash(image);
			binTree.addHash(algo.hash(image), uniqueId);
			if (cacheAddedHashes) {
				cachedHashes.get(algo).put(uniqueId, hash);
			}
		}
		addedImages.add(uniqueId);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((addedImages == null) ? 0 : addedImages.hashCode());
		result = prime * result + ((binTreeMap == null) ? 0 : binTreeMap.hashCode());
		result = prime * result + (cacheAddedHashes ? 1231 : 1237);
		result = prime * result + ((cachedHashes == null) ? 0 : cachedHashes.hashCode());
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
		if (!(obj instanceof PersitentBinaryTreeMatcher)) {
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
		if (cacheAddedHashes != other.cacheAddedHashes) {
			return false;
		}
		if (cachedHashes == null) {
			if (other.cachedHashes != null) {
				return false;
			}
		} else if (!cachedHashes.equals(other.cachedHashes)) {
			return false;
		}
		return true;
	}

	protected Hash getHash(HashingAlgorithm algo, String uniqueId, BufferedImage bImage) {
		if (uniqueId != null && cachedHashes.get(algo).containsKey(uniqueId)) {
			return cachedHashes.get(algo).get(uniqueId);
		}
		if (bImage != null) {
			return algo.hash(bImage);
		}
		throw new IllegalStateException("No hash and buffered image supplied. Can't retrieve hash");
	}

	/**
	 * Print all binary trees currently in use by this image matcher. This gives an
	 * internal view of the saved images
	 */
	public void printAllTrees() {
		binTreeMap.entrySet().forEach(c -> c.getValue().printTree());
	}

}
