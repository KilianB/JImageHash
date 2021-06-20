package dev.brachtendorf.jimagehash.matcher;

import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import dev.brachtendorf.jimagehash.hash.Hash;
import dev.brachtendorf.jimagehash.hashAlgorithms.HashingAlgorithm;

/**
 * Image matchers are a collection of classes which bundle the hashing operation
 * of one or multiple {@link dev.brachtendorf.jimagehash.hashAlgorithms.HashingAlgorithm
 * HashingAlgorithms} and expose functionalities to compare multiple images with
 * each other.
 * <p>
 * TypedImageMatchers accept hashing algorithms with additional configuration
 * details attached to each entry.
 * 
 * @author Kilian
 *
 */
public abstract class TypedImageMatcher {

	/**
	 * Contains multiple hashing algorithms applied in the order they were added to
	 * the image matcher
	 */
	protected LinkedHashMap<HashingAlgorithm, AlgoSettings> steps = new LinkedHashMap<>();

	/**
	 * Append a new hashing algorithm which will be executed after all hash
	 * algorithms passed the test.
	 * <p>
	 * The same algorithm may only be added once. Attempts to add an identical
	 * algorithm will instead update the settings of the old instance.
	 * <p>
	 * This method assumes the normalized hamming distance. If the definite distance
	 * shall be used take a look at
	 * {@link #addHashingAlgorithm(HashingAlgorithm, double, boolean)}
	 * 
	 * @param algo      The algorithms to be added
	 * @param threshold maximum normalized hamming distance between hashes in order
	 *                  to pass as identical image
	 */
	public void addHashingAlgorithm(HashingAlgorithm algo, double threshold) {
		addHashingAlgorithm(algo, threshold, true);
	}

	/**
	 * Append a new hashing algorithm which will be executed after all hash
	 * algorithms passed the test.
	 * 
	 * <p>
	 * The same algorithm may only be added once to an image hasher. Attempts to add
	 * an identical algorithm will instead update the settings of the old instance.
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

		if (threshold < 0) {
			throw new IllegalArgumentException(
					"Fatal error adding algorithm. Threshold has to be in the range of [0-X)");
		} else if (normalized && threshold > 1) {
			throw new IllegalArgumentException(
					"Fatal error adding algorithm. Normalized threshold has to be in the range of [0-1]");
		}

		steps.put(algo, new AlgoSettings(threshold, normalized));
	}

	/**
	 * Removes the hashing algorithms from the image matcher.
	 * 
	 * @param algo the algorithm to be removed
	 * @return true if the algorithms was removed, false otherwise
	 */
	public boolean removeHashingAlgo(HashingAlgorithm algo) {
		return steps.remove(algo) != null;
	}

	/**
	 * Remove all hashing algorithms used by this image matcher instance. At least
	 * one algorithm has to be supplied before imaages can be checked for similarity
	 */
	public void clearHashingAlgorithms() {
		steps.clear();
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

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((steps == null) ? 0 : steps.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		TypedImageMatcher other = (TypedImageMatcher) obj;
		if (steps == null) {
			if (other.steps != null)
				return false;
		} else if (!steps.equals(other.steps))
			return false;
		return true;
	}

	/**
	 * Settings used while computing if an algorithms consideres two images to be a
	 * close match
	 * 
	 * @author Kilian
	 *
	 */
	public static class AlgoSettings implements Serializable {

		private static final long serialVersionUID = 1L;
		/**
		 * Threshold value hash hamming may be for images to be considered equal
		 */
		private double threshold;
		/**
		 * Use normalized or ordinary hamming distance during calculation
		 */
		private boolean normalized;

		public AlgoSettings(double threshold, boolean normalized) {
			this.threshold = threshold;
			this.normalized = normalized;
		}

		public boolean apply(Hash hash, Hash hash1) {
			if (normalized) {
				return hash.normalizedHammingDistanceFast(hash1) <= threshold;
			} else {
				return hash.hammingDistanceFast(hash1) <= threshold;
			}
		}

		/**
		 * @return the threshold
		 */
		public double getThreshold() {
			return threshold;
		}

		/**
		 * @return if the normalized hamming distance shall be used.
		 */
		public boolean isNormalized() {
			return normalized;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + (normalized ? 1231 : 1237);
			long temp;
			temp = Double.doubleToLongBits(threshold);
			result = prime * result + (int) (temp ^ (temp >>> 32));
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (!(obj instanceof AlgoSettings)) {
				return false;
			}
			AlgoSettings other = (AlgoSettings) obj;
			if (normalized != other.normalized) {
				return false;
			}
			if (Double.doubleToLongBits(threshold) != Double.doubleToLongBits(other.threshold)) {
				return false;
			}
			return true;
		}

		@Override
		public String toString() {
			return "AlgoSettings [threshold=" + threshold + ", normalized=" + normalized + "]";
		}

	}
}
