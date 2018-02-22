package hashAlgorithms;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.io.IOException;
import java.math.BigInteger;

import javax.imageio.ImageIO;

/**
 * Calculates a hash based on gradient tracking. This hash is cheap to compute
 * and provides a high degree of accuracy. Robust to a huge range of color transformation
 * @author Kilian
 *
 */
public class DifferenceHash extends HashingAlgorithm {

	int height, width;
	boolean doublePrecision, triplePrecision;

	/**
	 * @param bitResolution
	 * The bit resolution specifies the final length of the generated hash. A higher resolution will increase computation
	 * time and space requirement while being able to track finer detail in the image. Be aware that a high key is not always
	 * desired.
	 * The bit resolution is only an approximation of the final hash length.
	 * @param doublePrecision
	 * 	Also compute the the top to bottom gradient. This will double the key size while increasing matching quality
	 * 
	 * @param triplePrecision
	 * Also compute the the diagonal gradient. This will triple the key size while increasing matching quality.
	 * This parameter only takes effect if the doublePrecision is true 
	 */
	public DifferenceHash(int bitResolution, boolean doublePrecision, boolean triplePrecision) {
		super(bitResolution);

		// width * height = bitResolution -> (height + 1) * height =
		// bitResolution

		// Substitute
		// (x * (x+1)) = y
		// x^2 + x = y -> 1/2 * ( +- sqrt(4y + 1) - 1)

		// only look at the positive part and truncate
		int x = (int) Math.round(0.5 * (Math.sqrt(4 * bitResolution + 1) - 1));

		height = x;
		//Width +1 for padding
		width = x + 1;
		
		this.doublePrecision = doublePrecision;
		this.triplePrecision = triplePrecision;
	}

	
	@Override
	public BigInteger hash(BufferedImage image) {
		BufferedImage transformed = getGrayScaledInstance(image, width, height);
		byte[] data = ((DataBufferByte) transformed.getRaster().getDataBuffer()).getData();
		
		//Calculate the left to right gradient 
		
		BigInteger hash = BigInteger.ZERO;
		final int pixelCount = data.length;
		int xCount = 1;

		for (int i = 1; i < pixelCount; i++) {
			// if we reached the end of a row skip the padding pixel row.
			if (xCount >= width) {
				xCount = 1;
				continue;
			}
			// No color model needed as we only care about equality
			if (data[i] >= data[i - 1]) {
				hash = hash.shiftLeft(1);
			} else {
				hash = hash.shiftLeft(1).add(BigInteger.ONE);
			}
			xCount++;
		}

		
		// Top to bottom gradient
		if (doublePrecision) {
			// We need a padding row at the top now.
			// Caution width and height are swapped
			transformed = getGrayScaledInstance(image, height, width);
			data = ((DataBufferByte) transformed.getRaster().getDataBuffer()).getData();

			int height = transformed.getHeight();
			int width = transformed.getWidth();
			
			for (int x = 0; x < width; x++) {
				for (int y = 1; y < height; y++) {

					/*
					 *   0  1  2  3  4  5  6  
					 * 0|0  1  2  3  4  5  6 
					 * 1|7  8  9 10 11 12 13 
					 * 2| 
					 * 3| 
					 * 4| 
					 * 5|
					 *  |____________________ 
					 *   0  1  2  3  4  5  6
					 */
					int index = y * width + x;
					int indexAbove = (y - 1) * width + x;

					if ((data[index] & 0xFF) >= (data[indexAbove] & 0xFF)) {
						hash = hash.shiftLeft(1);
					} else {
						hash = hash.shiftLeft(1).add(BigInteger.ONE);
					}
				}
			}
			//Diagonally hash
			if(triplePrecision){
				
				transformed = getGrayScaledInstance(image, this.height+1, this.width+1);
				data = ((DataBufferByte) transformed.getRaster().getDataBuffer()).getData();

				height = transformed.getHeight();
				width = transformed.getWidth();

				for (int x = 1; x < width; x++) {
					for (int y = 1; y < height; y++) {

						/*
						 *   0  1  2  3  4  5  6  
						 * 0|0  1  2  3  4  5  6 
						 * 1|7  8  9 10 11 12 13 
						 * 2| 
						 * 3| 
						 * 4| 
						 * 5|
						 *  |____________________ 
						 *   0  1  2  3  4  5  6
						 */
						int index = y * width + x;
						int indexUpperLeft = (y-1) * width + (x-1);

						if ((data[index] & 0xFF) >= (data[indexUpperLeft] & 0xFF)) {
							hash = hash.shiftLeft(1);
						} else {
							hash = hash.shiftLeft(1).add(BigInteger.ONE);
						}
					}
				}
			}
			
		}

		return hash;
	}
}
