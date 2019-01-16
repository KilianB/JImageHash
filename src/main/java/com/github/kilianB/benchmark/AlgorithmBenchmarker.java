package com.github.kilianB.benchmark;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.DoubleSummaryStatistics;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

import com.github.kilianB.ArrayUtil;
import com.github.kilianB.MathUtil;
import com.github.kilianB.hash.Hash;
import com.github.kilianB.hashAlgorithms.HashingAlgorithm;
import com.github.kilianB.matcher.TypedImageMatcher.AlgoSettings;
import com.github.kilianB.matcher.categorize.supervised.LabeledImage;
import com.github.kilianB.matcher.exotic.SingleImageMatcher;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.web.WebView;
import javafx.stage.Screen;
import javafx.stage.Stage;

/**
 * The algorithm benchmark is a utility class allowing to evaluate the
 * performance of multiple hashing algorithms for a set of user supplied test
 * images.
 * 
 * <p>
 * The report is generated as HTML and can either be saved as a file to later be
 * viewed in a browser of your choice, or be viewed in a javafx window.
 * Additionally the generated HTML can be displayed in the console.
 * 
 * <p>
 * <b>Charting</b>
 * <p>
 * The benchmark allows a small chart overview visualizing the results. Due to
 * javafx not fully supporting this feature charting is disabled for the
 * {@link #display()} method.
 * <p>
 * <a href="https://www.chartjs.org/">ChartJS</a> is used to display the chart.
 * While a copy is distributed via the resource folder the class may have issues
 * locating the requested file due to the way future jars are packaged. As a
 * fallback the js may be loaded from a cdn provider.
 * 
 * <p>
 * For full functionality it is advice to only use the normalized hamming
 * distance
 * 
 * 
 * @author Kilian
 * @since 2.0.0
 */
public class AlgorithmBenchmarker {

	private static final Logger LOGGER = Logger.getLogger(AlgorithmBenchmarker.class.getName());

	/** HTML base template */
	private static String htmlBase = buildHtmlBase();

	/** Format used to cut off decimal numbers */
	private DecimalFormat df = new DecimalFormat("0.000");

	/** Shall a speed benchmark be added to the report **/
	private boolean timming;

	// Charting

	/** The number of buckets to sort the distance into for charting */
	private int buckets;

	/** Stepped or linear interpolated chart? */
	private boolean stepped;

	// Internal state

	/** The image matcher holding individual hashing algorithms and settings */
	private SingleImageMatcher imageMatcher;

	/** The images to test */
	private List<LabeledImage> imagesToTest = new ArrayList<>();

	/** Algorithms extracted from the image matcher */
	private Map<HashingAlgorithm, AlgoSettings> algorithmsToTest;

	/**
	 * Construct an algorithm benchmarker with default settings for charting.
	 * 
	 * @param imageMatcher   the image matcher object holding all hashing algorithms
	 *                       which will be tested
	 * @param speedBenchmark if true a speed benchmark will be added to the table.
	 *                       The speed benchmark tries to compute the time a single
	 *                       hashing operation (without reading the image from disk)
	 *                       takes. Be aware that micro benchmarks are hard to
	 *                       design and if a true representation is required more
	 *                       suited libraries like Oracle's JMH should be used
	 *                       instead.
	 *                       <p>
	 *                       This benchmark either does 600 passes over every
	 *                       supplied images per algorithm or allows for a total
	 *                       computation time of 90 seconds. Whichever takes less.
	 */
	public AlgorithmBenchmarker(SingleImageMatcher imageMatcher, boolean speedBenchmark) {
		this.timming = speedBenchmark;
		this.imageMatcher = imageMatcher;
		this.buckets = 10;
		this.stepped = true;
	}

