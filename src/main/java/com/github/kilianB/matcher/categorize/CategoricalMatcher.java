package com.github.kilianB.matcher.categorize;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.DoubleSummaryStatistics;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.github.kilianB.datastructures.Pair;
import com.github.kilianB.hashAlgorithms.HashingAlgorithm;
import com.github.kilianB.matcher.FuzzyHash;
import com.github.kilianB.matcher.Hash;
import com.github.kilianB.matcher.ImageMatcher;
import com.github.kilianB.matcher.supervised.LabeledImage;

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
public class CategoricalMatcher extends ImageMatcher implements CategoricalImageMatcher {

	private static final Logger LOGGER = Logger.getLogger(WeightedCategoricalMatcher.class.getSimpleName());

	// per hashing algorithm / per category / per bit / count
	protected Map<HashingAlgorithm, Map<Integer, FuzzyHash>> clusterHash = new HashMap<>();

	protected Map<HashingAlgorithm, Map<FuzzyHash, Integer>> clusterReverseLookup = new HashMap<>();

	/**
	 * Hashes of the added images
	 */
	protected Map<String, Map<HashingAlgorithm, Hash>> cachedHashes = new HashMap<>();

	protected Map<Integer, List<String>> cachedImagesInCategory = new HashMap<>();
	protected Map<String, Integer> cachedImagesCategoryMap = new HashMap<>();

	protected Map<Integer, DoubleSummaryStatistics> clusterQuality = new HashMap<>();

	protected boolean clusterRecomputed = false;
	// TODO double sort.
	protected TreeSet<Integer> categories = new TreeSet<>();

	protected Map<Integer, CategoricalImageMatcher> subCategoryMatcher = new HashMap<>();

	@Override
	public void recomputeCategories() {
		recomputeClusters(10);
	}

	// Clustering
	/**
	 * Keep track which categories were changed during the last iteration. It's only
	 * necessary to compute the distance to the new categories (as well as the
	 * currently one). This step reduces the search quite a bit
	 */
	protected Set<Integer> categoriesAltered = new HashSet<>();

	protected boolean recomputeClusters(int maxIterations) {

		if (categoriesAltered.isEmpty()) {
			return false;
		}

		boolean globalChange = false;

		clusterPrecomputation();

		for (int iter = 0; iter < maxIterations; iter++) {
			int totalUpdate = 0;
			boolean changed = false;

			Map<String, Pair<Integer, Hash[]>> newImageCategoryMap = new HashMap<>();

			Set<Integer> catTemp = new HashSet<>();

			// For each image
			for (Entry<String, Map<HashingAlgorithm, Hash>> entry : cachedHashes.entrySet()) {
				String uniqueId = entry.getKey();
				Map<HashingAlgorithm, Hash> hashesAsMap = entry.getValue();
				int category;
				Hash[] hashes = new Hash[this.steps.size()];
				// Linked hashmap so we can preserve order
				int i = 0;
				for (HashingAlgorithm hasher : this.steps.keySet()) {
					hashes[i++] = hashesAsMap.get(hasher);
				}

				// Overwrite
				category = getCategory(iter, uniqueId, hashes, categoriesAltered);

				// TODO if no distance is within reach create a new category
				if (category == -1) {
					System.out.println(category);
				}

				newImageCategoryMap.put(uniqueId, new Pair<>(category, hashes));
				int oldCategory = cachedImagesCategoryMap.get(uniqueId);

				// If the image category changed note it.
				if (category != oldCategory) {
					changed = true;
					globalChange = true;
					totalUpdate++;
					catTemp.add(category);
					catTemp.add(oldCategory);
				}
			}

			updateCategories(newImageCategoryMap);

			categoriesAltered.clear();
			categoriesAltered.addAll(catTemp);

			LOGGER.fine("Recomputed cluster: " + iter + " Updated: " + totalUpdate);

			if (!changed) {
				break;
			}
		}

		clusterRecomputed = true;
		clusterPostcomputation();
		return globalChange;
	}

	protected void clusterPrecomputation() {
	}

	protected void clusterPostcomputation() {
	}

	protected int getCategory(int iter, String uniqueId, Hash[] hashes, Set<Integer> categoriesAltered) {
		Pair<Integer, Double> catResult = this.categorizeImage(uniqueId, hashes, categoriesAltered);
		return catResult.getFirst();
	}

