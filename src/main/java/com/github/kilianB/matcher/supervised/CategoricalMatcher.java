package com.github.kilianB.matcher.supervised;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;

import javax.imageio.ImageIO;

import com.github.kilianB.ArrayUtil;
import com.github.kilianB.datastructures.Pair;
import com.github.kilianB.graphics.ColorUtil;
import com.github.kilianB.graphics.FastPixel;
import com.github.kilianB.hashAlgorithms.AverageHash;
import com.github.kilianB.hashAlgorithms.HashingAlgorithm;
import com.github.kilianB.matcher.Hash;
import com.github.kilianB.matcher.ImageMatcher;

import javafx.scene.paint.Color;

/**
 * 
 * Computes the distance to the average hash of each category.
 * 
 * Be aware that the order in which images are added matters ... since the
 * average hash of a cluster is constantly updates without re checking old
 * entries.
 * 
 * @author Kilian
 *
 */
public class CategoricalMatcher extends ImageMatcher {

	public static void main(String[] args) throws IOException {
		CategoricalMatcher matcher = new CategoricalMatcher(true);

		BufferedImage highQuality = ImageIO.read(new File("src/test/resources/highQuality.jpg"));
		BufferedImage highQualityBright = ImageIO.read(new File("src/test/resources/highQualityBright.png"));
		BufferedImage lowQuality = ImageIO.read(new File("src/test/resources/lowQuality.jpg"));
		BufferedImage thumbnail = ImageIO.read(new File("src/test/resources/thumbnail.jpg"));
		BufferedImage copyright = ImageIO.read(new File("src/test/resources/copyright.jpg"));

		BufferedImage ballon = ImageIO.read(new File("src/test/resources/ballon.jpg"));

		// BufferedImage bi = ImageIO.read(new File("src/test/resources/ballon.jpg"));

		HashingAlgorithm hasher = new AverageHash(18000);

		ImageIO.write(hasher.hash(highQuality).toImage(10), "png", new File("PlainHash.png"));

		matcher.addHashingAlgorithm(hasher, 1f);

		//System.out.println(matcher.addCategoricalImage(highQualityBright, 0));

		System.out.println(matcher.categorizeImageAndAdd(highQuality, 0.2d));
		System.out.println(matcher.categorizeImageAndAdd(ballon, 0.2d));
		System.out.println(matcher.categorizeImageAndAdd(lowQuality, 0.2d));
		System.out.println(matcher.categorizeImageAndAdd(highQualityBright, 0.2d));
		System.out.println(matcher.categorizeImageAndAdd(thumbnail, 0.2d));
		System.out.println(matcher.categorizeImageAndAdd(copyright, 0.2d));
		
//		System.out.println(matcher.addCategoricalImage(highQuality, 0));
//		System.out.println(matcher.addCategoricalImage(highQualityBright, 0));
//		System.out.println(matcher.addCategoricalImage(lowQuality, 0));
//		System.out.println(matcher.addCategoricalImage(thumbnail, 0));
//		System.out.println(matcher.addCategoricalImage(copyright, 0));
////
//		System.out.println(matcher.addCategoricalImage(ballon, 1));

		// matcher.addCategoricalImage(ballon, 1);

		ImageIO.write(matcher.categoricalHashToImage(hasher, 0, 10), "png", new File("HashColor.png"));
//		ImageIO.write(matcher.categoricalHashToImage(hasher, 1, 10), "png", new File("HashBallon.png"));
	}

	// per hashing algorithm / per category / per bit / count
	protected Map<HashingAlgorithm, Map<Integer, int[][]>> averageBits = new HashMap<>();
	protected Map<HashingAlgorithm, Map<Integer, Hash>> averageHash = new HashMap<>();

	TreeSet<Integer> categories = new TreeSet<>();

	boolean weightedDistance;

	public CategoricalMatcher(boolean weightedDistanceClassification) {
		weightedDistance = weightedDistanceClassification;
	}

	public void addCategoricalImages(Collection<LabeledImage> images) {
		addCategoricalImages(images.toArray(new LabeledImage[images.size()]));
	}

	public void addCategoricalImages(LabeledImage... images) {
		for (LabeledImage lImage : images) {
			addCategoricalImage(lImage);
		}
	}

	public double addCategoricalImage(LabeledImage labeledImage) {
		return addCategoricalImage(labeledImage.getbImage(), labeledImage.getCategory());
	}

