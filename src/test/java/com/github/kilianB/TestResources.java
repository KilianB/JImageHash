package com.github.kilianB;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.math.BigInteger;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;

import com.github.kilianB.hash.Hash;

/**
 * @author Kilian
 *
 */
public class TestResources {

	public static BufferedImage ballon;
	// Similar images
	public static BufferedImage copyright;
	public static BufferedImage highQuality;
	public static BufferedImage lowQuality;
	public static BufferedImage thumbnail;
	
	public static BufferedImage lenna;
	public static BufferedImage lenna90;
	public static BufferedImage lenna180;
	public static BufferedImage lenna270;

	static {
		try {
			ballon = ImageIO.read(TestResources.class.getClassLoader().getResourceAsStream("ballon.jpg"));
			copyright = ImageIO.read(TestResources.class.getClassLoader().getResourceAsStream("copyright.jpg"));
			highQuality = ImageIO.read(TestResources.class.getClassLoader().getResourceAsStream("highQuality.jpg"));
			lowQuality = ImageIO.read(TestResources.class.getClassLoader().getResourceAsStream("lowQuality.jpg"));
			thumbnail = ImageIO.read(TestResources.class.getClassLoader().getResourceAsStream("thumbnail.jpg"));
			lenna = ImageIO.read(TestResources.class.getClassLoader().getResourceAsStream("Lenna.png"));
			lenna90 = ImageIO.read(TestResources.class.getClassLoader().getResourceAsStream("Lenna90.png"));
			lenna180 = ImageIO.read(TestResources.class.getClassLoader().getResourceAsStream("Lenna180.png"));
			lenna270 = ImageIO.read(TestResources.class.getClassLoader().getResourceAsStream("Lenna270.png"));

		} catch (IOException e) {
			e.printStackTrace();
		}
	};

	@Test
	public void allResourcesLoaded() {

		assertAll(() -> {
			assertTrue(ballon.getWidth() > 0);
		}, () -> {
			assertTrue(copyright.getWidth() > 0);
		}, () -> {
			assertTrue(highQuality.getWidth() > 0);
		}, () -> {
			assertTrue(lowQuality.getWidth() > 0);
		}, () -> {
			assertTrue(thumbnail.getWidth() > 0);
		},() -> {
			assertTrue(lenna.getWidth() > 0);
		},() -> {
			assertTrue(lenna90.getWidth() > 0);
		},() -> {
			assertTrue(lenna180.getWidth() > 0);
		},() -> {
			assertTrue(lenna270.getWidth() > 0);
		});
	}

	/**
	 * Create a dummy hash
	 * 
	 * @param bits
	 * @param algoId
	 * @return
	 */
	public static Hash createHash(String bits, int algoId) {
		return new Hash(new BigInteger(bits, 2), bits.length(), algoId);
	}

}
