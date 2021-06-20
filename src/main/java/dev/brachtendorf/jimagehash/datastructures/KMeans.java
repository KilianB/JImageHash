package dev.brachtendorf.jimagehash.datastructures;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import dev.brachtendorf.ArrayUtil;
import dev.brachtendorf.Require;
import dev.brachtendorf.jimagehash.hash.FuzzyHash;
import dev.brachtendorf.jimagehash.hash.Hash;

import com.github.kilianB.pcg.fast.PcgRSFast;

/**
 * @author Kilian
 *
 */
public class KMeans {

	private static final Logger LOGGER = Logger.getLogger(KMeans.class.getSimpleName());
	
	/**
	 * The number of cluster the data will be partitioned into
	 */
	protected int k;

	/**
	 * Create a KMeans clusterer
	 * 
	 * @param clusters the number of cluster to partition the data into
	 */
	public KMeans(int clusters) {
		this.k = Require.positiveValue(clusters);
	}

	public ClusterResult cluster(Hash[] hashes) {
		return cluster(hashes, Integer.MAX_VALUE);
	}

	public ClusterResult cluster(Hash[] hashes, int maxIter) {

		int[] cluster = new int[hashes.length];

		
		// If only one cluster is available return an array indicating all data
		// belonging to this one cluster
		if (k == 1) {
			return new ClusterResult(cluster, hashes);
		} else if (k > hashes.length) {
			ArrayUtil.fillArray(cluster, i ->{ return i;});
			LOGGER.info("Not enough images present for k categories. Assume: " + hashes.length + " cluster/s");
			return new ClusterResult(cluster, hashes);
		}

		// 0 = choose random start clusters
		FuzzyHash[] clusterMeans = computeStartingClusters(hashes);

		System.out.println("starting cluster");
		
		
		// Iteratively improve clusters
		computeKMeans(cluster, clusterMeans, hashes, maxIter);

		return new ClusterResult(cluster, hashes);
	}

	protected FuzzyHash[] computeStartingClusters(Hash[] hashes) {

		PcgRSFast rng = new PcgRSFast();

		// Lets randomly pick hashes
		List<Integer> randomIndices = new ArrayList<>(k);

		for (int i = 0; i < hashes.length; i++) {
			randomIndices.add(i);
		}

		Collections.shuffle(randomIndices, rng);

		FuzzyHash[] startingClusters = new FuzzyHash[k];

		System.out.println(randomIndices);
		
		for (int i = 0; i < k; i++) {
			startingClusters[i] = new FuzzyHash();
			startingClusters[i].mergeFast(hashes[randomIndices.remove(0)]);
		}

		return startingClusters;
	}

	protected void computeKMeans(int[] cluster, FuzzyHash[] clusterMeans, Hash[] hashes, int maxIter) {

		int iter = 0;

		boolean dirty = false;

		do {
			dirty = false;
			// For each datapoint
			for (int dataIndex = 0; dataIndex < hashes.length; dataIndex++) {

				// TODO reset
				double minDistance = Double.MAX_VALUE;
				int bestCluster = -1;

				for (int clusterIndex = 0; clusterIndex < clusterMeans.length; clusterIndex++) {

//					double distToCluster = clusterMeans[clusterIndex].weightedDistance(hashes[dataIndex]);

					double distToCluster = clusterMeans[clusterIndex].normalizedHammingDistanceFast(hashes[dataIndex]);

					if (distToCluster < minDistance) {
						bestCluster = clusterIndex;
						minDistance = distToCluster;
					}
				}

				if (cluster[dataIndex] != bestCluster) {
					cluster[dataIndex] = bestCluster;
					dirty = true;
				}
			}

			if (dirty) {
				// recompute cluster means

				// Reset
//				ArrayUtil.fillArrayMulti(clusterMeans, () -> {
//					return new FuzzyHash(hashResolution);
//				});

				Hash[] clones = new Hash[clusterMeans.length];
				int i = 0;
				for (FuzzyHash fuzzy : clusterMeans) {
					fuzzy.reset();
					clones[i++] = new Hash(fuzzy.getHashValue(), fuzzy.getBitResolution(), fuzzy.getAlgorithmId());
				}

				for (int dataIndex = 0; dataIndex < hashes.length; dataIndex++) {
					int clusterIndex = cluster[dataIndex];
					clusterMeans[clusterIndex].mergeFast(hashes[dataIndex]);
				}

				for (int j = 0; j < clones.length; j++) {
					clusterMeans[j].subtractFast(clones[j]);
				}
			}

			if (iter++ > maxIter) {
				break;
			}
//			System.out.println("iter: " + iter++ + " " + totalError);
		} while (dirty);
	}
}
