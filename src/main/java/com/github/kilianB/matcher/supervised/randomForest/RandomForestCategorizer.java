package com.github.kilianB.matcher.supervised.randomForest;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.github.kilianB.Experimental;
import com.github.kilianB.MathUtil;
import com.github.kilianB.Require;
import com.github.kilianB.datastructures.CountHashCollection;
import com.github.kilianB.datastructures.Pair;
import com.github.kilianB.datastructures.Triple;
import com.github.kilianB.hashAlgorithms.AverageHash;
import com.github.kilianB.hashAlgorithms.HashingAlgorithm;
import com.github.kilianB.matcher.FuzzyHash;
import com.github.kilianB.matcher.Hash;
import com.github.kilianB.matcher.pairwise.SingleImageMatcher;
import com.github.kilianB.matcher.supervised.LabeledImage;
import com.github.kilianB.pcg.fast.PcgRSFast;

/**
 * 
 * 
 * TODO this is not a single image matcher anymore. ...
 * 
 * @author Kilian
 *
 */
@Experimental(description = "The image matcher categorizes images based on their distance to the closest match. While this is an okay approach clustering the images "
		+ "based on category yields much cleaner results.")
public class RandomForestCategorizer extends SingleImageMatcher {

	/**
	 * Root nodes of all decision trees making up the random forest
	 */
	protected List<TreeNode> forest = new ArrayList<>();

	/**
	 * Test images used to create test sets to train the forest
	 */
	protected List<LabeledImage> labeledImages = new ArrayList<>();

//	private Map<HashingAlgorithm, Map<String, Hash>> preComputedHashes;

	/**
	 * Add test images to this image matcher which will be used to construct the
	 * random forest.
	 * 
	 * <p>
	 * Be aware that labeled images are kept in memory as long as
	 * {@link #clearTestImages()} has not been called.
	 * 
	 * @param data the images to add
	 */
	public void addTestImages(Collection<LabeledImage> data) {
		for (LabeledImage t : data) {
			addTestImages(t);
		}
	}

	/**
	 * Add test images to this image matcher which will be used to construct the
	 * random forest.
	 * 
	 * <p>
	 * Be aware that labeled images are kept in memory as long as
	 * {@link #clearTestImages()} has not been called.
	 * 
	 * @param data the images to add
	 */
	public void addTestImages(LabeledImage... data) {
		for (LabeledImage t : data) {
			addTestImages(t);
		}
	}

	/**
	 * Add a labeled image to this image matcher which will be used to construct the
	 * random forest.
	 * 
	 * <p>
	 * Be aware that labeled images are kept in memory as long as
	 * {@link #clearTestImages()} has not been called.
	 * 
	 * @param lData the image to add
	 */
	public void addTestImages(LabeledImage lData) {
		labeledImages.add(lData);
	}

	/**
	 * Clears the test images. Any references made by this object are released and
	 * allows the gc to free the underlaying buffered image if it's not referenced
	 * anywhere else.
	 * 
	 * <p>
	 * Be aware that you need to add new test images before calling
	 * {@link #trainMatcher(int, int, int)}.
	 */
	public void clearTestImages() {
		labeledImages.clear();
	}

	// TODO
	// Map<HashingAlgorithm, Map<BufferedImage, Double>> preComputedMinDistances;

