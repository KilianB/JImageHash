package com.github.kilianB.examples;

import java.io.File;

import com.github.kilianB.benchmark.AlgorithmBenchmarker;
import com.github.kilianB.hashAlgorithms.AverageHash;
import com.github.kilianB.hashAlgorithms.DifferenceHash;
import com.github.kilianB.hashAlgorithms.DifferenceHash.Precision;
import com.github.kilianB.hashAlgorithms.MedianHash;
import com.github.kilianB.hashAlgorithms.PerceptiveHash;
import com.github.kilianB.hashAlgorithms.RotAverageHash;
import com.github.kilianB.hashAlgorithms.RotPHash;
import com.github.kilianB.hashAlgorithms.experimental.HogHash;
import com.github.kilianB.hashAlgorithms.experimental.HogHashAngularEncoded;
import com.github.kilianB.hashAlgorithms.experimental.HogHashDual;
import com.github.kilianB.hashAlgorithms.filter.Kernel;
import com.github.kilianB.matcher.categorize.supervised.LabeledImage;
import com.github.kilianB.matcher.exotic.SingleImageMatcher;

/**
 * 
 * Benchmark utility to test how certain algorithms react to a set of test
 * images.
 * 
 * @author Kilian
 *
 */
@SuppressWarnings("deprecation")
public class AlgorithmBenchmark {

	public static void main(String[] args) {

		/*
		 * Benchmark commonly used algorithms to see how they behave if they encounter
		 * different test images. Benchmarking is important to see which algorithms work
		 * for your specific set of images, which bit resolution to choose and what an
		 * acceptable distance threshold is.
		 */

		// Chose one of the examples to run

		/* Commonly used algorithms */
		benchmakDefaultAlgorthms();

		/* Hue sat */
		//benchmarkDefaultHueSat();

		// Algorithms which are able to work with rotated images
		benchmarkRotationalHashes();

		// Algorithms who might be release ready in one of the following versions
		//benchmarkExperimentalHashingAlgos();
	}

	/**
	 * Benchmark the common algorithms with different settings.
	 */
	public static void benchmakDefaultAlgorthms() {

		// 0. Construct a single image matcher which acts as a shell to define algorithm
		// settings
		SingleImageMatcher matcher = new SingleImageMatcher();

		// 1, Add the desired algorithms we want to test
		// We configure the image matcher to see if t
		matcher.addHashingAlgorithm(new AverageHash(8), 0.4);
		matcher.addHashingAlgorithm(new AverageHash(32), 0.4);
		matcher.addHashingAlgorithm(new AverageHash(64), 0.4);
		
		matcher.addHashingAlgorithm(new PerceptiveHash(32), 0.4);
		matcher.addHashingAlgorithm(new PerceptiveHash(64), 0.4);
		
		matcher.addHashingAlgorithm(new MedianHash(32), 0.4);
		matcher.addHashingAlgorithm(new MedianHash(64), 0.4);
		
		matcher.addHashingAlgorithm(new DifferenceHash(64, Precision.Simple), 0.4);
		matcher.addHashingAlgorithm(new DifferenceHash(32, Precision.Triple), 0.4);
		
		
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

		/* 3.1 Display report as javafx application(charts won't be available) */
		// ab.display();
		
		/* 3.2 To console as html file */
		// ab.toConsole();
		
		/* 3.3 Output as html file (link will be printed to the console)*/
		ab.toFile();
	}

	/**
	 * Benchmark with additional hue and sat images
	 */
	public static void benchmarkDefaultHueSat() {
		// 0. Construct the image matcher which acts as a shell to define algorithm
		// settings
		SingleImageMatcher matcher = new SingleImageMatcher();

		// 1, Add the desired algorithms we want to test
		// We configure the image matcher to see if t
		matcher.addHashingAlgorithm(new AverageHash(16), 0.4);
		matcher.addHashingAlgorithm(new AverageHash(64), 0.4);

		matcher.addHashingAlgorithm(new DifferenceHash(64, Precision.Simple), 0.3);
		matcher.addHashingAlgorithm(new DifferenceHash(64, Precision.Double), 0.3);
		matcher.addHashingAlgorithm(new DifferenceHash(64, Precision.Triple), 0.3);

		// Experimental
		matcher.addHashingAlgorithm(new MedianHash(16), 0.4);
		matcher.addHashingAlgorithm(new MedianHash(32), 0.21);
		matcher.addHashingAlgorithm(new MedianHash(64), 0.21);

		matcher.addHashingAlgorithm(new PerceptiveHash(16), 0.4);

		// You may also use the non normalized version
		matcher.addHashingAlgorithm(new PerceptiveHash(64), 0.4);

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
		boolean speedBenchmark = false;

		AlgorithmBenchmarker ab = new AlgorithmBenchmarker(matcher, speedBenchmark);

		// 2. Add some images we want to test.
		addDefaultTestImages(ab);
		addHueSatTestImages(ab);

		// 3. generate the report

		/* 3.1 Display report as javafx application(charts won't be available) */
		ab.display();
		
		/* 3.2 To console as html file */
		// ab.toConsole();
		
		/* 3.3 Output as html file (link will be printed to the console)*/
		//ab.toFile();
	}

