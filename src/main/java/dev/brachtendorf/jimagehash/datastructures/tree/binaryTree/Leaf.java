package dev.brachtendorf.jimagehash.datastructures.tree.binaryTree;

import java.util.ArrayList;

/**
 * A leaf node in the binary tree containing multiple values associated with 
 * a specific hash value
 * @author Kilian
 *
 */
public class Leaf<T> extends Node{

	/**
	 * Values saved in this leaf
	 */
	private ArrayList<T> data = new ArrayList<>();
	
	/**
	 * Append new data to the leaf
	 * @param data	Value which will be associated with the hash this leaf represents
	 */
	public void addData(T data) {
		this.data.add(data);
	}
	
	/**
	 * @return a strong reference to the arraylist backing this leaf
	 */
	public ArrayList<T>getData(){
		return data;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((data == null) ? 0 : data.hashCode());
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
		if (!(obj instanceof Leaf)) {
			return false;
		}
		Leaf<?> other = (Leaf<?>) obj;
		if (data == null) {
			if (other.data != null) {
				return false;
			}
		} else if (!data.equals(other.data)) {
			return false;
		}
		return true;
	}
	
}
