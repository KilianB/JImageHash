package dev.brachtendorf.jimagehash.hashAlgorithms;

import java.awt.image.BufferedImage;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import dev.brachtendorf.ArrayUtil;
import dev.brachtendorf.Require;
import dev.brachtendorf.graphics.FastPixel;
import dev.brachtendorf.jimagehash.hashAlgorithms.filter.Kernel;

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
public class AverageKernelHash extends AverageHash{

	private static final long serialVersionUID = -5234612717498362659L;

	/**
	 * The kernel filtering the luminosity of the image
	 */
	private final List<Kernel> filters;

	/**
	 * @param bitResolution The bit resolution specifies the final length of the
	 *                      generated hash. A higher resolution will increase
	 *                      computation time and space requirement while being able
	 *                      to track finer detail in the image. Be aware that a high
	 *                      key is not always desired.
	 *                      <p>
	 * 
	 *                      The average kernel hash will produce a hash with at
	 *                      least the number of bits defined by this argument. In
	 *                      turn this also means that different bit resolutions may
	 *                      be mapped to the same final key length.
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
	 * 
	 * @param kernels       applied before the rescaled image is compared to the
	 *                      filter Since raw luminosity values are compared to the
	 *                      computed kernel value the kernel should be in the same
	 *                      range
	 */
	public AverageKernelHash(int bitResolution, Kernel... kernels) {
		super(bitResolution);
		filters = new ArrayList<Kernel>(Arrays.asList(Require.deepNonNull(kernels, "The kernel may not be null")));
	}

	@Override
	protected BigInteger hash(BufferedImage image, HashBuilder hash) {
		FastPixel fp = createPixelAccessor(image, width, height);

		int[][] luminosity = fp.getLuma();

		// Calculate the average color of the entire image

		// Kernel filter
		double[][] filtered = null;

		for (Kernel kernel : filters) {
			if (filtered == null) {
				filtered = kernel.apply(luminosity);
			} else {
				filtered = kernel.apply(filtered);
			}
		}

		double avgPixelValue = ArrayUtil.average(filtered);

		return computeHash(hash, filtered, avgPixelValue);
	}

	@Override
	protected int precomputeAlgoId() {
		//*31 to create a distinct id compare to v 2.0.0 bugfix
		return Objects.hash("com.github.kilianB.hashAlgorithms."+getClass().getSimpleName(), height, width, filters) *31;
	}

	@Override
	public String toString() {
		return "AverageKernelHash [height=" + height + ", width=" + width + ", filters=" + filters + "]";
	}

}
