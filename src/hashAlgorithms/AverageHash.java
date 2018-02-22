package hashAlgorithms;

import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.io.IOException;
import java.math.BigInteger;

import javax.imageio.ImageIO;

/**
 * Calculate a hash value based on the average color in an image. This hash reacts to changes 
 * in hue and saturation and is arguably slow
 * @author Kilian
 *
 */
public class AverageHash extends HashingAlgorithm{


	/**
	 * 
	 * 
	 * @param bitResolution 
	 * The bit resolution specifies the final length of the generated hash. A higher resolution will increase computation
	 * time and space requirement while being able to track finer detail in the image. Be aware that a high key is not always
	 * desired.
	 * 
	 * The average hash requires to re scale the base image according to the required bit resolution.
	 * 	If the square root of the bit resolution is not a natural number the resolution will be rounded to the next whole 
	 *  number.
	 *  
	 *  64 = 8x8 = 65 bit key
	 *  128 = 11.3 -> 12 -> 144 bit key
	 *  256 = 16 x 16 = 256 bit key
	 *  
	 */
	public AverageHash(int bitResolution) {
		super(bitResolution);	
			int dimension = (int)Math.round(Math.sqrt(bitResolution));
			this.width = dimension;
			this.height = dimension;
	}

	/**
	 * Width of the rescaled hash image
	 */
	private int width; 
	
	/**
	 * Hight of the rescaled hash image
	 */
	private int height;
	
	
	@Override
	public BigInteger hash(BufferedImage image) {
		BufferedImage transformed = getGrayScaledInstance(image,width,height);
		
		//Calculate the average color of the entire image
		
		int pixelCount = width* height;
		double avgPixelValue = 0;

		ColorModel colorModel = transformed.getColorModel();
		
		final byte[] pixelData = ((DataBufferByte) transformed.getRaster().getDataBuffer()).getData();
	
		//Fast pixel access
		for(byte b: pixelData){
			int grayValue = colorModel.getBlue(b & 0xFF);
			avgPixelValue += ((double)grayValue / pixelCount);
		}

		//Create hash
		BigInteger hash = BigInteger.ZERO;
	
		for(byte b : pixelData){
			if(colorModel.getBlue((b & 0xFF)) < avgPixelValue)
			{
				hash = hash.shiftLeft(1);
			}else{
				hash = hash.shiftLeft(1).add(BigInteger.ONE);
			}
		}
		
		return hash;
	}

}
