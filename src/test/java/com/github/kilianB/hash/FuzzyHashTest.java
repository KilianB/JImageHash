package com.github.kilianB.hash;

import static com.github.kilianB.TestResources.ballon;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.math.BigInteger;
import java.util.Arrays;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.github.kilianB.ArrayUtil;
import com.github.kilianB.TestResources;
import com.github.kilianB.hashAlgorithms.AverageHash;
import com.github.kilianB.pcg.fast.PcgRSFast;

/**
 * @author Kilian
 *
 */
class FuzzyHashTest{

	@Nested
	class MergeHash {

		@Test
		public void illegalAlgorithm() {

			Hash h = new Hash(BigInteger.ZERO, 1, 1);
			Hash h1 = new Hash(BigInteger.ZERO, 1, 0);

			FuzzyHash hash = new FuzzyHash();
			hash.merge(h);

			assertThrows(IllegalArgumentException.class, () -> {
				hash.merge(h1);
			});
		}

		@Test
		public void illegalLength() {

			Hash h = new Hash(BigInteger.ZERO, 1, 0);
			Hash h1 = new Hash(BigInteger.ZERO, 2, 0);

			FuzzyHash hash = new FuzzyHash();
			hash.merge(h);

			assertThrows(IllegalArgumentException.class, () -> {
				hash.merge(h1);
			});
		}

		@Test
		public void noHash() {
			FuzzyHash hash = new FuzzyHash();
			assertThrows(IllegalArgumentException.class, () -> {
				hash.merge();
			});
		}

		@Test
		public void noHashFast() {
			FuzzyHash hash = new FuzzyHash();
			assertThrows(IllegalArgumentException.class, () -> {
				hash.mergeFast();
			});
		}

		@Test
		public void mergeFastBatch() {
			FuzzyHash hash = new FuzzyHash();
			assertThrows(IllegalArgumentException.class, () -> {
				hash.mergeFast(new FuzzyHash[0]);
			});
		}
		
	}

	@Nested
	class Subtract {

		@Test
		public void illegalAlgorithm() {

			Hash h = new Hash(BigInteger.ZERO, 1, 1);
			Hash h1 = new Hash(BigInteger.ZERO, 1, 0);

			FuzzyHash hash = new FuzzyHash();
			hash.subtract(h);

			assertThrows(IllegalArgumentException.class, () -> {
				hash.subtract(h1);
			});
		}

		@Test
		public void illegalLength() {

			Hash h = new Hash(BigInteger.ZERO, 1, 0);
			Hash h1 = new Hash(BigInteger.ZERO, 2, 0);

			FuzzyHash hash = new FuzzyHash();
			hash.subtract(h);

			assertThrows(IllegalArgumentException.class, () -> {
				hash.subtract(h1);
			});
		}

		@Test
		void subtractOne() {
			int bits = 10;
			FuzzyHash hash = new FuzzyHash();
			Hash h = new Hash(new BigInteger("0000000000", 2), bits, 0);
			Hash h1 = new Hash(new BigInteger("1111111111", 2), bits, 0);
			hash.merge(h);
			hash.merge(h1);
			assertNotEquals(0, hash.normalizedHammingDistance(h));
			assertNotEquals(1, hash.normalizedHammingDistance(h1));
			hash.subtract(h1);
			assertEquals(0, hash.normalizedHammingDistance(h));
			assertEquals(1, hash.normalizedHammingDistance(h1));
		}

	}

	@Nested
	class UncertainMask {

		@Test
		public void fullCertainty() {
			int bits = 10;
			FuzzyHash hash = new FuzzyHash();
			Hash h = new Hash(new BigInteger("1111111111", 2), bits, 0);

			hash.merge(h);
			boolean mask[] = hash.getUncertaintyMask(0.3);
			boolean expected[] = new boolean[bits];
			ArrayUtil.fillArray(expected, () -> {
				return false;
			});

			assertArrayEquals(expected, mask);
		}

		@Test
		public void fullCertaintyMixed() {
			int bits = 10;
			FuzzyHash hash = new FuzzyHash();
			Hash h = new Hash(new BigInteger("1111100000", 2), bits, 0);

			hash.merge(h);
			boolean mask[] = hash.getUncertaintyMask(0.3);
			boolean expected[] = new boolean[bits];
			ArrayUtil.fillArray(expected, () -> {
				return false;
			});

			assertArrayEquals(expected, mask);
		}

