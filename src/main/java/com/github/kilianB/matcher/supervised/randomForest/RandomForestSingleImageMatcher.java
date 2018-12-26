package com.github.kilianB.matcher.supervised.randomForest;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Map.Entry;
import java.util.Set;

import com.github.kilianB.MathUtil;
import com.github.kilianB.StringUtil;
import com.github.kilianB.dataStrorage.tree.BinaryTree;
import com.github.kilianB.dataStrorage.tree.Result;
import com.github.kilianB.hashAlgorithms.HashingAlgorithm;
import com.github.kilianB.matcher.Hash;
import com.github.kilianB.matcher.ImageMatcher.AlgoSettings;
import com.github.kilianB.matcher.supervised.LabeledImage;
import com.github.kilianB.matcher.unsupervised.SingleImageMatcher;

/**
 * @author Kilian
 *
 */
public class RandomForestSingleImageMatcher extends SingleImageMatcher {

	// Random forest

	// Create a devision tree

	DecisionTreeNode root;

//	public PriorityQueue<Result<BufferedImage>> getMatchingImages(BufferedImage image) {
//		
//	}

	List<LabeledImage> labeledImages = new ArrayList<LabeledImage>();

	public void addTestImages(Collection<LabeledImage> data) {
		for (LabeledImage t : data) {
			addTestImages(t);
		}
	}

	public void addTestImages(LabeledImage... data) {
		for (LabeledImage t : data) {
			addTestImages(t);
		}
	}

	public void addTestImages(LabeledImage tData) {
		labeledImages.add(tData);
	}

	class Pair<S, U> {
		S first;
		U second;

		/**
		 * @param first
		 * @param second
		 */
		public Pair(S first, U second) {
			super();
			this.first = first;
			this.second = second;
		}

		/**
		 * @return
		 */
		public S getFirst() {
			return first;
		}

		public U getSecond() {
			return second;
		}

	}

	class TestData {

		BufferedImage b0;
		BufferedImage b1;
		boolean match;

		/**
		 * @param b0
		 * @param b1
		 * @param match
		 */
		public TestData(BufferedImage b0, BufferedImage b1, boolean match) {
			super();
			this.b0 = b0;
			this.b1 = b1;
			this.match = match;
		}
	}

	Map<HashingAlgorithm, Map<BufferedImage, Hash>> preComputedHashes;

	class DecisionTreeNode {

		HashingAlgorithm hasher;
		double threshold;

		// Not really entropy in all cases
		double quality;

		/**
		 * @param hasher
		 * @param bestCutoff
		 */
		public DecisionTreeNode(HashingAlgorithm hasher, double bestCutoff, double quality) {
			super();
			this.hasher = hasher;
			this.threshold = bestCutoff;
			this.quality = quality;
		}

		DecisionTreeNode leftNode;
		DecisionTreeNode rightNode;

		/**
		 * Attempt to find a single duplicate
		 * 
		 * @param bi
		 * @return
		 */
		public boolean predict(BufferedImage bi, BufferedImage bi2) {

			Hash targetHash = hasher.hash(bi);
			Hash targetHash1 = hasher.hash(bi2);

			double difference = targetHash.normalizedHammingDistance(targetHash1);

			if (difference < threshold) {
				if (leftNode == null) {
					return true;
				} else {
					return leftNode.predict(bi, bi2);
				}
			} else {
				if (rightNode != null) {
					return rightNode.predict(bi, bi2);
				}
			}
			return false;
		}

		public boolean predictAgainstAll(BufferedImage bi) {

			Hash targetHash = hasher.hash(bi);

			for (Entry<BufferedImage, Hash> e : preComputedHashes.get(hasher).entrySet()) {
				double difference = targetHash.normalizedHammingDistance(e.getValue());

				if (difference < threshold) {
					if (leftNode == null) {
						return true;
					} else {
						if (leftNode.predictAgainstAll(bi)) {
							return true;
						}
					}
				} else {
					if (rightNode != null) {
						if (rightNode.predictAgainstAll(bi)) {
							return true;
						}
					}
				}
			}
			return false;
		}

		@Override
		public String toString() {
			return "InnerNode " + this.hashCode() + " [hasher=" + hasher + ", threshold=" + threshold
					+ ", quality=" + quality + ", leftNode=" + (leftNode != null ? leftNode.hashCode() : "-")
					+ ", rightNode=" + (rightNode != null ? rightNode.hashCode() : "-") + "] ";
		}

