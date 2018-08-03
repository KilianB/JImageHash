package com.github.kilianB.matcher;

import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.PriorityQueue;

import com.github.kilianB.dataStrorage.tree.BinaryTree;
import com.github.kilianB.dataStrorage.tree.Result;
import com.github.kilianB.hashAlgorithms.DifferenceHash;
import com.github.kilianB.hashAlgorithms.HashingAlgorithm;
import com.github.kilianB.hashAlgorithms.PerceptiveHash;
import com.github.kilianB.hashAlgorithms.DifferenceHash.Precision;

/**
 * Convenience class allowing to chain multiple hashing algorithms to 
 * find similar images 
 * @author Kilian
 *
 */
public class InMemoryImageMatcher extends ImageMatcher{

	/**
	 * A preconfigured image matcher chaining dHash and pHash algorithms for 
	 * fast high quality results. <p>
	 * The dHash is a quick algorithms allowing to filter images which are very unlikely to 
	 * be similar images.
	 * pHash is computationally more expensive and used to inspect possible candidates further

	 * @return The matcher used to check if images are similar
	 */
	public static InMemoryImageMatcher createDefaultMatcher() {
		return createDefaultMatcher(Setting.Quality);
	}
	
	/**
	 * A preconfigured image matcher chaining dHash and pHash algorithms for 
	 * fast high quality results. <p>
	 * The dHash is a quick algorithms allowing to filter images which are very unlikely to 
	 * be similar images.
	 * pHash is computationally more expensive and used to inspect possible candidates further
	 * 
	 * @param algorithmSetting
	 * 	How agressive the algorithm advances while comparing images </br>
	 * 	<ul>
	 *  <li><b>Forgiving:</b>	Matches a bigger range of images</li>
	 * 	<li><b>Fair:</b> Matches all sample images</li>
	 * 	<li><b>Quality:</b> Recommended: Does not initially filter as aggressively as Fair but returns usable results</li>
	 * 	<li><b>Strict:</b> Only matches images which are closely related to each other</li>
	 * </ul>
	 * 
	 * @return The matcher used to check if images are similar
	 */
	public static InMemoryImageMatcher createDefaultMatcher(Setting algorithmSetting){
		
		InMemoryImageMatcher matcher = new InMemoryImageMatcher();
		
		switch(algorithmSetting) {	
			case Forgiving:
				matcher.addHashingAlgorithm(new DifferenceHash(32,Precision.Double), 20);
				matcher.addHashingAlgorithm(new PerceptiveHash(32), 15);
				break;
			case Fair:
				matcher.addHashingAlgorithm(new DifferenceHash(32,Precision.Double), 15);
				matcher.addHashingAlgorithm(new PerceptiveHash(32), 10);
				break;
			case Strict:
				matcher.addHashingAlgorithm(new DifferenceHash(32,Precision.Double), 10);
				matcher.addHashingAlgorithm(new PerceptiveHash(32), 6);
				break;
			case Quality: 
				matcher.addHashingAlgorithm(new DifferenceHash(32,Precision.Double), 15);
				matcher.addHashingAlgorithm(new PerceptiveHash(32), 15);
		}
		return matcher;
	}
	

	private HashSet<BufferedImage> addedImages = new HashSet<>();
	
	private HashMap<HashingAlgorithm,BinaryTree<BufferedImage>> binTreeMap = new HashMap<>();
	
	
	/**
	 * Append a new hashing algorithm which will be executed after all hash algorithms passed the test.
	 * @param algo	The algorithms to be added
	 * @param threshold  the threshold the hemming distance may be in order to pass as identical image. 
	 * @param normalized Weather the normalized or default hemming distance shall be used. 
	 * The normalized hamming distance will be in range of [0-1] while the hemming distance depends on the length of the hash
	 */
	public void addHashingAlgorithm(HashingAlgorithm algo, float threshold, boolean normalized) {
		super.addHashingAlgorithm(algo, threshold, normalized);
		
		BinaryTree<BufferedImage> binTree = new BinaryTree<>(true);
		binTreeMap.put(algo, binTree);
		
		//Also add all images which were added to the image matcher earlier
		if(!addedImages.isEmpty()) {
			addedImages.forEach(image -> binTree.addHash(algo.hash(image), image));
		}
	}

	
	/**
	 * Removes the hashing algorithms from the image matcher.
	 * @param algo the algorithm to be removed
	 * @return true if the algorithms was removed, false otherwise
	 */
	public boolean removeHashingAlgo(HashingAlgorithm algo) {
		binTreeMap.remove(algo);
		return super.removeHashingAlgo(algo);
	}
	
