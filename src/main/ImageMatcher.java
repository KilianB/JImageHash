package main;

import java.awt.image.BufferedImage;
import java.math.BigInteger;
import java.util.LinkedHashMap;
import java.util.Map.Entry;

import hashAlgorithms.DifferenceHash;
import hashAlgorithms.HashingAlgorithm;
import hashAlgorithms.PerceptiveHash;

/**
 * Convenience class to chain multiple hashing algorithms
 * @author Kilian
 *
 */
public class ImageMatcher {


	/**
	 * Return a preconfigured image matcher chaining dHash and pHash values for 
	 * fast high quality results.
	 * @param strict toggle between different matching modes.
	 * @return
	 */
	public static ImageMatcher createDefaultMatcher(boolean strict){
		
		if(!strict){
			ImageMatcher matcher = new ImageMatcher();
			//Fastest algorithm and even greater accuracy than average hash.  Fast filter
			matcher.addHashingAlgo(new DifferenceHash(32,true,false), 10);
			//Slower but more accurate. Only execute if difference hash found a potential match.
			matcher.addHashingAlgo(new PerceptiveHash(64), 5);
			return matcher;
		}else{
			ImageMatcher matcher = new ImageMatcher(){
				int threshold = 15;
				HashingAlgorithm dirtyHash = new DifferenceHash(32,true,false);
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

	LinkedHashMap<HashingAlgorithm,Integer> steps = new LinkedHashMap<HashingAlgorithm,Integer>();
	
	/**
	 * Append a new hashing algorithm which will be executed after all hash algorithms provided beforehand.
	 * @param algo 
	 * @param threshold the threshold the hemming distance has to be smaller to pass the test 
	 */
	public void addHashingAlgo(HashingAlgorithm algo, int threshold){
		steps.put(algo,threshold);
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
		
		for(Entry<HashingAlgorithm,Integer> entry : steps.entrySet()){	
			
			BigInteger hash = entry.getKey().hash(image);
			BigInteger hash1 = entry.getKey().hash(image1);
			//Considered a mismatch
			if(hammingDistance(hash,hash1) > entry.getValue())
				return false;
		}
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
	
	
	
}
