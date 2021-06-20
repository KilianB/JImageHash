package dev.brachtendorf.jimagehash.matcher.categorize;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import dev.brachtendorf.Require;
import dev.brachtendorf.jimagehash.datastructures.ClusterResult;
import dev.brachtendorf.jimagehash.datastructures.KMeansPlusPlus;
import dev.brachtendorf.jimagehash.hash.Hash;
import dev.brachtendorf.jimagehash.hashAlgorithms.HashingAlgorithm;


/**
 * The kMeans approach requires to know the number of clusters beforehand.
 * 
 * 
 * 
 * 
 * @author Kilian
 * @since 3.0.0
 */
public class KMeansClassifier implements CategoricalImageMatcher {

	/**
	 * The algorithm used to cluster the hashes
	 */
	private KMeansPlusPlus clusterer;

	/**
	 * Keep track of the added hashes so we can recompute clusters if necessary
	 */
	private LinkedHashMap<String, Hash> addedHashes = new LinkedHashMap<>();

	private Map<String, Integer> hashesToDataIndex  = new HashMap<>();
	
	/**
	 * The current clusters of this algorithm
	 */
	private ClusterResult res;

	/**
	 * The hashing algorithm used to create hashes to compare against our current
	 * cluster
	 */
	private HashingAlgorithm hasher;

	/**
	 * 
	 * @param k      The number of clusters to
	 * @param hasher The hashing algorithm used to create hashes
	 */
	public KMeansClassifier(int k, HashingAlgorithm hasher) {
		clusterer = new KMeansPlusPlus(Require.positiveValue(k));
		this.hasher = Objects.requireNonNull(hasher);
	}

	@Override
	public void recomputeCategories() {

		Hash[] h = addedHashes.values().toArray(new Hash[addedHashes.size()]);
		res = clusterer.cluster(h);
	}

	@Override
	public CategorizationResult categorizeImage(BufferedImage bi) {
		return this.categorizeImage(hasher.hash(bi));
	}

	private CategorizationResult categorizeImage(Hash hash) {
		if (res == null) {
			return new CategorizationResult(0, Double.NaN);
		}
		int cluster = res.getBestFitCluster(hash);
		return new CategorizationResult(cluster, res.getCenteroid(cluster).normalizedHammingDistanceFast(hash));

	}

	public void addImage(BufferedImage bi, String uniqueId) {
		Hash hash = hasher.hash(bi);
		this.addImage(hash, uniqueId);
	}

	private void addImage(Hash hash, String uniqueId) {
		addedHashes.put(uniqueId, hash);
		hashesToDataIndex.put(uniqueId,addedHashes.size()-1);
	}

	@Override
	public CategorizationResult categorizeImageAndAdd(BufferedImage bi, String uniqueId) {
		Hash hash = hasher.hash(bi);
		//Only add it to the queue
		this.addImage(hash, uniqueId);
		return this.categorizeImage(hash);
	}


	@Override
	public List<Integer> getCategories() {
		List<Integer> categories = new ArrayList<>(res.getClusters().keySet());
		// -1 is noise which does not happen in kmeans
		categories.remove(Integer.valueOf(-1));
		return categories;
	}

	@Override
	public List<String> getImagesInCategory(int category) {
		// TODO speed it up
		List<String> ids = new ArrayList<>(addedHashes.size());
		String[] indices = addedHashes.keySet().toArray(new String[addedHashes.size()]);

		List<Integer> data = res.clusterIndexToDataIndex(category);
		Collections.sort(data);

		for (Integer index : data) {
			ids.add(indices[index]);
		}
		return ids;
	}


	@Override
	public int getCategory(String uniqueId) {
		return res.indexToCluster(hashesToDataIndex.get(uniqueId));
	}

}
