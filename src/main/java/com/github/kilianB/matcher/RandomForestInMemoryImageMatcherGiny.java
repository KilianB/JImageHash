package com.github.kilianB.matcher;

import java.awt.image.BufferedImage;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import com.github.kilianB.MathUtil;
import com.github.kilianB.Require;
import com.github.kilianB.StringUtil;
import com.github.kilianB.benchmark.LabeledImage;
import com.github.kilianB.datastructures.CountHashCollection;
import com.github.kilianB.hashAlgorithms.AverageHash;
import com.github.kilianB.hashAlgorithms.HashingAlgorithm;
import com.github.kilianB.pcg.fast.PcgRSFast;

/**
 * 
 * 
 * TODO this is not a single image matcher anymore. ...
 * 
 * @author Kilian
 *
 */
public class RandomForestInMemoryImageMatcherGiny extends SingleImageMatcher {

	// Random forest

	// Create a devision tree

	// TreeNode root;

	List<TreeNode> forest = new ArrayList<>();

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

	abstract class TreeNode {
		public abstract boolean predictAgainstAll(BufferedImage bi);

		protected abstract void printTree(int depth);

		protected void printTree() {
			printTree(0);
		}
	}

	class LeafNode extends TreeNode {

		/** indicate if this node indicates a match or mismatch leaf node */
		boolean match;

		/**
		 * @param hasher
		 * @param bestCutoff
		 * @param quality
		 */
		public LeafNode(boolean match) {
			this.match = match;
		}

		protected void printTree(int depth) {
			System.out.println(StringUtil.multiplyChar("\t", depth) + this);
		}

		public String toString() {
			return "LeafNode " + this.hashCode() + " [Match:" + match + "] ";
		}

		public boolean predictAgainstAll(BufferedImage bi) {
			return match;
		}
	}

	class InnerNode extends TreeNode {

		HashingAlgorithm hasher;
		float threshold;

		// Not really entropy in all cases
		double quality;
		double qualityLeft;
		double qualityRight;

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
			this.threshold = (float) bestCutoff;
			this.quality = quality;
			this.qualityLeft = qualityLeft;
			this.qualityRight = qualityRight;
		}

