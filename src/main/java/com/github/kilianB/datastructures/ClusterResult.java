package com.github.kilianB.datastructures;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.DoubleSummaryStatistics;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import com.github.kilianB.ArrayUtil;
import com.github.kilianB.StringUtil;
import com.github.kilianB.matcher.FuzzyHash;
import com.github.kilianB.matcher.Hash;
import com.github.kilianB.mutable.MutableDouble;

public class ClusterResult {

	protected int numberOfClusters;

	/** Keep track to which cluster a certain points belongs */
	protected int clusterIndex[];

	/** Compute the min max average and mean of each cluster */
	protected HashMap<Integer, DoubleSummaryStatistics> stats = new HashMap<>();

	// Int array as reverse map
	protected HashMap<Hash, Integer> resolveEntryToCluster = new HashMap<>();

	/** Key the cluster index, value the data index */
	protected HashMap<Integer, List<Integer>> entriesInCluster = new HashMap<>();

	// For each cluster we have a list with features in n dimension
	protected HashMap<Integer, FuzzyHash> clusters = new HashMap<>();
	// clusters.get(clusterIndex).get(dataPoint)[dataDimension]

	Map<Integer, List<Hash>> hashesByCluster = new HashMap<>();

	// Cluster metrics
	private double sseSum;
	private HashMap<Integer, MutableDouble> sse = new HashMap<>();
	private HashMap<Integer, MutableDouble> silhouetteCoef = new HashMap<>();

	// Cohesion ...

	// Radius ... diameter
	// density volume/points

	public ClusterResult(int[] clusterIndex, Hash[] hashes) {

		int hashLength = hashes[0].getBitResolution();

		this.clusterIndex = clusterIndex;

		// How many clusters do we work with
		numberOfClusters = ArrayUtil.maximum(clusterIndex) + 1;

		// Prepare datastructures

		// -1 for noise
		for (int cluster = -1; cluster < numberOfClusters; cluster++) {
			clusters.put(cluster, new FuzzyHash());
			stats.put(cluster, new DoubleSummaryStatistics());
			sse.put(cluster, new MutableDouble(0));
			silhouetteCoef.put(cluster, new MutableDouble(0));
			hashesByCluster.put(cluster, new ArrayList<>());
			entriesInCluster.put(cluster, new ArrayList<>());
		}

		// After constructing the mean
		for (int i = 0; i < hashes.length; i++) {
			int cluster = clusterIndex[i];
			clusters.get(cluster).mergeFast(hashes[i]);
			hashesByCluster.get(cluster).add(hashes[i]);
			resolveEntryToCluster.put(hashes[i], i);
			entriesInCluster.get(cluster).add(i);
		}

		// Calculate the distance
		for (int i = 0; i < hashes.length; i++) {
			int cluster = clusterIndex[i];
			double distance = clusters.get(cluster).weightedDistance(hashes[i]);
			stats.get(cluster).accept(distance);

			// Summed squared error
			MutableDouble m = sse.get(cluster);
			m.setValue(m.getValue() + distance * distance);

			// Silhouette Coefficient

			// 0. For each point calculate the distance to all other points in the same
			// cluster
			List<Hash> sameCluster = hashesByCluster.get(cluster);

			// -1 don't count itself
			int pointsInCluster = sameCluster.size() - 1;

		}

//		// Compute metrics
//
//		DistanceFunction eucD = new EuclideanDistance();
//
//		// For each datapoint
//		for (int i = 0; i < data.length; i++) {
//
//			// Silhouette Coefficient
//
//			// 0. For each point calculate the distance to all other points in the same
//			// cluster
//
//			List<double[]> sameCluster = clusters.get(cluster);
//
//			// -1 don't count itself
//			int pointsInCluster = sameCluster.size() - 1;
//
//			double avgDistSameCluster = 0;
//			for (double[] p : sameCluster) {
//				avgDistSameCluster += (eucD.distance(data[i], p) / pointsInCluster);
//			}
//
//			double minAvgDistanceOtherCluster = Double.MAX_VALUE;
//
//			for (int j = 0; j < numberOfClusters; j++) {
//				if (j != cluster) {
//					double avgDistanceOtherCluster = 0;
//					List<double[]> otherCluster = clusters.get(j);
//					pointsInCluster = otherCluster.size();
//
//					for (double[] p : otherCluster) {
//						avgDistanceOtherCluster += (eucD.distance(data[i], p) / pointsInCluster);
//					}
//					if (avgDistanceOtherCluster < minAvgDistanceOtherCluster) {
//						minAvgDistanceOtherCluster = avgDistanceOtherCluster;
//					}
//				}
//			}
//
//			double silhoutteCoefficient;
//
//			if (avgDistSameCluster < minAvgDistanceOtherCluster) {
//				silhoutteCoefficient = 1 - (avgDistSameCluster / minAvgDistanceOtherCluster);
//			} else {
//				silhoutteCoefficient = (minAvgDistanceOtherCluster / avgDistSameCluster) - 1;
//			}
//
//			// System.out.println(avgDistSameCluster<minAvgDistanceOtherCluster);
//
//			MutableDouble sil = silhouetteCoef.get(cluster);
//			sil.setValue(sil.getValue() + silhoutteCoefficient / sameCluster.size());
//
//		}

		for (int i = 0; i < numberOfClusters; i++) {
			sseSum += sse.get(i).doubleValue();
		}

	}

