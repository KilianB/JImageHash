package dev.brachtendorf.jimagehash.datastructures;

import java.util.Random;

import dev.brachtendorf.ArrayUtil;
import dev.brachtendorf.jimagehash.hash.FuzzyHash;
import dev.brachtendorf.jimagehash.hash.Hash;

import com.github.kilianB.pcg.fast.PcgRSFast;

/**
 * Kmeans plus plus implementation. Opposed to Kmeans this algorithm
 * strategically chooses it's starting clusters to decrease iteration time at the
 * later stage
 * 
 * @author Kilian
 *
 */
public class KMeansPlusPlus extends KMeans {

	public KMeansPlusPlus(int clusters) {
		super(clusters);
	}

	@Override
	protected FuzzyHash[] computeStartingClusters(Hash[] hashes) {

		// Fast high quality rng
		Random rng = new PcgRSFast();

		FuzzyHash[] clusterMeans = new FuzzyHash[k];

		ArrayUtil.fillArrayMulti(clusterMeans, () -> {
			return new FuzzyHash();
		});

		// Randomly choose a starting point. Initial vector
		clusterMeans[0].mergeFast(hashes[rng.nextInt(hashes.length)]);

		for (int cluster = 1; cluster < k; cluster++) {

			// Choose a random cluster center with probability equal to the squared distance
			// of the closest existing center
			double[] distance = new double[hashes.length];
			ArrayUtil.fillArray(distance, () -> {
				return Double.MAX_VALUE;
			});

			double sum = 0;

			// For each point
			for (int i = 0; i < hashes.length; i++) {

				// find the minimum distance to all already existing clusters
				for (int j = 0; j < cluster; j++) {
					double distTemp = clusterMeans[j].normalizedHammingDistanceFast(hashes[i]);
					distTemp *= distTemp;
					if (distTemp < distance[i]) {
						distance[i] = distTemp;
					}
				}
				sum += distance[i];
			}
			int index = 0;
			double rand = rng.nextDouble() * sum;
			double runningSum = distance[0];
			for (; index < hashes.length; index++) {
				if (rand <= runningSum) {
					break;
				}
				runningSum += distance[index + 1];
			}
			clusterMeans[cluster].mergeFast(hashes[index]);
		}
		return clusterMeans;
	}
}
