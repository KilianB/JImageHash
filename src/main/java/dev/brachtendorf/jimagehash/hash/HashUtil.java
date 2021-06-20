package dev.brachtendorf.jimagehash.hash;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import dev.brachtendorf.jimagehash.hashAlgorithms.HashingAlgorithm;

/**
 * @author Kilian
 *
 */
public class HashUtil {

	public static FuzzyHash toFuzzyHash(Hash...hashes) {
		return new FuzzyHash(hashes);
	};
	
	public static FuzzyHash toFuzzyHash(HashingAlgorithm hasher, File... imageFiles) throws IOException {
		FuzzyHash fuzzy = new FuzzyHash();
		for(File imgFile : imageFiles) {
			fuzzy.mergeFast(hasher.hash(imgFile));
		}
		return fuzzy;
	}
	
	public static FuzzyHash toFuzzyHash(HashingAlgorithm hasher, BufferedImage... images){
		FuzzyHash fuzzy = new FuzzyHash();
		for(BufferedImage image: images) {
			fuzzy.mergeFast(hasher.hash(image));
		}
		return fuzzy;
	}
	
	public static boolean areCompatible(Hash h0, Hash h1) {
		return h0.getAlgorithmId() == h1.getAlgorithmId();
	}
	
}
