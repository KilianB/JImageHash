package dev.brachtendorf.jimagehash.hashAlgorithms;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.imageio.ImageIO;

import dev.brachtendorf.Require;
import dev.brachtendorf.graphics.ColorUtil;
import dev.brachtendorf.graphics.FastPixel;
import dev.brachtendorf.graphics.ImageUtil;
import dev.brachtendorf.jimagehash.hash.Hash;
import dev.brachtendorf.jimagehash.hashAlgorithms.filter.Filter;

/**
 * Base class for hashing algorithms returning perceptual hashes for supplied
 * images reducing the number of bits needed to represent said image.
 * 
 * <p>
 * Opposed to cryptographic hashes, hashes computed by these classes are
 * entirely predictable. Similarity metrics applied to these hashes shall
 * produce a higher score for closely related images.
 * 
 * <p>
 * If implementing impose a limitation on the lower bounds on the dimension of
 * hashable images the method {@link #getKeyResolution()} has to be overridden.
 * 
 * <p>
 * Unless otherwise noted hashing algorithms are thread safe.
 * 
 * @author Kilian
 * @since 1.0.0
 */

public abstract class HashingAlgorithm implements Serializable {

	// maybe move to bitsets//Mutable inetegers? not efficient for small keys?
	protected List<Filter> preProcessing = new ArrayList<>();

	private static final long serialVersionUID = 3L;

	/**
	 * The target bit resolution supplied during algorithm creation. This number
	 * represents the number of bits the final hash SHOULD have, but does not
	 * necessarily reflect it's actual length.
	 * <p>
	 * Therefore, it is not advised to use this value during computation of the hash
	 * unless you made sure that the value actually reflects
	 * 
	 */
	protected final int bitResolution;

	/** The actual bit resolution of produced hashes */
	protected int keyResolution = -1;

	/**
	 * The algorithm id of this hashing algorithm. The algorithm id specifies a
	 * unique identifier which allows to check if two distinct hashes are created by
	 * the same hashing algorithm and therefore are comparable. Even algorithms with
	 * the same
	 */
	private int algorithmId;

	/** Color used in replacement of opaque pixels */
	protected Color opaqueReplacementColor = Color.orange;

	/** Maximum alpha value a pixel must have in order to be replaced */
	protected int opaqueReplacementThreshold = -1;

	/**
	 * After a hash was created or the id was calculated the object may not be
	 * altered anymore.
	 */
	protected boolean immutableState = false;

	private static final String LOCKED_MODIFICATION_EXCEPTION = "Hashing algorithms may only be "
			+ "modified as long as no hash has been generated or hashcode has been used by this object. This limitation is "
			+ "imposed to ensure that each hash is associated with the correct algorithm id which "
			+ "might change if the internal state of the algorithm is altered. Be aware"
			+ " that method like getKeyResolution() already perform a hashing operation "
			+ "and therefore invalidate further modification requests";

	/**
	 * Promises a key with approximately bit resolution. Due to geometric
	 * requirements the key might be marginally larger or smaller than specified.
	 * Hashing algorithms shall try to at least provide the number of bits specified
	 * 
	 * @param bitResolution The bit count of the final hash
	 */
	public HashingAlgorithm(int bitResolution) {

		this.bitResolution = Require.positiveValue(bitResolution,
				"The bit resolution for hashing algorithms has to be positive");
	}

