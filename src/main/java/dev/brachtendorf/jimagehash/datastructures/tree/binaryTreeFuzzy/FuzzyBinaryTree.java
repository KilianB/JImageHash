package dev.brachtendorf.jimagehash.datastructures.tree.binaryTreeFuzzy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.PriorityQueue;

import dev.brachtendorf.MathUtil;
import dev.brachtendorf.jimagehash.datastructures.tree.AbstractBinaryTree;
import dev.brachtendorf.jimagehash.datastructures.tree.NodeInfo;
import dev.brachtendorf.jimagehash.datastructures.tree.Result;
import dev.brachtendorf.jimagehash.datastructures.tree.binaryTree.Leaf;
import dev.brachtendorf.jimagehash.datastructures.tree.binaryTree.Node;
import dev.brachtendorf.jimagehash.hash.FuzzyHash;
import dev.brachtendorf.jimagehash.hash.Hash;


/**
 * A binary tree implementation allowing to quickly lookup
 * 
 * be aware that fuzzy trees currently are of one time use and are rendered
 * invalid as soon as one of the underlying hashes changes it's internal
 * state.!
 * 
 * TODO for mutability add a list of uncertainties to each node and allow remove
 * operation by supplying the old and the new fuzzy hash state
 * 
 * @author Kilian
 * @since 3.0.0
 */
public class FuzzyBinaryTree extends AbstractBinaryTree<FuzzyHash> {

	private static final long serialVersionUID = -246416483525585695L;
	// TODO debug
	private int hashLengthDebug = -1;;

	public FuzzyBinaryTree(boolean ensureHashConsistency) {
		super(ensureHashConsistency);
		root = new FuzzyNode();
	}
	

	public void addHash(FuzzyHash hash) {
		addHash(hash, hash);
	}
	
	public void addHashes(FuzzyHash...fuzzyHashs) {
		for(FuzzyHash h: fuzzyHashs) {
			this.addHash(h);
		}
	}
	
	public void addHashes(Collection<FuzzyHash> fuzzyHashs) {
		for(FuzzyHash h: fuzzyHashs) {
			this.addHash(h);
		}
	}

	@SuppressWarnings("unchecked")
	protected void addHash(Hash hash, FuzzyHash value) {

		// value and hash are the same

		if (ensureHashConsistency) {
			if (algoId == 0) {
				algoId = hash.getAlgorithmId();
			} else {

				if (algoId != hash.getAlgorithmId())
					throw new IllegalStateException("Tried to add an incompatible hash to the binary tree");
			}
		}

		if (hashLengthDebug < 0) {
			hashLengthDebug = hash.getBitResolution();
		}

		FuzzyNode currentNode = (FuzzyNode) root;
		for (int i = hash.getBitResolution() - 1; i > 0; i--) {
			boolean bit = hash.getBitUnsafe(i);
			FuzzyNode tempNode = (FuzzyNode) currentNode.getChild(bit);
			if (tempNode == null) {
				currentNode = (FuzzyNode) currentNode.createChild(bit);
			} else {
				currentNode = tempNode;
			}
			// update uncertainty with value [0-1] Since we sum up the distance on each node
			// we are working with the unnormalized hamming distances
			currentNode.setNodeBounds(value.getWeightedDistance(i, bit));
		}

		// We reached the end
		boolean bit = hash.getBit(0);
		Node leafNode = currentNode.getChild(bit);
		Leaf<FuzzyHash> leaf;
		if (leafNode != null) {
			leaf = (Leaf<FuzzyHash>) leafNode;
		} else {
			leaf = (Leaf<FuzzyHash>) currentNode.setChild(bit, new Leaf<FuzzyHash>());
		}
		leaf.addData(value);
		hashCount++;
	}

	// TODO check if distance is correct

	public List<Result<FuzzyHash>> getNearestNeighbour(Hash hash) {

		if (ensureHashConsistency && algoId != hash.getAlgorithmId()) {
			throw new IllegalStateException("Tried to add an incompatible hash to the binary tree");
		}

		int treeDepth = hash.getBitResolution();

		if (hashLengthDebug != treeDepth) {
			throw new IllegalStateException("Tried to get neareast neighbor an incompatible hash to the binary tree");
		}

		PriorityQueue<NodeInfo<FuzzyHash>> queue = new PriorityQueue<>();

		// Potential results
		List<Result<FuzzyHash>> resultCandidates = new ArrayList<>();

		double curBestDistance = Double.MAX_VALUE;

		// Depth first search with aggressive pruning

		// Begin search at the root
		queue.add(new NodeInfo<FuzzyHash>(root, 0, treeDepth));

		while (!queue.isEmpty()) {

			NodeInfo<FuzzyHash> info = queue.poll();

			// If we found a better result ignore it.

			// This should scale down with distance
			if (info.distance > curBestDistance) {
				continue;
			}

			// We reached a leaf
			if (info.depth == 0) {

				@SuppressWarnings("unchecked")
				Leaf<FuzzyHash> leaf = (Leaf<FuzzyHash>) info.node;
				for (FuzzyHash o : leaf.getData()) {

					double normalizedDistance = o.weightedDistance(hash);
					double actualDistance = normalizedDistance * hash.getBitResolution();

					if (curBestDistance > actualDistance) {
						resultCandidates.clear();
						curBestDistance = actualDistance;
						// Compute the correct distance
						resultCandidates.add(new Result<FuzzyHash>(o, actualDistance, normalizedDistance));
					} else if (MathUtil.isDoubleEquals(curBestDistance, actualDistance, 1e-8)) {
						resultCandidates.add(new Result<FuzzyHash>(o, actualDistance, normalizedDistance));
					}
				}
				continue;
			}

			// Next bit

			boolean bit = hash.getBitUnsafe(info.depth - 1);

			// Children of the next level
			for (int i = 0; i < 2; i++) {
				boolean left = i == 0;

				if (info.depth != 1) {
					FuzzyNode node = (FuzzyNode) info.node.getChild(left);
					if (node != null) {

						double newDistance;
						if (bit == left) {
							newDistance = info.distance + node.lowerDistance;
						} else {
							newDistance = info.distance + (1 - node.uppderDistance);
						}
						if (newDistance <= curBestDistance) {
							queue.add(new NodeInfo<>(node, newDistance, info.depth - 1));
						}
					}
				} else {
					try {
						Leaf<?> node = (Leaf<?>) info.node.getChild(left);
						if (node != null) {
							// TODO add distance?
							queue.add(new NodeInfo<>(node, info.distance, info.depth - 1));
						}
					} catch (java.lang.ClassCastException e) {
						e.printStackTrace();
						printTree();
						System.out.println(info);
						System.out.println(hashLengthDebug + " " + treeDepth);
						throw e;
					}

				}

			}

		}
		return resultCandidates;
	}

	@Override
	public PriorityQueue<Result<FuzzyHash>> getElementsWithinHammingDistance(Hash hash, int maxDistance) {
		// TODO Auto-generated method stub
		return null;
	}

}
