package com.github.kilianB.matcher;

import java.awt.image.BufferedImage;
import java.math.BigInteger;
import java.util.LinkedHashMap;
import java.util.Map.Entry;

import com.github.kilianB.hashAlgorithms.DifferenceHash;
import com.github.kilianB.hashAlgorithms.HashingAlgorithm;
import com.github.kilianB.hashAlgorithms.PerceptiveHash;
import com.github.kilianB.hashAlgorithms.DifferenceHash.Precision;


/**
 * Convenience class to chain multiple hashing algorithms
 * @author Kilian
 *
 */
public class ImageMatcher {

	/**
	 * Return a preconfigured image matcher chaining dHash and pHash algorithms for 
	 * fast high quality results.
	 * @param strict toggle between different matching modes.
	 * 	true: 
	 * @return
	 */
	public static ImageMatcher createDefaultMatcher(boolean strict){
		
		if(!strict){
			ImageMatcher matcher = new ImageMatcher();
			//Fastest algorithm and even greater accuracy than average hash.  Fast filter
			matcher.addHashingAlgo(new DifferenceHash(32,Precision.Double), 10);
			//Slower but more accurate. Only execute if difference hash found a potential match.
			matcher.addHashingAlgo(new PerceptiveHash(64), 5);
			return matcher;
		}else{
			ImageMatcher matcher = new ImageMatcher(){
				int threshold = 15;
				HashingAlgorithm dirtyHash = new DifferenceHash(32,Precision.Double);
				HashingAlgorithm filterHash = new PerceptiveHash(64); 
				
				public boolean checkSimilarity(BufferedImage image, BufferedImage image1){
					
					BigInteger hash = dirtyHash.hash(image);
					BigInteger hash1 = dirtyHash.hash(image1);
					int dirtyDistance = hammingDistance(hash,hash1);
					//Considered a mismatch
					if(dirtyDistance <= threshold){
						
						hash = filterHash.hash(image);
						hash1 = filterHash.hash(image1);
						
						if(hammingDistance(hash,hash1) <= dirtyDistance){
							return true;
						}else{
							return false;
						}
					}
					return false;
				}
			};
			return matcher;
		}
	}

	/**
	 * Contains multiple hashing algorithms applied in the order they were added to the 
	 * image matcher
	 */
	private LinkedHashMap<HashingAlgorithm,AlgoSettings> steps = new LinkedHashMap<>();
	
	/**
	 * Append a new hashing algorithm which will be executed after all hash algorithms passed the test.
	 * @param algo The algorithms to be added
	 * @param threshold  the threshold the hemming distance may be in order to pass as identical image
	 */
	public void addHashingAlgo(HashingAlgorithm algo, float threshold){
		addHashingAlgorithm(algo,threshold,false);
	}
	
	/**
	 * Append a new hashing algorithm which will be executed after all hash algorithms passed the test.
	 * @param algo	The algorithms to be added
	 * @param threshold  the threshold the hemming distance may be in order to pass as identical image. 
	 * @param normalized Weather the normalized or default hemming distance shall be used. 
	 * The normalized hamming distance will be in range of [0-1] while the hemming distance depends on the length of the hash
	 */
	public void addHashingAlgorithm(HashingAlgorithm algo, float threshold, boolean normalized) {
		
		if(threshold < 0) {
			throw new IllegalArgumentException("Fatal error adding algorithm. Threshold has to be in the range of [0-X)");
		} else if(normalized && threshold > 1) {
			throw new IllegalArgumentException("Fatal error adding algorithm. Normalized threshold has to be in the range of [0-1]");
		}
		steps.put(algo,new AlgoSettings(threshold,normalized));
	}
	
	/**
	 * Removes the hashing algorithms from the image matcher.
	 * @param algo the algorithm to be removed
	 * @return true if the algorithms was removed, false otherwise
	 */
	public boolean removeHashingAlgo(HashingAlgorithm algo) {
		return steps.remove(algo) != null;
	}
	
	/**
	 * Remove all hashing algorithms used by this image matcher instance. At least one algorithm
	 * has to be supplied before imaages can be checked for similarity
	 */
	public void clearHashingAlgorithms() {
		steps.clear();
	}
	
	/**
	 * Return a strong reference to the algorithm hashmap. Altering this map directly affects the image matcher.
	 * Be aware that this instance is not thread safe.
	 * @return
	 */
	public LinkedHashMap<HashingAlgorithm,AlgoSettings> getAlgorithms(){
		return steps;
	}
	
	
	/**
	 * Execute all supplied hashing algorithms and check if the images are similar 
	 * @param image
	 * @param image1
	 * @return true if images are considered similar
	 */
	public boolean checkSimilarity(BufferedImage image, BufferedImage image1){
		if(steps.isEmpty())
			throw new IllegalStateException("Please supply at least one hashing algorithm prior to invoking the match method");
		
		for(Entry<HashingAlgorithm,AlgoSettings> entry : steps.entrySet()){	
			
			BigInteger hash = entry.getKey().hash(image);
			BigInteger hash1 = entry.getKey().hash(image1);
			
			//Check if the hashing algo is within the threshold. If it's not return early
			if(!entry.getValue().apply(hash, hash1)) {
				return false;
			}
		}
		//Everything matched
		return true;
	}
	
	
	/**
	 * Calculate the hamming distance of 2 hash values. Lower values indicate closer similarity.
	 * The longer the key supplied the greater the distance will be.
	 * Please be aware that only hashes produced by the same algorithm with the same bit key 
	 * will return a use able result.
	 * @param i
	 * @param i2
	 * @return similarity value ranging between 0 - bit resolution
	 */
	public static int hammingDistance(BigInteger i, BigInteger i2){
		if(i.bitLength() != i2.bitLength()) {
			throw new IllegalArgumentException("Can't compare two hash values with unequal length");
		}
		return i.xor(i2).bitCount();
	}
	
	/**
	 * Calculate the hamming distance of 2 hash values. Lower values indicate closer similarity.
	 * The value is normalized by the hash bit resolution.
	 * Please be aware that only hashes produced by the same algorithm with the same bit key 
	 * will return a use able result.
	 * @param i
	 * @param i2
	 * @return similarity value ranging between 0 and 1
	 */
	public static double normalizedHammingDistance(BigInteger i, BigInteger i2){
		//We expect both integers to contain the same bit key lengths!
		return hammingDistance(i,i2)  / (double)i.bitLength();
	}

	
	// TODO Overlapping hash values.
	
	/**
	 * Settings used while computing if an algorithms consideres two images to 
	 * be a close match
	 * @author Kilian
	 *
	 */
	private class AlgoSettings{
		float threshold;
		boolean normalized;
		
		public AlgoSettings(float threshold, boolean normalized) {
			this.threshold = threshold;
			this.normalized = normalized;
		}
		
		public boolean apply(BigInteger hash, BigInteger hash1) {
			if(normalized) {
				return normalizedHammingDistance(hash,hash1) <= threshold;
			}else {
				return hammingDistance(hash,hash1) <= threshold;
			}
		}
	}
	
	
}
