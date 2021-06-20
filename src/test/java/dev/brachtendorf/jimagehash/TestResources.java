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

	public static BufferedImage white;

	public static BufferedImage transparent0;
	public static BufferedImage transparent1;
	public static BufferedImage transparent0White;
	public static BufferedImage transparent1White;

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

			white = ImageIO.read(TestResources.class.getClassLoader().getResourceAsStream("white.jpg"));

			transparent0 = ImageIO.read(TestResources.class.getClassLoader().getResourceAsStream("transparent0.png"));
			transparent1 = ImageIO.read(TestResources.class.getClassLoader().getResourceAsStream("transparent1.png"));

			transparent0White = ImageIO
					.read(TestResources.class.getClassLoader().getResourceAsStream("transparent0White.png"));
			transparent1White = ImageIO
					.read(TestResources.class.getClassLoader().getResourceAsStream("transparent1White.png"));

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
		}, () -> {
			assertTrue(lenna.getWidth() > 0);
		}, () -> {
			assertTrue(lenna90.getWidth() > 0);
		}, () -> {
			assertTrue(lenna180.getWidth() > 0);
		}, () -> {
			assertTrue(lenna270.getWidth() > 0);
		}, () -> {
			assertTrue(transparent0.getWidth() > 0);
		}, () -> {
			assertTrue(transparent1.getWidth() > 0);
		}, () -> {
			assertTrue(transparent0White.getWidth() > 0);
		}, () -> {
			assertTrue(transparent1White.getWidth() > 0);
		}, () -> {
			assertTrue(white.getWidth() > 0);
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
