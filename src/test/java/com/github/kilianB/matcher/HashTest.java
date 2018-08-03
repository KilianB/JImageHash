package com.github.kilianB.matcher;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigInteger;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.github.kilianB.hashAlgorithms.AverageHash;

class HashTest {

	@Test 
	void incompatibleAlgorithms() {
		int algoId = new AverageHash(14).algorithmId();
		int algoId1 = new AverageHash(15).algorithmId();
		Hash hash0 = new Hash(BigInteger.ONE,algoId);
		Hash hash1 = new Hash(BigInteger.ONE,algoId1);
		assertThrows(IllegalArgumentException.class,()->{hash0.hammingDistance(hash1);});
	}
	
	
	@Nested
	class Equality{
		
		@Test
		void sameObject() {
			Hash hash0 = new Hash(new BigInteger("010101000",2),0);
			assertEquals(hash0,hash0);
		}
		
		@Test
		void equalContent() {
			Hash hash0 = new Hash(new BigInteger("1010101000",2),0);
			Hash hash1 = new Hash(new BigInteger("1010101000",2),0);
			assertEquals(hash0,hash1);
		}
		
	}
	
	
	@Nested
	class HemmingDistance{	
		@Test
		@DisplayName("Identical Hash")
		void identical(){
			Hash hash0 = new Hash(new BigInteger("10101010",2),0);
			assertEquals(0,hash0.hammingDistance(hash0));
		}
		
		@Test
		@DisplayName("Identical Hash Fast")
		void identicalFast(){
			Hash hash0 = new Hash(new BigInteger("10101010",2),0);
			assertEquals(0,hash0.hammingDistanceFast(hash0));
		}
		
		
		@Test
		@DisplayName("Difference Padded")
		void distanceThree(){
			Hash hash0 = new Hash(new BigInteger("10001100",2),0);
			Hash hash1 = new Hash(new BigInteger("11111100",2),0);
			assertEquals(3,hash0.hammingDistance(hash1));
		}
		
		@Test
		@DisplayName("Difference Padded Fast")
		void distanceThreeFast(){
			Hash hash0 = new Hash(new BigInteger("10001100",2),0);
			Hash hash1 = new Hash(new BigInteger("11111100",2),0);
			assertEquals(3,hash0.hammingDistanceFast(hash1));
		}	
		
	}

	@Nested
	class NormalizedHemmingDistance{
		@Test
		@DisplayName("Identical Hash")
		void identical(){
			Hash hash0 = new Hash(new BigInteger("10101010",2),0);
			assertEquals(0,hash0.normalizedHammingDistance(hash0));
		}
		
		@Test
		@DisplayName("Identical Hash Fast")
		void identicalFast(){
			Hash hash0 = new Hash(new BigInteger("10101010",2),0);
			assertEquals(0,hash0.normalizedHammingDistanceFast(hash0));
		}
		
		@Test
		@DisplayName("Difference Padded")
		void dstanceThree(){
			Hash hash0 = new Hash(new BigInteger("10001100",2),0);
			Hash hash1 = new Hash(new BigInteger("11111100",2),0);
			assertEquals(3/7d,hash0.normalizedHammingDistance(hash1));
		}
		
		@Test
		@DisplayName("Difference Padded Fast")
		void distanceThreeFast(){
			Hash hash0 = new Hash(new BigInteger("10001100",2),0);
			Hash hash1 = new Hash(new BigInteger("11111100",2),0);
			assertEquals(3/7d,hash0.normalizedHammingDistanceFast(hash1));
		}
	}
}
