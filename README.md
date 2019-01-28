<img align=left src = "https://user-images.githubusercontent.com/9025925/48595271-388ba280-e954-11e8-8bc6-8b8afe108682.png" />

# JImageHash

[![Travis](https://travis-ci.org/KilianB/JImageHash.svg?branch=master)](https://travis-ci.org/KilianB/JImageHash)
[![GitHub license](https://img.shields.io/github/license/KilianB/JImageHash.svg)](https://github.com/KilianB/JImageHash/blob/master/LICENSE)
[![Download](https://api.bintray.com/packages/kilianb/maven/JImageHash/images/download.svg)](https://bintray.com/kilianb/maven/JImageHash/_latestVersion)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/3c7db745b9ff4dd9b89484a6aa46ad2f)](https://www.codacy.com/app/KilianB/JImageHash?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=KilianB/JImageHash&amp;utm_campaign=Badge_Grade)

JImageHash is a performant perceptual image fingerprinting library entirely written in Java. The library returns a similarity score aiming to identify entities which are likely modifications of the original source while being robust variouse attack vectors ie. color, rotation and scale transformation.

>  A perceptual hash is a fingerprint of a multimedia file derived from various features from its content. Unlike cryptographic hash functions which rely on the avalanche effect of small changes in input leading to drastic changes in the output, perceptual hashes are "close" to one another if the features are similar.

This library was inspired by _Dr. Neal Krawetz_ blog post "<a href="http://www.hackerfactor.com/blog/index.php?/archives/529-Kind-of-Like-That.html">kind of like that</a>" and incorporates several improvements. A comprehensive overview of perceptual image hashing can be found in this <a href="https://www.phash.org/docs/pubs/thesis_zauner.pdf">paper</a> by Christoph Zauner. 

## Maven - Bintray

The project is hosted on bintray and jcenter. <b>Please be aware that migrating from one major version to another usually invalidates creatd hashes</b> 

````XML
<repositories>
	<repository>
		<id>jcenter</id>
		<url>https://jcenter.bintray.com/</url>
	</repository>
</repositories>

<dependency>
	<groupId>com.github.kilianB</groupId>
	<artifactId>JImageHash</artifactId>
	<version>3.0.0</version>
</dependency>

<!-- If you want to use the database image matcher you need to add h2 as well -->
<dependency>
	<groupId>com.h2database</groupId>
	<artifactId>h2</artifactId>
	<version>1.4.197</version>
</dependency>
````

## Hello World

````Java
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
````

## Examples

Below you can find examples of convenience methods used to get fast results. Further examples are provided in the examples folder explain how to choose 
and optimize individual algorithms on your own.

<table>
	<thead>
		<tr>
			<th>File</th>
			<th>Content</th>
		</tr>
	</thead>
	<tbody>
		<tr>
			<td><a href="/src/main/java/com/github/kilianB/examples/CompareImages.java">CompareImages.java</a></td>
			<td>Compare the similarity of two images using a single algorithm and a custom threshold</td>
		</tr>
		<tr>
			<td><a href="/src/main/java/com/github/kilianB/examples/ChainAlgorithms.java">ChainAlgorithms.java</a></td>
			<td>Chain multiple algorithms to achieve a better precision & recall.</td>
		</tr>
		<tr>
			<td><a href="/src/main/java/com/github/kilianB/examples/MatchMultipleImages.java">MatchMultipleImages.java</a></td>
			<td>Precompute the hash of multiple images to retrieve all relevant images in a batch.</td>
		</tr>
		<tr>
			<td><a href="/src/main/java/com/github/kilianB/examples/DatabaseExample.java">DatabaseExample.java</a></td>
			<td>Store hashes persistently in a database. Serialize and Deserialize the matcher.</td>
		</tr>
		<tr>
			<td><a href="/src/main/java/com/github/kilianB/examples/AlgorithmBenchmark.java">AlgorithmBenchmark.java</a></td>
			<td>Test different algorithm/setting combinations against your images to see which settings give the best result.</td>
		</tr>
		<tr>
			<td><a href="/src/main/java/com/github/kilianB/examples/nineGagDuplicateDetectionAndMemeCategorizer">Clustering Example</a></td>
			<td>Extensive tutotial matching 17.000 images . As described in the <a href="https://medium.com/@kilian.brachtendorf_83099/getting-tired-of-re-uploads-4a4f88908d52">blog<a/a></td>
		</tr>
	</tbody>
</table>

## Multiple types image matchers are available for each situation

The `persistent` package allows hashes and matchers to be saved to disk. In turn the images are not kept in memory and are only referenced by file path allowing to handle a great deal of images
at the same time.
The `cached` version keeps the BufferedImage image objects in memory allowing to change hashing algorithms on the fly and a direct retrieval of the buffered image objects of matching images.
The `categorize` package contains image clustering matchers. KMeans and Categorical as well as weighted matchers.
The `exotic` package features BloomFilter, and the SingleImageMatcher used to match 2 images without any fancy additions.

<table>
<tr> <th>Image</th>  <th></th> <th>High</th> <th>Low</th> <th>Copyright</th> <th>Thumbnail</th> <th>Ballon</th> </tr>

<tr> <td>High Quality</td>  <td><img width= 75% src="https://user-images.githubusercontent.com/9025925/36542413-046d8116-17e1-11e8-93ed-210f65293d51.jpg"></td> 
	<td><p align="center"><image src="https://placehold.it/30/228B22?text=+"/></p></td> 
	<td><p align="center"><image src="https://placehold.it/30/228B22?text=+"/></p></td> 
	<td><p align="center"><image src="https://placehold.it/30/228B22?text=+"/></p></td> 
	<td><p align="center"><image src="https://placehold.it/30/228B22?text=+"/></p></td> 
	<td><p align="center"><image src="https://placehold.it/30/DC143C?text=+"/></p></td> 
</tr> 
<tr> <td>Low Quality</td>  <td><img width= 75% src="https://user-images.githubusercontent.com/9025925/36542414-0498079c-17e1-11e8-9224-a9852797b96f.jpg"></td> 
<td><p align="center"><image src="https://placehold.it/30/228B22?text=+"/></p></td> 
	<td><p align="center"><image src="https://placehold.it/30/228B22?text=+"/></p></td> 
	<td><p align="center"><image src="https://placehold.it/30/228B22?text=+"/></p></td> 
	<td><p align="center"><image src="https://placehold.it/30/228B22?text=+"/></p></td> 
	<td><p align="center"><image src="https://placehold.it/30/DC143C?text=+"/></p></td>
</tr> 

 <tr> <td>Altered Copyright</td>  <td><img width= 75% src="https://user-images.githubusercontent.com/9025925/36542411-0438eb36-17e1-11e8-9a59-2c69937560bf.jpg"> </td> 
<td><p align="center"><image src="https://placehold.it/30/228B22?text=+"/></p></td> 
	<td><p align="center"><image src="https://placehold.it/30/228B22?text=+"/></p></td> 
	<td><p align="center"><image src="https://placehold.it/30/228B22?text=+"/></p></td> 
	<td><p align="center"><image src="https://placehold.it/30/228B22?text=+"/></p></td> 
	<td><p align="center"><image src="https://placehold.it/30/DC143C?text=+"/></p></td>
</tr> 

<tr> <td>Thumbnail</td>  <td><img src="https://user-images.githubusercontent.com/9025925/36542415-04ca8078-17e1-11e8-9be4-9a90b08c404b.jpg"></td> 
<td><p align="center"><image src="https://placehold.it/30/228B22?text=+"/></p></td> 
	<td><p align="center"><image src="https://placehold.it/30/228B22?text=+"/></p></td> 
	<td><p align="center"><image src="https://placehold.it/30/228B22?text=+"/></p></td> 
	<td><p align="center"><image src="https://placehold.it/30/228B22?text=+"/></p></td> 
	<td><p align="center"><image src="https://placehold.it/30/DC143C?text=+"/></p></td>
</tr> 
	
<tr> <td>Ballon</td>  <td><img width= 75% src="https://user-images.githubusercontent.com/9025925/36542417-04f3e6a2-17e1-11e8-91b2-50f9961524b4.jpg"></td> 
<td><p align="center"><image src="https://placehold.it/30/DC143C?text=+"/></p></td> 
	<td><p align="center"><image src="https://placehold.it/30/DC143C?text=+"/></p></td> 
	<td><p align="center"><image src="https://placehold.it/30/DC143C?text=+"/></p></td> 
	<td><p align="center"><image src="https://placehold.it/30/DC143C?text=+"/></p></td> 
	<td><p align="center"><image src="https://placehold.it/30/228B22?text=+"/></p></td>
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

See the wiki page on how to test differet hashing algorithms with your set of images

<img src="https://user-images.githubusercontent.com/9025925/49185669-c14a0b80-f362-11e8-92fa-d51a20476937.jpg" />