	/**
	 * Construct an algorithm benchmarker
	 * 
	 * @param imageMatcher   the image matcher object holding all hashing algorithms
	 *                       which will be tested
	 * @param speedBenchmark if true a speed benchmark will be added to the table.
	 *                       The speed benchmark tries to compute the time a single
	 *                       hashing operation (without reading the image from disk)
	 *                       takes. Be aware that micro benchmarks are hard to
	 *                       design and if a true representation is required more
	 *                       suited libraries like Oracle's JMH should be used
	 *                       instead.
	 *                       <p>
	 *                       This benchmark either does 600 passes over every
	 *                       supplied images per algorithm or allows for a total
	 *                       computation time of 90 seconds. Whichever takes less.
	 * @param bucket         the number of histogram buckets the hash distances will
	 *                       be sorted into. Default: 10. A higher number gives a
	 *                       more detailed view on the individual distances.
	 * @param stepped        Weather to use stepped chart to visualize the buckets
	 *                       or to use an interpolated bezier curve. Curves may look
	 *                       more esthetic but are not as accurate as the stepped
	 *                       version.
	 */
	public AlgorithmBenchmarker(SingleImageMatcher imageMatcher, boolean speedBenchmark, int bucket, boolean stepped) {
		this.timming = speedBenchmark;
		this.imageMatcher = imageMatcher;
		this.buckets = bucket;
		this.stepped = stepped;
	}

	/**
	 * Add labeled test images.
	 * 
	 * <p>
	 * Images which are expected to be matched should carry the same group id.
	 * 
	 * @param testImages the images which will be tested
	 */
	public void addTestImages(LabeledImage... testImages) {
		for (LabeledImage t : testImages) {
			imagesToTest.add(t);
		}
	}

	/**
	 * Compute the benchmark and output it's content as HTML to the console. Every
	 * call of this method recomputes the benchmark.
	 * 
	 * <p>
	 * Charting is enabled for this output mode.
	 */
	public void toConsole() {
		System.out.println(constructHTML(true));
	}

	/**
	 * Compute the benchmark and save the content to an HTML file located at the
	 * base of this project with the name Benchmark_TimeInMS.html Every call of this
	 * method recomputes the benchmark and creates a new file.
	 * 
	 * <p>
	 * Charting is enabled for this output mode.
	 * 
	 */
	public void toFile() {
		toFile(new File("Benchmark_" + System.currentTimeMillis() + ".html"));
	}

	/**
	 * /** Compute the benchmark and save it to file. The supplied file will be
	 * overwritten and contain the benchmark in html format. Every call of this
	 * method recomputes the benchmark.
	 * 
	 * <p>
	 * Charting is enabled for this output mode.
	 * 
	 * @param outputFile The file the content will be saved to
	 */
	public void toFile(File outputFile) {
		String output = constructHTML(true);
		try (FileWriter fw = new FileWriter(outputFile)) {
			fw.write(output);
			LOGGER.info("HTML File Created: " + outputFile.getAbsolutePath());
		} catch (IOException e) {
			LOGGER.severe("Can't create benchmark file: " + e.getCause());
			// e.printStackTrace();
			// output to console if we can't print to file
			System.out.println(output);
		}
	}

	/**
	 * Compute the benchmark and display the content in a JavaFX window. Every call
	 * of this method recomputes the benchmark and creates a new file.
	 * 
	 * <p>
	 * Due to JavaFXs webview not being able to handle javascript properly no charts
	 * will be displayed in the report
	 */
	public void display() {
		String html = constructHTML(false);
		new Thread(() -> {
			// As long as https://bugs.openjdk.java.net/browse/JDK-8090933 isn't fixed we
			// have to use this construct
			try {
				Application.launch(BenchmarkApplication.class, html);
			} catch (IllegalStateException state) {
				// JavaFX already running
				new BenchmarkApplication().spawnWindow(html);
			}
		}, "Display Algorithm Benchmark").start();
	}

