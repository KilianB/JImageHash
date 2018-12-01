package com.github.kilianB.dataStrorage.tree;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigInteger;
import java.util.PriorityQueue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.github.kilianB.matcher.Hash;

@SuppressWarnings({"rawtypes","unchecked"})
class BinaryTreeTest {

	
	private BinaryTree binTree;

	@BeforeEach
	void createTree() {
		binTree = new BinaryTree(true);
	}

	@Test
	void searchExactItem() {
		Hash hash = createHash("101010100011",0);

		binTree.addHash(hash, 1);
		PriorityQueue<Result> results = binTree.getElementsWithinHammingDistance(hash, 100);

		Result r = results.peek();
		assertEquals(1, r.value);
		assertEquals(0, r.distance);
	}
	
	@Test
	void searchExactItemZero() {
		//Doesn't fail
		String bits  = "00001010";
		Hash hash = createHash(bits,0);
		binTree.addHash(hash,0);
		PriorityQueue<Result> results = binTree.getElementsWithinHammingDistance(hash,0);
		assertEquals(1,results.size());
	}
	
	@Test
	void searchDistantItemZero() {
		//Doesn't fail
		String bits   = "00001010";
		String bits1  = "10001010";
		Hash hash = createHash(bits,0);
		Hash hash1 = createHash(bits1,0);
		
		binTree.addHash(hash,0);
		PriorityQueue<Result> results = binTree.getElementsWithinHammingDistance(hash1,1);
		assertEquals(1,results.size());
	}
	

	@Test
	void searchDistantItem() {
		Hash hash = createHash("101010100011", 0);
		Hash needle = createHash("101010101111", 0);

		binTree.addHash(hash, 1);
		PriorityQueue<Result> results = binTree.getElementsWithinHammingDistance(needle, 100);

		Result r = results.peek();
		assertEquals(1, r.value);
		assertEquals(2, r.distance);
	}

	@Test
	void searchDistantItemFail() {
		Hash hash = createHash("101010100011", 0);
		Hash needle = createHash("101010101111", 0);

		binTree.addHash(hash, 1);
		PriorityQueue<Result> results = binTree.getElementsWithinHammingDistance(needle, 1);

		assertEquals(0, results.size());
	}

	@Test
	void searchDistanceExact() {
		Hash hash = createHash("101010100011", 0);
		Hash needle = createHash("101010101111", 0);

		binTree.addHash(hash, 1);
		PriorityQueue<Result> results = binTree.getElementsWithinHammingDistance(needle, 2);

		assertEquals(1, results.size());
	}

	@Test
	void searchItemMultipleValues() {
		Hash hash = createHash("101010100011",0);

		binTree.addHash(hash, 1);
		binTree.addHash(hash, 2);
		binTree.addHash(hash, 3);

		PriorityQueue<Result> results = binTree.getElementsWithinHammingDistance(hash, 0);

		assertEquals(3, results.size());
	}

	@Test
	void searchItemMultipleValuesExact() {
		Hash hash = createHash("101010100011", 0);
		Hash hash1 = createHash("101010100010", 0);

		binTree.addHash(hash, 1);
		binTree.addHash(hash, 2);
		binTree.addHash(hash, 3);
		binTree.addHash(hash1, 3);
		binTree.addHash(hash1, 3);
		binTree.addHash(hash1, 3);

		PriorityQueue<Result> results = binTree.getElementsWithinHammingDistance(hash, 0);

		assertEquals(3, results.size());
	}

	@Test
	void searchItemMultipleValues2() {
		Hash hash = createHash("101010100011", 0);
		Hash hash1 = createHash("101010100010", 0);

		binTree.addHash(hash, 1);
		binTree.addHash(hash, 2);
		binTree.addHash(hash, 3);
		binTree.addHash(hash1, 3);
		binTree.addHash(hash1, 3);
		binTree.addHash(hash1, 3);

		PriorityQueue<Result> results = binTree.getElementsWithinHammingDistance(hash, 2);

		assertEquals(6, results.size());
	}

	@Test
	void ensureHashConsistencyAdd() {

		Hash hash = createHash("101010100011", 250);
		Hash hash1 = createHash("101010100010", 251);

		binTree.addHash(hash, 0);
		assertThrows(IllegalStateException.class, () -> {
			binTree.addHash(hash1, 0);
		});
	}

	@Test
	void ensureHashConsistencySearch() {
		Hash hash = createHash("101010100011", 250);
		Hash hash1 = createHash("101010100010", 251);

		binTree.addHash(hash, 0);
		assertThrows(IllegalStateException.class, () -> {
			binTree.getElementsWithinHammingDistance(hash1, 10);
		});
	}

	
	@Test 
	void addedHashCount() {
		assertEquals(0,binTree.getHashCount());
		
		Hash hash = createHash("101010100011", 250);
		Hash hash1 = createHash("101010100010", 250);

		binTree.addHash(hash, 0);
		assertEquals(1,binTree.getHashCount());
		binTree.addHash(hash1, 0);
		assertEquals(2,binTree.getHashCount());
		
		//Adding the same hash will increase the hash count
		binTree.addHash(hash, 0);
		assertEquals(3,binTree.getHashCount());
	}	
	
	/**
	 * Create a dummy hash 
	 * @param bits
	 * @param algoId
	 * @return
	 */
	private static Hash createHash(String bits, int algoId) {
		return new Hash(new BigInteger(bits,2),bits.length(),algoId);
	}
}
