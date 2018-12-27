package com.github.kilianB.matcher;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

import com.github.kilianB.hashAlgorithms.AverageHash;
import com.github.kilianB.hashAlgorithms.DifferenceHash;
import com.github.kilianB.hashAlgorithms.DifferenceHash.Precision;
import com.github.kilianB.hashAlgorithms.HashingAlgorithm;
import com.github.kilianB.hashAlgorithms.PerceptiveHash;
import com.github.kilianB.hashAlgorithms.RotPHash;

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
		 */
		Forgiving,
		/**
		 * Permissive setting. A bit more strict than forgiving
		 */
		Fair,
		/**
		 * Strict image matcher. Only matches close images.
		 */
		Strict,
		/**
		 * Default setting used as non argument. Currently the same as fair.
		 */
		Quality,
		/**
		 * An image matcher which is robust against rotational transforms
		 */
		Rotational,

		/**
		 * Preset focusing greatly on speed rather than accuracy.
		 */
		Speed

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

		case Speed:
			// Chain in the order of execution speed
			matcherToConfigure.addHashingAlgorithm(new AverageHash(16), 0.31);
			matcherToConfigure.addHashingAlgorithm(new DifferenceHash(64, Precision.Simple), 0.31);
			matcherToConfigure.addHashingAlgorithm(new PerceptiveHash(16), 0.33);
			break;
		case Rotational:
			// PHash scales better for higher resolutions. Average hash is good as well but
			// do we need to add it here?
			matcherToConfigure.addHashingAlgorithm(new RotPHash(64), 0.19);
		case Forgiving:
			matcherToConfigure.addHashingAlgorithm(new AverageHash(64), 0.5);
			matcherToConfigure.addHashingAlgorithm(new PerceptiveHash(32), 0.5);
			break;
		case Quality:
		case Fair:
			matcherToConfigure.addHashingAlgorithm(new AverageHash(64), 0.4);
			matcherToConfigure.addHashingAlgorithm(new PerceptiveHash(32), 0.3);
			break;
		case Strict:
			matcherToConfigure.addHashingAlgorithm(new AverageHash(8), 0);
			matcherToConfigure.addHashingAlgorithm(new PerceptiveHash(32), 0.15);
			matcherToConfigure.addHashingAlgorithm(new PerceptiveHash(64), 0.15);
			break;
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
	 * <p>
	 * The same algorithm may only be added once to an image hasher. Attempts to add
	 * an identical algorithm will instead update the settings of the old instance.
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
		double threshold;
		/**
		 * Use normalized or ordinary hamming distance during calculation
		 */
		boolean normalized;

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