	/**
	 * Construct the of this benchmark
	 * 
	 * @param initChart if charting should be attempted
	 * @return the benchmark report as html document
	 */
	protected String constructHTML(boolean initChart) {
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

		/* Keep track of every produced distance if we want to chart it */
		Map<HashingAlgorithm, List<Double>[]> scatterMap = new HashMap<>();

		// Init Map
		for (HashingAlgorithm h : algorithmsToTest.keySet()) {
			DoubleSummaryStatistics[] stats = new DoubleSummaryStatistics[6];
			ArrayUtil.fillArrayMulti(stats, () -> {
				return new DoubleSummaryStatistics();
			});
			statMap.put(h, stats);

			if (initChart) {
				@SuppressWarnings("unchecked")
				List<Double>[] l = new List[2];
				l[0] = new ArrayList<>();
				l[1] = new ArrayList<>();
				scatterMap.put(h, l);
			}
		}

		// 0. Compute all hashes for each image and hashing algorithm
		Map<HashingAlgorithm, Map<LabeledImage, Hash>> hashes = new HashMap<>();
		for (HashingAlgorithm h : algorithmsToTest.keySet()) {
			HashMap<LabeledImage, Hash> algorithmSpecificHash = new HashMap<>();
			hashes.put(h, algorithmSpecificHash);
			for (LabeledImage t : imagesToTest) {
				algorithmSpecificHash.put(t, h.hash(t.getbImage()));
			}
		}

		// Header
		StringBuilderI htmlBuilder = new StringBuilderI();

		htmlBuilder.row("<div>");

		// Table header
		appendHeader(htmlBuilder);

		// Algorithm bit resolution and algo settings
		appendAlgorithmInformation(htmlBuilder);

		// Append distance section of each image / algorithm
		appendHashingDistances(htmlBuilder, hashes, statMap, scatterMap, initChart);

		// Append statistics section
		appendStatistics(htmlBuilder, statMap);

		// Run benchmark and append it to the table
		if (timming) {
			appendTimmingBenchmark(htmlBuilder);
		}

		// Finalize
		htmlBuilder.append("</tbody></table>");

		// Additional information?
		if (initChart) {
			appendChartSection(htmlBuilder, scatterMap, statMap);
		}

		// Construct final html
		return htmlBase.replace("$body", htmlBuilder.append("</div>").toString());
	}

	/**
	 * Construct the header of the table and append it to the html builder
	 * 
	 * @param htmlBuilder the builder used to construct the output
	 */
	protected void appendHeader(StringBuilderI htmlBuilder) {
		htmlBuilder
				.append("<table id='rootTable'>\n").append("<thead><tr> <th>Images</th> <th>Category</th> <th colspan="
						+ algorithmsToTest.size() + ">Distance</th> </tr></thead>\n")
				.append("<tbody><tr><td colspan = 2></td>");

		// KeySet call is cached in hashmap
		for (HashingAlgorithm h : algorithmsToTest.keySet()) {
			htmlBuilder.append("<td id='" + h.algorithmId() + "'>" + h.toString() + "</td>");
		}

		htmlBuilder.append("</tr>\n");
	}

	/**
	 * Append meta information to the table and append it to the html builder
	 * 
	 * @param htmlBuilder the builder used to construct the output
	 */
	protected void appendAlgorithmInformation(StringBuilderI htmlBuilder) {
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
		emptyTableRow(htmlBuilder);
	}

