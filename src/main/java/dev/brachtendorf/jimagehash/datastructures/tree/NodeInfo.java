package dev.brachtendorf.jimagehash.datastructures.tree;

import dev.brachtendorf.jimagehash.datastructures.tree.binaryTree.Node;

/**
 * Helper class to iteratively search the tree {
 * 
 * @author Kilian
 *
 */
public class NodeInfo<T> implements Comparable<NodeInfo<T>> {
	public Node node;
	public double distance;
	public int depth;
	
	public NodeInfo(Node node, double distance, int depth) {
		this.node = node;
		this.distance = distance;
		this.depth = depth;
	}

	@Override
	public int compareTo(NodeInfo<T> o) {
		int compareTo = Integer.compare(depth,o.depth);
		if(compareTo == 0) {
			return Double.compare(this.distance,o.distance);
		}
		return compareTo;
	}

	@Override
	public String toString() {
		return "NodeInfo [node=" + node + ", distance=" + distance + ", depth=" + depth + "]";
	}
	
	
}