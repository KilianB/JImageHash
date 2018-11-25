package com.github.kilianB.examples;

import java.io.File;

import com.github.kilianB.benchmark.AlgorithmBenchmarker;
import com.github.kilianB.benchmark.AlgorithmBenchmarker.TestData;
import com.github.kilianB.hashAlgorithms.AverageHash;
import com.github.kilianB.hashAlgorithms.DifferenceHash;
import com.github.kilianB.hashAlgorithms.DifferenceHash.Precision;
import com.github.kilianB.hashAlgorithms.PerceptiveHash;
import com.github.kilianB.hashAlgorithms.RotAverageHash;
import com.github.kilianB.hashAlgorithms.RotPHash;
import com.github.kilianB.hashAlgorithms.filter.Kernel;
import com.github.kilianB.matcher.SingleImageMatcher;

/**
 * 
 * Benchmark utility to test how certain algorithms react to a set of test
 * images.
 * 
 * @author Kilian
 *
 */
public class AlgorithmBenchmark {

	public static void main(String[] args) {

		/*
		 * Benchmark commonly used algorithms to see how they behave if they encounter
		 * different test images. Benchmarking is important to see which algorithms work
		 * for your specific set of images, which bit resolution to choose and what an
		 * acceptable distance threshold is.
		 */

		// Chose one of the examples to run

		// The default algorithms
		// benchmakDefaultAlgorthms();

		/// Hue sat
		// benchmarkDefaultHueSat();

		// Algorithms which are able to work with rotated images
		// benchmarkRotationalHashes();

		// Algorithms who might be release ready in one of the following versions
		benchmarkExperimentalHashingAlgos();
	}

	/**
	 * Benchmark the common algorithms with different settings. Benchmarking
	 */
	public static void benchmakDefaultAlgorthms() {

		// 0. Construct the image matcher which acts as a shell to define algorithm
		// settings
		SingleImageMatcher matcher = new SingleImageMatcher();

		// 1, Add the desired algorithms we want to test
		// We configure the image matcher to see if t
		matcher.addHashingAlgorithm(new AverageHash(16), 0.4f);
		matcher.addHashingAlgorithm(new AverageHash(64), 0.4f);

		matcher.addHashingAlgorithm(new DifferenceHash(64, Precision.Simple), 0.4f);
		matcher.addHashingAlgorithm(new DifferenceHash(32, Precision.Double), 0.4f);

		matcher.addHashingAlgorithm(new PerceptiveHash(16), 0.4f);

		// You may also use the non normalized version
		matcher.addHashingAlgorithm(new PerceptiveHash(64), 0.4f);

		// 2. Create a benchmarker

		/*
		 * Depending on the algorithm speed benchmarks can be expensive and will take up
		 * a majority of the time spend while benchmarking. Please also be adviced that
		 * micro benchmarking is a complex topic and here we only follow a naive
		 * approach without taking care of JVM optimization like loop unfolding, dead
		 * code elimination, caching etc ... If you want to properly benchmark the
		 * algorithms please use a dedicated test harness like Oracle's JMH.
		 * https://openjdk.java.net/projects/code-tools/jmh/
		 */
		boolean speedBenchmark = true;

		AlgorithmBenchmarker ab = new AlgorithmBenchmarker(matcher, speedBenchmark);

		// 2. Add some images we want to test.
		addDefaultTestImages(ab);

		// 3. generate the report

		// Display report as javafx application
		ab.display();
		// To console as html file
		// ab.toConsole();
		// Output as html file
		// ab.toFile();
	}