		@Test
		public void fullUncertainty() {
			int bits = 10;
			FuzzyHash hash = new FuzzyHash();
			Hash h = new Hash(new BigInteger("1111100000", 2), bits, 0);
			Hash h2 = new Hash(new BigInteger("0000011111", 2), bits, 0);

			hash.merge(h, h2);

			boolean mask[] = hash.getUncertaintyMask(0.3);
			boolean expected[] = new boolean[bits];
			ArrayUtil.fillArray(expected, () -> {
				return true;
			});
			assertArrayEquals(expected, mask);
		}

		@Test
		public void zero() {
			FuzzyHash hash = new FuzzyHash();
			Hash h = new Hash(new BigInteger("0", 2), 1, 0);
			Hash h2 = new Hash(new BigInteger("0", 2), 1, 0);
			hash.merge(h, h2);
			boolean[] uncertainBit = hash.getUncertaintyMask(0.3);
			assertArrayEquals(new boolean[] { false }, uncertainBit);
		}

		@Test
		public void one() {
			FuzzyHash hash = new FuzzyHash();
			Hash h = new Hash(new BigInteger("1", 2), 1, 0);
			Hash h2 = new Hash(new BigInteger("1", 2), 1, 0);
			hash.merge(h, h2);
			boolean[] uncertainBit = hash.getUncertaintyMask(0.3);
			assertArrayEquals(new boolean[] { false }, uncertainBit);
		}

		@Test
		public void both() {
			FuzzyHash hash = new FuzzyHash();
			Hash h = new Hash(new BigInteger("0", 2), 1, 0);
			Hash h2 = new Hash(new BigInteger("1", 2), 1, 0);
			hash.merge(h, h2);
			boolean[] uncertainBit = hash.getUncertaintyMask(0.3);
			assertArrayEquals(new boolean[] { true }, uncertainBit);
		}

		@Test
		public void partially() {
			int bits = 5;
			FuzzyHash hash = new FuzzyHash();
			Hash h = new Hash(new BigInteger("01010", 2), bits, 0);
			Hash h2 = new Hash(new BigInteger("01010", 2), bits, 0);
			Hash h3 = new Hash(new BigInteger("11010", 2), bits, 0);
			Hash h4 = new Hash(new BigInteger("10110", 2), bits, 0);

			hash.merge(h, h2, h3, h4);

			boolean mask[] = hash.getUncertaintyMask(0.8);
			boolean expected[] = { false, false, true, true, true };
			assertArrayEquals(expected, mask);
		}

		@Test
		public void all() {
			int bits = 2;
			FuzzyHash hash = new FuzzyHash();
			Hash h = new Hash(new BigInteger("11", 2), bits, 0);
			Hash h2 = new Hash(new BigInteger("11", 2), bits, 0);

			hash.merge(h, h2);
			boolean mask[] = hash.getUncertaintyMask(0.2);
			boolean expected[] = new boolean[bits];
			ArrayUtil.fillArray(expected, () -> {
				return false;
			});
			assertArrayEquals(expected, mask);
		}
	}

	@Nested
	class Distance {

		@Test
		public void identity() {
			int bits = 10;
			FuzzyHash hash = new FuzzyHash();
			Hash h = new Hash(new BigInteger("1111111111", 2), bits, 0);
			hash.merge(h);
			assertEquals(0, hash.hammingDistance(hash));
		}

		@Test
		public void oneIdentity() {
			int bits = 10;
			FuzzyHash hash = new FuzzyHash();
			Hash h = new Hash(new BigInteger("1111111111", 2), bits, 0);
			hash.merge(h);
			assertEquals(hash.hammingDistance(h), h.hammingDistance(hash));
			assertEquals(0, hash.hammingDistance(h));
		}

		@Test
		public void zeroIdentity() {
			int bits = 10;
			FuzzyHash hash = new FuzzyHash();
			Hash h = new Hash(new BigInteger("0000000000", 2), bits, 0);
			hash.merge(h);
			assertEquals(hash.hammingDistance(h), h.hammingDistance(hash));
			assertEquals(0, hash.hammingDistance(h));
		}

		@Test
		public void maxDistOne() {
			int bits = 10;
			FuzzyHash hash = new FuzzyHash();
			Hash h = new Hash(new BigInteger("0000000000", 2), bits, 0);
			Hash h1 = new Hash(new BigInteger("1111111111", 2), bits, 0);
			hash.merge(h);
			assertEquals(hash.hammingDistance(h1), h1.hammingDistance(hash));
			assertEquals(10, hash.hammingDistance(h1));
		}

