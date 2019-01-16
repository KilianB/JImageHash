package com.github.kilianB.datastructures;

import static com.github.kilianB.TestResources.createHash;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.github.kilianB.hash.Hash;

/**
 * @author Kilian
 *
 */
class KMeansTest {


	@Test
	void testMatchingDataSizeAndClusterCount() {
		KMeans clusterer = new KMeans(2);
		ClusterResult clusterResult = clusterer.cluster(new Hash[] {createHash("0",0),createHash("1",0)});
		//-1 for the noise cluster
		assertEquals(2,clusterResult.getClusters().keySet().size()-1);
	}
	
	@Test
	void testSmallerDataSizeAndClusterCount() {
		KMeans clusterer = new KMeans(1);
		ClusterResult clusterResult = clusterer.cluster(new Hash[] {createHash("0",0),createHash("1",0)});
		//-1 for the noise cluster
		assertEquals(1,clusterResult.getClusters().keySet().size()-1);
	}
	
	@Test
	void testLargerDataSizeAndClusterCount() {
		KMeans clusterer = new KMeans(4);
		ClusterResult clusterResult = clusterer.cluster(new Hash[] {createHash("0",0),createHash("1",0)});
		//-1 for the noise cluster
		assertEquals(2,clusterResult.getClusters().keySet().size()-1);
	}
	

}
