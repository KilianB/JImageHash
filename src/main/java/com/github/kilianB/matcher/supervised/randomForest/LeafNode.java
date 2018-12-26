package com.github.kilianB.matcher.supervised.randomForest;

import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Map;

import com.github.kilianB.StringUtil;
import com.github.kilianB.hashAlgorithms.HashingAlgorithm;
import com.github.kilianB.matcher.Hash;

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
	boolean match;

	/**
	 * Create a leaf node.
	 * 
	 * @param match
	 */
	public LeafNode(boolean match) {
		this.match = match;
	}

	@Override
	protected void printTree(int depth) {
		System.out.println(StringUtil.multiplyChar("\t", depth) + this);
	}

	@Override
	public boolean predictAgainstAll(BufferedImage bi,Map<HashingAlgorithm,List<Hash>> precomputedHashes) {
		return match;
	}

	@Override
	public boolean predictAgainstAllExcludingSelf(BufferedImage bi,
			Map<HashingAlgorithm, Map<BufferedImage, Hash>> precomputedHashes) {
		return match;
	}
	
	public String toString() {
		return "LeafNode " + this.hashCode() + " [Match:" + match + "] ";
	}
}