		@Test
		public void maxDistZero() {
			int bits = 10;
			Hash h = new Hash(new BigInteger("1111111111", 2), bits, 0);
			FuzzyHash hash = new FuzzyHash();
			Hash h1 = new Hash(new BigInteger("0000000000", 2), bits, 0);
			hash.merge(h);
			assertEquals(hash.hammingDistance(h1), h1.hammingDistance(hash));
			assertEquals(10, hash.hammingDistance(h1));
		}

		@Test
		public void distMixed() {
			int bits = 10;
			Hash h = new Hash(new BigInteger("0000111111", 2), bits, 0);
			FuzzyHash hash = new FuzzyHash();
			Hash h1 = new Hash(new BigInteger("0000000000", 2), bits, 0);
			hash.merge(h);
			assertEquals(hash.hammingDistance(h1), h1.hammingDistance(hash));
			assertEquals(6, hash.hammingDistance(h1));
		}

		@Test
		public void identityNorm() {
			int bits = 10;
			FuzzyHash hash = new FuzzyHash();
			Hash h = new Hash(new BigInteger("1111111111", 2), bits, 0);
			hash.merge(h);
			assertEquals(0, hash.normalizedHammingDistance(hash));
		}

		@Test
		public void oneIdentityNorm() {
			int bits = 10;
			FuzzyHash hash = new FuzzyHash();
			Hash h = new Hash(new BigInteger("1111111111", 2), bits, 0);
			hash.merge(h);
			assertEquals(hash.normalizedHammingDistance(h), h.normalizedHammingDistance(hash));
			assertEquals(0, hash.normalizedHammingDistance(h));
		}

		@Test
		public void zeroIdentityNorm() {
			int bits = 10;
			FuzzyHash hash = new FuzzyHash();
			Hash h = new Hash(new BigInteger("0000000000", 2), bits, 0);
			hash.merge(h);
			assertEquals(hash.normalizedHammingDistance(h), h.normalizedHammingDistance(hash));
			assertEquals(0, hash.normalizedHammingDistance(h));
		}

		@Test
		public void maxDistOneNorm() {
			int bits = 10;
			FuzzyHash hash = new FuzzyHash();
			Hash h = new Hash(new BigInteger("0000000000", 2), bits, 0);
			Hash h1 = new Hash(new BigInteger("1111111111", 2), bits, 0);
			hash.merge(h);
			assertEquals(hash.normalizedHammingDistance(h1), h1.normalizedHammingDistance(hash));
			assertEquals(1, hash.normalizedHammingDistance(h1));
		}

		@Test
		public void maxDistZeroNorm() {
			int bits = 10;
			Hash h = new Hash(new BigInteger("1111111111", 2), bits, 0);
			FuzzyHash hash = new FuzzyHash();
			Hash h1 = new Hash(new BigInteger("0000000000", 2), bits, 0);
			hash.merge(h);
			assertEquals(hash.normalizedHammingDistance(h1), h1.normalizedHammingDistance(hash));
			assertEquals(1, hash.normalizedHammingDistance(h1));
		}

		@Test
		public void distMixedNorm() {
			int bits = 10;
			Hash h = new Hash(new BigInteger("0000111111", 2), bits, 0);
			FuzzyHash hash = new FuzzyHash();
			Hash h1 = new Hash(new BigInteger("0000000000", 2), bits, 0);
			hash.merge(h);
			assertEquals(hash.normalizedHammingDistance(h1), h1.normalizedHammingDistance(hash));
			assertEquals(6 / 10d, hash.normalizedHammingDistance(h1));
		}

		@Test
		public void ballonHash() {
			AverageHash aHash = new AverageHash(32);
			Hash baHash = aHash.hash(ballon);
			FuzzyHash hash = new FuzzyHash();

			hash.merge(baHash);

			assertEquals(hash.hammingDistance(baHash), baHash.hammingDistance(hash));
			assertEquals(0, hash.hammingDistance(baHash));
		}

