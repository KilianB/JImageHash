package com.github.kilianB.dataStrorage.tree;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.PriorityQueue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.github.kilianB.TestResources;
import com.github.kilianB.datastructures.tree.Result;
import com.github.kilianB.datastructures.tree.binaryTree.BinaryTree;
import com.github.kilianB.hash.Hash;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class BinaryTreeTest {

	private BinaryTree binTree;

	@BeforeEach
	public void createTree() {
		binTree = new BinaryTree(true);
	}

	@Test
	public void searchExactItem() {
		Hash hash = TestResources.createHash("101010100011", 0);

		binTree.addHash(hash, 1);
		PriorityQueue<Result> results = binTree.getElementsWithinHammingDistance(hash, 100);

		Result r = results.peek();
		assertEquals(1, r.value);
		assertEquals(0, r.distance);
	}

	@Test
	public void searchExactItemZero() {
		// Doesn't fail
		String bits = "00001010";
		Hash hash = TestResources.createHash(bits, 0);
		binTree.addHash(hash, 0);
		PriorityQueue<Result> results = binTree.getElementsWithinHammingDistance(hash, 0);
		assertEquals(1, results.size());
	}

	@Test
	public void searchDistantItemZero() {
		// Doesn't fail
		String bits = "00001010";
		String bits1 = "10001010";
		Hash hash = TestResources.createHash(bits, 0);
		Hash hash1 = TestResources.createHash(bits1, 0);

		binTree.addHash(hash, 0);
		PriorityQueue<Result> results = binTree.getElementsWithinHammingDistance(hash1, 1);
		assertEquals(1, results.size());
	}

	@Test
	public void searchDistantItem() {
		Hash hash = TestResources.createHash("101010100011", 0);
		Hash needle = TestResources.createHash("101010101111", 0);

		binTree.addHash(hash, 1);
		PriorityQueue<Result> results = binTree.getElementsWithinHammingDistance(needle, 100);

		Result r = results.peek();
		assertEquals(1, r.value);
		assertEquals(2, r.distance);
	}

	@Test
	public void searchDistantItemFail() {
		Hash hash = TestResources.createHash("101010100011", 0);
		Hash needle = TestResources.createHash("101010101111", 0);

		binTree.addHash(hash, 1);
		PriorityQueue<Result> results = binTree.getElementsWithinHammingDistance(needle, 1);

		assertEquals(0, results.size());
	}

	@Test
	public void searchDistanceExact() {
		Hash hash = TestResources.createHash("101010100011", 0);
		Hash needle = TestResources.createHash("101010101111", 0);

		binTree.addHash(hash, 1);
		PriorityQueue<Result> results = binTree.getElementsWithinHammingDistance(needle, 2);

		assertEquals(1, results.size());
	}

	@Test
	public void searchItemMultipleValues() {
		Hash hash = TestResources.createHash("101010100011", 0);

		binTree.addHash(hash, 1);
		binTree.addHash(hash, 2);
		binTree.addHash(hash, 3);

		PriorityQueue<Result> results = binTree.getElementsWithinHammingDistance(hash, 0);

		assertEquals(3, results.size());
	}

	@Test
	public void searchItemMultipleValuesExact() {
		Hash hash = TestResources.createHash("101010100011", 0);
		Hash hash1 = TestResources.createHash("101010100010", 0);

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
	public void searchItemMultipleValues2() {
		Hash hash = TestResources.createHash("101010100011", 0);
		Hash hash1 = TestResources.createHash("101010100010", 0);

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
	public void ensureHashConsistencyAdd() {

		Hash hash = TestResources.createHash("101010100011", 250);
		Hash hash1 = TestResources.createHash("101010100010", 251);

		binTree.addHash(hash, 0);
		assertThrows(IllegalStateException.class, () -> {
			binTree.addHash(hash1, 0);
		});
	}

	@Test
	public void ensureHashConsistencySearch() {
		Hash hash = TestResources.createHash("101010100011", 250);
		Hash hash1 = TestResources.createHash("101010100010", 251);

		binTree.addHash(hash, 0);
		assertThrows(IllegalStateException.class, () -> {
			binTree.getElementsWithinHammingDistance(hash1, 10);
		});
	}

	@Test
	public void addedHashCount() {
		assertEquals(0, binTree.getHashCount());

		Hash hash = TestResources.createHash("101010100011", 250);
		Hash hash1 = TestResources.createHash("101010100010", 250);

		binTree.addHash(hash, 0);
		assertEquals(1, binTree.getHashCount());
		binTree.addHash(hash1, 0);
		assertEquals(2, binTree.getHashCount());

		// Adding the same hash will increase the hash count
		binTree.addHash(hash, 0);
		assertEquals(3, binTree.getHashCount());
	}

	@Nested
	class NearestNeightbour {

		@Test
		public void searchItemExact() {
			Hash hash = TestResources.createHash("101010100011", 0);
			binTree.addHash(hash, 1);
			List<Result> r = binTree.getNearestNeighbour(hash);
			assertEquals(1, r.get(0).value);
			assertEquals(0, r.get(0).distance);
		}

		@Test
		public void searchDistantItem() {
			Hash hash = TestResources.createHash("101010100011", 0);
			Hash needle = TestResources.createHash("101010101111", 0);

			binTree.addHash(hash, 1);

			List<Result> results = binTree.getNearestNeighbour(needle);

			assertEquals(1, results.size());

			Result r = results.get(0);
			assertEquals(1, r.value);
			assertEquals(2, r.distance);
		}

		@Test
		public void nearestItem() {
			Hash needle = TestResources.createHash("00001", 0);

			binTree.addHash(TestResources.createHash("10000", 0), 0);
			binTree.addHash(TestResources.createHash("11111", 0), 1);

			List<Result> results = binTree.getNearestNeighbour(needle);

			Result r = results.get(0);
			assertEquals(0, r.value);
			assertEquals(2, r.distance);
		}

		@Test
		public void multipleValues() {
			Hash needle = TestResources.createHash("00001", 0);

			binTree.addHash(TestResources.createHash("10000", 0), 0);
			binTree.addHash(TestResources.createHash("10000", 0), 2);
			binTree.addHash(TestResources.createHash("11111", 0), 1);

			List<Result> results = binTree.getNearestNeighbour(needle);

			assertEquals(2, results.size());

			Result r = results.get(0);
			Result r2 = results.get(0);

			assertTrue(((int) r.value == 0) || (int) r.value == 2);
			assertTrue(((int) r2.value == 0 || (int) r2.value == 2));
		}

		
		@Test
		public void equidistant() {
			Hash needle = TestResources.createHash("00001", 0);

			binTree.addHash(TestResources.createHash("00010", 0), 0);
			binTree.addHash(TestResources.createHash("00100", 0), 2);
			binTree.addHash(TestResources.createHash("11111", 0), 1);

			List<Result> results = binTree.getNearestNeighbour(needle);

			assertEquals(2, results.size());

			Result r = results.get(0);
			Result r2 = results.get(0);

			assertTrue(((int) r.value == 0) || (int) r.value == 2);
			assertTrue(((int) r2.value == 0 || (int) r2.value == 2));
		}
	}
}
