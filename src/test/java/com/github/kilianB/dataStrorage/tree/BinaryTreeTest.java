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
		Hash hash = new Hash(new BigInteger("101010100011", 2), 0);

		binTree.addHash(hash, 1);
		PriorityQueue<Result> results = binTree.getElementsWithinHemmingDistance(hash, 100);

		Result r = results.peek();
		assertEquals(1, r.value);
		assertEquals(0, r.distance);
	}

	@Test
	void searchDistantItem() {
		Hash hash = new Hash(new BigInteger("101010100011", 2), 0);
		Hash needle = new Hash(new BigInteger("101010101111", 2), 0);

		binTree.addHash(hash, 1);
		PriorityQueue<Result> results = binTree.getElementsWithinHemmingDistance(needle, 100);

		Result r = results.peek();
		assertEquals(1, r.value);
		assertEquals(2, r.distance);
	}

	@Test
	void searchDistantItemFail() {
		Hash hash = new Hash(new BigInteger("101010100011", 2), 0);
		Hash needle = new Hash(new BigInteger("101010101111", 2), 0);

		binTree.addHash(hash, 1);
		PriorityQueue<Result> results = binTree.getElementsWithinHemmingDistance(needle, 1);

		assertEquals(0, results.size());
	}

	@Test
	void searchDistanceExact() {
		Hash hash = new Hash(new BigInteger("101010100011", 2), 0);
		Hash needle = new Hash(new BigInteger("101010101111", 2), 0);

		binTree.addHash(hash, 1);
		PriorityQueue<Result> results = binTree.getElementsWithinHemmingDistance(needle, 2);

		assertEquals(1, results.size());
	}

	@Test
	void searchItemMultipleValues() {
		Hash hash = new Hash(new BigInteger("101010100011", 2), 0);

		binTree.addHash(hash, 1);
		binTree.addHash(hash, 2);
		binTree.addHash(hash, 3);

		PriorityQueue<Result> results = binTree.getElementsWithinHemmingDistance(hash, 0);

		assertEquals(3, results.size());
	}

	@Test
	void searchItemMultipleValuesExact() {
		Hash hash = new Hash(new BigInteger("101010100011", 2), 0);
		Hash hash1 = new Hash(new BigInteger("101010100010", 2), 0);

		binTree.addHash(hash, 1);
		binTree.addHash(hash, 2);
		binTree.addHash(hash, 3);
		binTree.addHash(hash1, 3);
		binTree.addHash(hash1, 3);
		binTree.addHash(hash1, 3);

		PriorityQueue<Result> results = binTree.getElementsWithinHemmingDistance(hash, 0);

		assertEquals(3, results.size());
	}

	@Test
	void searchItemMultipleValues2() {
		Hash hash = new Hash(new BigInteger("101010100011", 2), 0);
		Hash hash1 = new Hash(new BigInteger("101010100010", 2), 0);

		binTree.addHash(hash, 1);
		binTree.addHash(hash, 2);
		binTree.addHash(hash, 3);
		binTree.addHash(hash1, 3);
		binTree.addHash(hash1, 3);
		binTree.addHash(hash1, 3);

		PriorityQueue<Result> results = binTree.getElementsWithinHemmingDistance(hash, 2);

		assertEquals(6, results.size());
	}

	@Test
	void ensureHashConsistencyAdd() {

		Hash hash = new Hash(new BigInteger("101010100011", 2), 250);
		Hash hash1 = new Hash(new BigInteger("101010100010", 2), 251);

		binTree.addHash(hash, 0);
		assertThrows(IllegalStateException.class, () -> {
			binTree.addHash(hash1, 0);
		});
	}

	@Test
	void ensureHashConsistencySearch() {
		Hash hash = new Hash(new BigInteger("101010100011", 2), 250);
		Hash hash1 = new Hash(new BigInteger("101010100010", 2), 251);

		binTree.addHash(hash, 0);
		assertThrows(IllegalStateException.class, () -> {
			binTree.getElementsWithinHemmingDistance(hash1, 10);
		});
	}

	
	@Test 
	void addedHashCount() {
		assertEquals(0,binTree.getHashCount());
		
		Hash hash = new Hash(new BigInteger("101010100011", 2), 250);
		Hash hash1 = new Hash(new BigInteger("101010100010", 2), 250);

		binTree.addHash(hash, 0);
		assertEquals(1,binTree.getHashCount());
		binTree.addHash(hash1, 0);
		assertEquals(2,binTree.getHashCount());
		
		//Adding the same hash will increase the hash count
		binTree.addHash(hash, 0);
		assertEquals(3,binTree.getHashCount());
	}
}
