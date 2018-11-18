package com.github.kilianB.benchmark;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.DoubleSummaryStatistics;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.imageio.ImageIO;

import com.github.kilianB.ArrayUtil;
import com.github.kilianB.MathUtil;
import com.github.kilianB.hashAlgorithms.HashingAlgorithm;
import com.github.kilianB.matcher.Hash;
import com.github.kilianB.matcher.ImageMatcher.AlgoSettings;
import com.github.kilianB.matcher.SingleImageMatcher;

/**
 * The algorithm benchmark is a utility class allowing to check how multiple
 * hashing algorithms perform for certain images images.
 * 
 * @author Kilian
 * @since 2.0.0
 */
public class AlgorithmBenchmarker {

	/** HTML base template*/
	private static String htmlBase = buildHtmlBase();

	/** Format used to cut off decimal numbers*/
	private DecimalFormat df = new DecimalFormat("0.000");
	
	/** Shall a speed benchmark be added to the report **/
	private boolean timming;

	/** The image matcher holding the hashing algorithms and settings */
	private SingleImageMatcher imageMatcher;
	
	/** The images to test */
	private List<TestData> imagesToTest = new ArrayList<>();

	/**  Algorithms extracted from the image matcher */
	private Map<HashingAlgorithm, AlgoSettings> algorithmsToTest;

	/**
	 * Construct an algorithm benchmarker.
	 * 
	 * @param imageMatcher the image matcher object holding all ahshing algorithms
	 *                     which will be tested
	 * @speedBenchmark if true a speed benchmark will be added to the table. The
	 *                 speed benchmark tries to compute the time a single hashing
	 *                 operation (without reading the image from disk) takes. Be
	 *                 aware that micro benchmarks are hard to design and if a true
	 *                 representation is required more suited libraries like
	 *                 Oracle's JMH should be used instead.
	 */
	public AlgorithmBenchmarker(SingleImageMatcher imageMatcher, boolean speedBenchmark) {
		this.timming = speedBenchmark;
		this.imageMatcher = imageMatcher;
	}

	/**
	 * Add labeled test images
	 * 
	 * @param testImages the images which will be tested
	 */
	public void addTestImages(TestData... testImages) {
		for (TestData t : testImages) {
			imagesToTest.add(t);
		}
	}

	/**
	 * Compute the benchmark and output it's content as HTML to the console. Every
	 * call of this method recomputes the benchmark.
	 */
	public void toConsole() {
		System.out.println(constructHTML());
	}

	/**
	 * Compute the benchmark and save the content to an HTML file located at the
	 * base of this project with the name Benchmark_TimeInMS.html Every call of this
	 * method recomputes the benchmark and creates a new file.
	 */
	public void toFile() {
		toFile(new File("Benchmark_" + System.currentTimeMillis() + ".html"));
	}

	/**
	 * /** Compute the benchmark and save it to file. The supplied file will be
	 * overwritten and contain the benchmark in html format. Every call of this
	 * method recomputes the benchmark.
	 * 
	 * @param outputFile The file the content will be saved to
	 */
	public void toFile(File outputFile) {
		String output = constructHTML();
		try (FileWriter fw = new FileWriter(outputFile)) {
			fw.write(output);
			System.out.println("HTML File Created: " + outputFile.getAbsolutePath());
		} catch (IOException e) {
			e.printStackTrace();
			// output to console if we can't print to file
			System.out.println(output);
		}
	}

	private String constructHTML() {
		// Setup

		// Sort test images to group by categories
		Collections.sort(imagesToTest);

		/*
				 * Statistics for each hashing algorithm
				 * @formatter:off
				 * 0 Supposed matches distances 
				 * 1 Supposed distinct distance 
				 * 2 True positive 
				 * 3 False Positive  
				 * 4 False Negative 
				 * 5 True Negative
				 * @formatter:on
				 */
		Map<HashingAlgorithm, DoubleSummaryStatistics[]> statMap = new HashMap<>();
		algorithmsToTest = imageMatcher.getAlgorithms();

		// Init Map
		for (HashingAlgorithm h : algorithmsToTest.keySet()) {
			DoubleSummaryStatistics[] stats = new DoubleSummaryStatistics[6];
			ArrayUtil.fillArrayMulti(stats, () -> {
				return new DoubleSummaryStatistics();
			});
			statMap.put(h, stats);
		}

		// 0. Compute all hashes for each image and hashing algorithm
		Map<HashingAlgorithm, Map<TestData, Hash>> hashes = new HashMap<>();
		for (HashingAlgorithm h : algorithmsToTest.keySet()) {
			HashMap<TestData, Hash> algorithmSpecificHash = new HashMap<>();
			hashes.put(h, algorithmSpecificHash);
			for (TestData t : imagesToTest) {
				algorithmSpecificHash.put(t, h.hash(t.bImage));
			}
		}

		// Header
		StringBuilder htmlBuilder = new StringBuilder();

		// Table header
		appendHeader(htmlBuilder);

		// Algorithm bit resolution and algo settings
		appendAlgorithmInformation(htmlBuilder);

		// Append distance section of each image / algorithm
		appendHashingDistances(htmlBuilder, hashes, statMap);

		// Append statistics section
		appendStatistics(htmlBuilder, statMap);

		// Run benchmark and append it to the table
		if (timming) {
			appendTimmingBenchmark(htmlBuilder);
		}

		// Finalize
		htmlBuilder.append("</tbody></table>");

		// Construct final html
		return htmlBase.replace("$body", htmlBuilder.toString());
	}

