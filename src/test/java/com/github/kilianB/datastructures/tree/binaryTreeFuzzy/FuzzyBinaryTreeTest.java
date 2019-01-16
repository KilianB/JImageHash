package com.github.kilianB.datastructures.tree.binaryTreeFuzzy;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import com.github.kilianB.TestResources;
import com.github.kilianB.hash.FuzzyHash;
import com.github.kilianB.hash.Hash;
/**
 * @author Kilian
 *
 */
class FuzzyBinaryTreeTest {

	
	@Test
	@Disabled
	void testSimple() {
		FuzzyBinaryTree fuzzyTree = new FuzzyBinaryTree(true);
		FuzzyHash fuzzy = new FuzzyHash(TestResources.createHash("0101010111011",0));
		fuzzyTree.addHash(fuzzy);
		assertEquals(fuzzy,fuzzyTree.getNearestNeighbour(fuzzy).get(0).value);
	}
	
	@Disabled
	@Test
	void testDepth0() {
		FuzzyBinaryTree fuzzyTree = new FuzzyBinaryTree(true);
		FuzzyHash fuzzy = new FuzzyHash(TestResources.createHash("0",0),TestResources.createHash("1",0),TestResources.createHash("1",0));
		FuzzyHash fuzzy1 = new FuzzyHash(TestResources.createHash("1",0),TestResources.createHash("1",0),TestResources.createHash("1",0));
		
		fuzzyTree.addHash(fuzzy);
		fuzzyTree.addHash(fuzzy1);
		
		assertEquals(fuzzy1,fuzzyTree.getNearestNeighbour(TestResources.createHash("1",0)).get(0).value);
	}
	
	
	@Test
	void testRuntime() {
		
		FuzzyBinaryTree fuzzyTree = new FuzzyBinaryTree(true);
//		FuzzyHash fuzzy = new FuzzyHash(createHash("10000000000001",0));
//		FuzzyHash fuzzy1 = new FuzzyHash(createHash("11111111100001",0));
//		
		FuzzyHash fuzzy = new FuzzyHash(TestResources.createHash("10000100",0));
		fuzzy.merge(TestResources.createHash("11100100",0));
		fuzzy.merge(TestResources.createHash("10101100",0));
		
		FuzzyHash fuzzy1 = new FuzzyHash();
		fuzzy1.merge(TestResources.createHash("11100100",0));
		fuzzy1.merge(TestResources.createHash("00101100",0));
		fuzzy1.merge(TestResources.createHash("00101100",0));

		
		fuzzyTree.addHash(fuzzy);
		fuzzyTree.addHash(fuzzy1);
	
		System.out.println(fuzzyTree.getNearestNeighbour(fuzzy));
	}
	
	
	@Test
	@Disabled
	void test() {
		
		FuzzyBinaryTree fuzzyTree = new FuzzyBinaryTree(true);
		Hash h =  TestResources.createHash("0101010111011",0);
		Hash h1 = TestResources.createHash("0101010111111",0);
		FuzzyHash fuzzy = new FuzzyHash();
		fuzzy.merge(h);
		fuzzy.merge(h1);
		fuzzyTree.addHash(fuzzy);
		
		System.out.println(fuzzyTree.getNearestNeighbour(fuzzy));
	}
	
	
}