	/**
	 * Append the hashing distances between the individual images for each algorithm
	 * 
	 * @param htmlBuilder the builder used to construct the output
	 * @param statMap     map to store the distances in
	 * @param hashes      the precomputed hashes
	 * @param initChart   should the chart be appeneded. (e.g. javafx does is not
	 *                    able to display the chart)
	 * @param scatterMap  map with all distance data points sorted by algorithm
	 */
	protected void appendHashingDistances(StringBuilderI htmlBuilder, Map<HashingAlgorithm, Map<LabeledImage, Hash>> hashes,
			Map<HashingAlgorithm, DoubleSummaryStatistics[]> statMap, Map<HashingAlgorithm, List<Double>[]> scatterMap,
			boolean initChart) {
		int lastCategory = 0;

		List<LabeledImage> sortedKeys = new ArrayList<>(imagesToTest);

		for (LabeledImage base : imagesToTest) {
			// Compute the distance for all crosses we have not looked at.
			// It's symmetric no need to check it twice
			sortedKeys.remove(base);

			if (base.getCategory() != lastCategory) {
				lastCategory = base.getCategory();
				emptyTableRow(htmlBuilder);
			}

			for (LabeledImage cross : sortedKeys) {
				htmlBuilder.append("<tr>");
				boolean first = true;
				for (Entry<HashingAlgorithm, AlgoSettings> entry : algorithmsToTest.entrySet()) {

					HashingAlgorithm h = entry.getKey();
					AlgoSettings algoSettings = entry.getValue();

					boolean supposedToMatch = base.getCategory() == cross.getCategory();

					Hash baseHash = hashes.get(h).get(base);
					Hash crossHash = hashes.get(h).get(cross);

					double distance;

					if (algoSettings.isNormalized()) {
						distance = baseHash.normalizedHammingDistance(crossHash);
					} else {
						distance = baseHash.hammingDistance(crossHash);
					}

					// TODO normal distance for distancemap
					if (initChart) {
						scatterMap.get(h)[supposedToMatch ? 0 : 1].add(distance);
					}

					boolean consideredMatch = algoSettings.getThreshold() >= distance;

					String backgroundColor = "";
					if (consideredMatch) {
						if (supposedToMatch) {
							statMap.get(h)[2].accept(1);
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
						}
					}

					if (first) {
						htmlBuilder.append("<td>").append(base.getName()).append("-").append(cross.getName())
								.append("</td><td class='category'>").append("[").append(base.getCategory()).append("-")
								.append(cross.getCategory()).append("]</td>");
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
		emptyTableRow(htmlBuilder);
	}

	/**
	 * Append statistics regarding the hashing algorithms to the table
	 * 
	 * @param htmlBuilder      The stringbuilder to append the output to
	 * @param statMap          map holding statistics
	 */
	protected void appendStatistics(StringBuilderI htmlBuilder,
			Map<HashingAlgorithm, DoubleSummaryStatistics[]> statMap) {

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
					htmlBuilder.append("<td>");
					if (truePositive > 0 || falsePositive > 0) {
						htmlBuilder.append(df.format(truePositive / (double) (truePositive + falsePositive)));
					} else {
						htmlBuilder.append("-");
					}
					htmlBuilder.append("</td>");
				}
			}
			if (i == 4) {
				i++;
			}

			htmlBuilder.append("</tr>\n");
		}
	}

	/**
	 * Append an empty table row to the table. Used as spacer.
	 * @param builder The stringbuilder to append the output tos
	 */
	private void emptyTableRow(StringBuilderI builder) {
		builder.append("<tr class='spacerRow'><td colspan='2'></td><td colspan='").append(algorithmsToTest.size())
				.row("'></td></tr>");
	}

	//
	/**
	 * Pseudo micro benchmark
	 * 
	 * @param htmlBuilder The stringbuilder to append the output to
	 * @return dummy value. Sum of bits of all calculated hashes
	 */
	protected long appendTimmingBenchmark(StringBuilderI htmlBuilder) {
		// Avoid dead code elimination
		long sum = 0;

		// Keep track of the average time;
		Map<HashingAlgorithm, Double> averageRuntime = new HashMap<>(algorithmsToTest.size());
		/*
		 * Keep track of the actual loop count performed for each algorithm. May be
		 * smaller due to time cutoff
		 */
		Map<HashingAlgorithm, Integer> actualLoops = new HashMap<>();

		// Init
		for (HashingAlgorithm hasher : algorithmsToTest.keySet()) {
			actualLoops.put(hasher, 0);
		}

		// Perform a few warmup cycles
		sum += performWarmup();

		/*
		 * Perform benchmark
		 */
		sum += performBenchmark(averageRuntime, actualLoops);

		/*
		 * Compute average
		 */
		for (HashingAlgorithm hasher : algorithmsToTest.keySet()) {
			double avg = averageRuntime.get(hasher) / actualLoops.get(hasher);
			averageRuntime.put(hasher, avg);
		}

		/*
		 * Build report
		 */
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
	 * Performs a few warmup cycles. During benchmarking this is usually done to get
	 * the JIT to do it's magic. We don't have much time during report generation.
	 * The loop count is tiny and may not accomplish the desired effect.
	 * 
	 * @return dummy value
	 */
	private long performWarmup() {
		// In seconds
		long warmUpCutoff = (5 * (long) 1e9);

		long sum = 0;

		for (HashingAlgorithm hasher : algorithmsToTest.keySet()) {
			long start = System.nanoTime();
			for (int i = 0; i < 100; i++) {
				for (LabeledImage testData : imagesToTest) {
					sum += hasher.hash(testData.getbImage()).getHashValue().bitCount();
				}
				if (System.nanoTime() - start > warmUpCutoff) {
					LOGGER.info("warmup cutoff surpassed.");
					return sum;
				}
			}
		}
		return sum;
	}

	/**
	 * Perform the actual benchmark
	 * 
	 * @param averageRuntime map saving the overall runtime for this algorithm
	 * @param actualLoops    the actual loops performed. Used to calculate average
	 *                       valuzes
	 * @return dummy value
	 */
	private long performBenchmark(Map<HashingAlgorithm, Double> averageRuntime,
			Map<HashingAlgorithm, Integer> actualLoops) {
		// Testing
		int loops = 500;
		long testCutoff = (60 * (long) 1e9);

		long start = System.nanoTime();
		/*
		 * Try to counter dead code elimination. Should not occur with so few loops but
		 * better be save ...
		 */
		long sum = 0;

		// Randomize order by looping throught the algorithms one at a time.
		for (int i = 0; i < loops; i++) {
			for (HashingAlgorithm hasher : algorithmsToTest.keySet()) {

				long startIndividual = System.nanoTime();

				for (LabeledImage testData : imagesToTest) {
					sum += hasher.hash(testData.getbImage()).getHashValue().bitCount();
				}
				double elapsed = (((System.nanoTime() - startIndividual))) / (double) 1e6 / imagesToTest.size();
				averageRuntime.merge(hasher, elapsed, (old, newVal) -> {
					return old + newVal;
				});
				actualLoops.merge(hasher, 1, (old, newVal) -> {
					return old + newVal;
				});

				if (System.nanoTime() - start > testCutoff) {
					LOGGER.info("test cutoff surpassed. finish with: " + i + " loops executed");
					return sum;
				}
			}
		}
		return sum;
	}

	protected void appendChartSection(StringBuilderI htmlBuilder, Map<HashingAlgorithm, List<Double>[]> scatterMap,
			Map<HashingAlgorithm, DoubleSummaryStatistics[]> statMap) {

		// Inject javascript charting library

		boolean success = false;

		URL jScript = AlgorithmBenchmarker.class.getClassLoader().getResource("Chart.bundle.min.js");
		if (jScript != null) {
			File javascriptScript = new File(jScript.getFile());
			htmlBuilder.append("<script src=\"").append(javascriptScript.getAbsolutePath()).append("\"></script>");
			success = true;
		} else {
			// Fallback attempt cdn via internet
			LOGGER.info("Could not find js file. fallback to cdn");
			try {
				String cdn = "https://cdnjs.cloudflare.com/ajax/libs/Chart.js/2.7.3/Chart.bundle.min.js";
				HttpURLConnection connection = (HttpURLConnection) new URL(cdn).openConnection();
				connection.setRequestMethod("HEAD");
				if (connection.getResponseCode() == 200) {
					success = true;
					htmlBuilder.append("<script src=\"").append(cdn).append("\"/></script>");
				}
			} catch (IOException e) {
				/* We handle exception via success var */}
		}

		if (success) {
			// Build data js objects for chart

			// Do some clustering

			StringBuilderI positiveDatBuilder = new StringBuilderI();
			StringBuilderI negativeDatBuilder = new StringBuilderI();
			StringBuilderI scatterDatBuilder = new StringBuilderI();
			StringBuilderI centeroidBuilder = new StringBuilderI();

			positiveDatBuilder.row("var dataDictMatch = {};");
			negativeDatBuilder.row("var dataDictDistinct = {};");
			scatterDatBuilder.row("var dataDictScatter = {};");
			centeroidBuilder.row("var dataDictCenter = {};");
			// Create data dictionary
			for (Entry<HashingAlgorithm, List<Double>[]> e : scatterMap.entrySet()) {
				HashingAlgorithm hasher = e.getKey();
				List<Double>[] occurances = e.getValue();

				double[][] data = new double[occurances[0].size() + occurances[1].size()][1];

				for (int i = 0; i < occurances[0].size(); i++) {
					data[i][0] = occurances[0].get(i);
				}
				for (int i = 0; i < occurances[1].size(); i++) {
					data[i + occurances[0].size()][0] = occurances[1].get(i);
				}

				// Compute centeroid

				// Compute buckets
				int[] matchBucket = new int[buckets];
				int[] distinctBucket = new int[buckets];

				// Two lines
				// TODO calculate the number of images we can not correctly predict no matter
				// where the threshold is. ROC curve as it's done in the forest image matcher

				// Average between centers.
				double avg = (statMap.get(hasher)[0].getAverage() + statMap.get(hasher)[1].getAverage()) / 2;

				// Average between max match and min distinct

				scatterDatBuilder.append("dataDictScatter['").append(hasher.algorithmId()).append("']=[");
				centeroidBuilder.append("dataDictCenter['").append(hasher.algorithmId()).append("']=[").append(avg)
						.row("];");

				int dataPoints = occurances[0].size();
				for (int i = 0; i < dataPoints; i++) {

					matchBucket[(int) (occurances[0].get(i) * buckets) + (stepped ? 0 : 1)]++;
					scatterDatBuilder.append("{x:").append(occurances[0].get(i)).append(",y:").append(i / 2d)
							.append("},");
				}

				dataPoints = occurances[1].size();
				if (dataPoints > 0) {
					scatterDatBuilder.append(",");
				}
				for (int i = 0; i < dataPoints; i++) {

					distinctBucket[(int) (occurances[1].get(i) * buckets) + (stepped ? 0 : 1)]++;
					scatterDatBuilder.append("{x:").append(occurances[1].get(i)).append(",y:").append(i / 2d)
							.append("}");
					if (i != dataPoints - 1) {
						scatterDatBuilder.append(",");
					}
				}
				scatterDatBuilder.append("];\n");

				positiveDatBuilder.append("dataDictMatch['").append(hasher.algorithmId()).append("']=[");
				negativeDatBuilder.append("dataDictDistinct['").append(hasher.algorithmId()).append("']=[");

				for (int i = 0; i < buckets; i++) {
					positiveDatBuilder.append("{x:").append(i / (double) buckets).append(",y:").append(matchBucket[i])
							.append("}");
					negativeDatBuilder.append("{x:").append(i / (double) buckets).append(",y:")
							.append(distinctBucket[i]).append("}");

					if (i != buckets - 1) {
						positiveDatBuilder.append(",");
						negativeDatBuilder.append(",");
					}
				}

				positiveDatBuilder.row("];");
				negativeDatBuilder.row("];");
			}

			htmlBuilder.row("<div style='width:40%; margin:auto;'>")
					.row("<canvas id ='chartCanvas' width='600' height='400' style='background-color:' />")
					.row("</div>");
			//@formatter:off 
			//append javascript to initialize chart data
			htmlBuilder.row("<script>")
			.append(positiveDatBuilder.toString())
			.append(negativeDatBuilder.toString())
			.append(scatterDatBuilder.toString())
			.append(centeroidBuilder.toString())
			.row(" Chart.defaults.global.defaultFontColor='white';")
			.row("var canvas = document.getElementById('chartCanvas').getContext('2d')")
			.row("var chart = new Chart(canvas, {")
			.row("	data: {\n")
			//.append("	labels: [0.0,'0.1', '0.2', '0.3', '0.4', '0.5', '0.6', '0.7', '0.8', '0.9','1.0'],\n")
			.row("		datasets: [{\n")
			.row("				label: 'points',")
			.row("				type: 'scatter',")
			.row("				fill: false,")
			.row("				showLine: false,")
			.row("				pointRadius: 5,")
			.row("				backgroundColor: 'orange'")
			.row("			},")
			.row("			{")
			.row("				type: 'line',")
			.row("				label: 'match',")
			.row("				steppedLine: "+stepped+",")	
			.row("				pointRadius: 0,")
			.row("				backgroundColor: 'rgba(32,178,170,0.65)'")
			.row("				},")		
			.row("			{")
			.row("				type: 'line',")
			.row("				label: 'distinct',")
			.row("				steppedLine: "+stepped+",")	
			.row("				pointRadius: 0,")
			.row("				backgroundColor: 'rgba(250,128,114,0.8)'")
			.row("			}")
			.row("		]")
			.row("	},")
			.row("	options: {")
			.row("		responsive: true,")
			.row("		scales: {")
			.row("				yAxes: [{") 
			.row("					gridLines: {")
			.row("						color: 'white'") 
			.row("					},")
			.row("					scaleLabel: {")
			.row("						display: true,")
			.row("						labelString: 'Occurances',")
			.row("					}")
			.row("				}],")
			.row("				xAxes: [{") 
			.row("					type:'linear',")			
			.row("					gridLines: {") 
			.row("						color: 'white'")
			.row("					},")
			.row("					ticks: {")
			.row("						min:0,")
			.row("						max:1,")
			.row("						stepSize:0.1")
			.row("					},")
			.row("					scaleLabel: {")
			.row("						display: true,")
			.row("						labelString: 'Distance',")
			.row("					}")
			.row("				}]")
			.row("			},")
			.row("		title:{")
			.row("			display:true,")
			.row("			text: 'Test'")
			.row("		}")
			.row("	}")
			.row("})")
			//Register javascript callback
			//tiny bit javascript magic
			
			.row("var table = document.getElementById('rootTable');")
			.row("var lastIndex = -1;")
			.row("var lastObject = undefined;")
			.row("table.addEventListener('mousemove',function(e){")
			.row("	var parentTr = e.path[1];")
			.row("	var target = e.path[0];")
			.row("	if(target == lastObject){")
			.row("		return;" )
			.row("	}")
			.row("	lastObject = target;")
			.row("	var correctCellIndex = 0;")
			.row("	//Fix: Compute correct cellindex due to colspans")
			.row("	for(let cell of parentTr.cells){")
			.row(" 		if(cell == target){")
			.row("			break;")
			.row("		}else{")
			.row("			correctCellIndex += cell.colSpan;")
			.row("		}")
			.row("	}")
			.row("	if(correctCellIndex != lastIndex) {")
			.row("		lastIndex = correctCellIndex;")
			.row("		//First tr in tbody with cellIndex - 1")
			.row("		var id = table.rows[1].cells[correctCellIndex-1].id")
			.row("		if(id === undefined){")
			.row("			return;")
			.row("		}\n")
			.row("		//Repopulate table\n")
			.row("		chart.options.title.text = table.rows[1].cells[correctCellIndex-1].innerText;\n")
			.row("		chart.data.datasets[1].data = dataDictMatch[id];")
			.row("		chart.data.datasets[2].data = dataDictDistinct[id];")
			.row("		chart.data.datasets[0].data = dataDictScatter[id];")
			.row("		chart.config.verticalMarker = dataDictCenter[id]")	
			.row("		chart.update();")
			.row("	}")
			
			//Adapted from https://stackoverflow.com/a/43092029/3244464
			.row("const verticalLinePlugin = {\r\n" + 
					"  getLinePosition: function (chart, value) {\r\n" + 
					"		return chart.scales['x-axis-0'].getPixelForValue(value);"+
					"  },\r\n" + 
					"  renderVerticalLine: function (chartInstance, xValue) {\r\n" + 
					"      const lineLeftOffset = this.getLinePosition(chartInstance, xValue);\r\n" + 
					"      const scale = chartInstance.scales['y-axis-0'];\r\n" + 
					"      const context = chartInstance.chart.ctx;\r\n" + 
					"\r\n" + 
					"      // render vertical line\r\n" + 
					"      context.beginPath();\r\n" + 
					"      context.strokeStyle = 'white';\r\n" + 
					"      context.lineWidth = 2;\n" +
					"      context.moveTo(lineLeftOffset, scale.top);\r\n" + 
					"      context.lineTo(lineLeftOffset, scale.bottom);\r\n" + 
					"      context.stroke();\r\n" + 
					"\r\n" + 
					"      // write label\r\n" + 
					//"      context.font =\"20px 'Helvetica Neue'\";\n"+		
					//"      context.fillStyle = \"white\";\r\n" + 
					//"      context.textAlign = 'center';\r\n" + 
					//"      context.fillText('Centeroid:'+xValue.toFixed(3), lineLeftOffset, (scale.bottom - scale.top) / 8 + scale.top);\r\n" + 
					"  },\r\n" + 
					"\r\n" + 
					"  afterDatasetsDraw: function (chart, easing) {\r\n" + 
					"      if (chart.config.verticalMarker) {\r\n" + 
					"          chart.config.verticalMarker.forEach(xValue => this.renderVerticalLine(chart, xValue));\r\n" + 
					"      }\r\n" + 
					"  }\r\n" + 
					"  };\r\n" + 
					"\r\n" + 
					"  Chart.plugins.register(verticalLinePlugin);")
			
			.row("});</script>");
			//@formatter:on
		} else {
			LOGGER.warning("Could not link to chartjs library. skip chart generation.");
		}
	}

	/**
	 * Construct the html frame to embed the benchmark result into
	 * 
	 * @return an html template
	 */
	private static String buildHtmlBase() {
		StringBuilderI htmlBuilder = new StringBuilderI();
		htmlBuilder.row("<!DOCTYPE html>").row("<html>").row("	<head>")
				.row("		<title>Algorithm Benchmarking</title>")
				/*
				 * CSS
				 * @formatter:off
				 */
				.row("		<style>")
				.row("			html{min-height:100%;}")
				.row("			body{min-height:100%; margin: 0; background: linear-gradient(45deg, #49a09d, #5f2c82); font-family: sans-serif; font-weight: 100; margin-bottom:25px;}")
				.row("			table{margin:auto; border-collapse: collapse; box-shadow: 0 0 20px rgba(0,0,0,0.1); margin-top:40px;}")
				.row("			thead th, td:first-child, .category{background-color: #55608f;}")
				.row("			tr:nth-child(1) {background-color:#3d54b9;}")
				.row("			th,td{padding: 10px;background-color: rgba(255,255,255,0.2);color: #fff;}")
				.row("			tbody tr:hover{background-color: rgba(255,255,255,0.3);}")
				.row("			.spacerRow td{ padding:7px;}")
				.row("			.circle{width:10px; height: 10px; background-color:green; border-radius:50%; display:inline-block; margin-right:5px;}")
				.row("			.fault{color:#fff700;}")
				.row("		</style>")
				.row("	</head>")
				.row("	<body>")
				.row("		$body")
				.row("	</body>")
				.row("</html>");
		return htmlBuilder.toString();
	}

	/**
	 * Utility StringBuilder to allow formating text
	 * 
	 * @author Kilian
	 * @since 2.0.0
	 */
	private static class StringBuilderI {
		private StringBuilder internal = new StringBuilder();
	
		public StringBuilderI append(String s) {
			internal.append(s);
			return this;
		}
		
		/**
		 * @param double1
		 * @return
		 */
		public StringBuilderI append(double d) {
			internal.append(d);
			return this;
		}

		public StringBuilderI append(long l) {
			internal.append(l);
			return this;
		}

		public StringBuilderI append(int i) {
			internal.append(i);
			return this;
		}

		public StringBuilderI row(String s) {
			internal.append(s).append("\n");
			return this;
		}
		
		public String toString() {
			return internal.toString();
		}
	}

	/**
	 * 
	 * @author Kilian
	 * @since 2.0.0
	 */
	public static class BenchmarkApplication extends Application {

		@Override
		public void start(Stage primaryStage) throws Exception {
			// Main entry point if javafx is not already started
			spawnNewWindow(primaryStage, getParameters().getRaw().get(0));
		}

		/**
		 * Spawn subsequent windows if Javafx is already spawned. Application.launch can
		 * only be called once, therefore a work around is needed.
		 * 
		 * @param html to display
		 */
		public void spawnWindow(String html) {
			Platform.runLater(() -> {
				spawnNewWindow(new Stage(), html);
			});
		}

		/**
		 * Spawn a full screen windows with a webview displaying the html content
		 * 
		 * @param stage       of the window
		 * @param htmlContent the content to display
		 */
		private void spawnNewWindow(Stage stage, String htmlContent) {

			WebView webView = new WebView();
			webView.getEngine().loadContent(htmlContent);

			// Fullscreen

			Rectangle2D screen = Screen.getPrimary().getVisualBounds();

			double w = screen.getWidth();
			double h = screen.getHeight();
			Scene scene = new Scene(webView, w, h);
			stage.setTitle("Image Hash Benchmarker");
			stage.getIcons().add(new Image("imageHashLogo.png"));
			stage.setScene(scene);
			stage.show();
		}
	}

}