	protected void updateCategories(Map<String, Pair<Integer, Hash[]>> newImageCategoryMap) {

		cachedImagesCategoryMap.clear();
		cachedImagesInCategory.clear();
		clusterQuality.clear();

		// clear average bits -> reset to what the average hash is and recompute
		// affilitation
		resetBitWeights();
		// Clone the current hash state
		Map<HashingAlgorithm, Map<Integer, Hash>> cachedHasheClone = new HashMap<>();
		for (HashingAlgorithm hasher : this.steps.keySet()) {
			Map<Integer, FuzzyHash> hashes = clusterHash.get(hasher);
			Map<Integer, Hash> clonedHashes = new HashMap<>();
			cachedHasheClone.put(hasher, clonedHashes);
			for (int category : categories) {
				FuzzyHash bHash = hashes.get(category);
				clonedHashes.put(category, new Hash(bHash.getHashValue(), bHash.getBitResolution(), Integer.MAX_VALUE));
			}
		}

		for (Entry<String, Pair<Integer, Hash[]>> entry : newImageCategoryMap.entrySet()) {
			String uniqueId = entry.getKey();
			int category = entry.getValue().getFirst();
			Hash[] hashes = entry.getValue().getSecond();

			// Add the image to the cluster and update the centeroid
			addCategoricalImage(hashes, category, uniqueId);

			cachedImagesCategoryMap.put(uniqueId, category);

			if (cachedImagesInCategory.containsKey(category)) {
				cachedImagesInCategory.get(category).add(uniqueId);
			} else {
				List<String> uniqueIds = new ArrayList<>();
				uniqueIds.add(uniqueId);
				cachedImagesInCategory.put(category, uniqueIds);
			}
		}
		// Extract the old hash again.
		for (HashingAlgorithm hasher : this.steps.keySet()) {
			Map<Integer, FuzzyHash> hashes = clusterHash.get(hasher);
			Map<Integer, Hash> clonedHashes = cachedHasheClone.get(hasher);
			for (int category : categories) {
				FuzzyHash categoryBase = hashes.get(category);
				categoryBase.subtractFast(clonedHashes.get(category));
			}
		}
		// Clean up categories
		cleanupEmptyCategories();
	}

	protected void cleanupEmptyCategories() {
		Iterator<Integer> iter = cachedImagesInCategory.keySet().iterator();
		while (iter.hasNext()) {
			int category = iter.next();
			if (cachedImagesInCategory.get(category).isEmpty()) {
				cachedImagesInCategory.remove(category);

				for (HashingAlgorithm hasher : steps.keySet()) {

					FuzzyHash removedHash = clusterHash.get(hasher).remove(category);
					clusterReverseLookup.get(hasher).remove(removedHash);
				}
				iter.remove();
			}
		}
	}

	protected void resetBitWeights() {
		for (HashingAlgorithm hasher : this.steps.keySet()) {
			Map<Integer, FuzzyHash> hashes = clusterHash.get(hasher);
			for (int category : categories) {
				hashes.get(category).reset();
			}
		}
	}

	@Override
	public void addHashingAlgorithm(HashingAlgorithm algo, double threshold, boolean normalized) {
		if (!steps.containsKey(algo)) {
			clusterHash.put(algo, new HashMap<>());
			clusterReverseLookup.put(algo, new HashMap<>());
		}
		super.addHashingAlgorithm(algo, threshold, normalized);
	}

