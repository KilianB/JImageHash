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
	 * @param data
	 */
	public void addData(T data) {
		this.data.add(data);
	}
	
	/**
	 * Return a strong reference to the arraylist backing this leaf
	 * @return
	 */
	public ArrayList<T>getData(){
		return data;
	}
	
}
