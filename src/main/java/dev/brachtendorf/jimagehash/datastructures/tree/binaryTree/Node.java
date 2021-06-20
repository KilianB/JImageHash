package dev.brachtendorf.jimagehash.datastructures.tree.binaryTree;

import java.io.Serializable;

/**
 * A tree node saving references to it's children.
 * 
 * @author Kilian
 *
 */
public class Node implements Serializable {
	
	private static final long serialVersionUID = 9168509020498037545L;
	
	public Node leftChild;	//1
	public Node rightChild;	//0
	
	public Node getChild(boolean left) {
		return left ? leftChild: rightChild;
	}
	
	/**
	 * Create and set a child of the current node
	 * @param left if true create the left child if false create the right child
	 * @return the created node
	 */
	public Node createChild(boolean left) {
		return setChild(left,new Node());
	}
	
	
	public Node setChild(boolean left, Node newNode) {
		if(left) {
			leftChild = newNode;
		}else {
			rightChild= newNode;
		}
		return newNode;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((leftChild == null) ? 0 : leftChild.hashCode());
		result = prime * result + ((rightChild == null) ? 0 : rightChild.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof Node)) {
			return false;
		}
		Node other = (Node) obj;
		if (leftChild == null) {
			if (other.leftChild != null) {
				return false;
			}
		} else if (!leftChild.equals(other.leftChild)) {
			return false;
		}
		if (rightChild == null) {
			if (other.rightChild != null) {
				return false;
			}
		} else if (!rightChild.equals(other.rightChild)) {
			return false;
		}
		return true;
	}
	
}