		public void printTree() {
			printTree(0);
		}

		private void printTree(int depth) {
			System.out.println(StringUtil.multiplyChar("\t", depth) + this);
			if (leftNode != null) {
				leftNode.printTree(++depth);
			}
			if (rightNode != null) {
				rightNode.printTree(depth);
			}
		}

	}

	// TODO handle overfitting
	public void createDecisionTree() {

		// System.out.println(testData.size() + " " + );

		// TODO do we really want to check everything against everything?
		// Maybe just used the already present examples...?

		// Pre compute all hashes
		// For training use a different set
		Map<HashingAlgorithm, Map<BufferedImage, Hash>> preComputedHashes = new HashMap<>();
		for (HashingAlgorithm hashAlgorithm : this.getAlgorithms().keySet()) {
			Map<BufferedImage, Hash> hashMap = new HashMap<>();
			for (LabeledImage image : labeledImages) {
				hashMap.put(image.getbImage(), hashAlgorithm.hash(image.getbImage()));
			}
			preComputedHashes.put(hashAlgorithm, hashMap);
		}

		// For now!
		this.preComputedHashes = preComputedHashes;

		// TODO this can become pretty expensive for a lot of test values?
		// We are working with numeric data. compute potential cutoff values.

		// Step 1 How good does each variable predict the outcome

		// Root Node

		List<TestData> testData = createTestData();

		DecisionTreeNode treeRoot = buildTree(testData, preComputedHashes);
		treeRoot.printTree();

//		System.out.println("");
//		System.out.println(
//				"duplicate: " + treeRoot.predict(testData.get(0).b0) + " expected: " + testData.get(0).match + "\n");
//
//		for (int i = 0; i < testData.size(); i++) {
//			TestData t = testData.get(i);
//			if (!t.match) {
//				System.out.println(t);
//				System.out.println(i + " duplicate: " + treeRoot.predict(t.b1) + " expected: " + t.match + "\n");
//			}
//		}
////		

		// Test

		// TP: 35 TN: 8595 FP: 4 FN: 12

		// TODO double
		int truePositive = 0;
		int trueNegative = 0;
		int falsePositive = 0;
		int falseNegative = 0;

		List<TestData> matchNode = new ArrayList<TestData>();
		List<TestData> distinctNode = new ArrayList<TestData>();

		for (int i = 0; i < testData.size(); i++) {

			TestData tData = testData.get(i);

			// boolean match = treeRoot.predict(tData.b0, tData.b1);

			boolean match = treeRoot.predictAgainstAll(tData.b0);
			boolean shouldMatch = false;

			for (TestData tt : testData) {
				if (tt.b0.equals(tData.b0) || tt.b1.equals(tData.b0)) {
					if (tt.match) {
						shouldMatch = true;
						break;
					}
				}
			}

			if (match) {
				if (shouldMatch) {
					truePositive++;
				} else {
					falsePositive++;
				}
				matchNode.add(tData);
			} else {
				if (shouldMatch) {
					falseNegative++;
				} else {
					trueNegative++;
				}
				distinctNode.add(tData);
			}
		}

		int sum = (int) (truePositive + falseNegative + trueNegative + falsePositive);
		// TODO lets weight ehese
		double weightMatching = (truePositive + falseNegative);
		double weightDistinct = (trueNegative + falsePositive);

		double tpW = truePositive * weightDistinct;
		double fnW = falseNegative * weightDistinct;
		double tnW = trueNegative * weightMatching;
		double fpW = falsePositive * weightMatching;

//		double giniImpurityMatch = 1 - Math.pow(tpW / (double) (tpW + fpW), 2)
//				- Math.pow(fpW / (double) (tpW + fpW), 2);
//
//		double giniImpurityDistinct = 1 - Math.pow(tnW / (double) (tnW + fnW), 2)
//				- Math.pow(fnW / (double) (tnW + fnW), 2);
//		double giniImpurity = (giniImpurityMatch + giniImpurityDistinct) / 2;
//		

		double giniImpurityMatch = 1 - Math.pow(truePositive / (double) (truePositive + falsePositive), 2)
				- Math.pow(falsePositive / (double) (truePositive + falsePositive), 2);

		double giniImpurityDistinct = 1 - Math.pow(trueNegative / (double) (trueNegative + falseNegative), 2)
				- Math.pow(falseNegative / (double) (trueNegative + falseNegative), 2);
		double giniImpurity = (giniImpurityMatch + giniImpurityDistinct) / 2;

		// The stuff we classify as duplicates really are duplicates!
		double recall = truePositive / (double) (truePositive + falseNegative);
		double specifity = trueNegative / (double) (trueNegative + falsePositive);

		double temp = truePositive + falsePositive;
		double precision = Double.NaN;
		if (temp != 0) {
			precision = truePositive / temp;
		}
		double f1 = 2 * (precision * recall) / (precision + recall);

		System.out.printf(
				"Gini impurity: %.4f | TP: %4d TN: %4d FP: %4d FN: %4d | Recall: %.4f Spec: %.3f Precision %.3f F1: %.3f %n",
				giniImpurity, truePositive, trueNegative, falsePositive, falseNegative, recall, specifity, precision,
				f1);

		// Predict the tree and check it's quality

	}

