package dev.brachtendorf.jimagehash.matcher;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import dev.brachtendorf.jimagehash.hashAlgorithms.HashingAlgorithm;

/**
 * Image matchers are a collection of classes bundling the hashing operation of
 * one or multiple {@link dev.brachtendorf.jimagehash.hashAlgorithms.HashingAlgorithm
 * HashingAlgorithms} together and usually exposing functionalities to compare
 * multiple images with each other.
 * <p>
 * PlainImage matchers accept hashing algorithms with no further configuration
 * arguments.
 *
 * @author Kilian
 * @since 3.0.0
 * @see dev.brachtendorf.jimagehash.matcher.TypedImageMatcher
 */
public class PlainImageMatcher {

	TypedImageMatcher t;

	protected LinkedHashSet<HashingAlgorithm> steps = new LinkedHashSet<>();

	/**
	 * Append a new hashing algorithm to be used by this matcher. The same algorithm
	 * may only be added once. Attempts to add the same algorithm twice is a NOP.
	 * <p>
	 * For some matchers the order of added hashing algorithms is crucial. The order
	 * the hashes are added is preserved.
	 * 
	 * @param hashingAlgorithm The algorithms to be added
	 * @return true if the algorithm was added, false if it was already present
	 */
	public boolean addHashingAlgorithm(HashingAlgorithm hashingAlgorithm) {
		return this.steps.add(hashingAlgorithm);
	}

	/**
	 * Remove a hashing algorithm from this matcher.
	 * 
	 * @param hashingAlgorithm The algorithms to be removed
	 * @return true if the algorithm was removed, false if it was not present.
	 */
	public boolean removeHashingAlgorithm(HashingAlgorithm hashingAlgorithm) {
		return this.steps.remove(hashingAlgorithm);
	}

	/**
	 * Remove all hashing algorithms used by this image matcher instance. At least
	 * one algorithm has to be supplied before images can be processed
	 */
	public void clearHashingAlgorithms() {
		steps.clear();
	}

	/**
	 * @return an unmofifiable collection of the hashing algorithms added to this
	 *         matcher.
	 */
	public Set<HashingAlgorithm> getAlgorithms() {
		return Collections.unmodifiableSet(steps);
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
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof PlainImageMatcher)) {
			return false;
		}
		PlainImageMatcher other = (PlainImageMatcher) obj;
		if (steps == null) {
			if (other.steps != null) {
				return false;
			}
		} else if (!steps.equals(other.steps)) {
			return false;
		}
		return true;
	}

}
