package dev.brachtendorf.jimagehash.hashAlgorithms;

import java.math.BigInteger;

/**
 * Helper class to quickly create a bitwise byte array representation which can
 * be converted to a big integer object.
 * 
 * <p>
 * To maintain the capability to decode the created hash value back to an image
 * the order of the byte array is of utmost importance. Due to hashes ability to
 * contain non 8 compliment bit values and {@link java.math.BigInteger}
 * stripping leading zero bits the partial bit value has to be present at the 0
 * slot.
 * <p>
 * The hashbuilder systematically grows the base byte[] array as needed but
 * performs the best if the correct amount of bits are known beforehand.
 * 
 * <p>
 * In other terms this class performs the same operation as
 * 
 * <pre>
 * <code>
 * 	StringBuilder sb = new StringBuilder();
 * 	sb.append("100011");
 * 	BigInteger b = new BigInteger(sb.toString(),2);
 * </code>
 * </pre>
 * 
 * But scales much much better for higher hash values. The order of the bits are
 * flipped using the hashbuilder approach.
 * 
 * @author Kilian
 * 
 *         TODO we don't really need a Big Integer at all. We can use a byte
 *         array.
 * @since 3.0.0
 */
public class HashBuilder {

	/**
	 * Mask used to add
	 */
	private static final byte[] MASK = { 1, 1 << 1, 1 << 2, 1 << 3, 1 << 4, 1 << 5, 1 << 6, (byte) (1 << 7) };

	private byte[] bytes;
	private int arrayIndex = 0;
	// The current bit offset used to calculate the byte packing [0-7]
	private int bitIndex = 0;
	protected int length;

	/**
	 * Create a hashbuilder.
	 * 
	 * @param bits the number of bits the hash will have [8 - Integer.MAX_VALUE]. If
	 *             the builder requires more space than specified copy operations
	 *             will take place to grow the builder automatically.
	 *             <p>
	 *             Allocating to much space also results in the correct hash value
	 *             being created but might also result in a slight performance
	 *             penalty
	 */
	public HashBuilder(int bits) {
		bytes = new byte[(int) Math.ceil(bits / 8d)];
		arrayIndex = bytes.length - 1;
	}

	/**
	 * Add a zero bit to the hash
	 */
	public void prependZero() {
		if (bitIndex == 8) {
			bitIndex = 0;
			arrayIndex--;
			if (arrayIndex == -1) {
				byte[] temp = new byte[bytes.length + 1];
				System.arraycopy(bytes, 0, temp, 1, bytes.length);
				bytes = temp;
				arrayIndex = 0;
			}
		}
		bitIndex++;
		length++;
	}

	/**
	 * Add a one bit to the hash
	 */
	public void prependOne() {
		if (bitIndex == 8) {
			bitIndex = 0;
			arrayIndex--;
			if (arrayIndex == -1) {
				byte[] temp = new byte[bytes.length + 1];
				System.arraycopy(bytes, 0, temp, 1, bytes.length);
				bytes = temp;
				arrayIndex = 0;
			}
		}
		bytes[arrayIndex] |= MASK[bitIndex++];
		length++;
	}

	/**
	 * Convert the internal state of the hashbuilder to a big integer object
	 * 
	 * @return a big integer object
	 */
	public BigInteger toBigInteger() {
		return new BigInteger(1, bytes);
	}
}