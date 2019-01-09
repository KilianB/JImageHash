package com.github.kilianB.matcher.supervised.randomForest;

import java.awt.image.BufferedImage;

import com.github.kilianB.StringUtil;

/**
 * A leaf node is a terminating . Reaching this point will r
 * 
 * ..
 * 
 * @author Kilian
 *
 */
class LeafNode extends TreeNode {

	/** indicate if this node indicates a match or mismatch leaf node */
	protected int category;

	/**
	 * Create a leaf node.
	 * 
	 * @param match
	 */
	public LeafNode(int category) {
		this.category = category;
	}

	@Override
	protected void printTree(int depth) {
		System.out.println(StringUtil.multiplyChar("\t", depth) + this);
	}

	@Override
	public int predictAgainstAll(BufferedImage bi) {
		return category;
	}
	
	public String toString() {
		return "LeafNode " + this.hashCode() + " [Category:" + category + "] ";
	}
}