	/**
	 * Remove all hashing algorithms used by this image matcher instance. At least one algorithm
	 * has to be supplied before imaages can be checked for similarity
	 */
	public void clearHashingAlgorithms() {
		binTreeMap.clear();
		super.clearHashingAlgorithms();
	}
	
	
	/**
	 * Return an immutable map copy of the algorithms currently used in the matcher.
	 * This map is a hard copy of the hashmap and does not updated if the underlying collection
	 * gets altered
	 * @return
	 */
	public Map<HashingAlgorithm,AlgoSettings> getAlgorithms(){
		return Collections.unmodifiableMap(new LinkedHashMap<HashingAlgorithm,AlgoSettings>(steps));
	}
	
	
	/**
	 * Add the images to the matcher allowing the image to be found in future searches.
	 * @param image
	 */
	public void addImages(BufferedImage...images) {
		for(BufferedImage image : images) {
			addImage(image);
		}
	}
	
	/**
	 * Add the image to the matcher allowing the image to be found in future searches.
	 * @param image
	 */
	public void addImage(BufferedImage image) {
		if (steps.isEmpty())
			throw new IllegalStateException(
					"Please supply at least one hashing algorithm prior to invoking the match method");

		if(addedImages.contains(image)) {
			return;
		}
		
		for (Entry<HashingAlgorithm, AlgoSettings> entry : steps.entrySet()) {
			HashingAlgorithm algo = entry.getKey();
			BinaryTree<BufferedImage> binTree = binTreeMap.get(algo);
			binTree.addHash(algo.hash(image), image);
		}
		addedImages.add(image);
	}
	
	/**
	 * Search for all similar images passing the algorithm filters supplied to this matcher. 
	 * If the image itself was added to the tree it will be returned with a distance of 0
	 * @param image The image other images will be matched against
	 * @return
	 * 	Return all images sorted by the <a href="https://en.wikipedia.org/wiki/Hamming_distance">hamming distance</a>
	 *  of the last applied algorithms
	 */
	public PriorityQueue<Result<BufferedImage>> getMatchingImages(BufferedImage image){
		
		if (steps.isEmpty())
			throw new IllegalStateException(
					"Please supply at least one hashing algorithm prior to invoking the match method");
		
		PriorityQueue<Result<BufferedImage>> returnValues = null;
		
		for (Entry<HashingAlgorithm, AlgoSettings> entry : steps.entrySet()) {
			HashingAlgorithm algo = entry.getKey();
			
			BinaryTree<BufferedImage> binTree = binTreeMap.get(algo);
			AlgoSettings settings = entry.getValue();
			
			Hash needleHash = algo.hash(image);

			int threshold = 0;
			if(settings.normalized) {
				int hashLength = needleHash.getHashValue().bitLength();
				threshold = Math.round(settings.threshold * hashLength);
			}else {
				threshold = (int) settings.threshold;
			}
			
			PriorityQueue<Result<BufferedImage>> temp = binTree.getElementsWithinHemmingDistance(needleHash,threshold);
			
			if(returnValues == null) {
				returnValues = temp;
			}else {
				temp.retainAll(returnValues);
				returnValues = temp;
			}
		}
		return returnValues;
	}
	
	
	public void printAllTrees() {
		binTreeMap.entrySet().forEach(c -> c.getValue().printTree());
	}
	
	//Don't keep a reference to the image so the garbage collector can release it
}
