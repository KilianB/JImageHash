package com.github.kilianB.hashAlgorithms;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;

import javax.imageio.ImageIO;

import com.github.kilianB.matcher.Hash;

/**
 * 
 * @author Kilian
 *
 */
public abstract class HashingAlgorithm implements Serializable{
	
	private static final long serialVersionUID = 2L;
	
	protected final int bitResolution;
	/**
	 * Promises a key with at approximately bit resolution (+ 1 padding bit). Due to geometric requirements the key might be marginally larger or smaller than specified
	 * @param bitResolution	The bit count of the final hash
	 */
	public HashingAlgorithm(int bitResolution){
		if(bitResolution < 0) {
			throw new IllegalArgumentException("The bit resolution for hashing algorithms has to be positive");
		}
		this.bitResolution = bitResolution;
	}
	
	/**
	 * Calculate a hash for the given image. Invoking the hash function on the same
	 * image has to return the same hash value.
	 * A comparison of the hashes relates to the similarity of the images.
	 * The lower the value the more similar the images are. Equal images will produce a similarity of 0.
	 * The hash will be preceded by a 1 bit counteracting truncation of 0 bits and a consistent key length.
	 * @param image	Image whose hash will be calculated
	 * @return	The hash representing the image
	 * @see Hash
	 */
	public abstract Hash hash(BufferedImage image);
	
	/**
	 * Calculate a hash for the given image. Invoking the hash function on the same
	 * image has to return the same hash value.
	 * A comparison of the hashes relates to the similarity of the images.
	 * The lower the value the more similar the images are. Equal images will produce a similarity of 0.
	 * The hash will be preceded by a 1 bit counteracting truncation of 0 bits and a consistent key length.
	 * @param imageFile The file pointing to the image 
	 * @return The hash representing the image
	 * @throws IOException if an error occurs during loading the image
	 * @see Hash
	 */
	public Hash hash(File imageFile) throws IOException {
		return hash(ImageIO.read(imageFile));
	}
	
	/**
	 * A unique id identifying the settings and algorithms used to generate the output result.
	 * The id shall stay consistent throughout restarts of the jvm
	 * @return the algorithm id
	 */
	public abstract int algorithmId();

	@Override
	public String toString() {
		return getClass().getSimpleName()+" [" + bitResolution + "]";
	}
	
	
}