	public static void benchmarkDefaultHueSat() {
		// 0. Construct the image matcher which acts as a shell to define algorithm
		// settings
		SingleImageMatcher matcher = new SingleImageMatcher();

		// 1, Add the desired algorithms we want to test
		// We configure the image matcher to see if t
		matcher.addHashingAlgorithm(new AverageHash(16), 0.4f);
		matcher.addHashingAlgorithm(new AverageHash(64), 0.4f);

		matcher.addHashingAlgorithm(new DifferenceHash(64, Precision.Simple), 0.4f);
		matcher.addHashingAlgorithm(new DifferenceHash(32, Precision.Double), 0.4f);

		matcher.addHashingAlgorithm(new PerceptiveHash(16), 0.4f);

		// You may also use the non normalized version
		matcher.addHashingAlgorithm(new PerceptiveHash(64), 0.4f);

		// 2. Create a benchmarker

		/*
		 * Depending on the algorithm speed benchmarks can be expensive and will take up
		 * a majority of the time spend while benchmarking. Please also be adviced that
		 * micro benchmarking is a complex topic and here we only follow a naive
		 * approach without taking care of JVM optimization like loop unfolding, dead
		 * code elimination, caching etc ... If you want to properly benchmark the
		 * algorithms please use a dedicated test harness like Oracle's JMH.
		 * https://openjdk.java.net/projects/code-tools/jmh/
		 */
		boolean speedBenchmark = true;

		AlgorithmBenchmarker ab = new AlgorithmBenchmarker(matcher, speedBenchmark);

		// 2. Add some images we want to test.
		addDefaultTestImages(ab);
		addHueSatTestImages(ab);

		// 3. generate the report

		// Display report as javafx application
		ab.display();
		// To console as html file
		// ab.toConsole();
		// Output as html file
		// ab.toFile();
	}

	public static void benchmarkRotationalHashes() {

		/*
		 * 1. Create a single image matcher with all the algorithms you want to test. To
		 * get a good visual result you might want to add only a few algorithms at a
		 * time.
		 */
		SingleImageMatcher imageMatcher = new SingleImageMatcher();

		imageMatcher.addHashingAlgorithm(new RotAverageHash(16), 0.4f);
		imageMatcher.addHashingAlgorithm(new RotAverageHash(64), 0.4f);
		imageMatcher.addHashingAlgorithm(new RotPHash(16), 0.1f);
		imageMatcher.addHashingAlgorithm(new RotPHash(64), 0.1f);
		imageMatcher.addHashingAlgorithm(new RotPHash(128), 0.1f);

		imageMatcher.addHashingAlgorithm(new AverageHash(16), 0.4f);
		imageMatcher.addHashingAlgorithm(new AverageHash(64), 0.4f);
		imageMatcher.addHashingAlgorithm(new PerceptiveHash(16), 0.4f);
		imageMatcher.addHashingAlgorithm(new PerceptiveHash(64), 0.4f);
		imageMatcher.addHashingAlgorithm(new DifferenceHash(16, Precision.Double), 0.4f);

		// 2. Create the object
		AlgorithmBenchmarker db = new AlgorithmBenchmarker(imageMatcher, false);

		// Add ballon as contrast

		addRotationalTestImages(db);
		db.addTestImages(new TestData(4, new File("src/test/resources/ballon.jpg")));

		db.display();
	}