	public static void benchmarkRotationalHashes() {

		/*
		 * 1. Create a single image matcher with all the algorithms you want to test. To
		 * get a good visual result you might want to add only a few algorithms at a
		 * time.
		 */
		SingleImageMatcher imageMatcher = new SingleImageMatcher();

		imageMatcher.addHashingAlgorithm(new RotAverageHash(16), 0.4);
		imageMatcher.addHashingAlgorithm(new RotAverageHash(64), 0.4);
		imageMatcher.addHashingAlgorithm(new RotAverageHash(128), 0.4);
		imageMatcher.addHashingAlgorithm(new RotPHash(16), 0.1);
		imageMatcher.addHashingAlgorithm(new RotPHash(64), 0.1);
		imageMatcher.addHashingAlgorithm(new RotPHash(128), 0.1);


		imageMatcher.addHashingAlgorithm(new AverageHash(16), 0.4);
		imageMatcher.addHashingAlgorithm(new AverageHash(64), 0.4);
		imageMatcher.addHashingAlgorithm(new PerceptiveHash(16), 0.4);
		imageMatcher.addHashingAlgorithm(new PerceptiveHash(64), 0.4);

		// 2. Create the object
		AlgorithmBenchmarker db = new AlgorithmBenchmarker(imageMatcher, true);

		// Add ballon as contrast

		addRotationalTestImages(db);
		db.addTestImages(new LabeledImage(4, new File("src/test/resources/ballon.jpg")));

		db.display();
		db.toFile();
	}

	public static void benchmarkExperimentalHashingAlgos() {

		SingleImageMatcher imageMatcher = new SingleImageMatcher();

		// 2. Create the object
		AlgorithmBenchmarker db = new AlgorithmBenchmarker(imageMatcher, false,30,false);

		//Hashing algorithm with filter  (really slow. Benchmarking will take a long time!)
		AverageHash aHash = new AverageHash(32);
		aHash.addFilter(Kernel.gaussianFilter(5, 5, 3));

		HogHash hog64 = new HogHash(64);
		HogHashDual hogDual64 = new HogHashDual(64);
		HogHashAngularEncoded hogAng64 = new HogHashAngularEncoded(64);
		HogHash hog128 = new HogHash(128);
		HogHashDual hogDual128 = new HogHashDual(128);
		HogHashDual hogAng128 = new HogHashDual(128);
		
		imageMatcher.addHashingAlgorithm(aHash,0.3);
		imageMatcher.addHashingAlgorithm(hog64,0.3);
		imageMatcher.addHashingAlgorithm(hogDual64,0.3);
		imageMatcher.addHashingAlgorithm(hogAng64,0.3);
		imageMatcher.addHashingAlgorithm(hog128,0.3);
		imageMatcher.addHashingAlgorithm(hogDual128,0.3);
		imageMatcher.addHashingAlgorithm(hogAng128,0.3);
		
		addDefaultTestImages(db);

		//db.display();
		db.toFile();
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
		benchmarker.addTestImages(new LabeledImage(0, new File("src/test/resources/ballon.jpg")));

		/*
		 * The following images are distinct to ballon therefore we choose a different
		 * group id. On the other hand they are all variations of the same image so they
		 * are labeled with an id of 1.
		 */
		benchmarker.addTestImages(new LabeledImage(1, new File("src/test/resources/copyright.jpg")));
		benchmarker.addTestImages(new LabeledImage(1, new File("src/test/resources/highQuality.jpg")));
		benchmarker.addTestImages(new LabeledImage(1, new File("src/test/resources/lowQuality.jpg")));
		benchmarker.addTestImages(new LabeledImage(1, new File("src/test/resources/thumbnail.jpg")));
	}

	/**
	 * 
	 * @param benchmarker
	 */
	private static void addHueSatTestImages(AlgorithmBenchmarker benchmarker) {
		benchmarker.addTestImages(new LabeledImage(1, new File("src/test/resources/highQualityBase.png")));
		benchmarker.addTestImages(new LabeledImage(1, new File("src/test/resources/highQualityBright.png")));
		benchmarker.addTestImages(new LabeledImage(1, new File("src/test/resources/highQualityDark.png")));
		benchmarker.addTestImages(new LabeledImage(1, new File("src/test/resources/highQualityHue.png")));
	}

	/**
	 * Add 4 testimages to the benchmarker which are simply rotations of one of the
	 * same image.
	 * 
	 * @param benchmarker to add the images to.
	 */
	private static void addRotationalTestImages(AlgorithmBenchmarker benchmarker) {
		benchmarker.addTestImages(new LabeledImage(3, new File("src/test/resources/Lenna.png")));
		benchmarker.addTestImages(new LabeledImage(3, new File("src/test/resources/Lenna90.png")));
		benchmarker.addTestImages(new LabeledImage(3, new File("src/test/resources/Lenna180.png")));
		benchmarker.addTestImages(new LabeledImage(3, new File("src/test/resources/Lenna270.png")));
	}

}