	// Cohesian /Area of the cluster.

	public void printInformation() {

		StringBuilder sb = new StringBuilder();
		sb.append("Observations: ").append(clusterIndex.length).append("\n").append("Number of Clusters: ")
				.append(numberOfClusters).append("\n");

		int clusterLength = StringUtil.charsNeeded(numberOfClusters);
		int obsLength = StringUtil.charsNeeded(clusterIndex.length);

		String format = "%-" + clusterLength + "d (Obs:%" + obsLength + "d) |";
		int hLength = Math.max("Clusters: |".length(), clusterLength + 1 + 5 + obsLength + 2);

		// Header
		sb.append(String.format("%-" + hLength + "s", "Clusters: ")).append("| Centeroids:\n");

		double silouetteCoeffificient = 0;

		// String formatCenteroid = "%.3f";
		DecimalFormat df = new DecimalFormat(".000");
		DecimalFormat sseDf = new DecimalFormat("0.00E0");

		for (int i = 0; i < numberOfClusters; i++) {
			sb.append(String.format(format, i, hashesByCluster.get(i).size()));
			// Cluster stats;
			DoubleSummaryStatistics cStats = stats.get(i);
			sb.append(" [ ");
			sb.append(clusters.get(i));
			silouetteCoeffificient += silhouetteCoef.get(i).getValue();
			sb.append("] Silhouette Coef: ").append(df.format(silhouetteCoef.get(i).getValue())).append(" SSE:")
					.append(sseDf.format(sse.get(i).doubleValue())).append("\n");
		}

		sb.append("SSE: " + df.format(sseSum)).append("\n");
		sb.append("Silhouette Coef/#clusters: " + df.format(silouetteCoeffificient / numberOfClusters)).append("\n");

		System.out.println(sb.toString());
	}

//	public void toImage(File outputFile) {
//		BufferedImage bi = new BufferedImage(700, 700, 0x1);
//		Graphics g = bi.getGraphics();
//
//		// Find the range of the data
//
//		double minVal = Double.MAX_VALUE;
//		double maxVal = -Double.MAX_VALUE;
//
//		for (int cluster = 0; cluster < numberOfClusters; cluster++) {
//			DoubleSummaryStatistics[] clusterStats = stats.get(cluster);
//			for (int dim = 0; dim < clusterStats.length; dim++) {
//
//				if (clusterStats[dim].getMax() > maxVal) {
//					maxVal = clusterStats[dim].getMax();
//				}
//				if (clusterStats[dim].getMin() < minVal) {
//					minVal = clusterStats[dim].getMin();
//				}
//			}
//		}
//
//		javafx.scene.paint.Color[] c = ColorUtil.ColorPalette.getPaletteHue(numberOfClusters, Color.BLUE, Color.RED);
//
//		// Scale data
//		g.fillRect(0, 0, 700, 700);
//
//		double newMin = 0;
//		double newMax = 700;
//		double observedRange = maxVal - minVal;
//		double newRange = newMax - newMin;
//
//		for (int i = -1; i < numberOfClusters; i++) {
//			if (i == -1) {
//				g.setColor(ColorUtil.fxToAwtColor(Color.GRAY));
//			} else {
//				g.setColor(ColorUtil.fxToAwtColor(c[i]));
//			}
//			List<double[]> points = clusters.get(i);
//			for (double[] point : points) {
//				int x = (int) MathUtil.normalizeValue(point[0], observedRange, maxVal, newRange, newMax, true);
//				int y = (int) MathUtil.normalizeValue(point[1], observedRange, maxVal, newRange, newMax, true);
//				g.fillOval(x, y, 10, 10);
//			}
//		}
//		g.dispose();
//		try {
//			ImageIO.write(bi, "png", outputFile);
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//	}

