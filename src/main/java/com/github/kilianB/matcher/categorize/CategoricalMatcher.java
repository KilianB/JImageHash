package com.github.kilianB.matcher.supervised;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.DoubleSummaryStatistics;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeSet;

import javax.imageio.ImageIO;

import com.github.kilianB.ArrayUtil;
import com.github.kilianB.datastructures.Pair;
import com.github.kilianB.graphics.ColorUtil;
import com.github.kilianB.graphics.FastPixel;
import com.github.kilianB.hashAlgorithms.AverageHash;
import com.github.kilianB.hashAlgorithms.DifferenceHash;
import com.github.kilianB.hashAlgorithms.DifferenceHash.Precision;
import com.github.kilianB.hashAlgorithms.HashingAlgorithm;
import com.github.kilianB.matcher.Hash;
import com.github.kilianB.matcher.ImageMatcher;

import javafx.scene.paint.Color;

/**
 * 
 * Cluster images into common categories. A category
 * 
 * Computes the distance to the average hash of each category.
 * 
 * Be aware that the order in which images are added matters ... since the
 * average hash of a cluster is constantly updates without re checking old
 * entries.
 * 
 * @author Kilian
 * @since 3.0.0
 */
public class CategoricalMatcher extends ImageMatcher {

	// per hashing algorithm / per category / per bit / count
	protected Map<HashingAlgorithm, Map<Integer, int[][]>> averageBits = new HashMap<>();
	protected Map<HashingAlgorithm, Map<Integer, Hash>> clusterAverageHash = new HashMap<>();

	/**
	 * Hashes of the added images
	 */
	protected Map<String, Map<HashingAlgorithm, Hash>> cachedHashes = new HashMap<>();

	// TODO 2nd pass with update images
	protected Map<Integer, List<String>> cachedImagesInCategory = new HashMap<>();
	// TODO add image and a

	protected Map<Integer, DoubleSummaryStatistics> clusterQuality = new HashMap<>();

	protected boolean clusterRecomputed = false;

	protected TreeSet<Integer> categories = new TreeSet<>();

	protected boolean weightedDistance;

	public boolean recomputeClusters(int maxIterations) {

		boolean globalChange = false;
		for (int iter = 0; iter < maxIterations; iter++) {
			boolean changed = false;
			// clear average bits -> reset to what the average hash is and recompute
			// affilitation
			for (HashingAlgorithm hasher : this.steps.keySet()) {
				for (int category : categories) {
					Hash averageHash = this.clusterAverageHash.get(hasher).get(category);
					int[][] bitArray = this.averageBits.get(hasher).get(category);

					for (int bit = bitArray.length - 1; bit >= 0; bit--) {
						if (averageHash.getBitUnsafe(bit)) {
							bitArray[bit][1] = 1;
							bitArray[bit][0] = 0;
						} else {
							bitArray[bit][1] = 0;
							bitArray[bit][0] = 1;
						}
					}
					averageHash = createAverageHash(hasher, category);
				}
			}

			// Add images without updating.

			Map<String, Integer> oldImageCategoryMap = new HashMap<>();
			for (Entry<Integer, List<String>> entry : cachedImagesInCategory.entrySet()) {
				int category = entry.getKey();
				for (String id : entry.getValue()) {
					oldImageCategoryMap.put(id, category);
				}
			}
			Map<String, Pair<Integer, Hash[]>> newImageCategoryMap = new HashMap<>();

			cachedImagesInCategory.clear();
			clusterQuality.clear();

			// For each image
			for (Entry<String, Map<HashingAlgorithm, Hash>> entry : cachedHashes.entrySet()) {
				String uniqueId = entry.getKey();
				Map<HashingAlgorithm, Hash> hashesAsMap = entry.getValue();
				Hash[] hashes = new Hash[this.steps.size()];
				// Linked hashmap so we can preserve order
				int i = 0;
				for (HashingAlgorithm hasher : this.steps.keySet()) {
					hashes[i++] = hashesAsMap.get(hasher);
				}
				Pair<Integer, Double> catResult = this.categorizeImage(hashes);

				int category = catResult.getFirst();

//				double distance = catResult.getSecond();
				// TODO
//				if (distance > maxThreshold) {
//					if (categories.isEmpty()) {
//						category = 0;
//					} else {
//						category = categories.last() + 1;
//					}
//					assert !categories.contains(category);
//				}

				newImageCategoryMap.put(uniqueId, new Pair<>(category, hashes));

				if (category != oldImageCategoryMap.get(uniqueId)) {
					changed = true;
					globalChange = true;
				}
			}

			for (Entry<String, Pair<Integer, Hash[]>> entry : newImageCategoryMap.entrySet()) {
				String uniqueId = entry.getKey();
				int category = entry.getValue().getFirst();
				Hash[] hashes = entry.getValue().getSecond();
				addCategoricalImage(hashes, category, uniqueId);

				if (cachedImagesInCategory.containsKey(category)) {
					cachedImagesInCategory.get(category).add(uniqueId);
				} else {
					List<String> uniqueIds = new ArrayList<>();
					uniqueIds.add(uniqueId);
					cachedImagesInCategory.put(category, uniqueIds);
				}
			}
			if (!changed) {
				break;
			}
		}
		clusterRecomputed = true;
		return globalChange;
	}

