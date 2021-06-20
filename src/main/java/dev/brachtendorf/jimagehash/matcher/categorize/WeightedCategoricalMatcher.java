package dev.brachtendorf.jimagehash.matcher.categorize;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Logger;


import dev.brachtendorf.jimagehash.datastructures.ClusterResult;
import dev.brachtendorf.jimagehash.datastructures.KMeans;
import dev.brachtendorf.jimagehash.datastructures.tree.binaryTreeFuzzy.FuzzyBinaryTree;
import dev.brachtendorf.jimagehash.hash.FuzzyHash;
import dev.brachtendorf.jimagehash.hash.Hash;
import dev.brachtendorf.jimagehash.hashAlgorithms.HashingAlgorithm;

/**
 ** Cluster images into common categories. This matcher clusters images by
 * computing the distance to the closest cluster and adds an image if it is
 * within a given distance. This method works only approximate
 * {@link #recomputeCategories()} has to be called after images have been added.
 * 
 * <p>
 * Cluster centeroids are represented as FuzzyHashes a prot hash represented as
 * mode hash of all added images.
 * <p>
 * Opposed to the categorical matcher this matcher calculates the distances to
 * clusters using the weighted distance, resulting in more accurate results but
 * at the cost of increase computational requirements.
 * 
 * @author Kilian
 * @since 3.0.0
 */
public class WeightedCategoricalMatcher extends CategoricalMatcher {

	private static final Logger LOGGER = Logger.getLogger(WeightedCategoricalMatcher.class.getSimpleName());

	/**
	 * 
	 * @author Kilian
	 *
	 */
	public enum DimReduction {
		/**
		 * Use no metric to speed up cluster recomputation.
		 */
		NONE,
		/**
		 * Use KMeans cluster to fit multiple clusters into subcategories reducing the
		 * number of clusters images have to be checked against.
		 * This approach usually performs well if enough images are added to the matcher but may
		 * fail at other occasions. Usually the fastest approach. It still is an approximation.!
		 */
		K_MEANS_APPROXIMATION, 
		/**
		 * Construct a binary tree prior to recomputing the clusters. This step takes time
		 * and the tree might not be able to be pruned quickly. It's usually slower than 
		 * KMeans but results in correct computation.
		 */
		BINARY_TREE
	}

	private DimReduction dimensionalityReduction = DimReduction.NONE;

	// TODO check serialization we are not in the persistent package but might as
	// well try to handle it.
	protected transient ClusterResult[] clusterResult = null;
	protected transient FuzzyBinaryTree fuzzyBinaryTree = null;

	public WeightedCategoricalMatcher(double newCategoryThreshold, DimReduction reductionTechnique) {
		super(newCategoryThreshold);
		this.dimensionalityReduction = reductionTechnique;
	}

	protected void clusterPostcomputation() {
		// Cleanup
		clusterResult = null;
		fuzzyBinaryTree = null;
	}

	protected void clusterPrecomputation() {

		// Setup dimensionality reduction
		if (dimensionalityReduction.equals(DimReduction.K_MEANS_APPROXIMATION)) {
			clusterResult = new ClusterResult[this.steps.size()];

			int categoryCount = this.getCategories().size();
			int clusterCount = categoryCount / 10;
			KMeans clusterer = new KMeans(clusterCount <= 0 ? 1 : clusterCount);
			int i = 0;
			for (Entry<HashingAlgorithm, Map<Integer, FuzzyHash>> e : clusterHash.entrySet()) {
				clusterResult[i++] = clusterer.cluster(e.getValue().values().toArray(new Hash[categoryCount]));
			}
		} else if (dimensionalityReduction.equals(DimReduction.BINARY_TREE)) {
			fuzzyBinaryTree = new FuzzyBinaryTree(false);
			for (Entry<HashingAlgorithm, Map<Integer, FuzzyHash>> e : clusterHash.entrySet()) {
				fuzzyBinaryTree.addHashes(e.getValue().values());
			}
		}
	}

	@Override
	protected int getCategory(int iter, String uniqueId, Hash[] hashes, Set<Integer> categoriesAltered) {
		int category = Integer.MIN_VALUE;
		if (iter == 0) {
			if (dimensionalityReduction.equals(DimReduction.K_MEANS_APPROXIMATION)) {
				categoriesAltered.clear();
				for (int i = 0; i < this.steps.size(); i++) {
					// These are the cluster id's that are reasonable
					Set<Integer> potentialClusterIds = clusterResult[i].getPotentialFits(hashes[i], 1).keySet();
					// Now we need to retrieve the categories that are associated with the cluster
					for (Integer clusters : potentialClusterIds) {
						categoriesAltered.addAll(clusterResult[i].clusterIndexToDataIndex(clusters));
					}
				}

			} else if (dimensionalityReduction.equals(DimReduction.BINARY_TREE)) {
				FuzzyHash binTree = fuzzyBinaryTree.getNearestNeighbour(hashes[0]).get(0).value;
				category = clusterReverseLookup.get(this.steps.iterator().next()).get(binTree);
			}
		}

		if (iter != 0 || !dimensionalityReduction.equals(DimReduction.BINARY_TREE)) {
			// Categorize the image based on the current clusters
			CategorizationResult catResult = this.categorizeImage(uniqueId, hashes, categoriesAltered);
			category = catResult.getCategory();
		}
		return category;
	}

	protected double computeDistanceForCategory(Hash[] hashes, int category, double bestDistance) {
		int j = 0;
		double hammingDistance = 0;
		for (HashingAlgorithm hashAlgorithm : this.steps) {
			FuzzyHash clusterMid = clusterHash.get(hashAlgorithm).get(category);
			Hash imageHash = hashes[j++];
			hammingDistance += clusterMid.weightedDistance(imageHash);
			if (hammingDistance > bestDistance) {
				return Double.MAX_VALUE;
			}
		}
		return hammingDistance;
	}

	@Override
	public boolean addHashingAlgorithm(HashingAlgorithm algo) {
		boolean added = super.addHashingAlgorithm(algo);

		if (steps.size() > 1 && dimensionalityReduction.equals(DimReduction.BINARY_TREE)) {
			dimensionalityReduction = DimReduction.K_MEANS_APPROXIMATION;
			LOGGER.warning(
					"Binary tree approximation not supported for multiple hashes. Fall back to K_Means_approximation");
		}

		return added;
	}

	protected double computeDistanceToCluster(FuzzyHash cluster, Hash imageHash) {
		return cluster.weightedDistance(imageHash);
	}

}
