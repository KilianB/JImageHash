package com.github.kilianB.matcher.supervised.randomForest;

import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Map;

import com.github.kilianB.hashAlgorithms.HashingAlgorithm;
import com.github.kilianB.matcher.Hash;

abstract class TreeNode {

	/**
	 * Check if the buffered image is considered a match by this subtree. This
	 * method takes into account itself and all subsequent lower nodes. To receive
	 * an accurate result this function should only be called on the root node.
	 * 
	 * @param bi the buffered image to check
	 * @param precomputedHashes
	 * @return true if the image is considered a match, false otherwise
	 */
	public abstract boolean predictAgainstAll(BufferedImage bi, Map<HashingAlgorithm, List<Hash>> precomputedHashes);

	/**
	 * Check if the buffered image is considered a match by this subtree. This
	 * method takes into account itself and all subsequent lower nodes. To receive
	 * an accurate result this function should only be called on the root node.
	 * 
	 * Opposed to the method {@link #predictAgainstAll(BufferedImage, Map)} this
	 * function takes a map containing a reference to a buffered image allowing to
	 * exclude self matches. Mostly applicable during training.
	 * 
	 * @param bi
	 * @param precomputedHashes
	 * @return true if the image is considered a match, false otherwise
	 */
	public abstract boolean predictAgainstAllExcludingSelf(BufferedImage bi,
			Map<HashingAlgorithm, Map<BufferedImage, Hash>> precomputedHashes);

	/**
	 * 
	 * @param depth
	 */
	protected abstract void printTree(int depth);

	/**
	 * 
	 */
	public void printTree() {
		printTree(0);
	}
}