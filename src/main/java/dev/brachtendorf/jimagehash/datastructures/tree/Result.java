package dev.brachtendorf.jimagehash.datastructures.tree;

/**
 * Search result returned when querying the tree
 * 
 * @author Kilian
 *
 */
public class Result<T> implements Comparable<Result<T>> {
	/**
	 * Value saved at the leaf node
	 */
	public T value;
	/**
	 * The hamming distance to the actual hash supplied during the search
	 */
	public double distance;
	
	/**
	 * The normalized hamming distance to the actual hash supplied during the search
	 */
	public double normalizedHammingDistance;

	public Result(T value, double distance, double normalizedDistance) {
		this.value = value;
		this.distance = distance;
		this.normalizedHammingDistance = normalizedDistance;
	}

	@Override
	public int compareTo(Result<T> o) {
		return normalizedHammingDistance > o.normalizedHammingDistance ? 1 : normalizedHammingDistance == o.normalizedHammingDistance ? 0 : -1;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((value == null) ? 0 : value.hashCode());
		return result;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Result other = (Result) obj;
		if (value == null) {
			if (other.value != null)
				return false;
		} else if (!value.equals(other.value))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return String.format("Result Distance:%.3f Normalized Distance %.3f, Value:%s", distance, normalizedHammingDistance, value);
	}

}