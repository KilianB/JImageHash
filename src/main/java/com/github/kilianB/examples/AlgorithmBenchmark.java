package com.github.kilianB.examples;

import java.io.File;

import com.github.kilianB.benchmark.AlgorithmBenchmarker;
import com.github.kilianB.benchmark.AlgorithmBenchmarker.TestData;
import com.github.kilianB.hashAlgorithms.AverageHash;
import com.github.kilianB.hashAlgorithms.DifferenceHash;
import com.github.kilianB.hashAlgorithms.DifferenceHash.Precision;
import com.github.kilianB.hashAlgorithms.HogHash;
import com.github.kilianB.hashAlgorithms.PerceptiveHash;
import com.github.kilianB.hashAlgorithms.RotPHash;
import com.github.kilianB.matcher.SingleImageMatcher;

/**
 * 
 * Benchmark utility to test how certain algorithms react to test images
 * 
 * @author Kilian
 *
 */
public class AlgorithmBenchmark {

	public static void main(String[] args) {

		/*
		 * 1. Create a single image matcher with all the algorithms you want to test. To
		 * get a good visual result you might want to add only a few algorithms at a
		 * time.
		 */
		SingleImageMatcher imageMatcher = new SingleImageMatcher();
		imageMatcher.addHashingAlgorithm(new AverageHash(16), 0.4f, true);
		imageMatcher.addHashingAlgorithm(new AverageHash(64), 0.4f, true);
		imageMatcher.addHashingAlgorithm(new PerceptiveHash(16), 0.4f, true);
		imageMatcher.addHashingAlgorithm(new PerceptiveHash(64), 0.4f, true);
		imageMatcher.addHashingAlgorithm(new DifferenceHash(16, Precision.Simple), 0.4f, true);
		imageMatcher.addHashingAlgorithm(new DifferenceHash(64, Precision.Simple), 0.4f, true);
		imageMatcher.addHashingAlgorithm(new DifferenceHash(16, Precision.Double), 0.4f, true);
		imageMatcher.addHashingAlgorithm(new HogHash(64), 0.4f, true);

		// You are also free to use the normal hemming distance instead of the
		// normalized version
		imageMatcher.addHashingAlgorithm(new RotPHash(128), 10);

		// 2. Create the object
		AlgorithmBenchmarker db = new AlgorithmBenchmarker(imageMatcher, true);

		// 3. Add arbitrary test images. Test Images are labeled to tell the benchmarker
		// which images are supposed to be a match
		db.addTestImages(new TestData(0, new File("src/test/resources/ballon.jpg")));
		db.addTestImages(new TestData(1, new File("src/test/resources/copyright.jpg")));
		db.addTestImages(new TestData(1, new File("src/test/resources/highQuality.jpg")));
		db.addTestImages(new TestData(1, new File("src/test/resources/lowQuality.jpg")));
		db.addTestImages(new TestData(1, new File("src/test/resources/thumbnail.jpg")));

		db.addTestImages(new TestData(2, new File("src/test/resources/Lenna.png")));
		db.addTestImages(new TestData(2, new File("src/test/resources/Lenna90.png")));
		db.addTestImages(new TestData(2, new File("src/test/resources/Lenna180.png")));
		db.addTestImages(new TestData(2, new File("src/test/resources/Lenna270.png")));

		db.toFile();
//		Prints the HTMl to the console!
//		db.toConsole();
	}
}
