package com.github.kilianB.hashAlgorithms.experimental;

import java.awt.image.BufferedImage;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import com.github.kilianB.ArrayUtil;
import com.github.kilianB.Require;
import com.github.kilianB.graphics.ImageUtil;
import com.github.kilianB.graphics.ImageUtil.FastPixel;
import com.github.kilianB.hashAlgorithms.HashingAlgorithm;
import com.github.kilianB.hashAlgorithms.filter.Kernel;

/**
 * Calculate a hash value based on the average luminosity in an image. The
 * kernel hash variant applies additional kernels to the already rescaled
 * version of the image.
 * 
 * <p>
 * Be aware that to guarantee hash consistency the supplied kernels supplied are
 * implicitly assumed to be immutable. Do NOT change the settings of kernels
 * after adding them to the hashing algorithm or else hash collections as well
 * as trouble with the database image matchers may arise.
 * 
 * @author Kilian
 *
 */
public class AverageKernelHash extends HashingAlgorithm {

	private static final long serialVersionUID = -5234612717498362659L;

	/**
	 * The height and width of the scaled instance used to compute the hash
	 */
	private int height, width;

	/**
	 * The kernel filtering the luminosity of the image
	 */
	private final List<Kernel> filters;

	/**
	 * 
	 * 
	 * @param bitResolution The bit resolution specifies the final length of the
	 *                      generated hash. A higher resolution will increase
	 *                      computation time and space requirement while being able
	 *                      to track finer detail in the image. Be aware that a high
	 *                      key is not always desired.
	 *                      <p>
	 * 
	 *                      The average hash requires to re scale the base image
	 *                      according to the required bit resolution. If the square
	 *                      root of the bit resolution is not a natural number the
	 *                      resolution will be rounded to the next whole number.
	 *                      </p>
	 * 
	 *                      The average hash will produce a hash with at least the
	 *                      number of bits defined by this argument. In turn this
	 *                      also means that different bit resolutions may be mapped
	 *                      to the same final key length.
	 * 
	 *                      <pre>
	 *  64 = 8x8 = 65 bit key
	 *  128 = 11.3 -&gt; 12 -&gt; 144 bit key
	 *  256 = 16 x 16 = 256 bit key
	 *                      </pre>
	 */
	public AverageKernelHash(int bitResolution) {
		this(bitResolution, Kernel.boxFilterNormalized(3, 3));
	}

	/**
	 * 
	 * @param bitResolution
	 * @param kernels       applied before the rescaled image is compared to the
	 *                      filter Since raw luminosity values are compared to the
	 *                      computed kernel value the kernel should be in the same
	 *                      range
	 */
	public AverageKernelHash(int bitResolution, Kernel... kernels) {
		super(bitResolution);

		/*
		 * Figure out how big our resized image has to be in order to create a hash with
		 * approximately bit resolution bits while trying to stay as squared as possible
		 * to not introduce bias via stretching or shrinking the image asymmetrically.
		 */
		computeDimension(bitResolution);

		filters = new ArrayList<Kernel>(Arrays.asList(Require.deepNonNull(kernels, "The kernel may not be null")));
	}

	@Override
	protected BigInteger hash(BufferedImage image, BigInteger hash) {

		FastPixel fp = new FastPixel(ImageUtil.getScaledInstance(image, width, height));

		int[][] luminocity = fp.getLuma();

		// Calculate the average color of the entire image

		// Kernel filter
		
		double[][] filtered = null;
		
		for(Kernel kernel : filters) {
			if(filtered==null) {
				filtered = kernel.apply(luminocity);
			}else {
				filtered = kernel.apply(filtered);
			}
		}
		
		// Create hash
		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				if (luminocity[x][y] < filtered[x][y]) {
					hash = hash.shiftLeft(1);
				} else {
					hash = hash.shiftLeft(1).add(BigInteger.ONE);
				}
			}
		}
		return hash;
	}

	/**
	 * Compute the resize width and height for our image.
	 * 
	 * 
	 * @param bitResolution the target bit resolution of the final hash
	 */
	private void computeDimension(int bitResolution) {

		// Allow for slightly non symmetry to get closer to the true bit resolution
		int dimension = (int) Math.round(Math.sqrt(bitResolution));

		// Lets allow for a +1 or -1 asymmetry and find the most fitting value
		int normalBound = (dimension * dimension);
		int higherBound = (dimension * (dimension + 1));

		this.height = dimension;
		this.width = dimension;
		if (normalBound < bitResolution || (normalBound - bitResolution) > (higherBound - bitResolution)) {
			this.width++;
		}
	}

	@Override
	protected int precomputeAlgoId() {
		return Objects.hash(getClass().getName(), height, width, filters);
	}

}
