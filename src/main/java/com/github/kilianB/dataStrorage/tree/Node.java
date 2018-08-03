package com.github.kilianB.dataStrorage.tree;

/**
 * A tree node saving references to it's children.
 * 
 * @author Kilian
 *
 */
public class Node {
	
	public Node leftChild;	//1
	public Node rightChild;	//0
	
	public Node getChild(boolean left) {
		return left ? leftChild: rightChild;
	}
	
	
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
}