	public double addCategoricalImage(BufferedImage bi, int category) {
		double averageDistance = 0;
		for (HashingAlgorithm hashAlgorithm : this.steps.keySet()) {
			Hash createdHash = hashAlgorithm.hash(bi);

			// lazily initialize
			boolean dirty = false;

			Map<Integer, int[][]> categoryMap;
			if (averageBits.containsKey(hashAlgorithm)) {
				categoryMap = averageBits.get(hashAlgorithm);
			} else {
				categoryMap = new HashMap<>();
				averageBits.put(hashAlgorithm, categoryMap);
				dirty = true;
			}

			if (!categoryMap.containsKey(category)) {
				categoryMap.put(category, new int[createdHash.getBitResolution()][2]);
				categories.add(category);
				dirty = true;
			}
			int[][] bitCount = categoryMap.get(category);

			for (int bit = createdHash.getBitResolution() - 1; bit >= 0; bit--) {

				int testBit = createdHash.getBit(bit) ? 1 : 0;
				// System.out.print(bit + ":" + testBit + ",");
				boolean pref = bitCount[bit][0] >= bitCount[bit][1];
				bitCount[bit][testBit]++;
				// dirty
				if (pref != bitCount[bit][0] >= bitCount[bit][1]) {
					dirty = true;
				}
			}

			// Update
			if (dirty) {
				if (!averageHash.containsKey(hashAlgorithm)) {
					averageHash.put(hashAlgorithm, new HashMap<>());
				}
				Map<Integer, Hash> aHash = averageHash.get(hashAlgorithm);
				aHash.put(category, createAverageHash(hashAlgorithm, category));
			}
			// TODO
			Hash categoricalHash = averageHash.get(hashAlgorithm).get(category);
			averageDistance += averageHash.get(hashAlgorithm).get(category).normalizedHammingDistanceFast(createdHash);
		}
		// TODO return normalized distance to the current average hash
		return averageDistance / this.steps.keySet().size();
	}

	/**
	 * Compute the category of the supplied image.
	 * 
	 * <p>
	 * TODO desribe what a category is
	 * 
	 * 
	 * @param bi The buffered image to categorize
	 * @return a pair whose first value returns the category and second value
	 *         returns a distance measure between the category and the supplied
	 *         image
	 */
	public Pair<Integer, Double> categorizeImage(BufferedImage bi) {
		Hash[] hashes = new Hash[this.steps.keySet().size()];
		int j = 0;
		// Compute hashes for the image
		for (HashingAlgorithm hashAlgorithm : this.steps.keySet()) {
			hashes[j] = hashAlgorithm.hash(bi);
			j++;
		}
		return categorizeImage(hashes);
	}

	protected Pair<Integer, Double> categorizeImage(Hash[] hashes) {

		double[] averageHashDistance = new double[categories.size()];

		// Categorize images based on the distance to the closest average cluster
		if (!weightedDistance) {
			int j = 0;
			for (HashingAlgorithm hashAlgorithm : this.steps.keySet()) {
				Map<Integer, Hash> categoricalAverageHash = averageHash.get(hashAlgorithm);
				for (Integer category : categories) {
					averageHashDistance[category] += categoricalAverageHash.get(category)
							.normalizedHammingDistanceFast(hashes[j]);
				}
				j++;
			}
		} else {
			// Categorize image based on the weighted distance based on bit importance
			int j = 0;
			for (HashingAlgorithm hashAlgorithm : this.steps.keySet()) {
				Map<Integer, Hash> categoricalAverageHash = averageHash.get(hashAlgorithm);
				Map<Integer, int[][]> categoricalAverageHashBit = averageBits.get(hashAlgorithm);
				for (Integer category : categories) {

					double normalizedHammingDistance = 0;

					Hash targetHash = categoricalAverageHash.get(category);
					int[][] weightBits = categoricalAverageHashBit.get(category);

					for (int bit = hashAlgorithm.getKeyResolution() - 1; bit >= 0; bit--) {

						boolean match = targetHash.getBit(bit) == hashes[j].getBit(bit);
						if (targetHash.getBit(bit)) {
							double oneWeightFactor = weightBits[bit][1]
									/ (double) (weightBits[bit][0] + weightBits[bit][1]);
							// 1
							if (match) {
								normalizedHammingDistance -= oneWeightFactor;
							} else {
								normalizedHammingDistance += oneWeightFactor;
							}

						} else {
							double zeroWeightFactor = weightBits[bit][0]
									/ (double) (weightBits[bit][0] + weightBits[bit][1]);
							// 0
							if (match) {
								normalizedHammingDistance -= zeroWeightFactor;
							} else {
								normalizedHammingDistance += zeroWeightFactor;
							}
						}
					}
					averageHashDistance[category] = normalizedHammingDistance;
				}
				j++;
			}
		}
		int category = ArrayUtil.minimumIndex(averageHashDistance);
		
		if(category == -1) {
			//Empty
			return new Pair<>(category,Double.MAX_VALUE);
		}else {
			return new Pair<>(category, averageHashDistance[category]);
		}
		
	}