	/**
	 * Construct the header of the table and append it to the html builder
	 * 
	 * @param htmlBuilder the builder used to construct the output
	 */
	private void appendHeader(StringBuilder htmlBuilder) {
		htmlBuilder
				.append("<table>\n").append("<thead><tr> <th>Images</th> <th>Category</th> <th colspan="
						+ algorithmsToTest.size() + ">Distance</th> </tr></thead>\n")
				.append("<tbody><tr><td colspan = 2></td>");

		// KeySet call is cached in hashmap
		for (HashingAlgorithm h : algorithmsToTest.keySet()) {
			htmlBuilder.append("<td>" + h.toString() + "</td>");
		}

		htmlBuilder.append("</tr>\n");
	}

	/**
	 * Append meta information to the table and append it to the html builder
	 * 
	 * @param htmlBuilder the builder used to construct the output
	 */
	private void appendAlgorithmInformation(StringBuilder htmlBuilder) {
		htmlBuilder.append("<tr><td colspan = 2><b>Actual Resolution:</b></td>");
		for (HashingAlgorithm h : algorithmsToTest.keySet()) {
			htmlBuilder.append("<td>").append(h.getKeyResolution()).append("bits</td>");
		}
		htmlBuilder.append("</tr>\n");
		htmlBuilder.append("<tr><td colspan = 2><b>Threshold:</b></td>");

		for (HashingAlgorithm h : algorithmsToTest.keySet()) {
			htmlBuilder.append("<td>").append(df.format(algorithmsToTest.get(h).getThreshold())).append("</td>");
		}
		htmlBuilder.append("</tr>\n");
		htmlBuilder.append(emptyTableRow(algorithmsToTest.size()));
	}

	/**
	 * Append the hashing distances between the individual images for each algorithm
	 * 
	 * @param htmlBuilder the builder used to construct the output
	 * @param statMap     map to store the distances in
	 * @param hashes      the precomputed hashes
	 */
	private void appendHashingDistances(StringBuilder htmlBuilder, Map<HashingAlgorithm, Map<TestData, Hash>> hashes,
			Map<HashingAlgorithm, DoubleSummaryStatistics[]> statMap) {
		int lastCategory = 0;

		List<TestData> sortedKeys = new ArrayList(imagesToTest);

		for (TestData base : imagesToTest) {
			// Compute the distance for all crosses we have not looked at.
			// It's symmetric no need to check it twice
			sortedKeys.remove(base);

			if (base.category != lastCategory) {
				lastCategory = base.category;
				htmlBuilder.append(emptyTableRow(algorithmsToTest.size()));
			}

			for (TestData cross : sortedKeys) {
				htmlBuilder.append("<tr>");
				boolean first = true;
				for (Entry<HashingAlgorithm, AlgoSettings> entry : algorithmsToTest.entrySet()) {

					HashingAlgorithm h = entry.getKey();
					AlgoSettings algoSettings = entry.getValue();

					boolean supposedToMatch = base.category == cross.category;

					Hash baseHash = hashes.get(h).get(base);
					Hash crossHash = hashes.get(h).get(cross);

					double distance;

					if (algoSettings.isNormalized()) {
						distance = baseHash.normalizedHammingDistance(crossHash);
					} else {
						distance = baseHash.hammingDistance(crossHash);
					}

					boolean consideredMatch = algoSettings.getThreshold() >= distance;

					String backgroundColor = "";
					if (consideredMatch) {
						if (supposedToMatch) {
							statMap.get(h)[2].accept(1);
							// backgroundColor = truePositiveCol;
						} else {
							statMap.get(h)[3].accept(1);
							backgroundColor = "fault";
						}
					} else {
						if (supposedToMatch) {
							statMap.get(h)[4].accept(1);
							backgroundColor = "fault";
						} else {
							statMap.get(h)[5].accept(1);
							// backgroundColor = trueNegativeCol;
						}
					}

					if (first) {
						htmlBuilder.append("<td>").append(base.name).append("-").append(cross.name)
								.append("</td><td class='category'>").append("[").append(base.category).append("-")
								.append(cross.category).append("]</td>");
						first = false;
					}

					htmlBuilder.append("<td class='").append(backgroundColor).append("'>");

					if (algoSettings.isNormalized()) {
						htmlBuilder.append(df.format(distance));
					} else {
						htmlBuilder.append((int) distance);
					}

					htmlBuilder.append("</td>");
					statMap.get(h)[supposedToMatch ? 0 : 1].accept(distance);
				}
				htmlBuilder.append("</tr>\n");
			}
		}
		htmlBuilder.append(emptyTableRow(algorithmsToTest.size()));
	}

