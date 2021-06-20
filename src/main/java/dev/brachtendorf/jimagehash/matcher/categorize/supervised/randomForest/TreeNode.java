package dev.brachtendorf.jimagehash.matcher.categorize.supervised.randomForest;

import java.awt.image.BufferedImage;

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
	public abstract int[] predictAgainstAll(BufferedImage bi);

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