package com.github.kilianB.hashAlgorithms;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.stream.Stream;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.github.kilianB.hashAlgorithms.HashingAlgorithm;
import com.github.kilianB.hashAlgorithms.PerceptiveHash;
import com.github.kilianB.hashAlgorithms.experimental.HogHash;
import com.github.kilianB.matcher.Hash;

class PerceptiveHashTest {

	@Nested
	@DisplayName("Algorithm Id")
	class AlgorithmId {

		/**
		 * The algorithms id shall stay consistent throughout different instances of the
		 * jvm. While simple hashcodes do not guarantee this behaviour hash codes
		 * created from strings and integers are by contract consistent.
		 */
		@Test
		@DisplayName("Consistent AlgorithmIds")
		public void consistency() {

			assertAll(() -> {
				assertEquals(1062023020, new PerceptiveHash(14).algorithmId()); // Was 748566082
			}, () -> {
				assertEquals(1062146028, new PerceptiveHash(25).algorithmId()); // Was 748566093
			});
		}
	}

	// Base Hashing algorithm tests
	@Nested
	class AlgorithmBaseTests extends HashTestBase {

		@Override
		protected HashingAlgorithm getInstance(int bitResolution) {
			return new PerceptiveHash(bitResolution);
		}
	}

}
