package hashAlgorithms;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.math.BigInteger;

public abstract class HashingAlgorithm {
	
	int bitResoluation;
	
	/**
	 * Promises a key with at approximately bit resolution. Due to geometric requirements the key might be marginally larger or smaller than specified
	 * @param bitResolution
	 */
	public HashingAlgorithm(int bitResolution){
		this.bitResoluation = bitResolution;
	}
	
	/**
	 * Calculate a hash for the given image. Invoking the hash function on the same
	 * image has to return the same hash value.
	 * A comparison of the hashes relates to the similarity of the images.
	 * The lower the value the more similar the images are. Equal images will produce a similarity of 0
	 * @param image
	 * @return
	 */
	public abstract BigInteger hash(BufferedImage image);
	
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
}