	/**
	 * The name of the labeled image serves as unique identifier
	 * 
	 * @param images the images to categories
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

			Map<Integer, FuzzyHash> categoryMap = clusterHash.get(hashAlgorithm);
			Map<FuzzyHash, Integer> reverseCategoryMap = clusterReverseLookup.get(hashAlgorithm);
			// lazily initializ
			if (!categoryMap.containsKey(category)) {
				FuzzyHash fHash = new FuzzyHash();
				categoryMap.put(category, fHash);
				reverseCategoryMap.put(fHash, category);
				categories.add(category);
			}

			FuzzyHash cHash = clusterHash.get(hashAlgorithm).get(category);
			cHash.mergeFast(createdHash);
			averageDistance += computeDistanceToCluster(cHash, createdHash);
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
		categoriesAltered.add(category);
		return distance;
	}

	protected double computeDistanceToCluster(FuzzyHash cluster, Hash imageHash) {
		return cluster.normalizedHammingDistanceFast(imageHash);
	}

	/**
	 * Compute the category of the supplied image. A category is a collection of
	 * similar images mapped to a common hash which minimizes the distance of all
	 * hashes mapped to this category.
	 * 
	 * @param bi The buffered image to categorize
	 * @return a pair whose first value returns the category and second value
	 *         returns a distance measure between the category and the supplied
	 *         image. Smaller distances meaning a closer match
	 */
	@Override
	public Pair<Integer, Double> categorizeImage(BufferedImage bi) {
		Pair<Integer, Double> categoryResult = this.categorizeImage(null, bi);

		// TODO avoid same category. e.g. if this matcher has category 0 -3
		if (subCategoryMatcher.containsKey(categoryResult.getFirst())) {
			// If this return cat 1 we don't know which category it actually belongs to
			return subCategoryMatcher.get(categoryResult.getFirst()).categorizeImage(bi);

			// TODO also take care of categorize and add!
			// TODO
		}
		return categoryResult;

	}

	/**
	 * Compute the category of the supplied image. A category is a collection of
	 * similar images mapped to a common hash which minimizes the distance of all
	 * hashes mapped to this category.
	 * 
	 * @param uniqueId The id used to reference the image in the future.
	 * @param bi       The buffered image to categorize
	 * @return a pair whose first value returns the category and second value
	 *         returns a distance measure between the category and the supplied
	 *         image
	 */
	private Pair<Integer, Double> categorizeImage(String uniqueId, BufferedImage bi) {
		Hash[] hashes = new Hash[this.steps.keySet().size()];
		int j = 0;
		// Compute hashes for the image
		for (HashingAlgorithm hashAlgorithm : this.steps.keySet()) {
			hashes[j] = hashAlgorithm.hash(bi);
			j++;
		}
		return categorizeImage(uniqueId, hashes, categories);
	}

	/**
	 * Categorize an image on a subset of all categories with hashes present. This
	 * method is used during recomputation of the clusters and cuts down the number
	 * of comparisons that have to be made. If an image is newly inserted the
	 * categoriesAltered variable should contain all available categories.
	 * 
	 * @param uniqueId          The id used to reference the image in the future.
	 * @param hashes            a hash of the image for each hashing algorithm
	 * @param categoriesAltered a list of the categories which got altered since
	 *                          last computation
	 * @return the best category and distance to it. if no matching category can be
	 *         found return -1 and Double.MaxValue
	 */
	protected Pair<Integer, Double> categorizeImage(String uniqueId, Hash[] hashes, Set<Integer> categoriesAltered) {
		double bestDistance = Double.MAX_VALUE;
		int bestCategory = -1;
		/*
		 * Native category. Is the image already matched to a category? We can prune the
		 * search tree much faster if we start with a potentially good value
		 */
		if (uniqueId != null && isCategorized(uniqueId)) {
			int oldCategory = getCategory(uniqueId);
			bestDistance = computeDistanceForCategory(hashes, getCategory(uniqueId), bestDistance);
			bestCategory = oldCategory;
		}
		// Categorize image based on the weighted distance based on bit importance

		for (Integer category : categoriesAltered) {
			if (category != bestCategory) {
				double hammingDistance = computeDistanceForCategory(hashes, category, bestDistance);
				if (hammingDistance < bestDistance) {
					bestDistance = hammingDistance;
					bestCategory = category;
				}
			}

		}
		// Normalize the distance

		if (bestCategory == -1) {
			// Empty
			return new Pair<>(bestCategory, Double.MAX_VALUE);
		} else {
			double normalizedHammingDistance = bestDistance / this.steps.keySet().size();
			return new Pair<>(bestCategory, normalizedHammingDistance);
		}
	}

	/**
	 * Compute the distance between an image and a category cluster midpoint. This
	 * method is used to compute the minimum distance and therefore might cut the
	 * computation short if the distance is higher than the supplied best distance
	 * cutoff.
	 * 
	 * 
	 * @param hashes       an array containing the hash for an image for each
	 *                     hashing algorithm added to this matcher
	 * @param category     the category to compute the distance for
	 * @param bestDistance the best distance found so far. May be used to
	 * @return the distance between the image and the category midpoint or
	 *         Double.MAX_VALUE if bestDistance was reached and copmutation was not
	 *         finished
	 */
	protected double computeDistanceForCategory(Hash[] hashes, int category, double bestDistance) {
		double hammingDistance = 0;
		int j = 0;
		for (HashingAlgorithm hashAlgorithm : this.steps.keySet()) {
			Map<Integer, FuzzyHash> categoricalAverageHash = clusterHash.get(hashAlgorithm);
			hammingDistance += categoricalAverageHash.get(category).normalizedHammingDistanceFast(hashes[j]);
			j++;
		}
		return hammingDistance;
	}

