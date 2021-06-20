package dev.brachtendorf.jimagehash.matcher.categorize.supervised.randomForest;

import java.awt.image.BufferedImage;
import java.util.concurrent.atomic.AtomicInteger;

import dev.brachtendorf.StringUtil;

/**
 * A leaf node is a terminating . Reaching this point will r
 * 
 * ..
 * 
 * @author Kilian
 *
 */
class LeafNode extends TreeNode {

	private static AtomicInteger uniqueId = new AtomicInteger();
	
	protected int id;
	
	/** indicate if this node indicates a match or mismatch leaf node */
	protected int category;

	/**
	 * Create a leaf node.
	 * 
	 * @param match
	 */
	public LeafNode(int category) {
		this.category = category;
		this.id = uniqueId.incrementAndGet();
	}

	@Override
	protected void printTree(int depth) {
		System.out.println(StringUtil.multiplyChar("\t", depth) + this);
	}

	@Override
	public int[] predictAgainstAll(BufferedImage bi) {
		return new int[]{category,id};
	}
	
	public String toString() {
		return "LeafNode " + this.hashCode() + " [Category:" + category + "] ";
	}
}