	/**
	 * Append statistics regarding the hashing algorithms to the table
	 * 
	 * @param htmlBuilder      The stringbuilder to append the output to
	 * @param algorithmsToTest Map holding the hashing algorithms
	 * @param statMap          map holding statistics
	 */
	private void appendStatistics(StringBuilder htmlBuilder, Map<HashingAlgorithm, DoubleSummaryStatistics[]> statMap) {

		int numberOfPairs = MathUtil.triangularNumber(imagesToTest.size() - 1);

		String[] columnNames = { "Avg match:", "Avg distinct:", "Min/Max match:", "Min/Max distinct:",
				"True Positive / False Positive", "", "False Negative / True Negative", "Accuracy", "Precision" };

		for (int i = 0; i < columnNames.length; i++) {

			htmlBuilder.append("<tr>");

			htmlBuilder.append("<td colspan = 2><b>").append(columnNames[i]).append("</b></td>");

			for (HashingAlgorithm h : algorithmsToTest.keySet()) {
				if (i < 2) {
					htmlBuilder.append("<td>").append(df.format(statMap.get(h)[i].getAverage())).append("</td>");
				} else if (i < 4) {
					DoubleSummaryStatistics dSum0 = statMap.get(h)[i - 2];

					if (algorithmsToTest.get(h).isNormalized()) {
						htmlBuilder.append("<td>").append(df.format(dSum0.getMin())).append("/")
								.append(df.format(dSum0.getMax())).append("</td>");
					} else {
						htmlBuilder.append("<td>").append((int) dSum0.getMin()).append("/").append((int) dSum0.getMax())
								.append("</td>");
					}

				} else if (i < 7) {
					DoubleSummaryStatistics dSum0 = statMap.get(h)[i - 2];
					DoubleSummaryStatistics dSum1 = statMap.get(h)[i - 1];

					htmlBuilder.append("<td>").append(dSum0.getCount()).append("/").append(dSum1.getCount())
							.append("</td>");
				} else if (i == 7) {
					// Accuracy = (TP + TN) / (TP + TN + FP + FN) => TP + TN / (testData.size())
					int truePositive = (int) statMap.get(h)[2].getCount();
					int trueNegative = (int) statMap.get(h)[5].getCount();
					htmlBuilder.append("<td>").append(df.format((truePositive + trueNegative) / (double) numberOfPairs))
							.append("</td>");
				} else if (i == 8) {
					// Precision
					int truePositive = (int) statMap.get(h)[2].getCount();
					int falsePositive = (int) statMap.get(h)[3].getCount();

					htmlBuilder.append("<td>").append(df.format(truePositive / (double) (truePositive + falsePositive)))
							.append("</td>");
				}
			}
			if (i == 4) {
				i++;
			}

			htmlBuilder.append("</tr>\n");
		}
	}

	private String emptyTableRow(int algoCount) {
		return "<tr class='spacerRow'><td colspan='2'></td><td colspan='" + algoCount + "'></td></tr>\n";
	}