	public CategoricalMatcher(boolean weightedDistanceClassification) {
		weightedDistance = weightedDistanceClassification;
	}

	@Override
	public void addHashingAlgorithm(HashingAlgorithm algo, double threshold, boolean normalized) {
		if (!steps.containsKey(algo)) {
			averageBits.put(algo, new HashMap<>());
			clusterAverageHash.put(algo, new HashMap<>());
		}
		super.addHashingAlgorithm(algo, threshold, normalized);
	}

	/**
	 * The name of the labeled image serves as unique identifier
	 * 
	 * @param images
	 */
	public void addCategoricalImages(Collection<LabeledImage> images) {
		addCategoricalImages(images.toArray(new LabeledImage[images.size()]));
	}

	public void addCategoricalImages(LabeledImage... images) {
		for (LabeledImage lImage : images) {
			addCategoricalImage(lImage);
		}
	}

	public double addCategoricalImage(LabeledImage labeledImage) {
		return addCategoricalImage(labeledImage.getbImage(), labeledImage.getCategory(), labeledImage.getName());
	}

	public double addCategoricalImage(BufferedImage bi, int category, String uniqueId) {
		int i = 0;
		Hash[] hashes = new Hash[this.steps.keySet().size()];
		for (HashingAlgorithm hashAlgorithm : this.steps.keySet()) {
			Hash createdHash = hashAlgorithm.hash(bi);
			hashes[i++] = createdHash;
			if (cachedHashes.containsKey(uniqueId)) {
				cachedHashes.get(uniqueId).put(hashAlgorithm, createdHash);
			} else {
				Map<HashingAlgorithm, Hash> hashMap = new HashMap<HashingAlgorithm, Hash>(
						(int) (this.steps.size() / 0.75 + 1));
				hashMap.put(hashAlgorithm, createdHash);
				cachedHashes.put(uniqueId, hashMap);
			}
		}
		return addCategoricalImage(hashes, category, uniqueId);
	}

	protected double addCategoricalImage(Hash[] hashes, int category, String uniqueId) {
		double averageDistance = 0;

		int i = 0;
		for (HashingAlgorithm hashAlgorithm : this.steps.keySet()) {
			Hash createdHash = hashes[i++];
			// lazily initialize
			boolean dirty = false;

			Map<Integer, int[][]> categoryMap = averageBits.get(hashAlgorithm);

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
				Map<Integer, Hash> aHash = clusterAverageHash.get(hashAlgorithm);
				aHash.put(category, createAverageHash(hashAlgorithm, category));
			}
			averageDistance += clusterAverageHash.get(hashAlgorithm).get(category)
					.normalizedHammingDistanceFast(createdHash);
		}

		DoubleSummaryStatistics stats;
		if (clusterQuality.containsKey(category)) {
			stats = clusterQuality.get(category);
		} else {
			stats = new DoubleSummaryStatistics();
			clusterQuality.put(category, stats);
		}

