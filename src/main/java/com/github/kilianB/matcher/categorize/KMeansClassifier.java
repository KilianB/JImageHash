package com.github.kilianB.matcher.categorize;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;

import com.github.kilianB.Require;
import com.github.kilianB.datastructures.ClusterResult;
import com.github.kilianB.datastructures.KMeansPlusPlus;
import com.github.kilianB.datastructures.Pair;
import com.github.kilianB.hashAlgorithms.HashingAlgorithm;
import com.github.kilianB.matcher.Hash;

/**
 * @author Kilian
 *
 */
public class KMeansClassifier implements CategoricalImageMatcher {

	int k;
	private KMeansPlusPlus clusterer;

	private LinkedHashMap<String,Hash> addedHashes = new LinkedHashMap<>();
	
	private ClusterResult res;
	private HashingAlgorithm hasher;

	public KMeansClassifier(int k, HashingAlgorithm hasher) {
		this.k = k;
		clusterer = new KMeansPlusPlus(Require.positiveValue(k));
		this.hasher = Objects.requireNonNull(hasher);
	}

	@Override
	public void recomputeCategories() {
		
		Hash[] h = addedHashes.values().toArray(new Hash[addedHashes.size()]);
		Hash[] h1 = new Hash[addedHashes.size()];
		
		int i = 0;
		for(Entry<String,Hash> e : addedHashes.entrySet()) {
			h1[i++] = e.getValue();
		}
		
		System.out.println(Arrays.equals(h,h1));
				
		res = clusterer.cluster(h);
		System.out.println("Categories recomputed: " + res);
	}

	@Override
	public Pair<Integer, Double> categorizeImage(BufferedImage bi) {
		return this.categorizeImage(hasher.hash(bi));
	}

	private Pair<Integer, Double> categorizeImage(Hash hash) {
		if (res == null) {
			return new Pair<>(0, Double.NaN);
		}
		int cluster = res.getBestFitCluster(hash);
		return new Pair<>(cluster, res.getCenteroid(cluster).normalizedHammingDistanceFast(hash));

	}

	public void addImage(BufferedImage bi, String uniqueId) {
		Hash hash = hasher.hash(bi);
		this.addImage(hash,uniqueId);
	}

	private void addImage(Hash hash, String uniqueId) {
		addedHashes.put(uniqueId,hash);
	}

	@Override
	public Pair<Integer, Double> categorizeImageAndAdd(BufferedImage bi, double maxThreshold, String uniqueId) {
		Hash hash = hasher.hash(bi);
		this.addImage(hash,uniqueId);
		return this.categorizeImage(hash);
	}

	@Override
	public void addNestedMatcher(int category, CategoricalImageMatcher catMatcher) {
		// TODO Auto-generated method stub

	}

	@Override
	public List<Integer> getCategories() {
		List<Integer> categories = new ArrayList<>(res.getClusters().keySet());
		//-1 is noise which does not happen in kmeans
		categories.remove(Integer.valueOf(-1));
		return categories;
	}

	@Override
	public int getCategory(String uniqueId) {
		return res.lookupClusterIdForKnownHash(addedHashes.get(uniqueId));
	}

	@Override
	public List<String> getImagesInCategory(int category) {
		//TODO speed it up
		List<String> ids = new ArrayList<>(addedHashes.size());
		String[] indices = addedHashes.keySet().toArray(new String[addedHashes.size()]);
		
		List<Integer> data = res.clusterIndexToDataIndex(category);
		Collections.sort(data);
		
		for(Integer index : data) {
			ids.add(indices[index]);
		}
		return ids;
	}

}