		@Test
		public void ballonHashAfterReset() {
			AverageHash aHash = new AverageHash(32);
			Hash baHash = aHash.hash(ballon);
			FuzzyHash hash = new FuzzyHash();

			hash.merge(baHash);
			hash.reset();
			assertEquals(hash.hammingDistance(baHash), baHash.hammingDistance(hash));
			assertEquals(0, hash.hammingDistance(baHash));
		}
	}

	@Nested
	class MaximumDistance {

		@Test
		public void empty() {
			FuzzyHash fuzzy = new FuzzyHash();
			assertEquals(0, fuzzy.getMaximalError());
		}

		@Test
		public void length1Zero() {
			FuzzyHash fuzzy = new FuzzyHash();
			fuzzy.merge(new Hash(BigInteger.ZERO, 1, 0));
			assertEquals(1, fuzzy.getMaximalError());
		}

		@Test
		public void length1One() {
			FuzzyHash fuzzy = new FuzzyHash();
			fuzzy.merge(new Hash(BigInteger.ONE, 1, 0));
			assertEquals(1, fuzzy.getMaximalError());
		}

		@Test
		public void length10Zero() {
			int bits = 10;
			FuzzyHash fuzzy = new FuzzyHash();
			Hash h = new Hash(new BigInteger("1111111111", 2), bits, 0);
			fuzzy.merge(h);
			assertEquals(1, fuzzy.getMaximalError(), 1e-12);
		}

		@Test
		public void length10One() {
			int bits = 10;
			FuzzyHash fuzzy = new FuzzyHash();
			Hash h = new Hash(new BigInteger("0000000000", 2), bits, 0);
			fuzzy.merge(h);
			assertEquals(1, fuzzy.getMaximalError(), 1e-12);
		}

	}

	@Nested
	class WeightedDistance {

		@Nested
		class FuzzyNormal{
			@Test
			public void identity() {
				int bits = 10;
				FuzzyHash hash = new FuzzyHash();
				Hash h = new Hash(new BigInteger("1111111111", 2), bits, 0);
				hash.merge(h);
				assertEquals(0, hash.weightedDistance(h));
			}

			@Test
			public void oneIdentity() {
				int bits = 10;
				FuzzyHash hash = new FuzzyHash();
				Hash h = new Hash(new BigInteger("1111111111", 2), bits, 0);
				hash.merge(h);
				assertEquals(0, hash.weightedDistance(h));
			}

			@Test
			public void zeroIdentity() {
				int bits = 10;
				FuzzyHash hash = new FuzzyHash();
				Hash h = new Hash(new BigInteger("0000000000", 2), bits, 0);
				hash.merge(h);
				assertEquals(0, hash.weightedDistance(h));
			}

			@Test
			public void maxDistOne() {
				int bits = 10;
				FuzzyHash hash = new FuzzyHash();
				Hash h = new Hash(new BigInteger("0000000000", 2), bits, 0);
				Hash h1 = new Hash(new BigInteger("1111111111", 2), bits, 0);
				hash.merge(h);
				assertEquals(1, hash.weightedDistance(h1));
			}

			@Test
			public void maxDistZero() {
				int bits = 10;
				Hash h = new Hash(new BigInteger("1111111111", 2), bits, 0);
				FuzzyHash hash = new FuzzyHash();
				Hash h1 = new Hash(new BigInteger("0000000000", 2), bits, 0);
				hash.merge(h);
				assertEquals(1, hash.weightedDistance(h1));
			}

			@Test
			public void distMixed() {
				int bits = 10;
				Hash h = new Hash(new BigInteger("0000111111", 2), bits, 0);
				FuzzyHash hash = new FuzzyHash();
				Hash h1 = new Hash(new BigInteger("0000000000", 2), bits, 0);
				hash.merge(h);
				assertEquals(.6, hash.weightedDistance(h1));
			}

			@Test
			public void ballonHash() {
				AverageHash aHash = new AverageHash(32);
				Hash baHash = aHash.hash(ballon);
				FuzzyHash hash = new FuzzyHash();

				hash.merge(baHash);

				assertEquals(hash.hammingDistance(baHash), baHash.hammingDistance(hash));
				assertEquals(0, hash.weightedDistance(baHash));
			}

			@Test
			public void ballonHashAfterReset() {
				AverageHash aHash = new AverageHash(32);
				Hash baHash = aHash.hash(ballon);
				FuzzyHash hash = new FuzzyHash();

				hash.merge(baHash);
				hash.reset();
				assertEquals(0, hash.weightedDistance(baHash));
			}

