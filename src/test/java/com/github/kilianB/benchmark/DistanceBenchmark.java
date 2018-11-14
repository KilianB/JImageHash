package com.github.kilianB.benchmark;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.DoubleSummaryStatistics;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.BeforeAll;

import com.github.kilianB.hashAlgorithms.AverageHash;
import com.github.kilianB.hashAlgorithms.DifferenceHash;
import com.github.kilianB.hashAlgorithms.DifferenceHash.Precision;
import com.github.kilianB.hashAlgorithms.HashingAlgorithm;
import com.github.kilianB.hashAlgorithms.HogHash;
import com.github.kilianB.hashAlgorithms.PerceptiveHash;
import com.github.kilianB.matcher.Hash;

/**
 * @author Kilian
 *
 */
public class DistanceBenchmark {

	// Test suit to check which hashing algorithm works for your data

	// Recall
	// Accuracy
	// Precision

	private static BufferedImage ballon;
	// Similar images
	private static BufferedImage copyright;
	private static BufferedImage highQuality;
	private static BufferedImage lowQuality;
	private static BufferedImage thumbnail;
	private static BufferedImage lena;

	public static void main(String[] args) {
		loadImages();
	}

	@BeforeAll
	static void loadImages() {

		DecimalFormat df = new DecimalFormat("0.000");

		List<TestData> imagesToTest = new ArrayList<>();

		imagesToTest.add(new TestData(0, new File("src/test/resources/ballon.jpg")));
		imagesToTest.add(new TestData(1, new File("src/test/resources/copyright.jpg")));
		imagesToTest.add(new TestData(1, new File("src/test/resources/highQuality.jpg")));
		imagesToTest.add(new TestData(1, new File("src/test/resources/lowQuality.jpg")));
		imagesToTest.add(new TestData(1, new File("src/test/resources/thumbnail.jpg")));
		// imagesToTest.add(new TestData(2, new File("src/test/resources/Lenna.png")));

		List<HashingAlgorithm> algorithmsToTest = new ArrayList<>();

		algorithmsToTest.add(new AverageHash(16));

		algorithmsToTest.add(new AverageHash(64));

		algorithmsToTest.add(new PerceptiveHash(16));
		algorithmsToTest.add(new PerceptiveHash(64));
		algorithmsToTest.add(new DifferenceHash(16, Precision.Simple));
		algorithmsToTest.add(new DifferenceHash(48, Precision.Simple));
		algorithmsToTest.add(new DifferenceHash(16, Precision.Triple));
		algorithmsToTest.add(new HogHash(16));

		try {
			System.out.println(new HogHash(16).hash(new File("src/test/resources/lowQuality.jpg")));
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		
		// 0. Compute all hashes for each image and hashing algorithm
		Map<HashingAlgorithm, Map<TestData, Hash>> hashes = new HashMap<>();
		for (HashingAlgorithm h : algorithmsToTest) {
			HashMap<TestData, Hash> algorithmSpecificHash = new HashMap<>();
			hashes.put(h, algorithmSpecificHash);
			for (TestData t : imagesToTest) {
				algorithmSpecificHash.put(t, h.hash(t.bImage));
			}
			}

		// 1. Compute the distances between each hash

		StringBuilder htmlBuilder = new StringBuilder();

		htmlBuilder.append("<table>\n").append("<tr> <td>Images</td> <td>Match</td> <td colspan="
				+ algorithmsToTest.size() + ">Normalized Distance</td> </tr>\n").append("<tr><td colspan = 2></td>");

		Map<HashingAlgorithm, DoubleSummaryStatistics[]> statMap = new HashMap<>();

		for (HashingAlgorithm h : algorithmsToTest) {
			htmlBuilder.append("<td>" + h.toString() + "</td>");

			DoubleSummaryStatistics[] stats = { new DoubleSummaryStatistics(), new DoubleSummaryStatistics() };

			statMap.put(h, stats);
		}

		htmlBuilder.append("</tr>\n");

		List<TestData> sortedKeys = new ArrayList(imagesToTest);

		Collections.sort(sortedKeys);

		int lastCategory = 0;
		for (TestData base : imagesToTest) {
			// Compute the distance for all crosses we have not looked at.
			// It's symmetric no need to check it twice
			sortedKeys.remove(base);

			if (base.category != lastCategory) {
				lastCategory = base.category;
				htmlBuilder.append("<hl>");
			}

			for (TestData cross : sortedKeys) {
				htmlBuilder.append("<tr>");
				boolean first = true;
				for (HashingAlgorithm h : algorithmsToTest) {
					boolean match = base.category == cross.category;
					double distance = hashes.get(h).get(base).normalizedHammingDistance(hashes.get(h).get(cross));
					if (first) {
						htmlBuilder.append("<td>").append(base.name).append("-").append(cross.name).append("</td>")
								.append("<td>").append(match).append("</td>");
						first = false;
					}
					htmlBuilder.append("<td>").append(df.format(distance)).append("</td>");
					statMap.get(h)[match ? 0 : 1].accept(distance);
				}
				htmlBuilder.append("</tr>\n");
			}
		}

		for (int i = 0; i < 4; i++) {

			htmlBuilder.append("<tr>");

			if (i == 0) {
				htmlBuilder.append("<td colspan = 2>").append("<b>Avg match:</b>").append("</td>");
			} else if (i == 1) {
				htmlBuilder.append("<td colspan = 2>").append("<b>Avg distinct:</b>").append("</td>");
			}else if (i == 2) {
				htmlBuilder.append("<td colspan = 2>").append("<b>Min/Max match:</b>").append("</td>");
			} else if (i == 3) {
				htmlBuilder.append("<td colspan = 2>").append("<b>Min/Max distinct:</b>").append("</td>");
			}

			for (HashingAlgorithm h : algorithmsToTest) {
				if(i < 2) {
					htmlBuilder.append("<td>").append(df.format(statMap.get(h)[i].getAverage())).append("</td>");
				}else {
					DoubleSummaryStatistics dSum = statMap.get(h)[i-2];
					htmlBuilder.append("<td>").append(df.format(dSum.getMin())).append("/").append(df.format(dSum.getMax())).append("</td>");
				}
				
			}

			htmlBuilder.append("</tr>\n");
		}

		htmlBuilder.append("</table>");

		System.out.println(htmlBuilder.toString());

	}

	static class TestData implements Comparable<TestData> {

		String name;
		int category;
		BufferedImage bImage;

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

}
