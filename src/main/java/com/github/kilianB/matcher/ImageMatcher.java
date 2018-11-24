package com.github.kilianB.matcher;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

import com.github.kilianB.hashAlgorithms.DifferenceHash;
import com.github.kilianB.hashAlgorithms.DifferenceHash.Precision;
import com.github.kilianB.hashAlgorithms.HashingAlgorithm;
import com.github.kilianB.hashAlgorithms.PerceptiveHash;

public abstract class ImageMatcher {

	/**
	 * Configuration level for the default matcher
	 * 
	 * @author Kilian
	 *
	 */
	// TODO rename description. It'S not set 
	public enum Setting {
		/**
		 * Most permissive setting. Many images will be matched. Be aware of false
		 * positives
		 * <ul>
		 * <li>DifferenceHash(32,Precision.Double) threshold 20</li>
		 * <li>PerceptiveHash(32) threshold 15</li>
		 * </ul>
		 */
		Forgiving,
		/**
		 * 2nd most permissive setting. Be aware of false positives
		 * <ul>
		 * <li>DifferenceHash(32,Precision.Double) threshold 15</li>
		 * <li>PerceptiveHash(32) threshold 10</li>
		 * </ul>
		 */
		Fair,
		/**
		 * Strict image matcher. Only very close images will be matched. e.g. Thumbnail
		 * does not trigger the matcher.
		 * <ul>
		 * <li>DifferenceHash(32,Precision.Double) threshold 10</li>
		 * <li>PerceptiveHash(32) threshold 6</li>
		 * </ul>
		 */
		Strict,
		/**
		 * Recommended for the supplied test images.
		 * <p>
		 * DiffernceHash(32, Double precision) threshold 15 followed by
		 * PerceptiveHash(32) threshold 15
		 */
		Quality
	}

	/**
	 * Factory helper method to configure a default hashing algorithm. Implementing
	 * classes may use this utility function to prevent repetitive code but are free
	 * to redefine what each setting stands for.
	 * 
	 * @param matcherToConfigure The matcher to add algorithm to
	 * @param settings           the configuration level of the matcher
	 */
	protected void addDefaultHashingAlgorithms(ImageMatcher matcherToConfigure, Setting settings) {
		switch (settings) {
		case Forgiving:
			matcherToConfigure.addHashingAlgorithm(new DifferenceHash(32, Precision.Double), 0.78f);
			matcherToConfigure.addHashingAlgorithm(new PerceptiveHash(32), 0.5f);
			break;
		case Fair:
			matcherToConfigure.addHashingAlgorithm(new DifferenceHash(32, Precision.Double), 0.78f);
			matcherToConfigure.addHashingAlgorithm(new PerceptiveHash(32), 0.3125f);
			break;
		case Strict:
			matcherToConfigure.addHashingAlgorithm(new DifferenceHash(32, Precision.Double), 0.3125f);
			matcherToConfigure.addHashingAlgorithm(new PerceptiveHash(32), 0.1875f);
			break;
		case Quality:
			matcherToConfigure.addHashingAlgorithm(new DifferenceHash(32, Precision.Double), 0.625f);
			matcherToConfigure.addHashingAlgorithm(new PerceptiveHash(32), 0.46875f);
		}
	}

	/**
	 * Contains multiple hashing algorithms applied in the order they were added to
	 * the image matcher
	 */
	protected LinkedHashMap<HashingAlgorithm, AlgoSettings> steps = new LinkedHashMap<>();

	/**
	 * Append a new hashing algorithm which will be executed after all hash
	 * algorithms passed the test.
	 * 
	 * <p>
	 * This method assumes the normalized hamming distance. If the definite distance
	 * shall be used take a look at
	 * {@link #addHashingAlgorithm(HashingAlgorithm, float, boolean)}
	 * 
	 * @param algo      The algorithms to be added
	 * @param threshold maximum normalized hamming distance between hashes in order
	 *                  to pass as identical image
	 */
	public void addHashingAlgorithm(HashingAlgorithm algo, float threshold) {
		addHashingAlgorithm(algo, threshold, true);
	}

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
	public void addHashingAlgorithm(HashingAlgorithm algo, float threshold, boolean normalized) {

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
	 * Return a strong reference to the algorithm hashmap. Altering this map
	 * directly affects the image matcher. Be aware that this instance is not thread
	 * safe.
	 * 
	 * @return A map containing all algorithms used in this matcher
	 */
	public Map<HashingAlgorithm, AlgoSettings> getAlgorithms() {
		return steps;
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
		ImageMatcher other = (ImageMatcher) obj;
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
		float threshold;
		/**
		 * Use normalized or ordinary hamming distance during calculation
		 */
		boolean normalized;

		public AlgoSettings(float threshold, boolean normalized) {
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
		public float getThreshold() {
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
			result = prime * result + Float.floatToIntBits(threshold);
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
			AlgoSettings other = (AlgoSettings) obj;
			if (normalized != other.normalized)
				return false;
			if (Float.floatToIntBits(threshold) != Float.floatToIntBits(other.threshold))
				return false;
			return true;
		}

		@Override
		public String toString() {
			return "AlgoSettings [threshold=" + threshold + ", normalized=" + normalized + "]";
		}

	}
}