	/**
	 * Define how the algorithm shall handle images with alpha value. Hashing
	 * algorithms usually depend on the luminosity value, which by default will be
	 * treated as being black.
	 * <p>
	 * Sometimes display software may choose to display missing pixels in a
	 * different color e.g. white. For the algorithm this would result in an
	 * entirely black image while for the user these images are perceptually
	 * different.
	 * 
	 * @param replacementColor The color used to replace opaque values. A color
	 *                         should be chosen which is unlikely to be part of the
	 *                         target images. By default an orange color is
	 *                         selected. If a value of null is provided
	 * @param alphaThreshold   All colors with a value lower or equal value [0-255]
	 *                         will be replaced.
	 *                         <ul>
	 *                         <li>0 means only invisible (entirely opaque pixels
	 *                         will be replaced)</li>
	 *                         <li></li>
	 *                         </ul>
	 * @throws IllegalStateException if a hash was already created and the object is
	 *                               considered immutable.
	 * @since 3.0.1
	 */
	public void setOpaqueHandling(Color replacementColor, int alphaThreshold) {

		if (immutableState) {
			throw new IllegalStateException(LOCKED_MODIFICATION_EXCEPTION);
		}
		this.opaqueReplacementColor = replacementColor;
		this.opaqueReplacementThreshold = alphaThreshold;
	}

	/**
	 * Define how the algorithm shall handle images with alpha value. Hashing
	 * algorithms usually depend on the luminosity value, which by default will be
	 * treated as being black.
	 * <p>
	 * Sometimes display software may choose to display missing pixels in a
	 * different color e.g. white. For the algorithm this would result in an
	 * entirely black image while for the user these images are perceptually
	 * different.
	 * 
	 * @param alphaThreshold All colors with a value lower or equal value [0-255]
	 *                       will be replaced.
	 *                       <ul>
	 *                       <li>0 means only invisible (entirely opaque pixels will
	 *                       be replaced)</li>
	 *                       <li></li>
	 *                       </ul>
	 * @throws IllegalStateException if a hash was already created and the object is
	 *                               considered immutable.
	 * @since 3.0.1
	 */
	public void setOpaqueHandling(int alphaThreshold) {
		setOpaqueHandling(this.opaqueReplacementColor, alphaThreshold);
	}

	/**
	 * @return color used in replacement of opaque pixels
	 * @since 3.0.1
	 */
	public Color getOpaqueReplacementColor() {
		return opaqueReplacementColor;
	}

	/**
	 * @return the maximum alpha value a pixel must have in order to be replaced by
	 *         the opaque replacement color [0-255].
	 * @since 3.0.1
	 */
	public int getOpaqueReplacementThreshold() {
		return opaqueReplacementThreshold;
	}

	/**
	 * Calculate hashes for the given images. Invoking the hash function on the same
	 * image has to return the same hash value. A comparison of the hashes relates
	 * to the similarity of the images. The lower the value the more similar the
	 * images are. Equal images will produce a similarity of 0.
	 * 
	 * @param images whose hash will be calculated
	 * @return The hash representing the image
	 * @see Hash
	 * @since 3.0.0
	 */
	public Hash[] hash(BufferedImage... images) {
		Hash[] returnValue = new Hash[images.length];

		for (int i = 0; i < images.length; i++) {
			returnValue[i] = this.hash(images[i]);
		}
		return returnValue;
	}

