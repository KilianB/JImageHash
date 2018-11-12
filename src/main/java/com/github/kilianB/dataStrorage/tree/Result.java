package com.github.kilianB.dataStrorage.tree;

/**
 * Search result returned when querying the tree
 * 
 * @author Kilians
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
	public int distance;
	// TODO maybe add the hash as big integer this result was found at (also save it
	// in the leaf?)

	public Result(T value, int distance) {
		this.value = value;
		this.distance = distance;
	}

	public T getValue() {
		return value;
	}

	public int getDistance() {
		return distance;
	}

	@Override
	public int compareTo(Result<T> o) {
		return distance > o.distance ? 1 : distance == o.distance ? 0 : -1;
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
		return String.format("Result Distance:%3d, Value:%s", distance, value);
	}

}