	public static void benchmarkExperimentalHashingAlgos() {

		SingleImageMatcher imageMatcher = new SingleImageMatcher();

		// 2. Create the object
		AlgorithmBenchmarker db = new AlgorithmBenchmarker(imageMatcher, false);

		AverageHash aHash = new AverageHash(32);
		AverageHash aHash1 = new AverageHash(32);

		aHash.addKernel(Kernel.gaussianFilter(5, 5, 3));

		imageMatcher.addHashingAlgorithm(aHash, 0.4f);
		imageMatcher.addHashingAlgorithm(aHash1, 0.4f);

//		imageMatcher.addHashingAlgorithm(new HogHash(16), 0.3f, true);
//		imageMatcher.addHashingAlgorithm(new HogHash(32), 0.3f, true);
//		imageMatcher.addHashingAlgorithm(new HogHash(128), 0.3f, true);
//		imageMatcher.addHashingAlgorithm(new HogHash(1024), 0.3f, true);
//		imageMatcher.addHashingAlgorithm(new HogHash(15000), 0.3f, true);

//		imageMatcher.addHashingAlgorithm(new AverageHash(32), 0.3f, true);
//		imageMatcher.addHashingAlgorithm(new MedianHash(32), 0.3f, true);
//		imageMatcher.addHashingAlgorithm(new AverageHash(64), 0.3f, true);
//		imageMatcher.addHashingAlgorithm(new MedianHash(64), 0.3f, true);
//		
//		
//		imageMatcher.addHashingAlgorithm(new HogHash(32), 0.3f, true);
//		imageMatcher.addHashingAlgorithm(new HogHash(128), 0.3f, true);

//		imageMatcher.addHashingAlgorithm(new HogHashAngularEncoded(16), 0.3f, true);
//		imageMatcher.addHashingAlgorithm(new HogHashAngularEncoded(32), 0.3f, true);
//		imageMatcher.addHashingAlgorithm(new HogHashAngularEncoded(128), 0.3f, true);
//		imageMatcher.addHashingAlgorithm(new HogHashAngularEncoded(1024), 0.3f, true);
//
//		imageMatcher.addHashingAlgorithm(new HogHashAngularEncoded(64, 64, 2, 4), 0.3f, true);
//		imageMatcher.addHashingAlgorithm(new HogHashAngularEncoded(64, 64, 2, 8), 0.3f, true);
//		imageMatcher.addHashingAlgorithm(new HogHashAngularEncoded(64, 64, 4, 4), 0.3f, true);
//		imageMatcher.addHashingAlgorithm(new HogHashAngularEncoded(64, 64, 4, 8), 0.3f, true);

//		AverageKernelHash hasher = new AverageKernelHash(32);
//		AverageKernelHash hasher1 = new AverageKernelHash(32, Kernel.gaussianFilter(3, 3, 1));
//
//		imageMatcher.addHashingAlgorithm(hasher, 0.25f, true);
//		imageMatcher.addHashingAlgorithm(hasher1, 0.25f, true);
//
//		
//		
//		
//		imageMatcher.addHashingAlgorithm(new HogHashDual(1024), 0.4f, true);

		addDefaultTestImages(db);

		db.display();
	}

	/*
	 * +###########################################################################+
	 * |########################### Helper methods ################################|
	 * +###########################################################################+
	 */

	/**
	 * Add a set of test images to the benchmarker. Test images are labeled images
	 * indicating which images are supposed to be matched. This set contains the
	 * images found in the readme file on github.
	 * 
	 * @param benchmarker The benchmarker to add the images to
	 */
	private static void addDefaultTestImages(AlgorithmBenchmarker benchmarker) {

		// Add the ballon image to the benchmarker with a group label of 0
		benchmarker.addTestImages(new TestData(0, new File("src/test/resources/ballon.jpg")));

		/*
		 * The following images are distinct to ballon therefore we choose a different
		 * group id. On the other hand they are all variations of the same image so they
		 * are labeled with an id of 1.
		 */
		benchmarker.addTestImages(new TestData(1, new File("src/test/resources/copyright.jpg")));
		benchmarker.addTestImages(new TestData(1, new File("src/test/resources/highQuality.jpg")));
		benchmarker.addTestImages(new TestData(1, new File("src/test/resources/lowQuality.jpg")));
		benchmarker.addTestImages(new TestData(1, new File("src/test/resources/thumbnail.jpg")));
	}

	/**
	 * 
	 * @param benchmarker
	 */
	private static void addHueSatTestImages(AlgorithmBenchmarker benchmarker) {
		benchmarker.addTestImages(new TestData(1, new File("src/test/resources/highQualityBase.png")));
		benchmarker.addTestImages(new TestData(1, new File("src/test/resources/highQualityBright.png")));
		benchmarker.addTestImages(new TestData(1, new File("src/test/resources/highQualityDark.png")));
		benchmarker.addTestImages(new TestData(1, new File("src/test/resources/highQualityHue.png")));
	}

	/**
	 * Add 4 testimages to the benchmarker which are simply rotations of one of the
	 * same image.
	 * 
	 * @param benchmarker to add the images to.
	 */
	private static void addRotationalTestImages(AlgorithmBenchmarker benchmarker) {
		benchmarker.addTestImages(new TestData(3, new File("src/test/resources/Lenna.png")));
		benchmarker.addTestImages(new TestData(3, new File("src/test/resources/Lenna90.png")));
		benchmarker.addTestImages(new TestData(3, new File("src/test/resources/Lenna180.png")));
		benchmarker.addTestImages(new TestData(3, new File("src/test/resources/Lenna270.png")));
	}

}
