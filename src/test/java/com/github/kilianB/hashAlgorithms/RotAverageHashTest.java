package com.github.kilianB.hashAlgorithms;

import static com.github.kilianB.TestResources.lenna;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * @author Kilian
 *
 */
class RotAverageHashTest {

	/**
	 * The algorithms id shall stay consistent throughout different instances of the
	 * jvm. While simple hashcodes do not guarantee this behavior hash codes created
	 * from strings and integers are by contract consistent.
	 */
	@Test
	@DisplayName("Consistent AlgorithmIds")
	public void consistency() {

		assertAll(() -> {
			assertEquals(-1456716412, new RotAverageHash(14).algorithmId());
		}, () -> {
			assertEquals(-1456694588, new RotAverageHash(25).algorithmId());
		});
	}

//Base Hashing algorithm tests
	@Nested
	class AlgorithmBaseTests extends RotationalTestBase {
		@Override
		protected HashingAlgorithm getInstance(int bitResolution) {
			return new RotAverageHash(bitResolution);
		}
	}
}
