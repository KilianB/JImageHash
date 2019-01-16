<img align=left src = "https://user-images.githubusercontent.com/9025925/48595271-388ba280-e954-11e8-8bc6-8b8afe108682.png" />

# JImageHash

[![Travis](https://travis-ci.org/KilianB/JImageHash.svg?branch=master)](https://travis-ci.org/KilianB/JImageHash)
[![GitHub license](https://img.shields.io/github/license/KilianB/JImageHash.svg)](https://github.com/KilianB/JImageHash/blob/master/LICENSE)
[![Download](https://api.bintray.com/packages/kilianb/maven/JImageHash/images/download.svg)](https://bintray.com/kilianb/maven/JImageHash/_latestVersion)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/abddaa1a0190440487ca955b088859c9)](https://www.codacy.com/app/KilianB/JImageHash?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=KilianB/JImageHash&amp;utm_campaign=Badge_Grade)

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
			<td><a href="/src/main/java/com/github/kilianB/examples/AlgorithmBenchmark.java">FuzzyHashes.java</a></td>
			<td>FuzzyHashes and clustering</td>
		</tr>
		<tr>
			<td><a href="/src/main/java/com/github/kilianB/examples/AlgorithmBenchmark.java">AlgorithmBenchmark.java</a></td>
			<td>Extensive tutotial matching 17.000 images . Following the block post ...</td>
		</tr>
	</tbody>
</table>

### Hello World: Check if two images are likely duplicates of each other

````java
public static void main(String[] args){

  //Load images
  BufferedImage img1 = ImageIO.read(new File("image1.jpg"));
  BufferedImage img2 = ImageIO.read(new File("image2.jpg"));
  
  SingleImageMatcher matcher = SingleImageMatcher.createDefaultMatcher();
	
  if(matcher.checkSimilarity(img1, img2)){
    //likely duplicate found
  }
}
````

### Check batch of images

````java
public void matchMultipleImagesInMemory() {

	InMemoryImageMatcher matcher = InMemoryImageMatcher.createDefaultMatcher();

	//Add all images of interest to the matcher and precalculate hashes
	matcher.addImages(ballon,copyright,highQuality,lowQuality,thumbnail);
		
	//Find all images which are similar to highQuality
	PriorityQueue<Result<BufferedImage>> similarImages = matcher.getMatchingImages(highQuality);
		
	//Print out results
	similarImages.forEach(result ->{
		System.out.printf("Distance: %3d Image: %s%n",result.distance,result.value);
	});
}
````


Multiple types image matchers are available for each situation

The `persistent` package allows hashes and matchers to be saved to disk. In turn the images are not kept in memory and are only referenced by file path allowing to handle a great deal of images
at the same time.
The `cached` version keeps the BufferedImage image objects in memory allowing to change hashing algorithms on the fly and a direct retrieval of the buffered image objects of matching images.
The `categorize` package contains image clustering matchers. KMeans and Categorical as well as weighted matchers.
The `ecotic` package 


<table>
	<tr> <th>Image Matcher Class</th> <th>Feature</th> </tr>
	<tr> <td>SingleImageMatcher</td> <td>Compare if two images are similar with multiple chained hashing algorithms. An allowed distance is defined for each algorithm. To consider images a match every filter has to be passed independently.</td> </tr>
	<tr>	<td>InMemoryMatcher</td> <td>Keep precomputed hashes in memory and quickly tell apart batches of images. An allowed distance is defined for each algorithm. To consider images a match every filter has to be passed independently.</td></tr>
	<tr>	<td>CumulativeInMemoryMatcher</td> <td>Keep precomputed hashes in memory and quickly tell apart batches of images. An overall distance threshold is defined which is checked against the sum of the distances produced by all filters</td></tr>
	<tr>	<td>DatabaseImageMatcher</td> <td>Store computed hashes in a SQL database to tell apart batches of images while still keeping the hashes around even after a restart of the JVM. Conceptually this class behaves identical to the InMemoryMatcher. Performance penalties may incur due to binary tree's not being used.</td></tr>
</table>

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

### Persistently Store Hashes (New Version 2.0.0)

The database image matcher allows you to store hashes persistently between JVM lifecycles.

````Java
String dbName = "imageHashDB";
String userName = "root";
String password = "";
	
DatabaseImageMatcher db = new DatabaseImageMatcher(dbName,userName,password);
	
// Proceed as normal
db.addHashingAlgorithm(new AverageHash(32),25);
db.addHashingAlgorithm(new PerceptiveHash(20),15);
	
//Image hashes are saved to the database and persistently available
db.addImage(new File("ImageFile.png"));
db.addImage("UniqueId",BufferedImage);
	
//Opposed to other matchers you get the absolute file path or the unique id returned
PriorityQueue results = db.getMatchingImages(...);
	
//....
//Find all images which are similar to any image in the database
Map allMatchingImages = db.getAllMatchingImages();
	
//Once done you can also save the matcher to the database for later retrieval desired.
db.serializeToDatabase(1);
````

You may also use a connection object to connect to a database of your choice. Matchers can be serialized to the db and can be reconstructed at a later time

````Java
	
//1. Connect via a connection object
Class.forName("org.h2.Driver");
Connection conn = DriverManager.getConnection("jdbc:h2:~/" + dbName, userName, password);
	
//2. Load from database
db = DatabaseImageMatcher.getFromDatabase(conn,1);
````

## Hashing algorithm

Image matchers can be configured using different algorithm. Each comes with individual properties
<table>
  <tr><th>Algorithm</th>  <th>Feature</th><th>Notes</th> </tr>
  <tr><td><a href="#averagehash-averagekernelhash-medianhash-averagecolorhash">AverageHash</a></td>  <td>Average Luminosity</td> <td>Fast and good all purpose algorithm</td> </tr>
  <tr><td><a href="#averagehash-averagekernelhash-medianhash-averagecolorhash">AverageColorHash</a></td>  <td>Average Color</td> <td>Version 1.x.x AHash. Usually worse off than AverageHash. Not robust against color changes</td> </tr>
  <tr><td><a href="#differencehash">DifferenceHash</a></td> <td>Gradient/Edge detection</td> <td>A bit more robust against hue/sat changes compared to AColorHash </td> </tr>
  <tr><td><a href="#perceptive-hash">Wavelet Hash</a></td> <td>Frequency & Location</td> <td>Feature extracting by applying haar wavlets multiple times to the input image. Detection quality better than inbetween aHash and pHash.</td> </tr>
  <tr><td><a href="#perceptive-hash">PerceptiveHash</a></td> <td>Frequency</td> <td>Hash based on Discrete Cosine Transformation. Smaller hash distribution but best accuracy / bitResolution.</td> </tr>
  <tr><td><a href="#averagehash-averagekernelhash-medianhash-averagecolorhash">MedianHash</a></td> <td>Median Luminosity</td> <td>Identical to AHash but takes the median value into account. A bit better to detect watermarks but worse at scale transformation</td> </tr>
  <tr><td><a href="#averagehash-averagekernelhash-medianhash-averagecolorhash">AverageKernelHash</a></td>  <td>Average luminosity </td> <td>Same as AHash with kernel preprocessing. So far usually performs worse, but testing is not done yet.</td> </tr>
  <tr><td colspan=3 align=center><b>Rotational Invariant</b></td></tr>
  <tr><td><a href="#rotaveragehash">RotAverageHash</a></td>  <td>Average Luminosity</td> <td>Rotational robust version of AHash. Performs well but performance scales disastrous with higher bit resolutions . Conceptual issue: pixels further away from the center are weightend less.</td> </tr>
  <tr><td><a href="#rotphash">RotPHash</a></td> <td>Frequency</td> <td> Rotational invariant version of pHash using ring partition to map pixels in a circular fashion. Lower complexity for high bit sizes but due to sorting pixel values usually maps to a lower normalized distance. Usually bit res of >= 64bits are preferable</td> </tr>  
   <tr><td colspan=3 align="center"><i><b>Experimental.</b> Hashes available but not well tuned and subject to changes</i></td></tr>
  <tr><td><a href="#hoghash">HogHash</a></td> <td>Angular Gradient based (detection of shapes?) </td> <td>A hashing algorithm based on hog feature detection which extracts gradients and pools them by angles. Usually used in support vector machine/NNs human outline detection. It's not entirely set how the feature vectors should be encoded. Currently average, but not great results, expensive to compute and requires a rather high bit resolution</td> </tr>  
</table>

A combination of Average and PerceptiveHashes are usually your way to go. 

## Algorithm chaining & fine tuning
In some situations it may be useful to chain multiple detection algorithms back to back to utilize the different features they are based on. 
A promising approach is to first filter images using the fast difference hash with a low resolution key and if a potential match is found checking again with the perceptive hash function.

The 'ImageMatchers' provide a set of classes to do exactly this.

Depending on the image domains you may want to play around with different algorithm & threshold combinations to see at which point you get a high retrieval rate without
too many false positives. The most granular control you can achieve by calculating the hammingDistance on 2 hashes. The hamming distance is a metric indicating how similar two hashes are. A small distance corresponds to closer related images. If two images are identical their hamming distance will be 0. 

The <b>hamming distance</b> returned by algorithms ranges from `[0 - bitKeyResolution]` (chosen during algorithm creation)
The <b>normalized hamming distance</b> ranges from `[0 - 1]`.

````java
/**
 * Compares the similarity of two images.
 * @param image1	First image to be matched against 2nd image
 * @param image2	The second image
 * @return	true if the algorithm defines the images to be similar.
 */
public boolean compareTwoImages(BufferedImage image1, BufferedImage image2) {

	// Key bit resolution
	int keyLength = 64;

	// Pick an algorithm
	HashingAlgorithm hasher = new AverageHash(keyLength);
		
	//Generate the hash for each image
	Hash hash1 = hasher.hash(image1);
	Hash hash2 = hasher.hash(image2);

	//Compute a similarity score
	// Ranges between 0 - 1. The lower the more similar the images are.
	double similarityScore = hash1.normalizedHammingDistance(hash2);

	return similarityScore < 0.3d;
}
````

<p align= "center">
<img src="https://user-images.githubusercontent.com/9025925/36545875-3805f32e-17ea-11e8-9b28-96e25ba0ea67.png">
	</p>
<p align= "center"><a href="https://www.phash.org/docs/pubs/thesis_zauner.pdf">Source</a>
</p>
The image describes the tradeoff between false retrieval rate and false acceptance rate.

Only hashes produced by the same algorithm with the same bit resolution can be compared.