	@Override
	public Pair<Integer, Double> categorizeImageAndAdd(BufferedImage bi, double maxThreshold, String uniqueId) {
		Pair<Integer, Double> catResult = categorizeImage(uniqueId, bi);

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

		cachedImagesCategoryMap.put(uniqueId, category);

		if (cachedImagesInCategory.containsKey(category)) {
			cachedImagesInCategory.get(category).add(uniqueId);
		} else {
			List<String> uniqueIds = new ArrayList<>();
			uniqueIds.add(uniqueId);
			cachedImagesInCategory.put(category, uniqueIds);
		}

		return new Pair<>(category, distance);
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

	/**
	 * Get the number of images that are were added in this category.
	 * 
	 * @param category to retrieve the number of images from.
	 * @return he number of images that were mapped and added to this category
	 */
	public int getImageCountInCategory(int category) {
		return cachedImagesInCategory.get(category).size();
	}

	/**
	 * Return the categories sorted by the number of images mapped to the category.
	 * An image is considered part of a category if it was either added by calling
	 * {@link #addCategoricalImage} or {@link #categorizeImageAndAdd}.
	 * 
	 * @return a list containing the sorted categories
	 */
	public LinkedHashMap<Integer, Integer> getCategoriesSortedByImageCount() {
		List<Integer> categories = getCategories();

		Map<Integer, Integer> map = new LinkedHashMap<>();

		for (int i = 0; i < categories.size(); i++) {
			int cat = categories.get(i);
			map.put(cat, getImageCountInCategory(categories.get(cat)));
		}

		Comparator<Map.Entry<Integer, Integer>> comp = (c1, c2) -> c1.getValue().compareTo(c2.getValue());

		return map.entrySet().stream().sorted(comp.reversed())
				.collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue(), (u, v) -> {
					throw new IllegalStateException(String.format("Duplicate key %s", u));
				}, LinkedHashMap::new));
	}

	/**
	 * Get the unique id's of all images mapped to this category
	 * 
	 * @param category to check for
	 * @return a list of all unique id's mapped to this category
	 */
	public List<String> getImagesInCategory(int category) {
		return Collections.unmodifiableList(cachedImagesInCategory.get(category));
	}

	// Debug functions

//	public void printHashArray(HashingAlgorithm hashAlgorithm, int category) {
//		System.out.println(ArrayUtil.deepToStringFormatted(averageBits.get(hashAlgorithm).get(category)));
//	}

	public BufferedImage categoricalHashToImage(HashingAlgorithm hashAlgorithm, int category, int blockSize) {
		if (!categories.contains(category)) {
			throw new IllegalArgumentException("No entry for category: " + category + " found");
		}
		return clusterHash.get(hashAlgorithm).get(category).toImage(blockSize, hashAlgorithm);
	}

	public int getCategory(String uniqueId) {
		return cachedImagesCategoryMap.get(uniqueId);
	}

	public boolean isCategorized(String uniqueId) {
		return cachedImagesCategoryMap.containsKey(uniqueId);
	}

	@Override
	public void addNestedMatcher(int category, CategoricalImageMatcher catMatcher) {
		subCategoryMatcher.put(category, catMatcher);
	}

	public void printClusterInfo(int minImagesInCluster) {
		for (Entry<Integer, DoubleSummaryStatistics> entry : clusterQuality.entrySet()) {
			if (entry.getValue().getCount() >= minImagesInCluster) {
				System.out.println(entry.getKey() + " " + entry.getValue().getAverage());
			}
		}
	}

	/**
	 * Get the average hash representing the midpoint of the category cluster.
	 * 
	 * @param algorithm the algorithm to get the hash for
	 * @param category  the category
	 * @return the average hash .
	 */
	public FuzzyHash getClusterAverageHash(HashingAlgorithm algorithm, int category) {
		return clusterHash.get(algorithm).get(category);
	}
}
