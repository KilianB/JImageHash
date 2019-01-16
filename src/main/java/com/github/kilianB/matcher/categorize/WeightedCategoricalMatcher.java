package com.github.kilianB.matcher.categorize;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Logger;

import com.github.kilianB.datastructures.ClusterResult;
import com.github.kilianB.datastructures.KMeans;
import com.github.kilianB.datastructures.tree.binaryTreeFuzzy.FuzzyBinaryTree;
import com.github.kilianB.hash.FuzzyHash;
import com.github.kilianB.hash.Hash;
import com.github.kilianB.hashAlgorithms.HashingAlgorithm;

/**
 * Cluster images into common categories. A category
 * 
 * Computes the distance to the average hash of each category.
 * 
 * Be aware that the order in which images are added matters ... since the
 * average hash of a cluster is constantly updates without re checking old
 * entries.
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
		NONE, K_MEANS_APPROXIMATION, BINARY_TREE
	}

	private DimReduction dimensionalityReduction = DimReduction.NONE;

	public WeightedCategoricalMatcher(double newCategoryThreshold, DimReduction reductionTechnique) {
		super(newCategoryThreshold);
		this.dimensionalityReduction = reductionTechnique;
	}

	//TODO check serialization
	protected transient ClusterResult[] clusterResult = null;
	protected transient FuzzyBinaryTree fuzzyBinaryTree = null;

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
