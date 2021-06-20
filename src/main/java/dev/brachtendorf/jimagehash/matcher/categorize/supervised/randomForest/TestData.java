package dev.brachtendorf.jimagehash.matcher.categorize.supervised.randomForest;

import java.awt.image.BufferedImage;

class TestData {

	protected BufferedImage b0;
	protected boolean match;

	/**
	 * @param b0
	 * @param b1
	 * @param match
	 */
	public TestData(BufferedImage b0, boolean match) {
		super();
		this.b0 = b0;
		this.match = match;
	}

	@Override
	public String toString() {
		return "TestData [b0=" + b0.hashCode() + ", match=" + match + "]";
	}

}