	public Map<Integer, List<Hash>> getClusters() {
		return hashesByCluster;
	}

	public List<Hash> getCluster(int cluster) {
		return hashesByCluster.get(cluster);
	}

	public DoubleSummaryStatistics getStats(int cluster) {
		return stats.get(cluster);
	}

	public int[] getClusterData() {
		return clusterIndex;
	}

	// Metrics

	public double getSumSquaredError() {
		return sseSum;
	}

	/**
	 * Return the cluster index whose centeroid is most similar to the supplied hash
	 * 
	 * @param testHash the hash to check against the clusters
	 * @return the category (index) of the best matching cluster
	 */
	public int getBestFitCluster(Hash testHash) {

		int bestCategory = -2;
		double bestFitness = Double.MAX_VALUE;

		for (Entry<Integer, FuzzyHash> entry : clusters.entrySet()) {

			double dist = entry.getValue().weightedDistance(testHash);
			if (dist < bestFitness) {
				bestFitness = dist;
				bestCategory = entry.getKey();
			}

		}
		return bestCategory;
	}

	/**
	 * Return the cluster index for a hash which was used during the clustering.
	 * <p>
	 * This method will return the same value as {@link #getBestFitCluster(Hash)}
	 * but is quicker.
	 * 
	 * @param testHash The hash to check the cluster id for.
	 * @throws NullPointerException if the hash wasn't used during clustering.
	 * @return the index of the cluster this hash belongs to
	 */
	public int lookupClusterIdForKnownHash(Hash testHash) {
		return indexToCluster(resolveEntryToCluster.get(testHash));
	}

	public List<Integer> clusterIndexToDataIndex(int clusterIndex) {
		return entriesInCluster.get(clusterIndex);
	}

	public int indexToCluster(int index) {
		return clusterIndex[index];
	}

	/**
	 * Get the fuzzy hash representing the specified cluster
	 * 
	 * @param cluster the cluster index
	 * @return the centeroid of the cluster
	 */
	public FuzzyHash getCenteroid(int cluster) {
		return clusters.get(cluster);
	}

	// Metrics

	/**
	 * Checks which cluster has a high chance to contain the closest neighbor to
	 * this hash
	 * 
	 * @param testHash the hash to check which cluster it belongs to
	 * @param sigma    a stretch factor indicating how much error from a cluster
	 *                 center to the hash is allowed based on the range of the
	 *                 distances within the cluster. With the original dataset a
	 *                 sigma of 1 would include with a 100% certainty.
	 * 
	 * @return the cluster indices a match is most likely
	 */
	public Map<Integer, Double> getPotentialFits(Hash testHash, double sigma) {

		// This maps to the cluster id.

		Map<Integer, Double> resultValue = new HashMap<>();

		int bestCategory = -2;
		double bestFitness = Double.MAX_VALUE;

		// For every cluster midpoit
		for (Entry<Integer, FuzzyHash> entry : clusters.entrySet()) {

			// Compute the distance
			double dist = entry.getValue().weightedDistance(testHash);

			// Common error range of the cluster
			double upperBound = stats.get(entry.getKey()).getMax();

			// TODO if the cluster only contains 1 entry this would add nothing! this can't
			// be true either.
			// Maybe under a certain threshold assume the average of the top 20% quantile.
			if (dist <= upperBound * sigma) {
				// Additionally + range...
				resultValue.put(entry.getKey(), dist);
			}

			// Also keep track of the best fitness even if it does not fall into the error
			// category.
			// The best metach might often be inside here
			if (dist < bestFitness) {
				bestFitness = dist;
				bestCategory = entry.getKey();
			}
		}

		if (!resultValue.containsKey(bestCategory)) {
			resultValue.put(bestCategory, 1d);
		}

		// Int
		/// get all

		// Sort it by distance (TODO should it be distance - error?)
		resultValue = resultValue.entrySet().stream().sorted(Map.Entry.comparingByValue())
				.collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue(), (u, v) -> {
					throw new IllegalStateException(String.format("Duplicate key %s", u));
				}, LinkedHashMap::new));

//		System.out.println("Values: " + resultValue);

		return resultValue;
	}
}