			@Test
			public void weightedDistZero() {
				Hash h = new Hash(new BigInteger("0", 2), 1, 0);
				Hash h1 = new Hash(new BigInteger("1", 2), 1, 0);

				FuzzyHash hash = new FuzzyHash();
				hash.merge(h);
				hash.merge(h);
				hash.merge(h1);
				assertEquals(2 / 3d, hash.weightedDistance(h1),1e-8);
				assertEquals(1 / 3d, hash.weightedDistance(h),1e-8);
			}
		}
		
		@Nested 
		class FuzzyFuzzy{
			@Test
			public void identity() {
				int bits = 10;
				FuzzyHash hash = new FuzzyHash();
				Hash h = new Hash(new BigInteger("1111111111", 2), bits, 0);
				hash.merge(h);
				assertEquals(0, hash.weightedDistance(hash));
			}

			@Test
			public void oneIdentity() {
				int bits = 10;
				FuzzyHash hash = new FuzzyHash();
				Hash h = new Hash(new BigInteger("1111111111", 2), bits, 0);
				hash.merge(h);
				FuzzyHash hash2 = new FuzzyHash(h);
				
				assertEquals(0, hash2.weightedDistance(hash));
			}

			@Test
			public void zeroIdentity() {
				int bits = 10;
				FuzzyHash hash = new FuzzyHash();
				Hash h = new Hash(new BigInteger("0000000000", 2), bits, 0);
				hash.merge(h);
				FuzzyHash hash2 = new FuzzyHash(h);
				
				assertEquals(0, hash2.weightedDistance(hash));
			}

			@Test
			public void maxDistOne() {
				int bits = 10;
				FuzzyHash hash = new FuzzyHash();
				Hash h = new Hash(new BigInteger("0000000000", 2), bits, 0);
				Hash h1 = new Hash(new BigInteger("1111111111", 2), bits, 0);
				hash.merge(h);
				FuzzyHash hash2 = new FuzzyHash(h1);
				assertEquals(1, hash.weightedDistance(hash2));
			}

			@Test
			public void maxDistZero() {
				int bits = 10;
				Hash h = new Hash(new BigInteger("1111111111", 2), bits, 0);
				FuzzyHash hash = new FuzzyHash();
				Hash h1 = new Hash(new BigInteger("0000000000", 2), bits, 0);
				hash.merge(h);
				FuzzyHash hash2 = new FuzzyHash(h1);
				
				System.out.println(hash + " " + hash2);
				
				assertEquals(1, hash.weightedDistance(hash2));
			}

			@Test
			public void distMixed() {
				int bits = 10;
				Hash h = new Hash(new BigInteger("0000111111", 2), bits, 0);
				FuzzyHash hash = new FuzzyHash();
				Hash h1 = new Hash(new BigInteger("0000000000", 2), bits, 0);
				hash.merge(h);
				FuzzyHash hash2 = new FuzzyHash(h1);
				assertEquals(.6, hash.weightedDistance(hash2));
			}

		

			@Test
			public void weightedDistZero() {
				Hash h = new Hash(new BigInteger("0", 2), 1, 0);
				Hash h1 = new Hash(new BigInteger("1", 2), 1, 0);

				FuzzyHash hash = new FuzzyHash();
				hash.merge(h);
				hash.merge(h);
				hash.merge(h1);  
				
				FuzzyHash hash2 = new FuzzyHash();
				hash2.merge(h1);
				hash2.merge(h1);
				hash2.merge(h1); 
				
				assertEquals(2 / 3d, hash.weightedDistance(hash2),1e-8);
				assertEquals(2 / 3d, hash2.weightedDistance(hash),1e-8);
			}
		}
		

	}

	@Nested
	class FuzzyDistance {

		@Test
		public void zeroBitAloneCheckForZero() {
			FuzzyHash hash = new FuzzyHash();
			hash.mergeFast(new Hash(BigInteger.ZERO, 1, 0));
			assertEquals(0, hash.getWeightedDistance(0, false));
		}
		
		@Test
		public void zeroBitAloneCheckForOne() {
			FuzzyHash hash = new FuzzyHash();
			hash.mergeFast(new Hash(BigInteger.ZERO, 1, 0));
			assertEquals(1, hash.getWeightedDistance(0, true));
		}

