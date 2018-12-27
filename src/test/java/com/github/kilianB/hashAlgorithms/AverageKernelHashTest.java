package com.github.kilianB.hashAlgorithms;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.github.kilianB.hashAlgorithms.filter.Kernel;

/**
 * @author Kilian
 *
 */
class AverageKernelHashTest{

	@Nested
	@DisplayName("Algorithm Id")
	class AlgorithmId {
		/**
		 * The algorithms id shall stay consistent throughout different instances of the
		 * jvm. While simple hashcodes do not guarantee this behavior hash codes created
		 * from strings and integers are by contract consistent.
		 */
		@Test
		@DisplayName("Consistent AlgorithmIds")
		public void consistency() {
			//TODO monitor!!!
			assertAll(() -> {
				assertEquals(1264492988, new AverageKernelHash(14).algorithmId());
			}, () -> {
				assertEquals(1264523740, new AverageKernelHash(25).algorithmId());
			});
		}
	}
	
	/**
	 * Adding a kernel produces a different hash
	 */
	@Test
	void addKernel() {
		HashingAlgorithm h0 = new AverageKernelHash(32);
		HashingAlgorithm h1 = new AverageKernelHash(32,Kernel.gaussianFilter(3,3,2));
		assertNotEquals(h0.algorithmId(),h1.algorithmId());
	}
	
	@Test
	void addMultipleKernel() {
		HashingAlgorithm h0 = new AverageKernelHash(32);
		HashingAlgorithm h1 = new AverageKernelHash(32,Kernel.gaussianFilter(3,3,2));
		HashingAlgorithm h2 = new AverageKernelHash(32,Kernel.gaussianFilter(3,3,2),Kernel.boxFilterNormalized(3,3));
		assertNotEquals(h0.algorithmId(),h1.algorithmId());
		assertNotEquals(h0.algorithmId(),h2.algorithmId());
		assertNotEquals(h1.algorithmId(),h2.algorithmId());
	}
	
	//Base Hashing algorithm tests
	@Nested
	class AlgorithmBaseTests extends HashTestBase{

		@Override
		protected HashingAlgorithm getInstance(int bitResolution) {
			return new AverageKernelHash(bitResolution);
		}
		
	}

	@Nested
	class AlgorithmBaseTestsWithFilter extends HashTestBase{
		@Override
		protected HashingAlgorithm getInstance(int bitResolution) {
			return new AverageKernelHash(bitResolution,Kernel.gaussianFilter(3,3,2));
		}
	}
	
	@Nested
	class AlgorithmBaseTestsWithMultipleFilters extends HashTestBase{
		@Override
		protected HashingAlgorithm getInstance(int bitResolution) {
			return new AverageKernelHash(bitResolution,Kernel.gaussianFilter(3,3,2),Kernel.gaussianFilter(5,3,2));
		}
	}
	
	@Test
	void testToString() {
		//Contains fields with kernels
		assertTrue(new AverageKernelHash(32).toString().contains("filters="));
	}
	
}
