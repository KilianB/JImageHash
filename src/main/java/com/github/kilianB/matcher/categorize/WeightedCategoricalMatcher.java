package com.github.kilianB.matcher.categorize;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.github.kilianB.datastructures.ClusterResult;
import com.github.kilianB.datastructures.KMeans;
import com.github.kilianB.datastructures.Pair;
import com.github.kilianB.datastructures.tree.binaryTreeFuzzy.FuzzyBinaryTree;
import com.github.kilianB.hashAlgorithms.HashingAlgorithm;
import com.github.kilianB.matcher.FuzzyHash;
import com.github.kilianB.matcher.Hash;

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

	public enum DimReduction {
		NONE, K_MEANS_APPROXIMATION, BINARY_TREE
	}

	// Careful binary tree does not call categorize image!

	private DimReduction dimensionalityReduction = DimReduction.NONE;

	public WeightedCategoricalMatcher(DimReduction reductionTechnique) {
		this.dimensionalityReduction = reductionTechnique;
	}

	// TODO binary tree clustering, random projection?
	// We need to reduce the dimensionality

	ClusterResult clusterResult = null;
	FuzzyBinaryTree fuzzyBinaryTree = null;

	protected void clusterPostcomputation() {
		// Cleanup
		clusterResult = null;
		fuzzyBinaryTree = null;
	}

	protected void clusterPrecomputation() {
		// The real category matched to an cluster center
		Map<FuzzyHash, Integer> hashClusterIdMap = new HashMap<>();
		Map<Integer, Integer> mapCategoryIdToClusterId = new HashMap<>();

		// Setup dimensionality reduction
		if (dimensionalityReduction.equals(DimReduction.K_MEANS_APPROXIMATION)) {
			Hash[] clusters = new Hash[this.getCategories().size()];

			int clusterCount = clusters.length / 10;
			KMeans clusterer = new KMeans(clusterCount <= 0 ? 1 : clusterCount);
			int i = 0;
			for (Entry<HashingAlgorithm, Map<Integer, FuzzyHash>> e : clusterHash.entrySet()) {
				for (Entry<Integer, FuzzyHash> fuzzy : e.getValue().entrySet()) {
					int category = fuzzy.getKey();
					hashClusterIdMap.put(fuzzy.getValue(), i);
					// TODO order
					mapCategoryIdToClusterId.put(i, category);
					clusters[i++] = fuzzy.getValue();
				}
			}
			clusterResult = clusterer.cluster(clusters);
		} else if (dimensionalityReduction.equals(DimReduction.BINARY_TREE)) {
			fuzzyBinaryTree = new FuzzyBinaryTree(false);
			for (Entry<HashingAlgorithm, Map<Integer, FuzzyHash>> e : clusterHash.entrySet()) {
				for (Entry<Integer, FuzzyHash> fuzzy : e.getValue().entrySet()) {
					fuzzyBinaryTree.addHash(fuzzy.getValue());
				}
			}
		}
	}

	@Override
	protected int getCategory(int iter, String uniqueId, Hash[] hashes, Set<Integer> categoriesAltered) {
		int category = Integer.MIN_VALUE;
		if (iter == 0) {
			if (dimensionalityReduction.equals(DimReduction.K_MEANS_APPROXIMATION)) {
				categoriesAltered.clear();

				// These are the cluster id's that are reasonable
				Set<Integer> potentialClusterIds = clusterResult.getPotentialFits(hashes[0], 1).keySet();
				// Now we need to retrieve the categories that are associated with the cluster
				for (Integer clusters : potentialClusterIds) {
					categoriesAltered.addAll(clusterResult.clusterIndexToDataIndex(clusters));
				}
			} else if (dimensionalityReduction.equals(DimReduction.BINARY_TREE)) {
				FuzzyHash binTree = fuzzyBinaryTree.getNearestNeighbour(hashes[0]).get(0).value;
				category = clusterReverseLookup.get(this.steps.keySet().iterator().next()).get(binTree);
			}
		}

		if (iter != 0 || !dimensionalityReduction.equals(DimReduction.BINARY_TREE)) {
			// Categorize the image based on the current clusters
			Pair<Integer, Double> catResult = this.categorizeImage(uniqueId, hashes, categoriesAltered);
			category = catResult.getFirst();
		}
		return category;
	}

	protected double computeDistanceForCategory(Hash[] hashes, int category, double bestDistance) {
		int j = 0;
		double hammingDistance = 0;
		for (HashingAlgorithm hashAlgorithm : this.steps.keySet()) {
			FuzzyHash clusterMid = clusterHash.get(hashAlgorithm).get(category);
			Hash imageHash = hashes[j++];
			hammingDistance += clusterMid.weightedDistance(imageHash);
			if (hammingDistance > bestDistance) {
				return Double.MAX_VALUE;
			}
		}
		return hammingDistance;
	}

	protected double computeDistanceToCluster(FuzzyHash cluster, Hash imageHash) {
		return cluster.weightedDistance(imageHash);
	}

}
