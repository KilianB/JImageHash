package dev.brachtendorf.jimagehash.matcher.categorize.supervised.randomForest;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;

import dev.brachtendorf.ArrayUtil;

import dev.brachtendorf.MathUtil;
import dev.brachtendorf.Require;
import dev.brachtendorf.datastructures.CountHashCollection;
import dev.brachtendorf.datastructures.Pair;
import dev.brachtendorf.datastructures.Triple;
import dev.brachtendorf.jimagehash.Experimental;
import dev.brachtendorf.jimagehash.hash.FuzzyHash;
import dev.brachtendorf.jimagehash.hash.Hash;
import dev.brachtendorf.jimagehash.hashAlgorithms.AverageHash;
import dev.brachtendorf.jimagehash.hashAlgorithms.HashingAlgorithm;
import dev.brachtendorf.jimagehash.hashAlgorithms.PerceptiveHash;
import dev.brachtendorf.jimagehash.hashAlgorithms.RotAverageHash;
import dev.brachtendorf.jimagehash.matcher.PlainImageMatcher;
import dev.brachtendorf.jimagehash.matcher.categorize.CategoricalImageMatcher;
import dev.brachtendorf.jimagehash.matcher.categorize.CategorizationResult;
import dev.brachtendorf.jimagehash.matcher.categorize.supervised.LabeledImage;

import com.github.kilianB.pcg.fast.PcgRSFast;

/**
 * 
 * @author Kilian
 * @deprecated not ready yet. got rewritten
 */
@Experimental("The image matcher categorizes images based on their distance to the closest match. While this is an okay approach clustering the images "
		+ "based on category yields much cleaner results.")
public class RandomForestCategorizer extends PlainImageMatcher implements CategoricalImageMatcher {

	/**
	 * Root nodes of all decision trees making up the random forest
	 */
	protected List<TreeNode> forest = new ArrayList<>();

	/**
	 * Test images used to create test sets to train the forest
	 */
	// TODO don't save the images directly. we don't want the reference to the
	// bufferedImage
	protected List<LabeledImage> labeledImages = new ArrayList<>();

	protected TreeSet<Integer> categories = new TreeSet<>();

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
		categories.add(lData.getCategory());
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
		categories.clear();
		labeledImages.clear();
	}

	/**
	 * Populate the decision trees used in this image matcher. The forest has to be
	 * initialized when ever new labeled test images are added.
	 * 
	 * @param trees              The number of trees created. Has to be odd. The
	 *                           more trees present the better the accuracy is
	 * @param numVarsSearchRange The number of variables used in each tree. Which
	 *                           variables are chosen is randomly decided. Not using
	 *                           every variable prevents overfitting. //TODO explain
	 *                           better
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

		System.out.println("Hashing algos available: " + steps);

		/*
		 * 0. precompute all hashes necessary.
		 */
		Map<HashingAlgorithm, Map<BufferedImage, Hash>> preComputedHashes = new HashMap<>();

		for (HashingAlgorithm hashAlgorithm : this.getAlgorithms()) {
			Map<BufferedImage, Hash> hashMap = new HashMap<>();
			for (LabeledImage lImage : labeledImages) {
				hashMap.put(lImage.getbImage(), hashAlgorithm.hash(lImage.getbImage()));
			}
			preComputedHashes.put(hashAlgorithm, hashMap);
		}

		// Create variables //TODO this should happen on a per tree basis. Move inside
		// loop
		List<Pair<FuzzyHash, HashingAlgorithm>> randomHashes = createFuzzyHashes(preComputedHashes);

		System.out.println(randomHashes);

		ExecutorService tPool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2);

		int maxNumberOfVariables = randomHashes.size();

		int numVars = (int) Math.sqrt(maxNumberOfVariables);

		boolean newBestFound = false;
//		for (int i = maxNumberOfVariables; i <= maxNumberOfVariables; i++) {
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

			// TODO here we don't event look at the i

			Object[] packedForestData = createForest(trees, i, numVarsRep, randomHashes, preComputedHashes, tPool);

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

		testDecisionTree(labeledImages);

