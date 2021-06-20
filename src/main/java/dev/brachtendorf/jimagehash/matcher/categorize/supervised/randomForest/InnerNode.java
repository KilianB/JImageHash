package dev.brachtendorf.jimagehash.matcher.categorize.supervised.randomForest;

import java.awt.image.BufferedImage;

import dev.brachtendorf.StringUtil;
import dev.brachtendorf.jimagehash.hash.FuzzyHash;
import dev.brachtendorf.jimagehash.hash.Hash;
import dev.brachtendorf.jimagehash.hashAlgorithms.HashingAlgorithm;

class InnerNode extends TreeNode {

	private FuzzyHash internalHash;
	private HashingAlgorithm hasher;
	private double threshold;

	// Not really entropy in all cases
	protected double quality;
	protected double qualityLeft;
	protected double qualityRight;
	
	protected TreeNode leftNode;
	protected TreeNode rightNode;

	protected InnerNode() {
	};

	/**
	 * @param hasher
	 * @param bestCutoff
	 */
	public InnerNode(FuzzyHash internalHash, HashingAlgorithm hasher, double bestCutoff, double quality, double qualityLeft,
			double qualityRight) {
		super();
		this.internalHash = internalHash;
		this.hasher = hasher;
		this.threshold = bestCutoff;
		this.quality = quality;
		this.qualityLeft = qualityLeft;
		this.qualityRight = qualityRight;
	}

	

	public int[] predictAgainstAll(BufferedImage bi) {

		Hash targetHash = hasher.hash(bi);

		double distance = internalHash.normalizedHammingDistance(targetHash);
	
		if (distance < threshold) {
			return leftNode.predictAgainstAll(bi);
		} else {
			return rightNode.predictAgainstAll(bi);
		}
	}

	

	@Override
	public String toString() {
		return "InnerNode [internalHash=" + internalHash + ", hasher=" + hasher + ", threshold=" + threshold
				+ ", quality=" + quality + "]";
	}

	public void printTree() {
		printTree(0);
	}

	public void printTree(int depth) {
		System.out.println(StringUtil.multiplyChar("\t", depth) + this);
		leftNode.printTree(++depth);

		if (rightNode != null)
			rightNode.printTree(depth);
	}

}