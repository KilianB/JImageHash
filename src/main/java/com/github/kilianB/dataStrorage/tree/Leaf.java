package com.github.kilianB.dataStrorage.tree;

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
	
}