		double distance = averageDistance / this.steps.keySet().size();
		stats.accept(distance);
		// TODO return normalized distance to the current average hash
		return distance;
	}

	public Pair<Integer, Double> categorizeImage(String uniqueId, BufferedImage bi) {
		Pair<Integer, Double> catResult = categorizeImage(bi);

		// TODO dangerouse if we rely on this set and can't find hashes
//		if (cachedImagesInCategory.containsKey(catResult.getFirst())) {
//			cachedImagesInCategory.get(catResult.getFirst()).add(uniqueId);
//		} else {
//			List<String> uniqueIds = new ArrayList<>();
//			uniqueIds.add(uniqueId);
//			cachedImagesInCategory.put(catResult.getFirst(), uniqueIds);
//		}
		// Still TODO
		return catResult;
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
				Map<Integer, Hash> categoricalAverageHash = clusterAverageHash.get(hashAlgorithm);
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
				Map<Integer, Hash> categoricalAverageHash = clusterAverageHash.get(hashAlgorithm);
				Map<Integer, int[][]> categoricalAverageHashBit = averageBits.get(hashAlgorithm);
				int hashLength = hashAlgorithm.getKeyResolution();
				for (Integer category : categories) {

					double normalizedHammingDistance = 0;

					Hash targetHash = categoricalAverageHash.get(category);
					int[][] weightBits = categoricalAverageHashBit.get(category);

					for (int bit = hashLength - 1; bit >= 0; bit--) {

						boolean match = targetHash.getBit(bit) == hashes[j].getBit(bit);
						if (!match) {
							if (targetHash.getBit(bit)) {
								double oneWeightFactor = weightBits[bit][1]
										/ (double) (weightBits[bit][0] + weightBits[bit][1]);

								normalizedHammingDistance += oneWeightFactor;
							} else {
								double zeroWeightFactor = weightBits[bit][0]
										/ (double) (weightBits[bit][0] + weightBits[bit][1]);
								// 0
								normalizedHammingDistance += zeroWeightFactor;
							}
						}
					}
					averageHashDistance[category] = normalizedHammingDistance / hashLength;
				}
				j++;
			}
		}

		int category = ArrayUtil.minimumIndex(averageHashDistance);

		if (category == -1) {
			// Empty
			return new Pair<>(category, Double.MAX_VALUE);
		} else {
			return new Pair<>(category, averageHashDistance[category]);
		}

	}

	public Pair<Integer, Double> categorizeImageAndAdd(BufferedImage bi, double maxThreshold, String uniqueId) {
		Pair<Integer, Double> catResult = categorizeImage(bi);

		int category = catResult.getFirst();
		double distance = catResult.getSecond();

		if (distance > maxThreshold) {
			if (categories.isEmpty()) {
				category = 0;
			} else {
				category = categories.last() + 1;
			}
			assert !categories.contains(category);
		}
		distance = addCategoricalImage(bi, category, uniqueId);

		if (cachedImagesInCategory.containsKey(category)) {
			cachedImagesInCategory.get(category).add(uniqueId);
		} else {
			List<String> uniqueIds = new ArrayList<>();
			uniqueIds.add(uniqueId);
			cachedImagesInCategory.put(category, uniqueIds);
		}

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

	/**
	 * Get the categories that are are present in this matcher. A category
	 * represents a set of images (or a single image) whose hashes are closely
	 * related to each other.
	 * 
	 * <p>
	 * If images are added by calling
	 * {@link #categorizeImageAndAdd(BufferedImage, double, String)} the categories
	 * will be in the range of [0 - n]. Gaps can appear if
	 * {@link #addCategoricalImage}
	 * 
	 * @return a list containing the category indices in sorted order.
	 */
	public List<Integer> getCategories() {
		List<Integer> categoriesAsList = new ArrayList<>(categories);
		categoriesAsList.sort(null);
		return categoriesAsList;
	}

	protected int getImagesInCategory(int category, HashingAlgorithm hasher) {
		int[][] bits = averageBits.get(hasher).get(category);
		return (bits[0][0] + bits[0][1]) - (clusterRecomputed ? 1 : 0);
	}

	/**
	 * Get the number of images that are were added in this category.
	 * 
	 * @param category to retrieve the number of images from.
	 * @return he number of images that were mapped and added to this category
	 */
	public int getImagesInCategory(int category) {
		return getImagesInCategory(category, this.steps.keySet().iterator().next());
	}

	// TODO not stable. we actually need a map ... sorted return
	public int[] getIndexImagesInCategories() {
		HashingAlgorithm randomAlgorithm = this.steps.keySet().iterator().next();
		List<Integer> categories = getCategories();

		int[] imagesInCategory = new int[categories.size()];
		for (int i = 0; i < categories.size(); i++) {
			imagesInCategory[i] = (getImagesInCategory(categories.get(i), randomAlgorithm));
		}
		int[] sortedCategoriesByNumberOfImages = ArrayUtil.getSortedIndices(imagesInCategory, true);
		return sortedCategoriesByNumberOfImages;
	}

	public List<String> getUniqueIds(int category) {
		return Collections.unmodifiableList(cachedImagesInCategory.get(category));
	}

	// Debug functions

	public void printHashArray(HashingAlgorithm hashAlgorithm, int category) {
		System.out.println(ArrayUtil.deepToStringFormatted(averageBits.get(hashAlgorithm).get(category)));
	}

	public BufferedImage toImage(DifferenceHash algorithm, int category, int blockSize) {

		Precision precision = algorithm.getPrecision();
		int hashLength = algorithm.getKeyResolution();

		if (precision.equals(Precision.Simple)) {
			int width = (int) Math.sqrt(hashLength) + 1;
			int height = width;

			BufferedImage bi = new BufferedImage(blockSize * width, blockSize * height, BufferedImage.TYPE_3BYTE_BGR);

			FastPixel fp = FastPixel.create(bi);
			drawHashSnippet(algorithm, category, hashLength, fp, width, 1, height, 0, blockSize, 0, 0);
			return bi;
		} else if (precision.equals(Precision.Double)) {
			int width = (int) Math.sqrt(hashLength / 2) + 1;
			int height = width;

			BufferedImage bi = new BufferedImage(blockSize * width, blockSize * height * 2,
					BufferedImage.TYPE_3BYTE_BGR);

			FastPixel fp = FastPixel.create(bi);
			drawHashSnippet(algorithm, category, hashLength, fp, width, 1, height, 0, blockSize, 0, 0);
			drawHashSnippet(algorithm, category, hashLength, fp, width, 0, height, 1, blockSize, hashLength / 2,
					height);
			return bi;
		} else {
			// TODO this is not entirely correct
			int width = (int) Math.sqrt(hashLength / 3) + 1;
			int height = width;

			BufferedImage bi = new BufferedImage(blockSize * width, blockSize * height * 3,
					BufferedImage.TYPE_3BYTE_BGR);

			FastPixel fp = FastPixel.create(bi);
			int hashOffset = 0;
			hashOffset += drawHashSnippet(algorithm, category, hashLength, fp, width, 1, height, 0, blockSize,
					hashOffset, 0);
			hashOffset += drawHashSnippet(algorithm, category, hashLength, fp, width, 0, height, 1, blockSize,
					hashOffset, height);
			drawHashSnippet(algorithm, category, hashLength, fp, width, 1, height, 1, blockSize, hashOffset,
					2 * height);
			return bi;
		}
	}

	private int drawHashSnippet(HashingAlgorithm algorithm, int category, int hashLength, FastPixel writer, int width,
			int wOffset, int height, int hOffset, int blockSize, int offset, int yOffset) {
		int i = hashLength - 1 - offset;

		int[][] hashAsBits = averageBits.get(algorithm).get(category);
		Color[] lowerCol = ColorUtil.ColorPalette.getPalette(15, Color.ORANGERED, Color.GRAY);
		Color[] higherCol = ColorUtil.ColorPalette.getPalette(15, Color.GRAY, Color.GREENYELLOW);

		Color[] colors = new Color[lowerCol.length + higherCol.length];
		System.arraycopy(lowerCol, 0, colors, 0, lowerCol.length);
		System.arraycopy(higherCol, 0, colors, lowerCol.length, higherCol.length);

		for (int w = 0; w < (width - wOffset) * blockSize; w = w + blockSize) {
			for (int h = 0; h < (height - hOffset) * blockSize; h = h + blockSize) {
				// boolean bit = hashValue.testBit(i++);
				int zero = hashAsBits[i][0];
				int one = hashAsBits[i--][1];

				int colorIndex = (int) ((zero / (double) (zero + one)) * (colors.length - 1));

				for (int m = 0; m < blockSize; m++) {
					for (int n = 0; n < blockSize; n++) {
						int x = w + m;
						int y = h + n + yOffset * blockSize;
						// bi.setRGB(y, x, bit ? black : white);
						writer.setRed(x, y, (int) (colors[colorIndex].getRed() * 255));
						writer.setGreen(x, y, (int) (colors[colorIndex].getGreen() * 255));
						writer.setBlue(x, y, (int) (colors[colorIndex].getBlue() * 255));
					}
				}
			}
		}
		System.out.println("Left: " + i);
		return ((hashLength - 1 - offset) - i);
	}

	public BufferedImage categoricalHashToImage(HashingAlgorithm hashAlgorithm, int category, int blockSize) {

		if (!categories.contains(category)) {
			throw new IllegalArgumentException("No entry for category: " + category + " found");
		}

		Hash averageClusterHash = getClusterAverageHash(hashAlgorithm, category);
		int hashLength = averageClusterHash.getBitResolution();

		// Build color palette
		Color[] lowerCol = ColorUtil.ColorPalette.getPalette(15, Color.ORANGERED, Color.GRAY);
		Color[] higherCol = ColorUtil.ColorPalette.getPalette(15, Color.GRAY, Color.GREENYELLOW);

		Color[] colors = new Color[lowerCol.length + higherCol.length];
		System.arraycopy(lowerCol, 0, colors, 0, lowerCol.length);
		System.arraycopy(higherCol, 0, colors, lowerCol.length, higherCol.length);

		// Build color index array

		int[][] hashAsBits = averageBits.get(hashAlgorithm).get(category);
		int[] colorIndex = new int[hashLength];
		for (int i = 0; i < hashLength; i++) {
			int zero = hashAsBits[i][0];
			int one = hashAsBits[i][1];
			colorIndex[i] = (int) ((zero / (double) (zero + one)) * (colors.length - 1));
		}

		return averageClusterHash.toImage(colorIndex, colors, blockSize);

//		int hashLength = hashAlgorithm.getKeyResolution();
//		int width = (int) Math.sqrt(hashLength);
//		int height = width;
//
//		BufferedImage bi = new BufferedImage(blockSize * width, blockSize * height, BufferedImage.TYPE_3BYTE_BGR);
//
//		int[][] hashAsBits = averageBits.get(hashAlgorithm).get(category);
//		
//
//		FastPixel fp = FastPixel.create(bi);
//		int i = hashLength - 1;
//		for (int w = 0; w < width * blockSize; w = w + blockSize) {
//			for (int h = 0; h < height * blockSize; h = h + blockSize) {
//
//				int zero = hashAsBits[i][0];
//				int one = hashAsBits[i--][1];
//
//				int colorIndex = (int) ((zero / (double) (zero + one)) * (colors.length - 1));
//
//				for (int m = 0; m < blockSize; m++) {
//					for (int n = 0; n < blockSize; n++) {
//						int x = w + m;
//						int y = h + n;
//						fp.setRed(x, y, (int) (colors[colorIndex].getRed() * 255));
//						fp.setGreen(x, y, (int) (colors[colorIndex].getGreen() * 255));
//						fp.setBlue(x, y, (int) (colors[colorIndex].getBlue() * 255));
//					}
//				}
//			}
//		}
//		return bi;
	}

	/**
	 * @param i
	 * @return
	 */
	public Hash getClusterAverageHash(HashingAlgorithm algorithm, int category) {
		return algorithm.createAlgorithmSpecificHash(clusterAverageHash.get(algorithm).get(category));
	}

	public static void main(String[] args) throws IOException {
		CategoricalMatcher matcher = new CategoricalMatcher(false);

		BufferedImage highQuality = ImageIO.read(new File("src/test/resources/highQuality.jpg"));
		BufferedImage highQualityBright = ImageIO.read(new File("src/test/resources/highQualityBright.png"));
		BufferedImage lowQuality = ImageIO.read(new File("src/test/resources/lowQuality.jpg"));
		BufferedImage thumbnail = ImageIO.read(new File("src/test/resources/thumbnail.jpg"));
		BufferedImage copyright = ImageIO.read(new File("src/test/resources/copyright.jpg"));

		BufferedImage ballon = ImageIO.read(new File("src/test/resources/ballon.jpg"));

		// BufferedImage bi = ImageIO.read(new File("src/test/resources/ballon.jpg"));

		HashingAlgorithm hasher = new AverageHash(32);

		ImageIO.write(hasher.hash(highQuality).toImage(10), "png", new File("PlainHash.png"));

		matcher.addHashingAlgorithm(hasher, 1);

		System.out.println(matcher.categorizeImageAndAdd(highQuality, 0.2d, "Hq"));
		System.out.println(matcher.categorizeImageAndAdd(ballon, 0.2d, "ballon") + " ballon");
		System.out.println(matcher.categorizeImageAndAdd(lowQuality, 0.2d, "lq"));
		System.out.println(matcher.categorizeImageAndAdd(highQualityBright, 0.2d, "hqBright"));
		System.out.println(matcher.categorizeImageAndAdd(thumbnail, 0.2d, "thumb"));
		System.out.println(matcher.categorizeImageAndAdd(copyright, 0.2d, "copy"));

		System.out.println("");

		System.out.println(matcher.categorizeImage(ballon));
		System.out.println(matcher.categorizeImage(lowQuality));
		System.out.println(matcher.categorizeImage(highQualityBright));
		System.out.println(matcher.categorizeImage(thumbnail));
		System.out.println(matcher.categorizeImage(copyright));

		ImageIO.write(matcher.categoricalHashToImage(hasher, 0, 10), "png", new File("HashColor.png"));

		matcher.recomputeClusters(10);

//		ImageIO.write(matcher.categoricalHashToImage(hasher, 1, 10), "png", new File("HashBallon.png"));
	}

}
