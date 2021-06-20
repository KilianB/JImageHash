package dev.brachtendorf.jimagehash.datastructures.tree.binaryTreeFuzzy;

import dev.brachtendorf.jimagehash.datastructures.tree.binaryTree.Node;

/**
 * A tree node saving references to it's children.
 * 
 * @author Kilian
 * @since 3.0.0
 */
public class FuzzyNode extends Node {

	private static final long serialVersionUID = 4043416513243595121L;
	// TODO explain better
	/** The lower bound error this node might have */
	public double lowerDistance = Double.MAX_VALUE;
	/** The upper bound error this node might have */
	public double uppderDistance = -Double.MAX_VALUE;

	/**
	 * Create and set a child of the current node
	 * 
	 * @param left if true create the left child if false create the right child
	 * @return the created node
	 */
	public Node createChild(boolean left) {
		return setChild(left, new FuzzyNode());
	}

	/**
	 * Refresh the node's the upper and lower bound error. This method should be
	 * called when ever a hash was added to this node. The bounds will be adapted
	 * correctly by this method.
	 * 
	 * @param distance the distance of the hash added to this node
	 */
	public void setNodeBounds(double distance) {

		if (distance < this.lowerDistance) {
			this.lowerDistance = distance;
		}

		if (distance > this.uppderDistance) {
			this.uppderDistance = distance;
		}
	}

	@Override
	public String toString() {
		return "FuzzyNode [lowerDistance=" + lowerDistance + ", uppderDistance=" + uppderDistance + "]";
	}

}