		@Test
		public void oneBitAloneCheckForZero() {
			FuzzyHash hash = new FuzzyHash();
			hash.mergeFast(new Hash(BigInteger.ONE, 1, 0));
			assertEquals(1, hash.getWeightedDistance(0, false));
		}
		
		@Test
		public void oneBitAloneCheckForOne() {
			FuzzyHash hash = new FuzzyHash();
			hash.mergeFast(new Hash(BigInteger.ONE, 1, 0));
			assertEquals(0, hash.getWeightedDistance(0, true));
		}

		@Test
		public void equalBitCheckForZero() {
			FuzzyHash hash = new FuzzyHash();
			hash.mergeFast(new Hash(BigInteger.ZERO, 1, 0));
			hash.mergeFast(new Hash(BigInteger.ONE, 1, 0));
			assertEquals(0.5, hash.getWeightedDistance(0, false));
		}
		
		@Test
		public void equalBitCheckForOne() {
			FuzzyHash hash = new FuzzyHash();
			hash.mergeFast(new Hash(BigInteger.ZERO, 1, 0));
			hash.mergeFast(new Hash(BigInteger.ONE, 1, 0));
			assertEquals(0.5, hash.getWeightedDistance(0, true));
		}
		
		@Test
		public void oneMajorCheckForOne() {
			FuzzyHash hash = new FuzzyHash();
			hash.mergeFast(new Hash(BigInteger.ZERO, 1, 0));
			hash.mergeFast(new Hash(BigInteger.ONE, 1, 0));
			hash.mergeFast(new Hash(BigInteger.ONE, 1, 0));
			assertEquals(1/3d, hash.getWeightedDistance(0, true),1e-8);
		}
		
		@Test
		public void oneMajorCheckForZero() {
			FuzzyHash hash = new FuzzyHash();
			hash.mergeFast(new Hash(BigInteger.ZERO, 1, 0));
			hash.mergeFast(new Hash(BigInteger.ONE, 1, 0));
			hash.mergeFast(new Hash(BigInteger.ONE, 1, 0));
			assertEquals(2/3d, hash.getWeightedDistance(0, false),1e-8);
		}
		
		@Test
		public void zeroMajorCheckForOne() {
			FuzzyHash hash = new FuzzyHash();
			hash.mergeFast(new Hash(BigInteger.ZERO, 1, 0));
			hash.mergeFast(new Hash(BigInteger.ZERO, 1, 0));
			hash.mergeFast(new Hash(BigInteger.ONE, 1, 0));
			assertEquals(2/3d, hash.getWeightedDistance(0, true),1e-8);
		}
		
		@Test
		public void zeroMajorCheckForZero() {
			FuzzyHash hash = new FuzzyHash();
			hash.mergeFast(new Hash(BigInteger.ZERO, 1, 0));
			hash.mergeFast(new Hash(BigInteger.ZERO, 1, 0));
			hash.mergeFast(new Hash(BigInteger.ONE, 1, 0));
			assertEquals(1/3d, hash.getWeightedDistance(0, false),1e-8);
		}
		
		@Test
		public void zeroMajorCheckForZero2() {
			FuzzyHash hash = new FuzzyHash(new Hash(BigInteger.ZERO, 1, 0));
			assertEquals(0, hash.getWeightedDistance(0, false),1e-8);
		}
		
		@Test
		public void zeroMajorCheckForOne2() {
			FuzzyHash hash = new FuzzyHash(new Hash(BigInteger.ZERO, 1, 0));
			assertEquals(1, hash.getWeightedDistance(0, true),1e-8);
		}
		
		@Test
		public void test() {
			FuzzyHash fuzzy = new FuzzyHash(TestResources.createHash("10",0));
			assertEquals(0, fuzzy.getWeightedDistance(0, false),1e-8);
			assertEquals(1, fuzzy.getWeightedDistance(1, false),1e-8);
		}
		
	}

	@Test
	public void testGetBitUnsafe() throws Throwable {

		int hashLength = 25;

		FuzzyHash hash = new FuzzyHash();

		MethodType mt = MethodType.methodType(boolean.class, int.class);
		// Receive a reference to the parent class implementation
		MethodHandle fuzzyHandle = MethodHandles.lookup().findVirtual(FuzzyHash.class, "getBitUnsafe", mt);

		// Randomly
		PcgRSFast rng = new PcgRSFast();
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < 10; i++) {
			for (int j = 0; j < hashLength; j++) {
				sb.append(rng.nextBoolean() ? "0" : "1");
			}
			hash.mergeFast(new Hash(new BigInteger(sb.toString(), 2), hashLength, 0));
			// reset
			sb.setLength(0);
		}

