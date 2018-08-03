package com.github.kilianB.hashAlgorithms;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import com.github.kilianB.matcher.Hash;

/**
 * 
 * @author Kilian
 *
 */
public abstract class HashingAlgorithm {
	
	protected final int bitResoluation;
	/**
	 * Promises a key with at approximately bit resolution (+ 1 padding bit). Due to geometric requirements the key might be marginally larger or smaller than specified
	 * @param bitResolution
	 */
	public HashingAlgorithm(int bitResolution){
		if(bitResolution < 0) {
			throw new IllegalArgumentException("The bit resolution for hashing algorithms has to be positive");
		}
		this.bitResoluation = bitResolution;
	}
	
	/**
	 * Calculate a hash for the given image. Invoking the hash function on the same
	 * image has to return the same hash value.
	 * A comparison of the hashes relates to the similarity of the images.
	 * The lower the value the more similar the images are. Equal images will produce a similarity of 0.
	 * The hash will be preceded by a 1 bit counteracting truncation of 0 bits and a consistent key length.
	 * @param image
	 * @return
	 * @see {@link Hash}
	 */
	public abstract Hash hash(BufferedImage image);
	
	/**
	 * Calculate a hash for the given image. Invoking the hash function on the same
	 * image has to return the same hash value.
	 * A comparison of the hashes relates to the similarity of the images.
	 * The lower the value the more similar the images are. Equal images will produce a similarity of 0.
	 * The hash will be preceded by a 1 bit counteracting truncation of 0 bits and a consistent key length.
	 * @param image
	 * @return
	 * @throws IOException if an error occurs during loading the image
	 * @see {@link Hash}
	 */
	public Hash hash(File imageFile) throws IOException {
		return hash(ImageIO.read(imageFile));
	}
	
	
	/**
	 * Create a scaled gray image of the buffered image
	 * @param in	Image to scale
	 * @param width	new width of the scaled image
	 * @param height new height of the scaled image
	 * @return
	 */
	protected BufferedImage getGrayScaledInstance(BufferedImage in, int width, int height){

		BufferedImage transformed = new BufferedImage(width,height,BufferedImage.TYPE_BYTE_GRAY);
		  
		Graphics2D g=transformed.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
		    RenderingHints.VALUE_INTERPOLATION_BICUBIC);
		g.drawImage(in, 0, 0, width, height, null);
		g.dispose();
		return transformed;
	}
	
	/**
	 * Create a scaled instance of the image
	 * @param in	image to scale
	 * @param width	new width of the scaled image
	 * @param height new height of the scaled image
	 * @return
	 */
	protected BufferedImage getScaledInstance(BufferedImage in, int width, int height){

		BufferedImage transformed = new BufferedImage(width,height,BufferedImage.TYPE_INT_ARGB);	  
		Graphics2D g=transformed.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
		    RenderingHints.VALUE_INTERPOLATION_BICUBIC);
		g.drawImage(in, 0, 0, width, height, null);
		g.dispose();
		return transformed;
	}
	
	/**
	 * A unique id identifying the settings and algorithms used to generate the output result.
	 * The id shall stay consistent throughout restarts of the jvm
	 * @return
	 */
	public abstract int algorithmId();
}