	public Pair<Integer, Double> categorizeImageAndAdd(BufferedImage bi, double maxThreshold) {
		Pair<Integer, Double> catResult = categorizeImage(bi);

		int category = catResult.getFirst();
		double distance = catResult.getSecond();

		if (distance > maxThreshold) {
			if(categories.isEmpty()) {
				category = 0;
			}else {
				category = categories.last() + 1;	
			}
			assert !categories.contains(category);
		}
		distance = addCategoricalImage(bi, category);
		return new Pair<>(category, distance);
	}

	protected Hash createAverageHash(HashingAlgorithm algorithm, int category) {

		// TODO benchmark row or column major
		int[][] bits = averageBits.get(algorithm).get(category);

		int hashLength = bits.length;

		BigInteger rawHash = BigInteger.ZERO;
		for (int i = hashLength - 1; i >= 0; i--) {

			if (bits[i][0] > bits[i][1]) {
				rawHash = rawHash.shiftLeft(1);
			} else if (bits[i][0] < bits[i][1]) {
				rawHash = rawHash.shiftLeft(1).add(BigInteger.ONE);
			} else {
				rawHash = rawHash.shiftLeft(1);
				// TODO what do we do now?
				// TODO what do we do now?
				// We could use this as a scale factor
			}
			;
		}
		return new Hash(rawHash, hashLength, 0);
	}

	public int getImagesInCategory(int category) {
		int[][] bits = averageBits.get(this.steps.keySet().iterator().next()).get(category);
		return (bits[0][0] + bits[0][1]);
	}

	// Debug functions

	public void printHashArray(HashingAlgorithm hashAlgorithm, int category) {
		System.out.println(ArrayUtil.deepToStringFormatted(averageBits.get(hashAlgorithm).get(category)));
	}

	public BufferedImage categoricalHashToImage(HashingAlgorithm hashAlgorithm, int category, int blockSize) {

		if (!categories.contains(category)) {
			throw new IllegalArgumentException("No entry for category: " + category + " found");
		}

		int hashLength = hashAlgorithm.getKeyResolution();
		int width = (int) Math.sqrt(hashLength);
		int height = width;

		BufferedImage bi = new BufferedImage(blockSize * width, blockSize * height, BufferedImage.TYPE_3BYTE_BGR);

		int[][] hashAsBits = averageBits.get(hashAlgorithm).get(category);
		Color[] lowerCol = ColorUtil.ColorPalette.getPalette(120, Color.ORANGERED, Color.GRAY);
		Color[] higherCol = ColorUtil.ColorPalette.getPalette(120, Color.GRAY, Color.GREENYELLOW);

		Color[] colors = new Color[lowerCol.length + higherCol.length];
		System.arraycopy(lowerCol, 0, colors, 0, lowerCol.length);
		System.arraycopy(higherCol, 0, colors, lowerCol.length, higherCol.length);

		FastPixel fp = FastPixel.create(bi);
		int i = hashLength - 1;
		for (int w = 0; w < width * blockSize; w = w + blockSize) {
			for (int h = 0; h < height * blockSize; h = h + blockSize) {

				int zero = hashAsBits[i][0];
				int one = hashAsBits[i--][1];

				int colorIndex = (int) ((zero / (double) (zero + one)) * (colors.length - 1));

				for (int m = 0; m < blockSize; m++) {
					for (int n = 0; n < blockSize; n++) {
						int x = w + m;
						int y = h + n;
						fp.setRed(x, y, (int) (colors[colorIndex].getRed() * 255));
						fp.setGreen(x, y, (int) (colors[colorIndex].getGreen() * 255));
						fp.setBlue(x, y, (int) (colors[colorIndex].getBlue() * 255));
					}
				}
			}
		}
		return bi;
	}

}
