package com.github.kilianB.datastructures.tree;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * @author Kilian
 *
 */
class NodeInfoTest {

	
	@Nested
	@SuppressWarnings("rawtypes")
	class Sorting{
	
		@Test
		public void distinctDepth() {
			
			NodeInfo<Void> nInfo =  new NodeInfo<>(null,1,3);
			NodeInfo<Void> nInfo2 = new NodeInfo<>(null,1,2);
			
			NodeInfo[] n = {nInfo,nInfo2};
			Arrays.sort(n);
			assertEquals(nInfo2,n[0]);
			assertEquals(nInfo,n[1]);
		}
		
		@Test
		public void identical() {
			
			NodeInfo<Void> nInfo =  new NodeInfo<>(null,1,3);
			NodeInfo<Void> nInfo2 = new NodeInfo<>(null,1,3);
			NodeInfo[] n = {nInfo,nInfo2};
			Arrays.sort(n);
			assertEquals(nInfo,n[0]);
			assertEquals(nInfo2,n[1]);
		}
		
		@Test
		public void distinctDistanceSameDepth() {
			
			NodeInfo<Void> nInfo =  new NodeInfo<>(null,2,3);
			NodeInfo<Void> nInfo2 = new NodeInfo<>(null,1,3);
			NodeInfo[] n = {nInfo,nInfo2};
			Arrays.sort(n);
			assertEquals(nInfo2,n[0]);
			assertEquals(nInfo,n[1]);
		}
	}
	
	
}