	// TODO average hash has a good f1 score due to filtering out many true
	// negatives. but fails at false negatives..
	// TODO also get the false negatives...

	public DecisionTreeNode buildTree(List<TestData> testData,
			Map<HashingAlgorithm, Map<BufferedImage, Hash>> preComputedHashes) {
		return buildTree(testData, preComputedHashes, -Double.MAX_VALUE);
	}

	private DecisionTreeNode buildTree(List<TestData> testData,
			Map<HashingAlgorithm, Map<BufferedImage, Hash>> preComputedHashes, double threshold) {

		Pair<DecisionTreeNode, List<TestData>[]> packed = computeNode(testData, preComputedHashes, threshold);

		if (packed == null) {
			return null;
		}

		DecisionTreeNode node = packed.first;

		// TODO dataset is empty we are done
		if (packed.getSecond()[0].size() > 1) {
			System.out.println("Left Node: " + packed.getSecond()[0].size());
			node.leftNode = buildTree(packed.getSecond()[0], preComputedHashes, node.quality);
		}

		if (packed.getSecond()[1].size() > 1) {
			System.out.println("Right Node: " + packed.getSecond()[0].size());
			node.rightNode = buildTree(packed.getSecond()[1], preComputedHashes, node.quality);
		}

		return node;
	}