		TreeNode leftNode;
		TreeNode rightNode;

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
				if (leftNode.predictAgainstAll(bi)) {
					return true;
				}
			} else {
				if (rightNode.predictAgainstAll(bi)) {
					return true;
				}
			}
			return false;
		}

		// TODO bootstrap data

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

			// TODO!!!
			if (rightNode != null)
				rightNode.printTree(depth);
		}

	}

	// TODO
	// Map<HashingAlgorithm, Map<BufferedImage, Double>> preComputedMinDistances;

	// TODO handle overfitting

	/**
	 * Populate the decision trees used in this image matcher. The forest has to be
	 * initialized when ever new labeled test images are added.
	 * 
	 * @param trees      The number of trees created. Has to be odd. The more trees
	 *                   present the better the accuracy is
	 * @param numVars    The number of variables used in each tree. Which variables
	 *                   are chosen is randomly decided. Not using every variable
	 *                   prevents overfitting.
	 * @param numVarsRep The variables used are numerical values which can appear
	 *                   multiple times per branch. Limit the number of consecutive
	 *                   times a single var can appear in the same brench.
	 */
	public void createDecisionTree(int trees, int numVars, int numVarsRep) {

		Require.inRange(numVars, 0, steps.size(), "Can't create a tree with more variables than vars available.");
		Require.oddValue(trees, "The number of trees should be odd to prevent ambiguity");

		System.out.println("");
		// System.out.println(testData.size() + " " + );

		/**
		 * Compute the minimum distance the hash of one image has to all other hashes
		 * indexed in the tree.
		 */
		List<TestData> testData = createTestData();

		System.out.println("Hashing algos available: " + steps.keySet());

		/*
		 * 0. precompute all hashes necessary
		 */
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

		this.preComputedHashes = preComputedHashes;

		PcgRSFast rng = new PcgRSFast();

		// TODO this can be multi threaded

		// TODO build random forest with numVars changing

		// TODO overwrite numVars

		int numVarsEffectiveFinal = (int) Math.sqrt(this.getAlgorithms().size());
		System.out.println("build rf with " + numVars);

		ExecutorService tPool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2);

		ReentrantLock lock = new ReentrantLock();

		for (int i = 0; i < trees; i++) {
			tPool.execute(() -> {
				/*
				 * 1. Bootstrap. Draw a random subsample of datasets
				 */
				List<TestData> bootstrappedData = bootstrapDataset(testData, rng);
				List<TestData> outOfBagData = new ArrayList<>(testData);
				outOfBagData.removeAll(bootstrappedData);

				Map<HashingAlgorithm, Map<BufferedImage, Hash>> preComputedHashesBootstrap = new HashMap<>();

				CountHashCollection<HashingAlgorithm> cHasher = new CountHashCollection<>();

				for (int j = 0; j < numVarsRep; j++) {
					cHasher.addAll(this.getAlgorithms().keySet());
				}

				// List<HashingAlgorithm> variableCopy = new
				// ArrayList<>(this.getAlgorithms().keySet());

				// We don't need to rehash
				for (HashingAlgorithm hashAlgorithm : this.getAlgorithms().keySet()) {
					Map<BufferedImage, Hash> cache = preComputedHashes.get(hashAlgorithm);
					Map<BufferedImage, Hash> hashMap = new HashMap<>();
					for (TestData t : bootstrappedData) {
						hashMap.put(t.b0, cache.get(t.b0));
					}
					preComputedHashesBootstrap.put(hashAlgorithm, hashMap);
				}
				// this.preComputedHashes = preComputedHashes;
				TreeNode root = buildTree(bootstrappedData, cHasher, numVarsEffectiveFinal, preComputedHashesBootstrap);
				forest.add(root);

//				lock.lock();
//				root.printTree();
//				lock.unlock();
				// testIndividualTree(root, bootstrappedData);
				System.out.println("");
			});

		}

		tPool.shutdown();
		try {
			tPool.awaitTermination(1, TimeUnit.MINUTES);

			// Test with out of bag TODO

			// TODO itterate with different settings.
			test(testData);

		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		// testArtificialDecisionTree(bootstrappedData);
	}

	/**
	 * Create a dataset with only a subset of the available data (about 1/3 of the
	 * data will not be present in the data
	 * 
	 * A bootstrapped set may include duplicate entries
	 * 
	 * @param testData the labeled images to bootstrap the test data from-
	 * @param rng      a random number generator used to draw the samples
	 * @return the bootstrapped dataset
	 */
	private List<TestData> bootstrapDataset(List<TestData> testData, Random rng) {

		int size = testData.size();
		List<TestData> bootstrapped = new ArrayList<>(size);

		for (int i = 0; i < size; i++) {
			bootstrapped.add(testData.get(rng.nextInt(size)));
		}
		return bootstrapped;
	}

	void testIndividualTree(TreeNode root, List<TestData> testData) {
		// System.out.println("\n----- TEST ------\n");

		// Test

//				// TODO double
		int truePositive = 0;
		int trueNegative = 0;
		int falsePositive = 0;
		int falseNegative = 0;

		List<TestData> matchNode = new ArrayList<TestData>();
		List<TestData> distinctNode = new ArrayList<TestData>();

		for (int i = 0; i < testData.size(); i++) {

			TestData tData = testData.get(i);

			int matchCount = 0;
			int distinctCount = 0;
			if (root.predictAgainstAll(tData.b0)) {
				matchCount++;
			} else {
				distinctCount++;
			}

			boolean match = matchCount > distinctCount;

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
					// root.predictAgainstAllDebug(tData.b0);
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

		// Weighted gini impurity
		double leftWeight = (truePositive + falsePositive) / (double) sum;
		double rightWeight = (trueNegative + falseNegative) / (double) sum;

		double giniImpurity = (leftWeight * giniImpurityMatch + rightWeight * giniImpurityDistinct);

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

	void test(List<TestData> testData) {
		// System.out.println("\n----- TEST ------\n");

		// Test

//		// TODO double
		int truePositive = 0;
		int trueNegative = 0;
		int falsePositive = 0;
		int falseNegative = 0;

		List<TestData> matchNode = new ArrayList<TestData>();
		List<TestData> distinctNode = new ArrayList<TestData>();

		for (int i = 0; i < testData.size(); i++) {

			TestData tData = testData.get(i);

			int matchCount = 0;
			int distinctCount = 0;
			for (TreeNode tree : forest) {
				if (tree.predictAgainstAll(tData.b0)) {
					matchCount++;
				} else {
					distinctCount++;
				}
			}

			boolean match = matchCount > distinctCount;

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
					// root.predictAgainstAllDebug(tData.b0);
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

		// Weighted gini impurity
		double leftWeight = (truePositive + falsePositive) / (double) sum;
		double rightWeight = (trueNegative + falseNegative) / (double) sum;

		double giniImpurity = (leftWeight * giniImpurityMatch + rightWeight * giniImpurityDistinct);

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

	public TreeNode buildTree(List<TestData> testData, CountHashCollection<HashingAlgorithm> cHasher, int numVars,
			Map<HashingAlgorithm, Map<BufferedImage, Hash>> preComputedHashes) {
		return buildTree(testData, cHasher, numVars, preComputedHashes, Double.MAX_VALUE, true);
	}

	private TreeNode buildTree(List<TestData> testData, CountHashCollection<HashingAlgorithm> cHasher, int numVars,
			Map<HashingAlgorithm, Map<BufferedImage, Hash>> preComputedHashes, double threshold, boolean left) {

		CountHashCollection<HashingAlgorithm> algorithmCopy = new CountHashCollection<>(cHasher);
		Pair<TreeNode, List<TestData>[]> packed = computeNode(testData, algorithmCopy, numVars, preComputedHashes,
				threshold);

		if (packed == null) {
			return new LeafNode(left);
		}

		if (packed.first instanceof LeafNode) {
			return packed.first;
		}

		InnerNode node = (InnerNode) packed.first;

		// TODO dataset is empty we are done
		if (packed.getSecond()[0].size() > 0 && !MathUtil.isDoubleEquals(node.qualityLeft, 0, 1e-8)) {
			// System.out.println("--------------\n Attempt Left Node: ");
			node.leftNode = buildTree(packed.getSecond()[0], algorithmCopy, numVars, preComputedHashes,
					node.qualityLeft, true);

		} else {
			// Check if it's a true or false node. due to gini coefficient this may swap

			long size = packed.getSecond()[0].size();
			long match = packed.getSecond()[0].stream().filter(i -> i.match).count();
			long distinct = size - match;

			// Either we have 1 node left or they are all the same
			if (match > distinct) {
				node.leftNode = new LeafNode(true);
			} else {
				node.leftNode = new LeafNode(false);
			}
		}

		if (packed.getSecond()[1].size() > 0 && !MathUtil.isDoubleEquals(node.qualityRight, 0, 1e-8)) {
			long size = packed.getSecond()[1].size();
			long match = packed.getSecond()[1].stream().filter(i -> i.match).count();
			long distinct = size - match;

			// System.out.println(
			// "--------------\n Attempt Right Node: " + size + " match: " + match + "
			// distinct: " + distinct);
			node.rightNode = buildTree(packed.getSecond()[1], algorithmCopy, numVars, preComputedHashes,
					node.qualityRight, false);

		} else {
			long size = packed.getSecond()[1].size();
			long match = packed.getSecond()[1].stream().filter(i -> i.match).count();
			long distinct = size - match;

			if (match > distinct) {
				node.rightNode = new LeafNode(true);
			} else {
				node.rightNode = new LeafNode(false);
			}
		}

		return node;
	}

	private Pair<TreeNode, List<TestData>[]> computeNode(List<TestData> testData,
			CountHashCollection<HashingAlgorithm> algorithmCopy, int numVars,
			Map<HashingAlgorithm, Map<BufferedImage, Hash>> preComputedHashes, double qualityThreshold) {

		// Compute the actual gini coefficient.

		double bestCutoff = 0;
		HashingAlgorithm bestHashingAlgo = null;
		double bestGini = Double.MAX_VALUE;

		double giniLeft = Double.MAX_VALUE;
		double giniRight = Double.MAX_VALUE;

		double bestF1Score = -Double.MAX_VALUE;
		String bestDebug = "";

		List<TestData>[] propagatedTestData = new ArrayList[2];

		propagatedTestData[0] = new ArrayList<>();
		propagatedTestData[1] = new ArrayList<>();

		PcgRSFast rng = new PcgRSFast();

		// Compute numVars random indices

		int numVarsAvailable = algorithmCopy.sizeUnique();

		// Randomly select hashing algorithms indices
		List<Integer> indices = new ArrayList<>(numVarsAvailable);
		for (int i = 0; i < numVarsAvailable; i++) {
			indices.add(i);
		}

		Collections.shuffle(indices, new PcgRSFast());

		HashingAlgorithm[] algorithmsAvailable = algorithmCopy.toArrayUnique();

		for (int i = 0; i < numVars; i++) {

			if (indices.isEmpty()) {
				break;
			}

			HashingAlgorithm hashAlgorithm = algorithmsAvailable[indices.remove(0)];

			// System.out.println("\n" + hashAlgorithm + " CompTo: " + qualityThreshold);

			// Test every image against each others image
			Map<BufferedImage, Hash> hashes = preComputedHashes.get(hashAlgorithm);

			Set<Double> distanceSet = new LinkedHashSet<>();

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
				distanceSet.add(minDistance);
			}
			// Numeric double data is a bit funky ...
			List<Double> distances = new ArrayList(distanceSet);
			Collections.sort(distances);

			// Potential cuttoffs
			Set<Double> potentialCutoffValues = new LinkedHashSet<>();
			for (int j = 0; j < distances.size() - 1; j++) {
				// compute avg values
				potentialCutoffValues.add((distances.get(j) + distances.get(j + 1)) / 2);
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

				// Weighted gini impurity
				double leftWeight = (truePositive + falsePositive) / (double) sum;
				double rightWeight = (trueNegative + falseNegative) / (double) sum;

				double giniImpurity = (leftWeight * giniImpurityMatch + rightWeight * giniImpurityDistinct);

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

//				System.out.println(String.format(
//						"Gini impurity: %.4f Cutoff: %.3f | TP: %4d TN: %4d FP: %4d FN: %4d | Recall: %.4f Spec: %.3f Precision %.3f F1: %.3f GiniLeft %.3f Weight: %.3f GiniRight %.3f Weight: %.3f",
//						giniImpurity, cutoff, truePositive, trueNegative, falsePositive, falseNegative, recall,
//						specifity, precision, f1, giniImpurityMatch, leftWeight, giniImpurityDistinct, rightWeight));

				if (giniImpurity < bestGini) {
					// if (f1 > bestF1Score) {
					bestCutoff = cutoff;
					bestF1Score = f1;
					bestHashingAlgo = hashAlgorithm;
					propagatedTestData[0] = matchNode;
					propagatedTestData[1] = distinctNode;
					bestGini = giniImpurity;

					giniLeft = giniImpurityMatch;
					giniRight = giniImpurityDistinct;

					bestDebug = String.format(
							"Gini impurity: %.4f Cutoff: %.3f | TP: %4d TN: %4d FP: %4d FN: %4d | Recall: %.4f Spec: %.3f Precision %.3f F1: %.3f",
							giniImpurity, cutoff, truePositive, trueNegative, falsePositive, falseNegative, recall,
							specifity, precision, f1);
					// System.out.println(bestDebug);
				}

				// For distincts

				// + hashAlgorithm
//				System.out.printf(
//						"Gini impurity: %.4f Cutoff: %.3f | TP: %4d TN: %4d FP: %4d FN: %4d | Recall: %.4f Spec: %.3f Precision %.3f F1: %.3f %n",
//						giniImpurity, cutoff, truePositive, trueNegative, falsePositive, falseNegative, recall,
//						specifity, precision, f1);
			}
		}

		// Gini impurity of the entire category. combined

		// best
		algorithmCopy.remove(bestHashingAlgo);
		// Remove the best algorithm from this list.

		if (bestGini < qualityThreshold && !MathUtil.isDoubleEquals(bestGini, qualityThreshold, 1e-8)) {
			// if (bestF1Score > qualityThreshold) {
			InnerNode node = new InnerNode(bestHashingAlgo, bestCutoff, bestGini, giniLeft, giniRight);
//			System.out.println("\nBestNode:\n" + node + " Best Gini" + bestGini + " Threshold: " + qualityThreshold);
//			System.out.println(bestDebug);
//
//			System.out.println("LeftNode: " + propagatedTestData[0].size() + " "
//					+ propagatedTestData[0].stream()
//							.collect(Collectors.groupingBy((i) -> i.match, Collectors.counting()))
//					+ " RightNode: " + propagatedTestData[1].size() + " " + propagatedTestData[1].stream()
//							.collect(Collectors.groupingBy((i) -> i.match, Collectors.counting()))
//					+ "\n");

			return new Pair(node, propagatedTestData);
		} else {
			// TODO we are done. it does not make the tree better

			long size = testData.size();
			long match = testData.stream().filter(i -> i.match).count();
			long distinct = size - match;

			if (match > distinct) {
				return new Pair(new LeafNode(true), null);
			} else if (match == distinct) {
				return null;
			} else {
				return new Pair(new LeafNode(false), null);
			}

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
				testData.add(new TestData(b, match));
				if (match) {
					matchData.add(new TestData(b, match));
				} else {
					distinctData.add(new TestData(b, match));
				}
			}
		}

		int matchC = matchData.size();
		int distinctC = distinctData.size();

		PcgRSFast rng = new PcgRSFast();

		if (matchC > distinctC) {
			while (matchC > distinctC) {
				matchData.remove(rng.nextInt(matchC--));
			}
		} else if (distinctC > matchC) {
			while (distinctC > matchC) {
				distinctData.remove(rng.nextInt(distinctC--));
			}
		}

//		testData.addAll(matchData);
//		testData.addAll(distinctData);

		System.out.println("Match: " + matchC + " Distinct: " + distinctC);

		// Balance the dataset;

		return testData;
	}

	/**
	 * @param bootstrappedData
	 * 
	 */
//	public void testArtificialDecisionTree(List<TestData> bootstrappedData) {
//
//		// Build a tree ourselves
//
//		InnerNode treeNode = new InnerNode(new AverageHash(32), 0.125, 0, 0, 0);
//		treeNode.leftNode = new LeafNode(true);
//		treeNode.rightNode = new LeafNode(false);
//		test(treeNode, bootstrappedData);
//
//	}

}
