<img align=left src = "https://user-images.githubusercontent.com/9025925/48595271-388ba280-e954-11e8-8bc6-8b8afe108682.png" />

# JImageHash

[![Travis](https://travis-ci.org/KilianB/JImageHash.svg?branch=master)](https://travis-ci.org/KilianB/JImageHash)
[![GitHub license](https://img.shields.io/github/license/KilianB/JImageHash.svg)](https://github.com/KilianB/JImageHash/blob/master/LICENSE)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/3c7db745b9ff4dd9b89484a6aa46ad2f)](https://www.codacy.com/app/KilianB/JImageHash?utm_source=github.com&utm_medium=referral&utm_content=KilianB/JImageHash&utm_campaign=Badge_Grade)

JImageHash is a performant perceptual image fingerprinting library entirely written in Java. The library returns a similarity score aiming to identify entities which are likely modifications of the original source while being robust various attack vectors ie. color, rotation and scale transformation.

> A perceptual hash is a fingerprint of a multimedia file derived from various features from its content. Unlike cryptographic hash functions which rely on the avalanche effect of small changes in input leading to drastic changes in the output, perceptual hashes are "close" to one another if the features are similar.

This library was inspired by _Dr. Neal Krawetz_ blog post "<a href="http://www.hackerfactor.com/blog/index.php?/archives/529-Kind-of-Like-That.html">kind of like that</a>" and incorporates several improvements. A comprehensive overview of perceptual image hashing can be found in this <a href="https://www.phash.org/docs/pubs/thesis_zauner.pdf">paper</a> by Christoph Zauner.



## Maven

The project is hosted on maven central

```XML
<dependency>
	<groupId>dev.brachtendorf</groupId>
	<artifactId>JImageHash</artifactId>
	<version>1.0.0</version>
</dependency>

<!-- If you want to use the database image matcher you need to add h2 as well -->
<dependency>
	<groupId>com.h2database</groupId>
	<artifactId>h2</artifactId>
	<version>1.4.200</version>
</dependency>
```

### Breaking Changes: migration guide to version 1.0.0

**Please be aware that migrating from one major version to another usually invalidates created hashes in order to retain validity when persistently storing the hashes.**
The algorithm id of hashes is adjusted in order for the jvm to throw an error if the possibility exist that hashes generated for the same input image are not consistent throughout the compared versions.

 Hashes generated with the following 2 algorithm have to be regenerated:

- RotPAverage hash was fixed to correctly return hashes when the algorithm is used multiple times.
- KernelAverageHash algorithm id changed due to JVMs internal hashcode calculation and the package name update. Hashes generated with this algorithm have to be regenerated.

The package is now published to maven central under a new group id. The internal package structure has been adjusted from `com.github.kilianB` to `dev.brachtendorf.jimagehash`. Adjust your imports accordingly.


## Hello World

```Java
File img0 = new File("path/to/file.png");
File img1 = new File("path/to/secondFile.jpg");

HashingAlgorithm hasher = new PerceptiveHash(32);

Hash hash0 = hasher.hash(img0);
Hash hash1 = hasher.hash(img1);

double similarityScore = hash0.normalizedHammingDistance(hash1);

if(similarityScore < .2) {
    //Considered a duplicate in this particular case
}

//Chaining multiple matcher for single image comparison

SingleImageMatcher matcher = new SingleImageMatcher();
matcher.addHashingAlgorithm(new AverageHash(64),.3);
matcher.addHashingAlgorithm(new PerceptiveHash(32),.2);

if(matcher.checkSimilarity(img0,img1)) {
    //Considered a duplicate in this particular case
}
```

## Examples

Examples and convenience methods can be found in the [examples repository](https://github.com/KilianB/JImageHash-Examples)

## Transparent image support

Support for transparent images has to be enabled specifically due to backwards compatibility and force users of the libraries to understand the implication of this setting.

The `setOpaqueHandling(Color? replacementColor, int alphaThreshold)` will replace transparent pixels with the specified color before calculating the hash.

### Be aware of the following culprits: 

- the replacement color must be consistent throughout hash calculation for the entire sample space to ensure robustness against color transformations of the images.
- the replacement color should be a color that does not appear often in the input space to avoid masking out available information.
- when not specified `Orange` will be used as replacement. This choice was arbitrary and ideally, a default color should be chosen which results in 0 and 1 bits being computed in 50% of the time in respect to all other pixels and hashing algorithms.
- supplying a replacement value of null will attempt to either use black or white as a replacement color conflicting with the advice given above. Computing the contrast color will fail if the transparent area of an image covers a large space and comes with a steep performance penalty.

```java
HashingAlgorithm hasher = new PerceptiveHash(32);

//Replace all pixels with alpha values smaller than 0-255. The alpha value cutoff is taken into account after down scaling the image, therefore choose a reasonable value.  
int alphaThreshold = 253;
hasher.setOpaqueHandling(alphaThreshold)

```

## Multiple types image matchers are available for each situation

The `persistent` package allows hashes and matchers to be saved to disk. In turn the images are not kept in memory and are only referenced by file path allowing to handle a great deal of images
at the same time.
The `cached` version keeps the BufferedImage image objects in memory allowing to change hashing algorithms on the fly and a direct retrieval of the buffered image objects of matching images.
The `categorize` package contains image clustering matchers. KMeans and Categorical as well as weighted matchers.
The `exotic` package features BloomFilter, and the SingleImageMatcher used to match 2 images without any fancy additions.

<table>
<tr> <th>Image</th>  <th></th> <th>High</th> <th>Low</th> <th>Copyright</th> <th>Thumbnail</th> <th>Ballon</th> </tr>

<tr> <td>High Quality</td>  <td><img width= 75% src="https://user-images.githubusercontent.com/9025925/36542413-046d8116-17e1-11e8-93ed-210f65293d51.jpg"></td> 
	<td><p align="center"><image src="https://via.placeholder.com/30/228B22?text=+"/></p></td> 
	<td><p align="center"><image src="https://via.placeholder.com/30/228B22?text=+"/></p></td> 
	<td><p align="center"><image src="https://via.placeholder.com/30/228B22?text=+"/></p></td> 
	<td><p align="center"><image src="https://via.placeholder.com/30/228B22?text=+"/></p></td> 
	<td><p align="center"><image src="https://via.placeholder.com/30/DC143C?text=+"/></p></td> 
</tr> 
<tr> <td>Low Quality</td>  <td><img width= 75% src="https://user-images.githubusercontent.com/9025925/36542414-0498079c-17e1-11e8-9224-a9852797b96f.jpg"></td> 
<td><p align="center"><image src="https://via.placeholder.com/30/228B22?text=+"/></p></td> 
	<td><p align="center"><image src="https://via.placeholder.com/30/228B22?text=+"/></p></td> 
	<td><p align="center"><image src="https://via.placeholder.com/30/228B22?text=+"/></p></td> 
	<td><p align="center"><image src="https://via.placeholder.com/30/228B22?text=+"/></p></td> 
	<td><p align="center"><image src="https://via.placeholder.com/30/DC143C?text=+"/></p></td>
</tr>

 <tr> <td>Altered Copyright</td>  <td><img width= 75% src="https://user-images.githubusercontent.com/9025925/36542411-0438eb36-17e1-11e8-9a59-2c69937560bf.jpg"> </td> 
<td><p align="center"><image src="https://via.placeholder.com/30/228B22?text=+"/></p></td> 
	<td><p align="center"><image src="https://via.placeholder.com/30/228B22?text=+"/></p></td> 
	<td><p align="center"><image src="https://via.placeholder.com/30/228B22?text=+"/></p></td> 
	<td><p align="center"><image src="https://via.placeholder.com/30/228B22?text=+"/></p></td> 
	<td><p align="center"><image src="https://via.placeholder.com/30/DC143C?text=+"/></p></td>
</tr>

<tr> <td>Thumbnail</td>  <td><img src="https://user-images.githubusercontent.com/9025925/36542415-04ca8078-17e1-11e8-9be4-9a90b08c404b.jpg"></td> 
<td><p align="center"><image src="https://via.placeholder.com/30/228B22?text=+"/></p></td> 
	<td><p align="center"><image src="https://via.placeholder.com/30/228B22?text=+"/></p></td> 
	<td><p align="center"><image src="https://via.placeholder.com/30/228B22?text=+"/></p></td> 
	<td><p align="center"><image src="https://via.placeholder.com/30/228B22?text=+"/></p></td> 
	<td><p align="center"><image src="https://via.placeholder.com/30/DC143C?text=+"/></p></td>
</tr> 
	
<tr> <td>Ballon</td>  <td><img width= 75% src="https://user-images.githubusercontent.com/9025925/36542417-04f3e6a2-17e1-11e8-91b2-50f9961524b4.jpg"></td> 
<td><p align="center"><image src="https://via.placeholder.com/30/DC143C?text=+"/></p></td> 
	<td><p align="center"><image src="https://via.placeholder.com/30/DC143C?text=+"/></p></td> 
	<td><p align="center"><image src="https://via.placeholder.com/30/DC143C?text=+"/></p></td> 
	<td><p align="center"><image src="https://via.placeholder.com/30/DC143C?text=+"/></p></td> 
	<td><p align="center"><image src="https://via.placeholder.com/30/228B22?text=+"/></p></td>
</tr> 
	
</table>

## Hashing algorithm

Image matchers can be configured using different algorithm. Each comes with individual properties

<table>
  <tr><th>Algorithm</th>  <th>Feature</th><th>Notes</th> </tr>
  <tr><td><a href="https://github.com/KilianB/JImageHash/wiki/Hashing-Algorithms#averagehash-averagekernelhash-medianhash-averagecolorhash">AverageHash</a></td>  <td>Average Luminosity</td> <td>Fast and good all purpose algorithm</td> </tr>
  <tr><td><a href="https://github.com/KilianB/JImageHash/wiki/Hashing-Algorithms#averagehash-averagekernelhash-medianhash-averagecolorhash">AverageColorHash</a></td>  <td>Average Color</td> <td>Version 1.x.x AHash. Usually worse off than AverageHash. Not robust against color changes</td> </tr>
  <tr><td><a href="https://github.com/KilianB/JImageHash/wiki/Hashing-Algorithms#differencehash">DifferenceHash</a></td> <td>Gradient/Edge detection</td> <td>A bit more robust against hue/sat changes compared to AColorHash </td> </tr>
  <tr><td>Wavelet Hash</td> <td>Frequency & Location</td> <td>Feature extracting by applying haar wavlets multiple times to the input image. Detection quality better than inbetween aHash and pHash.</td> </tr>
  <tr><td><a href="https://github.com/KilianB/JImageHash/wiki/Hashing-Algorithms#perceptive-hash">PerceptiveHash</a></td> <td>Frequency</td> <td>Hash based on Discrete Cosine Transformation. Smaller hash distribution but best accuracy / bitResolution.</td> </tr>
  <tr><td><a href="https://github.com/KilianB/JImageHash/wiki/Hashing-Algorithms#averagehash-averagekernelhash-medianhash-averagecolorhash">MedianHash</a></td> <td>Median Luminosity</td> <td>Identical to AHash but takes the median value into account. A bit better to detect watermarks but worse at scale transformation</td> </tr>
  <tr><td><a href="https://github.com/KilianB/JImageHash/wiki/Hashing-Algorithms#averagehash-averagekernelhash-medianhash-averagecolorhash">AverageKernelHash</a></td>  <td>Average luminosity </td> <td>Same as AHash with kernel preprocessing. So far usually performs worse, but testing is not done yet.</td> </tr>
  <tr><td colspan=3 align=center><b>Rotational Invariant</b></td></tr>
  <tr><td><a href="https://github.com/KilianB/JImageHash/wiki/Hashing-Algorithms#rotphash">RotAverageHash</a></td>  <td>Average Luminosity</td> <td>Rotational robust version of AHash. Performs well but performance scales disastrous with higher bit resolutions . Conceptual issue: pixels further away from the center are weightend less.</td> </tr>
  <tr><td><a href="https://github.com/KilianB/JImageHash/wiki/Hashing-Algorithms#rotphash">RotPHash</a></td> <td>Frequency</td> <td> Rotational invariant version of pHash using ring partition to map pixels in a circular fashion. Lower complexity for high bit sizes but due to sorting pixel values usually maps to a lower normalized distance. Usually bit res of >= 64bits are preferable</td> </tr>  
   <tr><td colspan=3 align="center"><i><b>Experimental.</b> Hashes available but not well tuned and subject to changes</i></td></tr>
  <tr><td><a href="https://github.com/KilianB/JImageHash/wiki/Hashing-Algorithms#hoghash">HogHash</a></td> <td>Angular Gradient based (detection of shapes?) </td> <td>A hashing algorithm based on hog feature detection which extracts gradients and pools them by angles. Usually used in support vector machine/NNs human outline detection. It's not entirely set how the feature vectors should be encoded. Currently average, but not great results, expensive to compute and requires a rather high bit resolution</td> </tr>  
</table>

### Version 3.0.0 Image clustering

Image clustering with fuzzy hashes allowing to represent hashes with probability bits instead of simple 0's and 1's

![1_fxpw79yoon8xo3slqsvmta](https://user-images.githubusercontent.com/9025925/51272388-439d9600-19ca-11e9-8220-fe3539ed6061.png)

### Algorithm benchmarking

See the wiki page on how to test different hashing algorithms with your set of images
Code available at the example repo: https://github.com/KilianB/JImageHash-Examples/tree/main/src/main/java/com/github/kilianB/benchmark

<img src="https://user-images.githubusercontent.com/9025925/49185669-c14a0b80-f362-11e8-92fa-d51a20476937.jpg" />
