package com.github.kilianB;

import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.imageio.ImageIO;

import com.github.kilianB.hashAlgorithms.AverageHash;
import com.github.kilianB.hashAlgorithms.HashingAlgorithm;
import com.github.kilianB.hashAlgorithms.PerceptiveHash;
import com.github.kilianB.hashAlgorithms.Rotational;
import com.github.kilianB.hashAlgorithms.RotPHash;
import com.github.kilianB.matcher.Hash;

/**
 * @author Kilian
 *
 */
public class RotationalTest {

	// Check if the algorithms are rotational save

	public static void main(String[] args) throws IOException {
		BufferedImage lenna = ImageIO.read(RotationalTest.class.getClassLoader().getResourceAsStream("Lenna.png"));
		BufferedImage lenna90 = ImageIO.read(RotationalTest.class.getClassLoader().getResourceAsStream("Lenna90.png"));
		BufferedImage lenna180 = ImageIO
				.read(RotationalTest.class.getClassLoader().getResourceAsStream("Lenna180.png"));
		BufferedImage lenna270 = ImageIO
				.read(RotationalTest.class.getClassLoader().getResourceAsStream("Lenna270.png"));

		BufferedImage ballon = ImageIO.read(RotationalTest.class.getClassLoader().getResourceAsStream("ballon.jpg"));
		BufferedImage highQuality = ImageIO
				.read(RotationalTest.class.getClassLoader().getResourceAsStream("highQuality.jpg"));
		BufferedImage lowQuality = ImageIO
				.read(RotationalTest.class.getClassLoader().getResourceAsStream("lowQuality.jpg"));
		BufferedImage thumbnail = ImageIO
				.read(RotationalTest.class.getClassLoader().getResourceAsStream("thumbnail.jpg"));
		
		System.out.println("Average");

		HashingAlgorithm hasher = new AverageHash(64);
//
//		System.out.println(hasher.hash(lenna));
//		System.out.println(hasher.hash(lenna90));
//		System.out.println(hasher.hash(lenna180));
//		System.out.println(hasher.hash(lenna270));
//		
//		System.out.println("Difference");
//		
//		hasher = new DifferenceHash(64, Precision.Double);
//
//		System.out.println(hasher.hash(lenna));
//		System.out.println(hasher.hash(lenna90));
//		System.out.println(hasher.hash(lenna180));
//		System.out.println(hasher.hash(lenna270));
//		
//		System.out.println("Perceptive");
//		
		hasher = new PerceptiveHash(64);

//		System.out.println(hasher.hash(lenna));
//		System.out.println(hasher.hash(lenna90));
//		System.out.println(hasher.hash(lenna180));
//		System.out.println(hasher.hash(lenna270));
//		
//		hasher = new Rotational(32);
//		ImageIO.write(hasher.hash(lenna).toImage(2),"png",new File("Rotational.png"));
//		
//		System.out.println(hasher.hash(lenna90));
//		System.out.println(hasher.hash(lenna180));
//		System.out.println(hasher.hash(lenna270));
//		System.out.println(hasher.hash(ballon));
//		System.out.println(hasher.hash(highQuality));
//		System.out.println(hasher.hash(lowQuality));

//		hasher = new RotPHash(32);
		
		hasher = new AverageHash(32);
		Hash lennaHash = hasher.hash(lenna);
		
		System.out.println();
		Hash ballonHash = hasher.hash(ballon);
		System.out.println();
		Hash hqHash = hasher.hash(highQuality);
		System.out.println();
		Hash lqHash = hasher.hash(lowQuality);
		System.out.println();
		Hash thumbHash = hasher.hash(thumbnail);
		System.out.println();
		Hash lena90Hash = hasher.hash(lenna90);
//		System.out.println();
//		System.out.println(hasher.hash(lenna180));
//		System.out.println();
//		System.out.println(hasher.hash(lenna270));
////		
//		System.out.println(hasher.hash(ballon));
////		
//		System.out.println(hasher.hash(highQuality));
////		
//		System.out.println(hasher.hash(lowQuality));
//
		System.out.println("Lenna l: " + lennaHash.normalizedHammingDistanceFast(lena90Hash));
		
		System.out.println("HQ Thumb: " + hqHash.normalizedHammingDistanceFast(thumbHash));
		System.out.println("HQ LQ   : " + hqHash.normalizedHammingDistanceFast(lqHash));
	
		System.out.println("HQ Ballon: " + hqHash.normalizedHammingDistanceFast(ballonHash));
		System.out.println(lqHash.normalizedHammingDistanceFast(ballonHash));
		System.out.println(thumbHash.normalizedHammingDistanceFast(ballonHash));
		System.out.println(hqHash.normalizedHammingDistanceFast(lennaHash));
		System.out.println(lqHash.normalizedHammingDistanceFast(lennaHash));
		System.out.println(thumbHash.normalizedHammingDistanceFast(lennaHash));
		System.out.println(lennaHash.normalizedHammingDistanceFast(ballonHash));
//		
		/*
		 *
		 * Hash: 1101001000110110011010000010100110110 [algoId: 1035058042] Hash:
		 * 1100001111001011001100100100100001001 [algoId: 1035058042] Hash:
		 * 1000010100111001101111001001011001111 [algoId: 1035058042] Hash:
		 * 1001011011000110000011011000111011001 [algoId: 1035058042] Hash:
		 * 1100011100011100111001110111100001100 [algoId: 1035058042] Hash:
		 * 1000000001011111111100100100101001110 [algoId: 1035058042] Hash:
		 * 1000000001011111111100100100001001010 [algoId: 1035058042]
		 */
	}

}