	private Pair<DecisionTreeNode, List<TestData>[]> computeNode(List<TestData> testData,
			Map<HashingAlgorithm, Map<BufferedImage, Hash>> preComputedHashes, double qualityThreshold) {

		// Compute the actual gini coefficient.

		double bestCutoff = 0;
		HashingAlgorithm bestHashingAlgo = null;
		double bestGini = Double.MAX_VALUE;
		double bestF1Score = -Double.MAX_VALUE;
		String bestDebug = "";

		List<TestData>[] propagatedTestData = new ArrayList[2];

		propagatedTestData[0] = new ArrayList<>();
		propagatedTestData[1] = new ArrayList<>();

		for (HashingAlgorithm hashAlgorithm : this.getAlgorithms().keySet()) {
			// Test every image against each others image
			Map<BufferedImage, Hash> hashes = preComputedHashes.get(hashAlgorithm);

			List<Double> distances = new ArrayList<>();

			/* Prepare numerical categories */
			for (TestData tData : testData) {
				Hash h0 = hashes.get(tData.b0);
				Hash h1 = hashes.get(tData.b1);
				double distance = h0.normalizedHammingDistanceFast(h1);
				distances.add(distance);
			}
			// Numeric double data is a bit funky ...
			Collections.sort(distances);

			// Potential cuttoffs
			Set<Double> potentialCutoffValues = new LinkedHashSet<>();
			for (int i = 0; i < distances.size() - 1; i++) {
				// compute avg values
				potentialCutoffValues.add((distances.get(i) + distances.get(i + 1)) / 2);
			}

			// TODO maybe not use all double values but generalize at some point.

			List<String> debug = new ArrayList<>();

			for (double cutoff : potentialCutoffValues) {

				// TODO double
				int truePositive = 0;
				int trueNegative = 0;
				int falsePositive = 0;
				int falseNegative = 0;

				List<TestData> matchNode = new ArrayList<TestData>();
				List<TestData> distinctNode = new ArrayList<TestData>();

				for (TestData tData : testData) {
					Hash h0 = hashes.get(tData.b0);
					Hash h1 = hashes.get(tData.b1);
					double distance = h0.normalizedHammingDistanceFast(h1);

					if (distance < cutoff) {
						if (tData.match) {
							truePositive++;
						} else {
							falsePositive++;
						}
						matchNode.add(tData);
					} else {
						if (tData.match) {
							falseNegative++;
						} else {
							trueNegative++;
						}
						distinctNode.add(tData);
					}
				}

				int sum = (int) (truePositive + falseNegative + trueNegative + falsePositive);
				// TODO lets weight ehese
				double weightMatching = (truePositive + falseNegative);
				double weightDistinct = (trueNegative + falsePositive);

				double tpW = truePositive * weightDistinct;
				double fnW = falseNegative * weightDistinct;
				double tnW = trueNegative * weightMatching;
				double fpW = falsePositive * weightMatching;

//				double giniImpurityMatch = 1 - Math.pow(tpW / (double) (tpW + fpW), 2)
//						- Math.pow(fpW / (double) (tpW + fpW), 2);
//
//				double giniImpurityDistinct = 1 - Math.pow(tnW / (double) (tnW + fnW), 2)
//						- Math.pow(fnW / (double) (tnW + fnW), 2);
//				double giniImpurity = (giniImpurityMatch + giniImpurityDistinct) / 2;
//				

				double giniImpurityMatch = 1 - Math.pow(truePositive / (double) (truePositive + falsePositive), 2)
						- Math.pow(falsePositive / (double) (truePositive + falsePositive), 2);

				double giniImpurityDistinct = 1 - Math.pow(trueNegative / (double) (trueNegative + falseNegative), 2)
						- Math.pow(falseNegative / (double) (trueNegative + falseNegative), 2);
				double giniImpurity = (giniImpurityMatch + giniImpurityDistinct) / 2;

				// The stuff we classify as duplicates really are duplicates!
				double recall = truePositive / (double) (truePositive + falseNegative);
				double specifity = trueNegative / (double) (trueNegative + falsePositive);

				double temp = truePositive + falsePositive;
				double precision = Double.NaN;
				if (temp != 0) {
					precision = truePositive / temp;
				}
				double f1 = 2 * (precision * recall) / (precision + recall);

				if (f1 > bestF1Score) {
					bestCutoff = cutoff;
					bestF1Score = f1;
					bestHashingAlgo = hashAlgorithm;
					propagatedTestData[0] = matchNode;
					propagatedTestData[1] = distinctNode;

					bestDebug = String.format(
							"Gini impurity: %.4f Cutoff: %.3f | TP: %4d TN: %4d FP: %4d FN: %4d | Recall: %.4f Spec: %.3f Precision %.3f F1: %.3f %n",
							giniImpurity, cutoff, truePositive, trueNegative, falsePositive, falseNegative, recall,
							specifity, precision, f1);
				}

				// For distincts

				// + hashAlgorithm
//				System.out.printf(
//						"Gini impurity: %.4f Cutoff: %.3f | TP: %4d TN: %4d FP: %4d FN: %4d | Recall: %.4f Spec: %.3f Precision %.3f F1: %.3f %n",
//						giniImpurity, cutoff, truePositive, trueNegative, falsePositive, falseNegative, recall,
//						specifity, precision, f1);
			}
		}

		System.out.println("\nTest Data 'Size: " + testData.size());
		System.out.println("LeftNode: " + propagatedTestData[0].size() + " RightNode: " + propagatedTestData[1].size());

		// best
		if (bestF1Score > qualityThreshold) {
			DecisionTreeNode node = new DecisionTreeNode(bestHashingAlgo, bestCutoff, bestF1Score);
			System.out.println(node);
			System.out.println(bestDebug);
			return new Pair(node, propagatedTestData);
		} else {
			// TODO we are done. it does not make the tree better
			return null;
		}
	}

	private List<TestData> createTestData() {

		int testCases = MathUtil.triangularNumber(labeledImages.size() - 1);
		List<TestData> testData = new ArrayList<>(testCases);

		List<LabeledImage> labeledImagesCopy = new ArrayList<>(labeledImages);

		// Create every possible combintation
		for (LabeledImage img0 : labeledImages) {
			// No reason to check against itself or others against this value in the future
			labeledImagesCopy.remove(img0);
			for (LabeledImage img1 : labeledImagesCopy) {
				boolean match = img0.getCategory() == img1.getCategory();
				testData.add(new TestData(img0.getbImage(), img1.getbImage(), match));
			}
		}

		return testData;
	}

}