		boolean[] baseImpl = new boolean[hashLength];
		boolean[] fuzzyImpl = new boolean[hashLength];

		for (int i = 0; i < hashLength; i++) {
			fuzzyImpl[i] = hash.getBitUnsafe(i);
			baseImpl[i] = (boolean) fuzzyHandle.invoke(hash, i);
		}
		assertArrayEquals(baseImpl,fuzzyImpl);
	}

	@Nested
	class Serialization{
		
		@Test
		public void reconstructHash() throws IOException, ClassNotFoundException {
			
			Hash h = new Hash(BigInteger.valueOf(5),16,2);
			Hash h1 = new Hash(BigInteger.valueOf(6),16,2);
			
			FuzzyHash fuzzy = new FuzzyHash(h1,h);
			
			File serTestFile = new File("testFuzzyHash.ser");
			serTestFile.deleteOnExit();
			fuzzy.toFile(serTestFile);
			FuzzyHash fuzzy2 = (FuzzyHash) Hash.fromFile(serTestFile);
			//Equality
			
			System.out.println(Arrays.toString(fuzzy.bits) + " " + fuzzy.hashLength);
			System.out.println(Arrays.toString(fuzzy2.bits) + " " + fuzzy2.hashLength);
			
			
			System.out.println(fuzzy);
			System.out.println(fuzzy2);
			
			assertEquals(0,fuzzy.hammingDistance(fuzzy2));
		}
	}
	
	//
//	class UncertainHash{
//		
//		@Test
//		/**
//		 * In an non initialized hash all bits appear 0 times. no bits are certain
//		 */
//		public void empty() {
//			int bits = 10;
//			FuzzyHash hash = new FuzzyHash();
//			
//			hash.getUncertaintyHash(.2);
//			
//			
//			
//			assertArrayEquals(expected,mask);
//		}
//		
//		@Test
//		public void fullCertainty() {
//			int bits = 10;
//			FuzzyHash hash = new FuzzyHash();
//			Hash h = new Hash(new BigInteger("1111111111",2),10,0);
//			
//			hash.merge(h);
//			boolean mask[] = hash.getUncertaintyMask(0.3);
//			boolean expected[] = new boolean[bits];
//			ArrayUtil.fillArray(expected,()-> {return false;});
//			
//			assertArrayEquals(expected,mask);
//		}
//		
//		@Test
//		public void fullCertaintyMixed() {
//			int bits = 10;
//			FuzzyHash hash = new FuzzyHash();
//			Hash h = new Hash(new BigInteger("1111100000",2),10,0);
//			
//			hash.merge(h);
//			boolean mask[] = hash.getUncertaintyMask(0.3);
//			boolean expected[] = new boolean[bits];
//			ArrayUtil.fillArray(expected,()-> {return false;});
//			
//			assertArrayEquals(expected,mask);
//		}
//		
//		@Test
//		public void fullUncertainty() {
//			int bits = 10;
//			FuzzyHash hash = new FuzzyHash();
//			Hash h =  new Hash(new BigInteger("1111100000",2),10,0);
//			Hash h2 = new Hash(new BigInteger("0000011111",2),10,0);
//			
//			hash.merge(h);
//			hash.merge(h2);
//			boolean mask[] = hash.getUncertaintyMask(0.3);
//			boolean expected[] = new boolean[bits];
//			ArrayUtil.fillArray(expected,()-> {return true;});
//			assertArrayEquals(expected,mask);
//		}
//		
//		@Test
//		public void partially() {
//			int bits = 10;
//			FuzzyHash hash = new FuzzyHash();
//			Hash h =  new Hash(new BigInteger("1111100000",2),10,0);
//			Hash h2 = new Hash(new BigInteger("0011111111",2),10,0);
//			Hash h3 = new Hash(new BigInteger("1111100000",2),10,0);
//			Hash h4 = new Hash(new BigInteger("0000011111",2),10,0);
//			
//			hash.merge(h);
//			hash.merge(h2);
//			boolean mask[] = hash.getUncertaintyMask(0.2);
//			boolean expected[] = new boolean[bits];
//			ArrayUtil.fillArray(expected,()-> {return true;});
//			assertArrayEquals(expected,mask);
//		}
//	}

}
