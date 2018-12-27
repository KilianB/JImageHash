package com.github.kilianB.matcher;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigInteger;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.github.kilianB.hashAlgorithms.AverageHash;

class HashTest {

	@Nested 
	class IncompatibleAlgorithms{
		@Test
		void normalizedHammingDistance() {
			int algoId = new AverageHash(14).algorithmId();
			int algoId1 = new AverageHash(500).algorithmId();
			Hash hash0 = new Hash(BigInteger.ONE, 1, algoId);
			Hash hash1 = new Hash(BigInteger.ONE, 1, algoId1);
			assertThrows(IllegalArgumentException.class, () -> {
				hash0.hammingDistance(hash1);
			});
		}
		
		@Test
		void hammingDistance() {
			int algoId = new AverageHash(14).algorithmId();
			int algoId1 = new AverageHash(500).algorithmId();
			Hash hash0 = new Hash(BigInteger.ONE, 1, algoId);
			Hash hash1 = new Hash(BigInteger.ONE, 1, algoId1);
			assertThrows(IllegalArgumentException.class, () -> {
				hash0.normalizedHammingDistance(hash1);
			});
		}
	}
	

	@Nested
	class Equality {

		@Test
		void sameObject() {
			Hash hash0 = new Hash(new BigInteger("010101000", 2), 0, 0);
			assertEquals(hash0, hash0);
		}

		@Test
		void equalContent() {
			Hash hash0 = new Hash(new BigInteger("1010101000", 2), 0, 0);
			Hash hash1 = new Hash(new BigInteger("1010101000", 2), 0, 0);
			assertEquals(hash0, hash1);
		}

	}

	@Nested
	class HammingDistance {
		@Test
		@DisplayName("Identical Hash")
		void identical() {
			String bits = "10101010";
			Hash hash0 = new Hash(new BigInteger(bits, 2), bits.length(), 0);
			assertEquals(0, hash0.hammingDistance(hash0));
		}

		@Test
		@DisplayName("Identical Hash Fast")
		void identicalFast() {
			String bits = "10101010";
			Hash hash0 = new Hash(new BigInteger(bits, 2), bits.length(), 0);
			assertEquals(0, hash0.hammingDistanceFast(hash0));
		}

		@Test
		@DisplayName("Difference Padded")
		void distanceThree() {
			String bits = "10001100";
			String bits1 = "11111100";
			Hash hash0 = new Hash(new BigInteger(bits, 2), bits.length(), 0);
			Hash hash1 = new Hash(new BigInteger(bits1, 2), bits1.length(), 0);
			assertEquals(3, hash0.hammingDistance(hash1));
		}

		@Test
		@DisplayName("Difference Padded Fast")
		void distanceThreeFast() {
			String bits = "10001100";
			String bits1 = "11111100";
			Hash hash0 = new Hash(new BigInteger(bits, 2), bits.length(), 0);
			Hash hash1 = new Hash(new BigInteger(bits1, 2), bits1.length(), 0);
			assertEquals(3, hash0.hammingDistanceFast(hash1));
		}
	}

	@Nested
	class NormalizedHammingDistance {
		@Test
		@DisplayName("Identical Hash")
		void identical() {
			String bits = "10101010";
			Hash hash0 = new Hash(new BigInteger(bits, 2), bits.length(), 0);
			assertEquals(0, hash0.normalizedHammingDistance(hash0));
		}

		@Test
		@DisplayName("Identical Hash Fast")
		void identicalFast() {
			String bits = "10101010";
			Hash hash0 = new Hash(new BigInteger(bits, 2), bits.length(), 0);
			assertEquals(0, hash0.normalizedHammingDistanceFast(hash0));
		}

		@Test
		@DisplayName("Difference Padded")
		void dstanceThree() {
			String bits = "10001100";
			String bits1 = "11111100";
			Hash hash0 = new Hash(new BigInteger(bits, 2), bits.length(), 0);
			Hash hash1 = new Hash(new BigInteger(bits1, 2), bits1.length(), 0);
			assertEquals(3 / 8d, hash0.normalizedHammingDistance(hash1));
		}

		@Test
		@DisplayName("Difference Padded Fast")
		void distanceThreeFast() {
			String bits = "10001100";
			String bits1 = "11111100";
			Hash hash0 = new Hash(new BigInteger(bits, 2), bits.length(), 0);
			Hash hash1 = new Hash(new BigInteger(bits1, 2), bits1.length(), 0);
			assertEquals(3 / 8d, hash0.normalizedHammingDistanceFast(hash1));
		}

		@Test
		@DisplayName("Difference Leading zero")
		void dstanceThreeLeading() {
			String bits = "01001100";
			String bits1 = "11111100";
			Hash hash0 = new Hash(new BigInteger(bits, 2), bits.length(), 0);
			Hash hash1 = new Hash(new BigInteger(bits1, 2), bits1.length(), 0);
			assertEquals(3 / 8d, hash0.normalizedHammingDistance(hash1));
		}

	}

	@Nested
	class TestBit {

		@Test
		void negativeOutOfBounds() {
			String bits = "10101010";
			Hash hash0 = new Hash(new BigInteger(bits, 2), bits.length(), 0);
			assertThrows(IllegalArgumentException.class, () -> {
				hash0.getBit(-1);
			});
		}
		
		@Test
		void positiveOutOfBounds() {
			String bits = "10101010";
			Hash hash0 = new Hash(new BigInteger(bits, 2), bits.length(), 0);
			assertThrows(IllegalArgumentException.class, () -> {
				hash0.getBit(100);
			});
		}
		
		@Test
		void trueBit() {
			String bits = "10101010";
			Hash hash0 = new Hash(new BigInteger(bits, 2), bits.length(), 0);
			assertTrue(hash0.getBit(1));
		}
		
		@Test
		void falseBit() {
			String bits = "10101010";
			Hash hash0 = new Hash(new BigInteger(bits, 2), bits.length(), 0);
			assertFalse(hash0.getBit(0));
		}
		
		@Test
		void falseBitOutOfBounds() {
			String bits = "10101010";
			Hash hash0 = new Hash(new BigInteger(bits, 2), bits.length(), 0);
			assertFalse(hash0.getBitUnsafe(100));
		}
	}
	
	@Nested
	class ToString{
		
		@Test
		void displayAllBits() {
			String bits = "10101010";
			String toString = new Hash(new BigInteger(bits, 2), bits.length(), 0).toString();
			
			//Leading zeros
			
			String bitsLZero = "00001000";
			String toStringLZero = new Hash(new BigInteger(bitsLZero, 2), bitsLZero.length(), 0).toString();
			
			String bitsOnlyZero = "00000000";
			String toStringOZero = new Hash(new BigInteger(bitsOnlyZero, 2), bitsOnlyZero.length(), 0).toString();	
			
			assertAll(
					()->{assertEquals(toString.length(),toStringLZero.length());},
					()->{assertEquals(toString.length(),toStringOZero.length());}
					);
		}		
	}

	
}