	/**
	 * Populate the decision trees used in this image matcher. The forest has to be
	 * initialized when ever new labeled test images are added.
	 * 
	 * @param trees              The number of trees created. Has to be odd. The
	 *                           more trees present the better the accuracy is
	 * @param numVarsSearchRange The number of variables used in each tree. Which
	 *                           variables are chosen is randomly decided. Not using
	 *                           every variable prevents overfitting. //TODO explain better
	 * @param numVarsRep         The variables used are numerical values which can
	 *                           appear multiple times per branch. Limit the number
	 *                           of consecutive times a single var can appear in the
	 *                           same brench.
	 */
	public void trainMatcher(int trees, int numVarsSearchRange, int numVarsRep) {

		Require.positiveValue(numVarsSearchRange, "NumVarsSearchRange has to be positive.");
		Require.oddValue(trees, "The number of trees should be odd to prevent ambiguity");

		System.out.println("");
		// System.out.println(testData.size() + " " + );

		/**
		 * Compute the minimum distance the hash of one image has to all other hashes
		 * indexed in the tree.
		 */
//		List<TestData> testData = createTestData();

		System.out.println("Hashing algos available: " + steps.keySet());

		/*
		 * 0. precompute all hashes necessary.
		 */
		Map<HashingAlgorithm, Map<BufferedImage, Hash>> preComputedHashes = new HashMap<>();

		for (HashingAlgorithm hashAlgorithm : this.getAlgorithms().keySet()) {
			Map<BufferedImage, Hash> hashMap = new HashMap<>();
			for (LabeledImage lImage : labeledImages) {
				hashMap.put(lImage.getbImage(), hashAlgorithm.hash(lImage.getbImage()));
			}
			preComputedHashes.put(hashAlgorithm, hashMap);
		}

		// Create variables //TODO this should happen on a per tree basis. Move inside
		// loop
		List<Pair<FuzzyHash, HashingAlgorithm>> randomHashes = createFuzzyHashes(preComputedHashes);

		ExecutorService tPool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2);

		
		int maxNumberOfVariables = randomHashes.size();
		
		int numVars = (int) Math.sqrt(maxNumberOfVariables);

		boolean newBestFound = false;
		for (int i = numVars - numVarsSearchRange; i < numVars + numVarsSearchRange || newBestFound; i++) {
			// If we have less than 1 var skip it

			System.out.println("Create Forest with number of vars: " + i + "/" + maxNumberOfVariables);
			if (i < 0) {
				continue;
			}
			// Done
			if (i > maxNumberOfVariables) {
				break;
			}

			//TODO here we don't event look at the i
			
			Object[] packedForestData = createForest(trees, numVarsRep, randomHashes, preComputedHashes, tPool);

			@SuppressWarnings("unchecked")
			List<TreeNode> forest = (List<TreeNode>) packedForestData[0];

			double outOfBagClassificationError = (double) packedForestData[1];
			double classificationErrorAll = (double) packedForestData[2];

			System.out.println("Out of bag error: " + outOfBagClassificationError);
			System.out.println("Class error all : " + classificationErrorAll);

			this.forest = forest;
			System.out.println(" ");
		}

		tPool.shutdown();