//		testArtificialDecisionTree(bootstrappedData);
	}

	/**
	 * @param labeledImages2
	 */
	private void testDecisionTree(List<LabeledImage> labeledImages) {

		int match = 0;
		int mismatch = 0;

		for (LabeledImage lImage : labeledImages) {
			if (lImage.getCategory() == this.categorizeImage(lImage.getbImage()).getCategory()) {
				match++;
			} else {
				mismatch++;
			}
		}

		System.out.println("Classification Error: " + mismatch / ((double) match + mismatch));
	}

	/**
	 * 
	 * @param trees                        number of trees to create
	 * @param numVars                      number of variables to try at each
	 *                                     brench. preven overfitting
	 * @param numVarsRep                   number of duplicates for the same
	 *                                     variable allowed in a single tree
	 * @param randomVariables              variables to check against
	 * @param preComputedHashesTestAgainst hashes of test data
	 * @param tPool                        thread pool executor
	 * @return Object array containing a reference to the root, the out of bag
	 *         classification error and the total error including training data
	 */
	protected Object[] createForest(int trees, int numVars, int numVarsRep,
			List<Pair<FuzzyHash, HashingAlgorithm>> randomVariables,
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

				TreeNode root = buildTree(bootstrappedData, cHasher, numVars, preComputedHashesTestAgainst);
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
		return new Object[] { forestCandidate, 0d, 0d };
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

	private TreeNode buildTree(List<LabeledImage> testData,
			CountHashCollection<Pair<FuzzyHash, HashingAlgorithm>> cHasher, int numVars,
			Map<HashingAlgorithm, Map<BufferedImage, Hash>> preComputedHashes, double threshold) {

		CountHashCollection<Pair<FuzzyHash, HashingAlgorithm>> algorithmCopy = new CountHashCollection<>(cHasher);

		Triple<TreeNode, List<LabeledImage>[], int[]> packed = computeNode(testData, algorithmCopy, numVars,
				preComputedHashes, threshold);

		if (packed.getFirst() instanceof LeafNode) {
			return packed.getFirst();
		}

		InnerNode node = (InnerNode) packed.getFirst();

		// TODO move this into the compute node function

		if (packed.getSecond()[0].size() > 0 && !MathUtil.isDoubleEquals(node.qualityLeft, 0, 1e-8)) {
			node.leftNode = buildTree(packed.getSecond()[0], algorithmCopy, numVars, preComputedHashes,
					node.qualityLeft);
		} else {
			System.out.println("Create Leaf node");
			node.leftNode = new LeafNode(packed.getThird()[0]);
		}

		if (packed.getSecond()[1].size() > 0 && !MathUtil.isDoubleEquals(node.qualityRight, 0, 1e-8)) {
			node.rightNode = buildTree(packed.getSecond()[1], algorithmCopy, numVars, preComputedHashes,
					node.qualityRight);
		} else {
			System.out.println("Create Leaf node");
			node.rightNode = new LeafNode(packed.getThird()[1]);
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
	private Triple<TreeNode, List<LabeledImage>[], int[]> computeNode(List<LabeledImage> testData,
			CountHashCollection<Pair<FuzzyHash, HashingAlgorithm>> randomVariables, int numVars,
			Map<HashingAlgorithm, Map<BufferedImage, Hash>> preComputedHashes, double qualityThreshold) {

		// Compute the actual gini coefficient.

		double bestCutoff = 0;
		Pair<FuzzyHash, HashingAlgorithm> bestVariable = null;
		double bestGini = Double.MAX_VALUE;

		double giniLeft = Double.MAX_VALUE;
		double giniRight = Double.MAX_VALUE;

		int nodeCategoryLeft = -1;
		int nodeCategoryRight = -1;

//		String bestDebug = "";
		double bestF1Score = -Double.MAX_VALUE;

		// How many items did we find that match? In case of a gini impurity of
		int matchSize = 0;

		List<LabeledImage>[] propagatedTestData = new ArrayList[2];

		propagatedTestData[0] = new ArrayList<>();
		propagatedTestData[1] = new ArrayList<>();

		PcgRSFast rng = new PcgRSFast();

		// Compute numVars random indices

		int numVarsAvailable = randomVariables.sizeUnique();

//		System.out.println("\n" + numVarsAvailable);

		// Randomly select hashing algorithms indices
		List<Integer> indices = new ArrayList<>(numVarsAvailable);
		for (int i = 0; i < numVarsAvailable; i++) {
			indices.add(i);
		}

		Collections.shuffle(indices, rng);

		@SuppressWarnings("unchecked")
		Pair<FuzzyHash, HashingAlgorithm>[] variablesAvailable = randomVariables
				.toArrayUnique(new Pair[randomVariables.sizeUnique()]);

//		System.out.println("compute node: numVars: " + numVarsAvailable + " to test: " + numVars);
//		System.out.println("Vars: " + ArrayUtil.deepToStringFormatted(variablesAvailable));

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
//
//			System.out.println("Test variables: " + testData);

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

//			System.out.println("Pot cutoff values: " + potentialCutoffValues);

			for (double cutoff : potentialCutoffValues) {

				List<LabeledImage> leftNode = new ArrayList<>();
				List<LabeledImage> rightNode = new ArrayList<>();

				// Check which category this node is checking for.

				// TODO do we really need a map? Isn't an array enough
				Map<Integer, Integer> dominantCategoryLeft = new HashMap<>();
				Map<Integer, Integer> dominantCategoryRight = new HashMap<>();

				for (LabeledImage tData : testData) {
					double distance = minDistanceMap.get(tData.getbImage());
					if (distance < cutoff) {
						leftNode.add(tData);
						int category = tData.getCategory();
						dominantCategoryLeft.merge(category, 1, (oldV, newV) -> {
							return oldV + newV;
						});
					} else {
						rightNode.add(tData);
						int category = tData.getCategory();
						dominantCategoryRight.merge(category, 1, (oldV, newV) -> {
							return oldV + newV;
						});
					}
				}

				int leftCategory = -1;
				int rightCategory = -1;

				// Assign them either left or right node
				int bestCount = 0;
				for (Entry<Integer, Integer> e : dominantCategoryLeft.entrySet()) {
					int count = e.getValue();
					int cat = e.getKey();
					if (leftCategory == -1) {
						leftCategory = cat;
						bestCount = count;
					} else if (count > bestCount) {
						leftCategory = cat;
						bestCount = count;
					}
				}

				bestCount = 0;
				for (Entry<Integer, Integer> e : dominantCategoryRight.entrySet()) {
					int count = e.getValue();
					int cat = e.getKey();
					if (rightCategory == -1) {
						rightCategory = cat;
						bestCount = count;
					} else if (count > bestCount) {
						rightCategory = cat;
						bestCount = count;
					}
				}

				// TODO double
				int truePositiveLeft = 0;
				int trueNegativeLeft = 0;
				int falsePositiveLeft = 0;
				int falseNegativeLeft = 0;

				int truePositiveRight = 0;
				int trueNegativeRight = 0;
				int falsePositiveRight = 0;
				int falseNegativeRight = 0;

				for (LabeledImage tData : leftNode) {
					boolean matchLeft = tData.getCategory() == leftCategory;
					boolean matchRight = tData.getCategory() == rightCategory;
					if (matchLeft) {
						truePositiveLeft++;
					} else {
						falsePositiveLeft++;
					}
					if (matchRight) {
						falseNegativeRight++;
					} else {
						trueNegativeRight++;
					}
				}

				for (LabeledImage tData : rightNode) {
					boolean matchLeft = tData.getCategory() == leftCategory;
					boolean matchRight = tData.getCategory() == rightCategory;
					if (matchRight) {
						truePositiveRight++;
					} else {
						falsePositiveRight++;
					}
					if (matchLeft) {
						falseNegativeLeft++;
					} else {
						trueNegativeLeft++;
					}
				}

				int sum = (int) (truePositiveLeft + trueNegativeLeft + falsePositiveLeft + falseNegativeLeft);
				// TODO lets weight ehese

				double giniImpurityLeft = 1
						- Math.pow(truePositiveLeft / (double) (truePositiveLeft + falsePositiveLeft), 2)
						- Math.pow(falsePositiveLeft / (double) (truePositiveLeft + falsePositiveLeft), 2);

				double giniImpurityRight = 1
						- Math.pow(truePositiveRight / (double) (truePositiveRight + falsePositiveRight), 2)
						- Math.pow(falsePositiveRight / (double) (truePositiveRight + falsePositiveRight), 2);

				// Weighted gini impurity
				double leftWeight = (truePositiveLeft + falsePositiveLeft) / (double) sum;
				double rightWeight = (truePositiveRight + falsePositiveRight) / (double) sum;

				double giniImpurity = (leftWeight * giniImpurityLeft + rightWeight * giniImpurityRight);

				// The stuff we classify as duplicates really are duplicates!
				double recall = (truePositiveLeft + truePositiveRight)
						/ (double) ((truePositiveLeft + truePositiveRight) + (falseNegativeLeft + falseNegativeRight));
				double specifity = (trueNegativeLeft + trueNegativeRight)
						/ (double) ((trueNegativeLeft + trueNegativeRight) + (falsePositiveLeft + falsePositiveRight));

				double temp = (truePositiveLeft + truePositiveRight) + (falsePositiveLeft + falsePositiveRight);
				double precision = Double.NaN;
				if (temp != 0) {
					precision = (truePositiveLeft + truePositiveRight) / temp;
				}
				double f1 = 2 * (precision * recall) / (precision + recall);

//				bestDebug = String.format(
//						"Gini impurity: %.4f Cutoff: %.3f | TP: %4d TN: %4d FP: %4d FN: %4d | Recall: %.4f Spec: %.3f Precision %.3f F1: %.3f",
//						giniImpurity, cutoff, (truePositiveLeft + truePositiveRight),
//						(trueNegativeLeft + trueNegativeRight), (falsePositiveLeft + falsePositiveRight),
//						(falseNegativeLeft + falseNegativeRight), recall, specifity, precision, f1);
//				
//				System.out.println(bestDebug);

//				System.out.println(String.format(
//						"Gini impurity: %.4f Cutoff: %.3f | TP: %4d TN: %4d FP: %4d FN: %4d | Recall: %.4f Spec: %.3f Precision %.3f F1: %.3f GiniLeft %.3f Weight: %.3f GiniRight %.3f Weight: %.3f",
//						giniImpurity, cutoff, truePositive, trueNegative, falsePositive, falseNegative, recall,
//						specifity, precision, f1, giniImpurityMatch, leftWeight, giniImpurityDistinct, rightWeight));

				if (giniImpurity < bestGini || giniImpurity == bestGini && leftNode.size() > matchSize) {
					// if (f1 > bestF1Score) {
					bestCutoff = cutoff;
					bestVariable = randomVariable;
					propagatedTestData[0] = leftNode;
					propagatedTestData[1] = rightNode;
					bestGini = giniImpurity;

					matchSize = leftNode.size();
					nodeCategoryLeft = leftCategory;
					nodeCategoryRight = rightCategory;

					giniLeft = giniImpurityLeft;
					giniRight = giniImpurityRight;
//
//					bestDebug = String.format(
//							"Gini impurity: %.4f Cutoff: %.3f | TP: %4d TN: %4d FP: %4d FN: %4d | Recall: %.4f Spec: %.3f Precision %.3f F1: %.3f",
//							giniImpurity, cutoff, (truePositiveLeft + truePositiveRight),
//							(trueNegativeLeft + trueNegativeRight), (falsePositiveLeft + falsePositiveRight),
//							(falseNegativeLeft + falseNegativeRight), recall, specifity, precision, f1);
//					System.out.println(bestDebug);
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

		if (bestGini < qualityThreshold && !MathUtil.isDoubleEquals(giniLeft, qualityThreshold, 1e-8)) {
			// if (bestF1Score > qualityThreshold) {

			// TODO right impurity calculation
			TreeNode node = new InnerNode(bestVariable.getFirst(), bestVariable.getSecond(), bestCutoff, bestGini,
					giniLeft, giniRight);
			int[] categories = { nodeCategoryLeft, nodeCategoryRight };
			return new Triple<>(node, propagatedTestData, categories);
		} else {

			// Best category

			Map<Integer, Long> count = testData.stream()
					.collect(Collectors.groupingBy(e -> e.getCategory(), Collectors.counting()));

			long maxCount = Long.MIN_VALUE;
			int bestCat = -1;
			for (Entry<Integer, Long> c : count.entrySet()) {

				if (c.getValue() > maxCount) {
					maxCount = c.getValue();
					bestCat = c.getKey();
				}
			}
//			
//			for(LabeledImage i : testData) {
//				dominantCategory.merge(i.getCategory(),1,(oldV,newV)->{return oldV + newV;});
//			}
//			
			LeafNode n = new LeafNode(bestCat);
			return new Triple<>(n, propagatedTestData, null);
		}
	}

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

		Set<Integer> cat = new HashSet<>();
		for (LabeledImage lImage : labeledImages) {
			cat.add(lImage.getCategory());
		}

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

		for (HashingAlgorithm algo : this.steps) {

			HashMap<Integer, FuzzyHash> clusteroid = new HashMap<>();
//			for (LabeledImage lImage : labeledImages) {
//				FuzzyHash fuzzy;
//
//				if (clusteroid.containsKey(lImage.getCategory())) {
//					fuzzy = clusteroid.get(lImage.getCategory());
//				} else {
//					fuzzy = new FuzzyHash();
//					clusteroid.put(lImage.getCategory(), fuzzy);
//					variableList.add(new Pair<>(fuzzy, algo));
//				}
//				fuzzy.mergeFast(preComputedHashes.get(algo).get(lImage.getbImage()));
//			}

			int keyResolution = algo.getKeyResolution();
			PcgRSFast rng = new PcgRSFast();

			// Lets work with randomized hashes
			for (int i = 0; i < cat.size(); i++) {
				BigInteger bInt = new BigInteger(keyResolution, rng);
				variableList.add(new Pair(new FuzzyHash(new Hash(bInt, keyResolution, algo.algorithmId())), algo));
			}

		}
//		
//		preComputedHashes
//		
		return variableList;
	}

	public Map<Integer, Integer> countLeafCategories() {

		Map<Integer, Integer> categoryTreeCount = new HashMap<>();

		Queue<TreeNode> queue = new ArrayDeque<>();
		queue.add(this.forest.get(0));

		while (!queue.isEmpty()) {
			TreeNode node = queue.poll();
			if (node instanceof InnerNode) {
				queue.add(((InnerNode) node).rightNode);
				queue.add(((InnerNode) node).leftNode);
			} else {
				categoryTreeCount.merge(((LeafNode) node).category, 1, (oldV, newV) -> {
					return oldV + newV;
				});
			}
		}
		return categoryTreeCount;
	}

	public static void main(String[] args) throws IOException {

		// Simple test case
		RandomForestCategorizer randomForst = new RandomForestCategorizer();

		randomForst.addHashingAlgorithm(new AverageHash(32));
		randomForst.addHashingAlgorithm(new PerceptiveHash(32));
		randomForst.addHashingAlgorithm(new RotAverageHash(32));

		randomForst.addTestImages(new LabeledImage(0, new File("src/test/resources/ballon.jpg")));
		randomForst.addTestImages(new LabeledImage(1, new File("src/test/resources/copyright.jpg")));
		randomForst.addTestImages(new LabeledImage(1, new File("src/test/resources/highQuality.jpg")));
		randomForst.addTestImages(new LabeledImage(1, new File("src/test/resources/lowQuality.jpg")));
		randomForst.addTestImages(new LabeledImage(2, new File("src/test/resources/Lenna.png")));
		randomForst.addTestImages(new LabeledImage(2, new File("src/test/resources/Lenna90.png")));
		randomForst.addTestImages(new LabeledImage(2, new File("src/test/resources/Lenna180.png")));
		randomForst.addTestImages(new LabeledImage(2, new File("src/test/resources/LennaSaltAndPepper.png")));
		randomForst.addTestImages(new LabeledImage(3, new File("src/test/resources/TestShapes.png")));

		randomForst.trainMatcher(3, 2, 1);
		randomForst.forest.get(0).printTree();

		BufferedImage bi = ImageIO.read(new File("src/test/resources/lowQuality.jpg"));

		System.out.println(randomForst.categorizeImage(bi));
	}

	/**
	 * 
	 * 
	 * {@inheritDoc}
	 * 
	 * <p>
	 * The distance returned by this method calls indicates how many percent of the
	 * trees in the random forest agree with the decision range (0 - 1].
	 */
	@Override
	public CategorizationResult categorizeImage(BufferedImage bi) {

		List<Integer> categories = getCategories();

		int[] catCount = new int[categories.size()];

		for (TreeNode root : forest) {
			int cat = root.predictAgainstAll(bi)[0];
			if (cat != -1) {
				catCount[cat]++;
			}
//			}else {
//				return new Pair<>(-1, 0d);
//			}
		}

		int maxIndex = ArrayUtil.maximumIndex(catCount);

		double agree = catCount[maxIndex] / (double) forest.size();

		int bestFitCategory = categories.get(maxIndex);
		// TODO what should we use here as distance?
		return new CategorizationResult(bestFitCategory, agree);
	}

	@Override
	public List<Integer> getCategories() {
		return new ArrayList<>(categories);
	}

	@Override
	public void recomputeCategories() {
		// TODO Auto-generated method stub

	}

	@Override
	public List<String> getImagesInCategory(int category) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getCategory(String uniqueId) {
		// TODO Auto-generated method stub
		return 0;
	}

	/**
	 * 
	 */
	public void printTree() {
		forest.get(0).printTree();
	}

	@Override
	public CategorizationResult categorizeImageAndAdd(BufferedImage bi, String uniqueId) {
		//Cache results and rebuild later?
		throw new UnsupportedOperationException("Can't add images on the fly. Rebuilding time to expensive");
	}

	// leaf nodes with the same category might indicate that we are dealing with 2
	// distinct groups of images.
	// TODO check if this is true
	// TODO cover the search space more optimally by created intermediate hashes
	// between all fuzzy hashes
	// TODO weight categorizes. Currently the biggest category will always be
	// filtered first
}
