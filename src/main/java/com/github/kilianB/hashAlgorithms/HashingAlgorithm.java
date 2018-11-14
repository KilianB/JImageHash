package com.github.kilianB.hashAlgorithms;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.math.BigInteger;

import javax.imageio.ImageIO;

import com.github.kilianB.matcher.Hash;

/**
 * 
 * @author Kilian
 *
 */
public abstract class HashingAlgorithm implements Serializable {

	// TODO maybe move to bitsets? not efficient for small keys

	private static final long serialVersionUID = 2L;

	protected final int bitResolution;

	/** The actual bit resolution of produced hashes */
	protected int keyResolution = -1;

	/**
	 * Promises a key with approximately bit resolution (+ 1 padding bit). Due to
	 * geometric requirements the key might be marginally larger or smaller than
	 * specified. Hashing algorithms shall try to at least provide the number of
	 * bits specified
	 * 
	 * @param bitResolution The bit count of the final hash
	 */
	public HashingAlgorithm(int bitResolution) {
		if (bitResolution < 0) {
			throw new IllegalArgumentException("The bit resolution for hashing algorithms has to be positive");
		}
		this.bitResolution = bitResolution;
	}

	/**
	 * Calculate a hash for the given image. Invoking the hash function on the same
	 * image has to return the same hash value. A comparison of the hashes relates
	 * to the similarity of the images. The lower the value the more similar the
	 * images are. Equal images will produce a similarity of 0.
	 * 
	 * @param image Image whose hash will be calculated
	 * @return The hash representing the image
	 * @see Hash
	 */
	public Hash hash(BufferedImage image) {
		return new Hash(hash(image, BigInteger.ZERO), getKeyResolution(), algorithmId());
	}

	/**
	 * Calculate a hash for the given image. Invoking the hash function on the same
	 * image has to return the same hash value. A comparison of the hashes relates
	 * to the similarity of the images. The lower the value the more similar the
	 * images are. Equal images will produce a similarity of 0.
	 * 
	 * @param imageFile The file pointing to the image
	 * @return The hash representing the image
	 * @throws IOException if an error occurs during loading the image
	 * @see Hash
	 */
	public Hash hash(File imageFile) throws IOException {
		return hash(ImageIO.read(imageFile));
	}

	/**
	 * Calculate a hash for the given image. Invoking the hash function on the same
	 * image has to return the same hash value. A comparison of the hashes relates
	 * to the similarity of the images. The lower the value the more similar the
	 * images are. Equal images will produce a similarity of 0.
	 * 
	 * <p>
	 * This method is intended to be overwritten by implementations and takes a
	 * baseHash argument to allow concatenating multiple hashes as well to be able
	 * to compute the effective hash length in {@link #getKeyResolution()}.
	 * Preceding 0's are omitted in big integer objects, while the usual hemming
	 * distance can be calculated due to xoring without issue the normalized
	 * distance requires the potential length of the key to be known.
	 * 
	 * @param image Image whose hash will be calculated
	 * @param hash  the big integer used to store the hash value
	 * @return the hash encoded as a big integer
	 */
	protected abstract BigInteger hash(BufferedImage image, BigInteger hash);

	/**
	 * A unique id identifying the settings and algorithms used to generate the
	 * output result. The id shall stay consistent throughout restarts of the jvm.
	 * 
	 * <p>
	 * Even if different bitResolutions are used in the constructor
	 * {@link #HashingAlgorithm(int)} the algorithId <b>MUST</b> return the same id
	 * for two instances if the returned hashes for the same input will always be
	 * equal. Therefore instead of checking against the bitResolution the actual
	 * resolution as returned by {@link #getKeyResolution()} should be used.
	 * 
	 * @return the algorithm id identifying this hashing algorithm
	 */
	public abstract int algorithmId();

	/**
	 * 
	 * @return the actual bit resolution of the hash. Be aware that the underlying
	 *         biginteger does not guarantee to hold the specified number of bits
	 *         when {@link java.math.BigInteger#bitCount()}
	 */
	public int getKeyResolution() {
		if (keyResolution < 0) {
			BufferedImage bi = new BufferedImage(1, 1, BufferedImage.TYPE_3BYTE_BGR);
			keyResolution = this.hash(bi, BigInteger.ONE).bitLength() - 1;
		}
		return keyResolution;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + " [" + bitResolution + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + algorithmId();
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
		HashingAlgorithm other = (HashingAlgorithm) obj;
		if (algorithmId() != other.algorithmId())
			return false;
		return true;
	}
}