	// Pseudo micro benchmark
	/**
	 * 
	 * @param htmlBuilder
	 */
	long appendTimmingBenchmark(StringBuilder htmlBuilder) {

		// timming

		// Avoid dead code elimination
		long sum = 0;

		Map<HashingAlgorithm, Double> averageRuntime = new HashMap<>(algorithmsToTest.size());

		// warmup
		for (int i = 0; i < 100; i++) {
			for (HashingAlgorithm hasher : algorithmsToTest.keySet()) {
				for (TestData testData : imagesToTest) {
					sum += hasher.hash(testData.bImage).getHashValue().bitCount();
				}
			}
		}

		// Testing
		int loops = 500;
		for (int i = 0; i < loops; i++) {

			for (HashingAlgorithm hasher : algorithmsToTest.keySet()) {
				long start = System.nanoTime();
				for (TestData testData : imagesToTest) {
					sum += hasher.hash(testData.bImage).getHashValue().bitCount();
				}
				double elapsedAvg = (((System.nanoTime() - start)) / (double) loops) / 1_000_000 / imagesToTest.size();

				averageRuntime.merge(hasher, elapsedAvg, (old, newVal) -> {
					return old + newVal;
				});
			}
		}

		htmlBuilder.append("<tr><td colspan=2><b>Timming</b> (ms/picture): *</td>");

		for (HashingAlgorithm hasher : algorithmsToTest.keySet()) {
			htmlBuilder.append("<td>").append(df.format(averageRuntime.get(hasher)) + " ms").append("</td>");
		}

		htmlBuilder.append("</tr>");

		// Disclaimer
		htmlBuilder.append("<tfoot><tr>" + "<td  style='text-align:center;' colspan =")
				.append(algorithmsToTest.size() + 2).append(">")
				.append("* Please note that speed benchmarks are not representative and should only be used to get a rough estimated of the magnitude of the speed.")
				.append("</td></tr></tfoot>");

		return sum;
	}

	/**
	 * A labeled image used to benchmark hashing algorithms. 
	 * @author Kilian
	 * @since 2.0.0
	 */
	public static class TestData implements Comparable<TestData> {

		/** A character representation of the file for easy feedback*/
		private String name;
		
		/** The category of the image. Same categories equals similar images*/
		private int category;
		
		/** The image to test */
		private BufferedImage bImage;

		/**
		 * 
		 * @param category The image category. Images with the same category are
		 *                 expected to be classified as similar images
		 * @param f The Fie pointing to the image
		 */
		public TestData(int category, File f) {
			try {
				this.bImage = ImageIO.read(f);
			} catch (IOException e) {
				e.printStackTrace();
			}
			this.name = f.getName().substring(0, f.getName().lastIndexOf("."));
			this.category = category;
		}

		@Override
		public int compareTo(TestData o) {
			return Integer.compare(category, o.category);
		}

		@Override
		public String toString() {
			return "TestData [name=" + name + ", category=" + category + "]";
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((bImage == null) ? 0 : bImage.hashCode());
			result = prime * result + category;
			result = prime * result + ((name == null) ? 0 : name.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			TestData other = (TestData) obj;
			if (bImage == null) {
				if (other.bImage != null)
					return false;
			} else if (!bImage.equals(other.bImage))
				return false;
			if (category != other.category)
				return false;
			if (name == null) {
				if (other.name != null)
					return false;
			} else if (!name.equals(other.name))
				return false;
			return true;
		}
	}

	/**
	 * Construct the html frame to embed the benchmark result into
	 * @return an html template
	 */
	private static String buildHtmlBase() {
		StringBuilderI htmlBuilder = new StringBuilderI();
		htmlBuilder.row("<!DOCTYPE html>").row("<html>").row("<head>").row("<title>Algorithm Benchmarking</title>")
				/*
				 * CSS
				 */
				.row("<style>").tRow("html{min-height:100%;}")
				.tRow("body{min-height:100%; margin: 0; background: linear-gradient(45deg, #49a09d, #5f2c82); font-family: sans-serif; font-weight: 100; margin-bottom:25px;}")
				.tRow("table{margin:auto; border-collapse: collapse; box-shadow: 0 0 20px rgba(0,0,0,0.1); margin-top:40px;}")
				.tRow("thead th, td:first-child, .category{background-color: #55608f;}")
				.tRow("tr:nth-child(1) {background-color:#3d54b9;}")
				.tRow("th,td{padding: 10px;background-color: rgba(255,255,255,0.2);color: #fff;}")
				.tRow("tbody tr:hover{background-color: rgba(255,255,255,0.3);}").tRow(".spacerRow td{ padding:7px;}")
				.tRow(".circle{width:10px; height: 10px; background-color:green; border-radius:50%; display:inline-block; margin-right:5px;}")
				.tRow(".fault{color:#fff700;}").row("</style>").row("<body>").row("$body").row("</body>")
				.row("</html>");
		return htmlBuilder.toString();
	}

	/**
	 * Utility StringBuilder to allow formating text
	 * @author Kilian
	 *
	 */
	private static class StringBuilderI {
		StringBuilder internal = new StringBuilder();

		public StringBuilderI row(String s) {
			internal.append(s).append("\n");
			return this;
		}

		public StringBuilderI tRow(String s) {
			internal.append("\t").append(s).append("\n");
			return this;
		}

		public String toString() {
			return internal.toString();
		}
	}

	/**
	 * 
	 */

}