		// testArtificialDecisionTree(bootstrappedData);
	}

	/**
	 * 
	 * @param trees                        number of trees to create
	 * @param numVarsRep                   number of duplicates for the same
	 *                                     variable allowed in a single tree
	 * @param randomVariables              variables to check against
	 * @param preComputedHashesTestAgainst hashes of test data
	 * @param tPool                        thread pool executor
	 * @return Object array containing a reference to the root, the out of bag
	 *         classification error and the total error including training data
	 */
	protected Object[] createForest(int trees, int numVarsRep, List<Pair<FuzzyHash, HashingAlgorithm>> randomVariables,
			Map<HashingAlgorithm, Map<BufferedImage, Hash>> preComputedHashesTestAgainst, ExecutorService tPool) {
		Map<TreeNode, Set<TestData>> bootstrappedOutOfBag = new HashMap<>();

		List<TreeNode> forestCandidate = new ArrayList<TreeNode>();

		PcgRSFast rng = new PcgRSFast();

		Collection<Future<Void>> tasks = new ArrayList<>();

		for (int i = 0; i < trees; i++) {
			tasks.add(tPool.submit(() -> {

				/*
				 * 1. Bootstrap. Draw a random subsample of datasets
				 */
				// TODO reimplement
//				List<TestData> bootstrappedData = bootstrapDataset(randomVariables, rng);
//				List<TestData> outOfBagData = new ArrayList<>(randomVariables);
//				outOfBagData.removeAll(bootstrappedData);

//				Map<HashingAlgorithm, Map<BufferedImage, Hash>> preComputedHashesBootstrap = new HashMap<>();

				CountHashCollection<Pair<FuzzyHash, HashingAlgorithm>> cHasher = new CountHashCollection<>();

				for (int j = 0; j < numVarsRep; j++) {
					cHasher.addAll(randomVariables);
				}

//				// We don't need to rehash
//				for (HashingAlgorithm hashAlgorithm : this.getAlgorithms().keySet()) {
//					Map<BufferedImage, Hash> cache = preComputedHashesTestAgainst.get(hashAlgorithm);
//					Map<BufferedImage, Hash> hashMap = new HashMap<>();
//					for (TestData t : bootstrappedData) {
//						hashMap.put(t.b0, cache.get(t.b0));
//					}
//					preComputedHashesBootstrap.put(hashAlgorithm, hashMap);
//				}
				// this.preComputedHashes = preComputedHashes;

				// TODO it's not really this bootstrap correctly
				List<LabeledImage> bootstrappedData = new ArrayList(labeledImages);

				TreeNode root = buildTree(bootstrappedData, cHasher, numVarsRep, preComputedHashesTestAgainst);
				forestCandidate.add(root);

//				bootstrappedOutOfBag.put(root, new HashSet<>(outOfBagData));
				return null;
			}));
		}

		// Wait for completion
		for (Future<?> task : tasks) {
			try {
				task.get();
			} catch (InterruptedException | ExecutionException e) {
				e.printStackTrace();
			}
		}

//		double outOfBagError = test(forestCandidate, randomVariables, bootstrappedOutOfBag, preComputedHashesTestAgainst,
//				true);
//		double classificationErrorAll = test(forestCandidate, randomVariables, null, preComputedHashesTestAgainst, true);

		// TODO
//		return new Object[] { forestCandidate, outOfBagError, classificationErrorAll };
		return new Object[] { forestCandidate, 0d, 0d};
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

	private TreeNode buildTree(List<LabeledImage> testData,
			CountHashCollection<Pair<FuzzyHash, HashingAlgorithm>> cHasher, int numVars,
			Map<HashingAlgorithm, Map<BufferedImage, Hash>> preComputedHashes) {
		return buildTree(testData, cHasher, numVars, preComputedHashes, Double.MAX_VALUE);
	}
	// TODO
//	/**
//	 * 
//	 * @param testData
//	 * @param bootstrappedOutOfBag         provide an out of bag test set. if null
//	 *                                     test against everything
//	 * @param preComputedHashesTestAgainst
//	 * @param verbose                      print computed metrics
//	 * @return the classification error (out of bag error)
//	 */
	/*
	 * // private double test(List<TreeNode> forest, List<TestData> testData, //
	 * Map<TreeNode, Set<TestData>> bootstrappedOutOfBag, // Map<HashingAlgorithm,
	 * Map<BufferedImage, Hash>> preComputedHashesTestAgainst, boolean verbose) { //
	 * // System.out.println("\n----- TEST ------\n"); // // // Test // int
	 * truePositive = 0; // int trueNegative = 0; // int falsePositive = 0; // int
	 * falseNegative = 0; // // List<TestData> matchNode = new
	 * ArrayList<TestData>(); // List<TestData> distinctNode = new
	 * ArrayList<TestData>(); // // boolean outOfBag = bootstrappedOutOfBag != null;
	 * // // for (int i = 0; i < testData.size(); i++) { // // TestData tData =
	 * testData.get(i); // // int matchCount = 0; // int distinctCount = 0; // //
	 * for (TreeNode tree : forest) { // // Only test if it's an out of bag sample
	 * // // if (!outOfBag || bootstrappedOutOfBag.get(tree).contains(tData)) { //
	 * if (tree.predictAgainstAllExcludingSelf(tData.b0,
	 * preComputedHashesTestAgainst)) { // matchCount++; // } else { //
	 * distinctCount++; // } // } // } // // boolean match = matchCount >
	 * distinctCount; // // if (match) { // if (tData.match) { // truePositive++; //
	 * } else { // falsePositive++; // } // matchNode.add(tData); // } else { // if
	 * (tData.match) { // falseNegative++; // //
	 * root.predictAgainstAllDebug(tData.b0); // } else { // trueNegative++; // } //
	 * distinctNode.add(tData); // } // } // // int sum = (int) (truePositive +
	 * falseNegative + trueNegative + falsePositive); // // if (verbose) { ////
	 * double weightMatching = (truePositive + falseNegative); //// double
	 * weightDistinct = (trueNegative + falsePositive); // //// double tpW =
	 * truePositive * weightDistinct; //// double fnW = falseNegative *
	 * weightDistinct; //// double tnW = trueNegative * weightMatching; //// double
	 * fpW = falsePositive * weightMatching; // //// double giniImpurityMatch = 1 -
	 * Math.pow(tpW / (double) (tpW + fpW), 2) //// - Math.pow(fpW / (double) (tpW +
	 * fpW), 2); // // //// double giniImpurityDistinct = 1 - Math.pow(tnW /
	 * (double) (tnW + fnW), 2) //// - Math.pow(fnW / (double) (tnW + fnW), 2); ////
	 * double giniImpurity = (giniImpurityMatch + giniImpurityDistinct) / 2; //// //
	 * // double giniImpurityMatch = 1 - Math.pow(truePositive / (double)
	 * (truePositive + falsePositive), 2) // - Math.pow(falsePositive / (double)
	 * (truePositive + falsePositive), 2); // // double giniImpurityDistinct = 1 -
	 * Math.pow(trueNegative / (double) (trueNegative + falseNegative), 2) // -
	 * Math.pow(falseNegative / (double) (trueNegative + falseNegative), 2); // //
	 * // Weighted gini impurity // double leftWeight = (truePositive +
	 * falsePositive) / (double) sum; // double rightWeight = (trueNegative +
	 * falseNegative) / (double) sum; // // double giniImpurity = (leftWeight *
	 * giniImpurityMatch + rightWeight * giniImpurityDistinct); // // // The stuff
	 * we classify as duplicates really are duplicates! // double recall =
	 * truePositive / (double) (truePositive + falseNegative); // double specifity =
	 * trueNegative / (double) (trueNegative + falsePositive); // // double temp =
	 * truePositive + falsePositive; // double precision = Double.NaN; // if (temp
	 * != 0) { // precision = truePositive / temp; // } // double f1 = 2 *
	 * (precision * recall) / (precision + recall); // // System.out.printf( //
	 * "Gini impurity: %.4f | TP: %4d TN: %4d FP: %4d FN: %4d | Recall: %.4f Spec: %.3f Precision %.3f F1: %.3f %n"
	 * , // giniImpurity, truePositive, trueNegative, falsePositive, falseNegative,
	 * recall, specifity, // precision, f1); // } // // return (falsePositive +
	 * falseNegative) / (double) sum; // }
	 * 
	 * // TODO average hash has a good f1 score due to filtering out many true //
	 * negatives. but fails at false negatives.. // TODO also get the false
	 * negatives...
	 * 
	 * // private TreeNode buildTree(List<LabeledImage> testData,
	 * CountHashCollection<Pair<FuzzyHash,HashingAlgorithm>> cHasher, int numVars,
	 * // Map<HashingAlgorithm, Map<BufferedImage, Hash>> preComputedHashes) { //
	 * return buildTree(testData, cHasher, numVars, preComputedHashes,
	 * Double.MAX_VALUE); // }
	 */

	private TreeNode buildTree(List<LabeledImage> testData,
			CountHashCollection<Pair<FuzzyHash, HashingAlgorithm>> cHasher, int numVars,
			Map<HashingAlgorithm, Map<BufferedImage, Hash>> preComputedHashes, double threshold) {

		CountHashCollection<Pair<FuzzyHash, HashingAlgorithm>> algorithmCopy = new CountHashCollection<>(cHasher);
		Triple<TreeNode, List<LabeledImage>[], Integer> packed = computeNode(testData, algorithmCopy, numVars,
				preComputedHashes, threshold);

//		if (packed == null) {
//			return new LeafNode(left);
//		}

		if (packed.getFirst() instanceof LeafNode) {
			return packed.getFirst();
		}

		InnerNode node = (InnerNode) packed.getFirst();

		// TODO move this into the compute node function

		if (packed.getSecond()[0].size() > 0 && !MathUtil.isDoubleEquals(node.qualityLeft, 0, 1e-8)) {
			node.leftNode = buildTree(packed.getSecond()[0], algorithmCopy, numVars, preComputedHashes,
					node.qualityLeft);
		} else {
//			// Check if it's a true or false node. due to gini coefficient this may swap
//			long size = packed.getSecond()[0].size();
//			long match = packed.getSecond()[0].stream().filter(i -> i.match).count();
//			long distinct = size - match;
//
//			//TODO also get the best category back.
//			
//			// Either we have 1 node left or they are all the same
//			if (match > distinct) {
//				node.leftNode = new LeafNode(true);
//			} else {
			node.leftNode = new LeafNode(packed.getThird());

			// }
		}

		if (packed.getSecond()[1].size() > 0 && !MathUtil.isDoubleEquals(node.qualityRight, 0, 1e-8)) {
			node.rightNode = buildTree(packed.getSecond()[1], algorithmCopy, numVars, preComputedHashes,
					node.qualityRight);
		} else {
//			long size = packed.getSecond()[1].size();
//			long match = packed.getSecond()[1].stream().filter(i -> i.match).count();
//			long distinct = size - match;
//
//			if (match > distinct) {
//				node.rightNode = new LeafNode(true);
//			} else {
			node.rightNode = new LeafNode(packed.getThird());
//			}
		}

		return node;
	}

	/**
	 * Create a tree node at the given point in the tree.
	 * <p>
	 * If a new node was found which improves distinction an inner node is returned.
	 * The algorithm copy is reduces by the algorithm picked to allow to precisely
	 * chose how often a single hashing algorithm appears in a branch.
	 * <p>
	 * If no better node can be found a leaf node is created if the node has way to
	 * tell if more matches or mismatches are present. If it's indistinguishable
	 * null will be returned.
	 * 
	 * 
	 * @param testData          The data used to test
	 * @param algorithmCopy     The algorithms (variables) available to choose from
	 * @param numVars           The number of variables that may be selected (only a
	 *                          subset of variables may be chosen randomly to
	 *                          prevent overfitting)
	 * @param preComputedHashes The cached hashes
	 * @param qualityThreshold  the quality the node has to suppress in order for
	 *                          this node to contribute positively to the tree
	 * @return
	 */
	private Triple<TreeNode, List<LabeledImage>[], Integer> computeNode(List<LabeledImage> testData,
			CountHashCollection<Pair<FuzzyHash, HashingAlgorithm>> randomVariables, int numVars,
			Map<HashingAlgorithm, Map<BufferedImage, Hash>> preComputedHashes, double qualityThreshold) {

		// Compute the actual gini coefficient.

		double bestCutoff = 0;
		Pair<FuzzyHash, HashingAlgorithm> bestVariable = null;
		double bestGini = Double.MAX_VALUE;

		double giniLeft = Double.MAX_VALUE;
		double giniRight = Double.MAX_VALUE;

		int nodeCategory = -1;

//		String bestDebug = "";
		double bestF1Score = -Double.MAX_VALUE;

		List<LabeledImage>[] propagatedTestData = new ArrayList[2];

		propagatedTestData[0] = new ArrayList<>();
		propagatedTestData[1] = new ArrayList<>();

		PcgRSFast rng = new PcgRSFast();

		// Compute numVars random indices

		int numVarsAvailable = randomVariables.sizeUnique();

		// Randomly select hashing algorithms indices
		List<Integer> indices = new ArrayList<>(numVarsAvailable);
		for (int i = 0; i < numVarsAvailable; i++) {
			indices.add(i);
		}

		Collections.shuffle(indices, rng);

		@SuppressWarnings("unchecked")
		Pair<FuzzyHash, HashingAlgorithm>[] variablesAvailable = randomVariables
				.toArrayUnique(new Pair[randomVariables.sizeUnique()]);

		for (int i = 0; i < numVars; i++) {

			if (indices.isEmpty()) {
				break;
			}

			Pair<FuzzyHash, HashingAlgorithm> randomVariable = variablesAvailable[indices.remove(0)];

			FuzzyHash variableHash = randomVariable.getFirst();
			HashingAlgorithm hashAlgorithm = randomVariable.getSecond();

			// System.out.println("\n" + hashAlgorithm + " CompTo: " + qualityThreshold);

			// Test every image against each category
			Map<BufferedImage, Hash> hashes = preComputedHashes.get(hashAlgorithm);

			Set<Double> distanceSet = new LinkedHashSet<>();

			Map<BufferedImage, Double> minDistanceMap = new HashMap<>();

			// TODO cache
			/* Prepare numerical categories */
			for (LabeledImage tData : testData) {
				Hash tHash = hashes.get(tData.getbImage());

				// TODO hamming distance fast alternative.
				double distance = variableHash.normalizedHammingDistance(tHash);
				minDistanceMap.put(tData.getbImage(), distance);
				distanceSet.add(distance);
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

				List<LabeledImage> matchNode = new ArrayList<>();
				List<LabeledImage> distinctNode = new ArrayList<>();

				// Check which category this node is checking for.

				// TODO do we really need a map? Isn't an array enough
				Map<Integer, Integer> dominantCategory = new HashMap<>();

				for (LabeledImage tData : testData) {
					double distance = minDistanceMap.get(tData.getbImage());

					if (distance < cutoff) {
						int category = tData.getCategory();
						dominantCategory.merge(category, 1, (oldV, newV) -> {
							return oldV + newV;
						});
					}
				}

				int bestCategory = -1;
				int bestCount = 0;
				for (Entry<Integer, Integer> e : dominantCategory.entrySet()) {
					int count = e.getValue();
					int cat = e.getKey();
					if (bestCategory == -1) {
						bestCategory = cat;
						bestCount = count;
					} else if (count > bestCount) {
						bestCategory = cat;
						bestCount = count;
					}
				}

				if (bestCategory == -1) {
					System.out.println("Error no best category found?");
				}

				for (LabeledImage tData : testData) {
					double distance = minDistanceMap.get(tData.getbImage());

					boolean match = tData.getCategory() == bestCategory;

					if (distance < cutoff) {
						if (match) {
							truePositive++;
						} else {
							falsePositive++;
						}
						matchNode.add(tData);
					} else {
						if (match) {
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

//				double tpW = truePositive * weightDistinct;
//				double fnW = falseNegative * weightDistinct;
//				double tnW = trueNegative * weightMatching;
//				double fpW = falsePositive * weightMatching;

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
					bestVariable = randomVariable;
					propagatedTestData[0] = matchNode;
					propagatedTestData[1] = distinctNode;
					bestGini = giniImpurity;

					nodeCategory = bestCategory;

					giniLeft = giniImpurityMatch;
					giniRight = giniImpurityDistinct;

//					bestDebug = String.format(
//							"Gini impurity: %.4f Cutoff: %.3f | TP: %4d TN: %4d FP: %4d FN: %4d | Recall: %.4f Spec: %.3f Precision %.3f F1: %.3f",
//							giniImpurity, cutoff, truePositive, trueNegative, falsePositive, falseNegative, recall,
//							specifity, precision, f1);
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
		randomVariables.remove(bestVariable);
		// Remove the best algorithm from this list.

		if (bestGini < qualityThreshold && !MathUtil.isDoubleEquals(bestGini, qualityThreshold, 1e-8)) {
			// if (bestF1Score > qualityThreshold) {
			InnerNode node = new InnerNode(bestVariable.getFirst(), bestVariable.getSecond(), bestCutoff, bestGini,
					giniLeft, giniRight);
//			System.out.println("\nBestNode:\n" + node + " Best Gini" + bestGini + " Threshold: " + qualityThreshold);
//			System.out.println(bestDebug);
//
//			System.out.println("LeftNode: " + propagatedTestData[0].size() + " "
//					+ propagatedTestData[0].stream()
//							.collect(Collectors.groupingBy((i) -> i.match, Collectors.counting()))
//					+ " RightNode: " + propagatedTestData[1].size() + " " + propagatedTestData[1].stream()
//							.collect(Collectors.groupingBy((i) -> i.match, Collectors.counting()))
//					+ "\n");

			return new Triple<>(node, propagatedTestData, nodeCategory);
		} else {

			return new Triple<>(new LeafNode(nodeCategory), null, nodeCategory);

			// TODO we don't return null if count is equal....
//			long size = testData.size();
//			long match = testData.stream().filter(i -> i.match).count();
//			long distinct = size - match;
//
//			if (match > distinct) {
//				return new Pair<>(new LeafNode(true), null);
//			} else if (match == distinct) {
//				return null;
//			} else {
//				return new Pair<>(new LeafNode(false), null);
//			}

		}
	}

//	/**
//	 * 
//	 * 
//	 * @implNote Create a balanced test data set by pairing labeled images and build
//	 *           every possible combination.
//	 * 
//	 * 
//	 * 
//	 * @return
//	 */
//	private List<TestData> createTestData() {
//
//		int testCases = MathUtil.triangularNumber(labeledImages.size() - 1);
//		List<TestData> testData = new ArrayList<>(testCases);
//
//		List<TestData> matchData = new ArrayList<>(testCases);
//		List<TestData> distinctData = new ArrayList<>(testCases);
//
//		// Create every possible combination (TODO this can become expensive if we have
//		// many images).
//		// We could also just randomly drawn and create what is needed.
//		for (List<BufferedImage> images : labeledImages.values()) {
//			boolean match = images.size() > 1;
//			for (BufferedImage b : images) {
//				testData.add(new TestData(b, match));
//				if (match) {
//					matchData.add(new TestData(b, match));
//				} else {
//					distinctData.add(new TestData(b, match));
//				}
//			}
//		}
//
//		int matchC = matchData.size();
//		int distinctC = distinctData.size();
//
//		PcgRSFast rng = new PcgRSFast();
//
//		// Create a balanced dataset between matches and distinct ...
//		if (matchC > distinctC) {
//			while (matchC > distinctC) {
//				matchData.remove(rng.nextInt(matchC--));
//			}
//		} else if (distinctC > matchC) {
//			while (distinctC > matchC) {
//				distinctData.remove(rng.nextInt(distinctC--));
//			}
//		}
//
//		// TODO
//		testData = new ArrayList<>(testCases);
//		testData.addAll(matchData);
//		testData.addAll(distinctData);
//
//		System.out.println("Match: " + matchC + " Distinct: " + distinctC + " Size : " + testData.size());
//
//		// Balance the dataset;
//
//		return testData;
//	}

	/**
	 * As variables for our tree we take the distance to random points in our hash
	 * space.
	 * 
	 * @param preComputedHashes
	 * 
	 * @return
	 */
	private List<Pair<FuzzyHash, HashingAlgorithm>> createFuzzyHashes(
			Map<HashingAlgorithm, Map<BufferedImage, Hash>> preComputedHashes) {

		List<Pair<FuzzyHash, HashingAlgorithm>> variableList = new ArrayList<>();

		// At first take distinct corners.

//		for(HashingAlgorithm algo : this.steps.keySet()) {
//			
//			int keyLength = algo.getKeyResolution();
//			
//			FuzzyHash fuzzy = new FuzzyHash();
//			//All 0 hash
//			fuzzy.mergeFast(new Hash(BigInteger.ZERO,keyLength,algo.algorithmId()));
//			
//			StringBuilder sb = new StringBuilder()
//			//All 1 hash
//			
//			//0101 Hash
//			
//			//1010 Hash
//		}
//		
		// Lets work with the cluster midpoints ...

		for (HashingAlgorithm algo : this.steps.keySet()) {

			Map<Integer, FuzzyHash> fuzzyHashMap = new HashMap<>();
			for (LabeledImage lImage : labeledImages) {
				FuzzyHash fuzzy;

				if (fuzzyHashMap.containsKey(lImage.getCategory())) {
					fuzzy = fuzzyHashMap.get(lImage.getCategory());
				} else {
					fuzzy = new FuzzyHash();
					fuzzyHashMap.put(lImage.getCategory(), fuzzy);
					variableList.add(new Pair<>(fuzzy, algo));
				}
				fuzzy.mergeFast(preComputedHashes.get(algo).get(lImage.getbImage()));
			}
		}
//		
//		preComputedHashes
//		
		return variableList;
	}

	public static void main(String[] args) {
		
		//Simple test case
		RandomForestCategorizer randomForst = new RandomForestCategorizer();
		
		
		
		randomForst.addHashingAlgorithm(new AverageHash(32),1);
		randomForst.addTestImages(new LabeledImage(0,new File("src/test/resources/ballon.jpg")));
		randomForst.addTestImages(new LabeledImage(1,new File("src/test/resources/copyright.jpg")));
		randomForst.addTestImages(new LabeledImage(1,new File("src/test/resources/highQuality.jpg")));
		randomForst.addTestImages(new LabeledImage(1,new File("src/test/resources/lowQuality.jpg")));
		randomForst.addTestImages(new LabeledImage(2,new File("src/test/resources/Lenna.png")));
		
		randomForst.trainMatcher(1,1,1);
		randomForst.forest.get(0).printTree();
	}
	
}
