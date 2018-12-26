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
import com.github.kilianB.pcg.fast.PcgRSFast;

/**
 * @author Kilian
 *
 */
public class RandomForestInMemoryImageMatcher extends SingleImageMatcher {

	// Random forest

	// Create a devision tree

	DecisionTreeNode root;

	Map<Integer, List<BufferedImage>> labeledImages = new HashMap<>();

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

		if (labeledImages.containsKey(tData.getCategory())) {
			labeledImages.get(tData.getCategory()).add(tData.getbImage());
		} else {
			List<BufferedImage> list = new ArrayList<>();
			list.add(tData.getbImage());
			labeledImages.put(tData.getCategory(), list);
		}
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
		boolean match;

		/**
		 * @param b0
		 * @param b1
		 * @param match
		 */
		public TestData(BufferedImage b0, boolean match) {
			super();
			this.b0 = b0;
			this.match = match;
		}

		@Override
		public String toString() {
			return "TestData [b0=" + b0.hashCode() + ", match=" + match + "]";
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

		public boolean predictAgainstAll(BufferedImage bi) {

			Hash targetHash = hasher.hash(bi);

			double minDif = Double.MAX_VALUE;

			for (Entry<BufferedImage, Hash> e : preComputedHashes.get(hasher).entrySet()) {
				double difference = targetHash.normalizedHammingDistance(e.getValue());
				if (e.getKey().equals(bi)) {
					continue; // TODO just for debug
				}
				if (difference < minDif) {
					minDif = difference;
				}
			}

			if (minDif < threshold) {
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
			return false;
		}
		
		
		//TODO bootstrap data

		/**
		 * @param b0
		 */
		public boolean predictAgainstAllDebug(BufferedImage bi) {

			Hash targetHash = hasher.hash(bi);

			double minDif = Double.MAX_VALUE;

			for (Entry<BufferedImage, Hash> e : preComputedHashes.get(hasher).entrySet()) {
				double difference = targetHash.normalizedHammingDistance(e.getValue());
				if (difference == 0.0) {
					continue; // TODO just for debug
				}
				if (difference < minDif) {
					minDif = difference;
				}
			}

			
			
			if (minDif < threshold) {
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

	Map<HashingAlgorithm, Map<BufferedImage, Double>> preComputedMinDistances;

	// TODO handle overfitting
	public void createDecisionTree(int numVars) {

		System.out.println("");
		// System.out.println(testData.size() + " " + );

		// TODO do we really want to check everything against everything?
		// Maybe just used the already present examples...?

		// Pre compute all hashes
		// For training use a different set
		Map<HashingAlgorithm, Map<BufferedImage, Hash>> preComputedHashes = new HashMap<>();

		for (HashingAlgorithm hashAlgorithm : this.getAlgorithms().keySet()) {
			Map<BufferedImage, Hash> hashMap = new HashMap<>();
			for (List<BufferedImage> images : labeledImages.values()) {
				for (BufferedImage image : images) {
					hashMap.put(image, hashAlgorithm.hash(image));
				}
			}
			preComputedHashes.put(hashAlgorithm, hashMap);
		}

		/**
		 * Compute the minimum distance the hash of one image has to all other hashes
		 * indexed in the tree.
		 */
		List<TestData> testData = createTestData();

		preComputedMinDistances = new HashMap<>();
		for (HashingAlgorithm hashAlgorithm : this.getAlgorithms().keySet()) {

			Map<BufferedImage, Hash> hashes = preComputedHashes.get(hashAlgorithm);

			Map<BufferedImage, Double> distanceMap = new HashMap<>();

			for (TestData tData : testData) {
				Hash tHash = hashes.get(tData.b0);
				double minDistance = Double.MAX_VALUE;
				for (TestData tData1 : testData) {
					if (tData != tData1) {
						Hash t1Hash = hashes.get(tData1.b0);
						double distance = tHash.normalizedHammingDistanceFast(t1Hash);
						if (distance < minDistance) {
							minDistance = distance;
						}
					}
				}
				distanceMap.put(tData.b0, minDistance);
			}
			preComputedMinDistances.put(hashAlgorithm, distanceMap);
		}
		this.preComputedHashes = preComputedHashes;

		root = buildTree(testData, preComputedHashes);
		root.printTree();
		test();
	
	}

	void test(){
		System.out.println("\n----- TEST ------\n");

		// Test

		List<TestData> testData = createTestData();
		
//		// TODO double
		int truePositive = 0;
		int trueNegative = 0;
		int falsePositive = 0;
		int falseNegative = 0;

		List<TestData> matchNode = new ArrayList<TestData>();
		List<TestData> distinctNode = new ArrayList<TestData>();

		for (int i = 0; i < testData.size(); i++) {

			TestData tData = testData.get(i);

			boolean match = root.predictAgainstAll(tData.b0);

			if (match) {
				if (tData.match) {
					truePositive++;
				} else {
					falsePositive++;
				}
				matchNode.add(tData);
			} else {
				if (tData.match) {
					falseNegative++;
					root.predictAgainstAllDebug(tData.b0);
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
		
		//Weighted gini impurity
		double leftWeight = (truePositive + falsePositive) / (double)sum;
		double rightWeight = (trueNegative + falseNegative) / (double)sum;
		
		double giniImpurity = (leftWeight*giniImpurityMatch + rightWeight*giniImpurityDistinct) / 2;

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

	}
	
	// TODO average hash has a good f1 score due to filtering out many true
	// negatives. but fails at false negatives..
	// TODO also get the false negatives...

	public DecisionTreeNode buildTree(List<TestData> testData,
			Map<HashingAlgorithm, Map<BufferedImage, Hash>> preComputedHashes) {
		return buildTree(testData, preComputedHashes, Double.MAX_VALUE);
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
			node.leftNode = buildTree(packed.getSecond()[0], preComputedHashes, node.quality);
		}

		if (packed.getSecond()[1].size() > 1) {
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
			
			System.out.println(hashAlgorithm);
			
			// Test every image against each others image
			Map<BufferedImage, Hash> hashes = preComputedHashes.get(hashAlgorithm);

			List<Double> distances = new ArrayList<>();

			Map<BufferedImage, Double> minDistanceMap = new HashMap<>();

			// TODO cache
			/* Prepare numerical categories */
			for (TestData tData : testData) {
				Hash tHash = hashes.get(tData.b0);
				double minDistance = Double.MAX_VALUE;
				for (TestData tData1 : testData) {
					if (tData != tData1) {
						Hash t1Hash = hashes.get(tData1.b0);
						double distance = tHash.normalizedHammingDistanceFast(t1Hash);
						if (distance < minDistance) {
							minDistance = distance;
						}
					}
				}
				minDistanceMap.put(tData.b0, minDistance);
				distances.add(minDistance);
			}
			// Numeric double data is a bit funky ...
			Collections.sort(distances);

			// Potential cuttoffs
			Set<Double> potentialCutoffValues = new LinkedHashSet<>();
			for (int i = 0; i < distances.size() - 1; i++) {
				// compute avg values
				potentialCutoffValues.add((distances.get(i) + distances.get(i + 1)) / 2);
			}
			for (double cutoff : potentialCutoffValues) {

				// TODO double
				int truePositive = 0;
				int trueNegative = 0;
				int falsePositive = 0;
				int falseNegative = 0;

				List<TestData> matchNode = new ArrayList<TestData>();
				List<TestData> distinctNode = new ArrayList<TestData>();

				for (TestData tData : testData) {
					double distance = minDistanceMap.get(tData.b0);

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

//				bestDebug = String.format(
//						"Gini impurity: %.4f Cutoff: %.3f | TP: %4d TN: %4d FP: %4d FN: %4d | Recall: %.4f Spec: %.3f Precision %.3f F1: %.3f",
//						giniImpurity, cutoff, truePositive, trueNegative, falsePositive, falseNegative, recall,
//						specifity, precision, f1);
//				
//				System.out.println(bestDebug);
				
				bestDebug = String.format(
						"Gini impurity: %.4f Cutoff: %.3f | TP: %4d TN: %4d FP: %4d FN: %4d | Recall: %.4f Spec: %.3f Precision %.3f F1: %.3f",
						giniImpurity, cutoff, truePositive, trueNegative, falsePositive, falseNegative, recall,
						specifity, precision, f1);
				System.out.println(bestDebug);
				
				if (giniImpurity < bestGini) {
				//if (f1 > bestF1Score) {
					bestCutoff = cutoff;
					bestF1Score = f1;
					bestHashingAlgo = hashAlgorithm;
					propagatedTestData[0] = matchNode;
					propagatedTestData[1] = distinctNode;
					bestGini = giniImpurity;
					
					bestDebug = String.format(
							"Gini impurity: %.4f Cutoff: %.3f | TP: %4d TN: %4d FP: %4d FN: %4d | Recall: %.4f Spec: %.3f Precision %.3f F1: %.3f",
							giniImpurity, cutoff, truePositive, trueNegative, falsePositive, falseNegative, recall,
							specifity, precision, f1);
					//System.out.println(bestDebug);
				}

				// For distincts

				// + hashAlgorithm
//				System.out.printf(
//						"Gini impurity: %.4f Cutoff: %.3f | TP: %4d TN: %4d FP: %4d FN: %4d | Recall: %.4f Spec: %.3f Precision %.3f F1: %.3f %n",
//						giniImpurity, cutoff, truePositive, trueNegative, falsePositive, falseNegative, recall,
//						specifity, precision, f1);
			}
		}

		// best
		
		if (bestGini < qualityThreshold) {
		//if (bestF1Score > qualityThreshold) {
			DecisionTreeNode node = new DecisionTreeNode(bestHashingAlgo, bestCutoff, bestGini);
			System.out.println(node);
			System.out.println(bestDebug);
			System.out.println(
					"LeftNode: " + propagatedTestData[0].size() + " RightNode: " + propagatedTestData[1].size() + "\n");

			return new Pair(node, propagatedTestData);
		} else {
			// TODO we are done. it does not make the tree better
			return null;
		}
	}

	private List<TestData> createTestData() {

		int testCases = MathUtil.triangularNumber(labeledImages.size() - 1);
		List<TestData> testData = new ArrayList<>(testCases);

		
		
		List<TestData> matchData = new ArrayList<>(testCases);
		List<TestData> distinctData = new ArrayList<>(testCases);

		
		// Create every possible combintation
		for (List<BufferedImage> images : labeledImages.values()) {
			boolean match = images.size() > 1;
			for (BufferedImage b : images) {
				//testData.add(new TestData(b, match));
				if(match) {
					matchData.add(new TestData(b, match));
				}else {
					distinctData.add(new TestData(b, match));
				}
			}
		}
		
		int matchC = matchData.size();
		int distinctC = distinctData.size();
		
		PcgRSFast rng = new PcgRSFast();
		
		if(matchC > distinctC) {
			while(matchC > distinctC) {
				matchData.remove(rng.nextInt(matchC--));
			}
		}else if(distinctC > matchC) {
			while(distinctC > matchC) {
				distinctData.remove(rng.nextInt(distinctC--));
			}
		}
		
		testData.addAll(matchData);
		testData.addAll(distinctData);
		
		System.out.println("Match: " + matchC + " Distinct: " + distinctC);
		
		//Balance the dataset;
		
		return testData;
	}

}