	/**
	 * Calculate hashes for the given images. Invoking the hash function on the same
	 * image has to return the same hash value. A comparison of the hashes relates
	 * to the similarity of the images. The lower the value the more similar the
	 * images are. Equal images will produce a similarity of 0.
	 * 
	 * @param imageFiles pointing to the images
	 * @return The hash representing the images
	 * @throws IOException if an error occurs during loading the image
	 * @see Hash
	 * @since 3.0.0
	 */
	public Hash[] hash(File... imageFiles) throws IOException {
		Hash[] returnValue = new Hash[imageFiles.length];

		for (int i = 0; i < imageFiles.length; i++) {
			returnValue[i] = this.hash(imageFiles[i]);
		}
		return returnValue;
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

		BufferedImage bi = image;

		// If we have kernels defined alter red green and blue values accordingly
		if (!preProcessing.isEmpty()) {

			for (Filter kernel : preProcessing) {
				if (bi == null) {
					bi = kernel.filter(image);
				} else {
					bi = kernel.filter(bi);
				}
			}
		}
		immutableState = true;

		BigInteger hashValue;

		if (keyResolution < 0) {
			HashBuilder hb = new HashBuilder(this.bitResolution);
			hashValue = hash(bi, hb);
			keyResolution = hb.length;
		} else {
			hashValue = hash(bi, new HashBuilder(getKeyResolution()));
		}
		return new Hash(hashValue, getKeyResolution(), algorithmId());
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
		immutableState = true;
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
	 * Preceding 0's are omitted in big integer objects, while the usual hamming
	 * distance can be calculated due to xoring without issue the normalized
	 * distance requires the potential length of the key to be known.
	 * 
	 * @param image       Image whose hash will be calculated
	 * @param hashBuilder a hash builder used to construct the hash
	 * @return the hash encoded as a big integer
	 */
	protected abstract BigInteger hash(BufferedImage image, HashBuilder hashBuilder);

	protected FastPixel createPixelAccessor(BufferedImage image, int width, int height) {

		BufferedImage scaledInstance = ImageUtil.getScaledInstance(image, width, height);
		FastPixel fp = FastPixel.create(scaledInstance);

		// If opaque handling is specified and the image has an alpha channel
		if (this.opaqueReplacementThreshold >= 0 && fp.hasAlpha()) {
			/**
			 * If no color is specified grab the contrast color. This operation might not be
			 * the best for hash calculation depending on how the color is interpolated. The
			 * interpolated Color might be black for white images if the alpha is in the
			 * majority.
			 */
			if (this.opaqueReplacementColor == null) {

				javafx.scene.paint.Color interpolatedColor = ImageUtil.interpolateColor(image);
				Color replacementColor = ColorUtil.getContrastColor(ColorUtil.fxToAwtColor(interpolatedColor));
				fp.setReplaceOpaqueColors(this.opaqueReplacementThreshold, replacementColor);
			} else {
				fp.setReplaceOpaqueColors(this.opaqueReplacementThreshold, this.opaqueReplacementColor);
			}
		}
		return fp;
	}

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
	public final int algorithmId() {
		if (algorithmId == 0) {
			algorithmId = 31 * precomputeAlgoId();
			// Make sure the algo id doesn't collide with version 2.0.0 id's
			algorithmId = 31 * algorithmId + 5 + preProcessing.hashCode();

			// Change hash code only if transparency is supported
			if (this.opaqueReplacementThreshold >= 0) {
				algorithmId = 31 * algorithmId
						+ Objects.hash(this.opaqueReplacementThreshold, this.opaqueReplacementColor);
			}

			immutableState = true;
		}
		return algorithmId;
	}

	/**
	 * A unique id identifying the settings and algorithms used to generate the
	 * output result. This method shall contain a hash code for the object which
	 * 
	 * <ul>
	 * <li>Stays consistent throughout restart of the jvm</li>
	 * <li>Value does not change after the constructor finished</li>
	 * <li>Must return the same value if two instances compute the same hashes for
	 * identical input</li>
	 * </ul>
	 * 
	 * <p>
	 * Even if different bitResolutions are used in the constructor
	 * {@link #HashingAlgorithm(int)} the algorithId <b>MUST</b> return the same id
	 * for two instances if the returned hashes for the same input will always be
	 * equal. Therefore instead of checking against the bitResolution the actual
	 * resolution as returned by {@link #getKeyResolution()} should be used.
	 * 
	 * This method algorithm id's as information available to the child class and
	 * will be extended by the hashcode of the kernels.
	 * 
	 * @return the preliminary algorithm id identifying this hashing algorithm
	 */
	protected abstract int precomputeAlgoId();

	/**
	 * Get the actual bit key resolution of all hashes computed by this algorithm.
	 * 
	 * <p>
	 * Be aware that this value may differ from:
	 * 
	 * <ul>
	 * <li>the supplied bit resolution during algorithm creation due to geometric
	 * constraints of the hashing algorithm.</li>
	 * <li>the returned hash's {@link java.math.BigInteger#bitCount()} value due to
	 * preceding 0 bits being truncated in the big integer</li>
	 * </ul>
	 * 
	 * @return the actual bit resolution of the hash.
	 */
	public int getKeyResolution() {
		// If they key resolution is not know compute a sample hash and cache it's
		// return value
		if (keyResolution < 0) {
			BufferedImage bi = new BufferedImage(1, 1, BufferedImage.TYPE_3BYTE_BGR);
			HashBuilder sb = new HashBuilder(this.bitResolution);
			this.hash(bi, sb);
			keyResolution = sb.length;
		}
		return keyResolution;
	}

	/**
	 * Add a {@link dev.brachtendorf.jimagehash.hashAlgorithms.filter.Filter Filter} to this
	 * hashing algorithm which will be used to alter the image before the hashing
	 * operation is applied. Kernels are invoked in the order they are added and are
	 * performed individually on all 3 RGB channels.
	 * 
	 * <p>
	 * Be aware that filters can only be added or removed until the first hash is
	 * computed. This limitation is enforced due to modified Kernels changing the
	 * hashcode of the object which might be used in hash collections leading to the
	 * object not being found after said operation.
	 * 
	 * @param filter The filter to add.
	 * @throws NullPointerException  if filter is null
	 * @throws IllegalStateException if a hash was already created and the object is
	 *                               considered immutable.
	 * @since 2.0.0
	 */
	public void addFilter(Filter filter) {
		Objects.requireNonNull(filter);

		if (immutableState) {
			throw new IllegalStateException(LOCKED_MODIFICATION_EXCEPTION);
		}

		this.preProcessing.add(filter);
	}

	/**
	 * Remove the first occurance of a
	 * {@link dev.brachtendorf.jimagehash.hashAlgorithms.filter.Filter Filter} from this
	 * hashing algorithm.
	 * 
	 * <p>
	 * Be aware that filters can only be added or removed until the first hash is
	 * computed. This limitation is enforced due to modified Kernels changing the
	 * hashcode of the object which might be used in hash collections leading to the
	 * object not being found after said operation.
	 * 
	 * @param filter The filters to remove.
	 * @return true if the kernel was removed. False otherwise
	 * @throws IllegalStateException if a hash was already created and the object is
	 *                               considered immutable.
	 * @since 2.0.0
	 */
	public boolean removeFilter(Filter filter) {

		if (immutableState) {
			throw new IllegalStateException(LOCKED_MODIFICATION_EXCEPTION);
		}
		return this.preProcessing.remove(filter);
	}

	/**
	 * Wraps the values supplied in the argument hash into a hash object as it would
	 * be produced by this algorithm.
	 * <p>
	 * Some algorithms may choose to return an extended hash class to overwrite
	 * certain behavior, in particular the
	 * {@link dev.brachtendorf.jimagehash.hash.Hash#toImage(int)} is likely to differ.
	 * 
	 * <p>
	 * If the algorithm does not utilize a special hash sub class this method
	 * returns the supplied argument.
	 * 
	 * @param original the hash to transform
	 * @return a hash as it would be created by this algorithm.
	 * @since 3.0.0
	 */
	public Hash createAlgorithmSpecificHash(Hash original) {
		return original;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + " [" + bitResolution + "]";
	}

	@Override
	public int hashCode() {
		return algorithmId();
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

		if (this.opaqueReplacementThreshold >= 0 && other.opaqueReplacementThreshold >= 0) {

			if (this.opaqueReplacementThreshold != other.opaqueReplacementThreshold) {
				return false;
			}
			if (!this.opaqueReplacementColor.equals(other.opaqueReplacementColor)) {
				return false;
			}

		} else if (this.opaqueReplacementThreshold < 0 && other.opaqueReplacementThreshold < 0) {

		} else {
			return false;
		}

		return true;
	}
}