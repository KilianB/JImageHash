package com.github.kilianB.matcher.supervised.randomForest;

import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.github.kilianB.StringUtil;
import com.github.kilianB.hashAlgorithms.HashingAlgorithm;
import com.github.kilianB.matcher.Hash;

class InnerNode extends TreeNode {

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
	public InnerNode(HashingAlgorithm hasher, double bestCutoff, double quality, double qualityLeft,
			double qualityRight) {
		super();
		this.hasher = hasher;
		this.threshold = bestCutoff;
		this.quality = quality;
		this.qualityLeft = qualityLeft;
		this.qualityRight = qualityRight;
	}

	

	public boolean predictAgainstAll(BufferedImage bi, Map<HashingAlgorithm, List<Hash>> precomputedHashes) {

		Hash targetHash = hasher.hash(bi);

		double minDif = Double.MAX_VALUE;

		for (Hash hash : precomputedHashes.get(hasher)) {
			double difference = targetHash.normalizedHammingDistance(hash);
			if (difference < minDif) {
				minDif = difference;
				if (difference == 0) {
					break;
				}
			}
		}

		if (minDif < threshold) {
			if (leftNode.predictAgainstAll(bi, precomputedHashes)) {
				return true;
			}
		} else {
			if (rightNode.predictAgainstAll(bi, precomputedHashes)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean predictAgainstAllExcludingSelf(BufferedImage bi,
			Map<HashingAlgorithm, Map<BufferedImage, Hash>> precomputedHashes) {
		Hash targetHash = hasher.hash(bi);

		double minDif = Double.MAX_VALUE;

		for (Entry<BufferedImage, Hash> entry : precomputedHashes.get(hasher).entrySet()) {

			if (entry.getKey().equals(bi)) {
				continue;
			}
			double difference = targetHash.normalizedHammingDistance(entry.getValue());
			if (difference < minDif) {
				minDif = difference;
				if (difference == 0) {
					break;
				}
			}
		}

		if (minDif < threshold) {
			if (leftNode.predictAgainstAllExcludingSelf(bi, precomputedHashes)) {
				return true;
			}
		} else {
			if (rightNode.predictAgainstAllExcludingSelf(bi, precomputedHashes)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public String toString() {
		return "InnerNode " + this.hashCode() + " [hasher=" + hasher + ", threshold=" + threshold + ", quality="
				+ quality + "] ";
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