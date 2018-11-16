# JImageHash



[![Travis](https://travis-ci.org/KilianB/JImageHash.svg?branch=master)](https://travis-ci.org/KilianB/JImageHash)
[![GitHub license](https://img.shields.io/github/license/KilianB/JImageHash.svg)](https://github.com/KilianB/JImageHash/blob/master/LICENSE)
 [ ![Download](https://api.bintray.com/packages/kilianb/maven/JImageHash/images/download.svg) ](https://bintray.com/kilianb/maven/JImageHash/_latestVersion)


JImageHash is a performant perceptual image fingerprinting library written entirely in Java. The library returns a similarity score aiming to identify entities which are likely modifications of the original source while being robust to color, scale and rotational transformation.

>  A perceptual hash is a fingerprint of a multimedia file derived from various features from its content. Unlike cryptographic hash functions which rely on the avalanche effect of small changes in input leading to drastic changes in the output, perceptual hashes are "close" to one another if the features are similar.

This library was inspired by _Dr. Neal Krawetz_ blog post "<a href="http://www.hackerfactor.com/blog/index.php?/archives/529-Kind-of-Like-That.html">kind of like that</a>" and incorporates several improvements i.e. adjustable hash resolution, diagonal gradient detection, rotational invariant hash. A comprehensive overview of perceptual image hashing can be found in this <a href="https://www.phash.org/docs/pubs/thesis_zauner.pdf">paper</a> by Christoph Zauner. 

## Maven - Bintray

The project is hosted on bintray and jcenter. Please be aware that hashes created with versions 1.x.x are not compatible with versions >= 2.0.0

````
<repositories>
	<repository>
		<id>jcenter</id>
		<url>https://jcenter.bintray.com/</url>
	</repository>
</repositories>

<dependency>
	<groupId>com.github.kilianB</groupId>
	<artifactId>JImageHash</artifactId>
	<version>1.0.2</version>
</dependency>
````


## Example 

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
			<td>Test the algorithm settings against your images to see which settings are correct.</td>
		</tr>
	</tbody>
</table>


#### Hello World: Check if two images are likely duplicates of each other
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

#### Check batch of images

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


#### Persistently Store Hashes (New Version 2.0.0)

The database image matcher allows you to store hashes persistently between jvm lifecycles.

````Java
	//0. Either create the matcher yourself
		
	String dbName = "imageHashDB";
	String userName = "root";
	String password = "";
	
	DatabaseImageMatcher db = new DatabaseImageMatcher(dbName,userName,password);
	
	// Proceed as normal
	db.addHashingAlgorithm(new AverageHash(32),25);
	db.addHashingAlgorithm(new PerceptiveHash(20),15);
	
	db.addImage(new File("ImageFile.png"));
	db.addImage("UniqueId",BufferedImage);
	
	//Opposed to all other matchers you either get a filename or the unique id returned
	PriorityQueue results = db.getMatchingImages(...);
	
	//1. Connect via a connection object
	Class.forName("org.h2.Driver");
	Connection conn = DriverManager.getConnection("jdbc:h2:~/" + dbName, userName, password);
	db = DatabaseImageMatcher.createDefaultMatcher(conn);
	
	
	//2. Load from database
	db = DatabaseImageMatcher.getFromDatabase(conn,1);
	
	//2.1 to load from database it has to be saved to the database first
	db.serializeToDatabase(1);
````

## Hashing algorithm

Each algorithm comes with individual properties
<table>
  <tr><th>Algorithm</th>  <th>Feature</th><th>Notes</th> </tr>
  <tr><td>AverageHash</td>  <td>Color based</td> <td>Slow. Not robust against hue/sat changes</td> </tr>
  <tr><td>DifferenceHash</td> <td>Gradient based</td> <td>Generally prefered algorithm. Fast and accurate</td> </tr>
  <tr><td>Perceptive Hash</td> <td>Frequency based</td> <td>In some cases more accurate than dHash. Best accuracy / bitResolution </td> </tr>  
  <tr><td>RotPHash *</td> <td>Frequency based (rotational invariant)</td> <td> Rotational invariant version of pHash</td> </tr>  
  <tr><td>HogHash*</td> <td>Directional Gradient based</td> <td>A hashing algorithm based on hog feature detection which extracts gradients and pools them by angles. Currently work in progress and not ready yet</td> </tr>  
</table>

* these hashes are included starting with version 2.

````java

//Key bit resolution
int keyLength = 64;
	
//Pick an algorithm
HashingAlgorithm hasher = new AverageHash(keyLength);
	
public boolean compareTwoImages(File image1, File image2) throws IOException {
		
	//Hash images
	Hash hash1 = hasher.hash(image1);
	Hash hash2 = hasher.hash(image2);
		
	//Ranges between 0 - keyLength.  The lower the more similar the image is.
	int similarityScore = hash1.hammingDistance(hash2);
		
	return similarityScore < 20;	
}
  
````
Only hashes produced by the same algorithm with the same bit resolution can be compared.

### DifferenceHash
The dHash algorithm is the fastest algorithm while mostly being on par on detection quality with the perceptive hash. Usually this algorithm is the choice to go with. Difference hash calculates the gradient in the image. As gradients are depended on the direction of the scanning it's a good idea to take additional dimensions into account.

- DoublePrecision will double the resulting hashSize but additionally accounts for top to bottom gradients
- TripplePrecision will triple the resulting hashSize but additionally accounts for diagonal gradients 

## Algorithm chaining & fine tuning
In some situations it might be useful to chain multiple detection algorithms back to back to utilize the different features they are based on. 
A promising approach is to first filter images using the fast difference hash with a low resolution key and if a potential match is found checking again with the perceptive hash function.

The 'ImageMatchers' provide a set of classes to do exactly this.

Depending on the image domains you may want to play around with different algorithm & threshold combinations to see at which point you get a high retrieval rate without
too many false positives. The most granular control you can achieve by calculating the hammingDistance on 2 hashes. A small hamming distance corresponds to closer related images. If two images are identical their hamming distance will be 0. 
Be aware that unlike the normalized hamming distance the hamming distance ranges from 0 -> bitKeyResolution.


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
Describes the tradeoff between false positives and false negatives. Also see his comments on Equal Error Rate which are applicable if you want to minimze the amount of falsely categorized images.


## Use the benchmark utility to see how algorithms react to individual images

Figuring out which algorithm to use with which settings is a bit tricky. Starting with version 2.0.0 the `AlgorithmBenchmarker` allows you to directly look at your test images and composes statistics on how the individual algorithm are doing.

````Java

//Easy setup. Configure a SingleImageMatcher with the algorithms you want to test.

	SingleImageMatcher matcher = new SingleImageMatcher();
	
	//Add a bunch of algorithms using normalized hamming distance with a 40% treshold
	matcher.addHashingAlgorithm(new AverageHash(16), 0.4f, true);
	matcher.addHashingAlgorithm(new AverageHash(64), 0.4f, true);
	matcher.addHashingAlgorithm(new PerceptiveHash(16), 0.4f, true);
	matcher.addHashingAlgorithm(new PerceptiveHash(64), 0.4f, true);
	matcher.addHashingAlgorithm(new DifferenceHash(16, Precision.Simple), 0.4f, true);
	matcher.addHashingAlgorithm(new DifferenceHash(64, Precision.Simple), 0.4f, true);
	matcher.addHashingAlgorithm(new DifferenceHash(16, Precision.Double), 0.4f, true);
	matcher.addHashingAlgorithm(new HogHash(64), 0.4f, true);
	matcher.addHashingAlgorithm(new RotPHash(64), 0.4f, true);

	//Create the benchmarker 
	AlgorithmBenchmarker bm = new AlgorithmBenchmarker(matcher);

 	//Add test images  Category label, image file
	bm.addTestImages(new TestData(0, new File("src/test/resources/ballon.jpg")));
	bm.addTestImages(new TestData(1, new File("src/test/resources/copyright.jpg")));
	bm.addTestImages(new TestData(1, new File("src/test/resources/highQuality.jpg")));
	bm.addTestImages(new TestData(1, new File("src/test/resources/lowQuality.jpg")));
	bm.addTestImages(new TestData(1, new File("src/test/resources/thumbnail.jpg")));

	//Enjoy your html file
	bm.asHTML();

````
You will get something like this:

![statistics](https://user-images.githubusercontent.com/9025925/48593402-ff9bff80-e94c-11e8-9213-70900e619667.png)

Lets run down the table and see if we can interpret the results:
Each image is tested against each other with each hashing algorithm. If two images are carrying the same class label they are expected to be matched. Yellow numbers indicating a mismatch. If categories are matching a lower number is better. Distinct categories should produce a higher number.

The <b>avg match</b> category displays the average distance of all the images in the same category. 
The <b>avg distinct</b> column shows the avergae distance of all images which are not in the same category.

The actual value of these isn't really important. It doesn't matter if the avg match is at .2 or .5 as long as the delta between match and distinct is big enough to work with.

The perceptive hash is showing a great differentiation between matches and distinct images. We can see that the difference hash (16) is struggling. When using this algorithm you may want to increase the bit resolution to achieve a better result as seen in the 64 version. 
The HogHash (not yet released) and RotPHash demonstrate  how this matrix can help fine tuning algorithms. While they fail to identify distinct images using the 40% settings they are in fact able to differentiate the images if the threshold is correctly adjusted to a more suited value of .2 and .1 respectively.

At the bottom of the table you can find a confusion matrix allowing you to calculate recall or any other metric as desired.

<b>Precision</b> indicates that if images are considered a match how likely are they matched. It's noted that due to chaining algorithms a weak precision value can be increased.


The same algorithms as before, but with other types of images:!

````Java
	//Running the test with your expected type of images is important!!
	db.addTestImages(new TestData(0, new File("src/test/resources/ballon.jpg")));

	//Rotated images
	db.addTestImages(new TestData(2, new File("src/test/resources/Lenna.png")));
	db.addTestImages(new TestData(2, new File("src/test/resources/Lenna90.png")));
	db.addTestImages(new TestData(2, new File("src/test/resources/Lenna180.png")));
	db.addTestImages(new TestData(2, new File("src/test/resources/Lenna270.png")));
````

![rot](https://user-images.githubusercontent.com/9025925/48594271-4a6b4680-e950-11e8-8a9e-8c871584b37d.png)


Who would have guessed that the rotation invariant hash will be the best algorithm when dealing with rotated images. Suddenly all other algorithms fail. If the treshold of the RotPHash algorithm gets adjusted to .1 it would be able to identify all images perfectly.



## Key bit resolution

Using the bit resolution setting you may alter the resulting hash length as well as the hash resolution. Altering the key resolution offers a trade off between computation time, storage space and feature detection quality. 

<b>Note</b> a huge resolution is **not** always preferable as image fingerprinting shall be more concerned about broad feature and structure of an image rather than individual details. Resolution ranges between from 32 - 128 bit have proven to be suitable values.

Original Image:
<p align="center"><img src="https://user-images.githubusercontent.com/9025925/36540877-562da094-17dc-11e8-95cf-f2da269258f4.jpg"></p>

<table>
<tr>
  <th>aHash</th>
  <th>dHash Top/Bottom</th>
  <th>dHash Left/Right</th>
  <th>dHash Diagonal</th>
  <th>pHash</th>
</tr>
   
   <tr>
  <td colspan = 5> 2^6 = 64 bit</td>
</tr>
  <tr> 
    <td><img width = 100% src="https://user-images.githubusercontent.com/9025925/36540058-3a4fd520-17d9-11e8-805d-7483334699d8.png"></td> 
    <td><img width = 100% src="https://user-images.githubusercontent.com/9025925/36540059-3a7dbb2a-17d9-11e8-97e0-36600e0a5446.png"></td>  
    <td><img width = 100% src="https://user-images.githubusercontent.com/9025925/36540060-3ac4c39e-17d9-11e8-803b-9230f36c5a4a.png"></td> 
    <td><img width = 100% src="https://user-images.githubusercontent.com/9025925/36540061-3b193f00-17d9-11e8-85f2-d0fd38f7ef0b.png"></td> 
    <td><img width = 100% src="https://user-images.githubusercontent.com/9025925/36540063-3b4315b4-17d9-11e8-8b20-ac7e49f3f637.png"></td> 
  </tr>

<tr>
  <td colspan = 5> 2^8 = 256 bit</td>
</tr>
<tr> 
    <td><img width = 100% src="https://user-images.githubusercontent.com/9025925/36540119-7aacd79e-17d9-11e8-935f-86f7213e46e3.png"></td> 
    <td><img width = 100% src="https://user-images.githubusercontent.com/9025925/36540120-7aeeba92-17d9-11e8-9044-9fea43be1dd4.png"></td>  
    <td><img width = 100% src="https://user-images.githubusercontent.com/9025925/36540121-7b1837dc-17d9-11e8-9e8e-56e828aa3376.png"></td> 
    <td><img width = 100% src="https://user-images.githubusercontent.com/9025925/36540122-7b4167f6-17d9-11e8-81a5-60fe0c040b69.png"></td> 
    <td><img width = 100% src="https://user-images.githubusercontent.com/9025925/36540124-7b8a8f12-17d9-11e8-9d72-db969711082f.png"></td> 
  </tr>
  
  <tr>
  <td colspan = 5> 2^12 = 4096 bit</td>
</tr>
<tr> 
    <td><img width = 100% src="https://user-images.githubusercontent.com/9025925/36540233-eb95f544-17d9-11e8-93cc-87d001263c5c.png"></td> 
    <td><img width = 100% src="https://user-images.githubusercontent.com/9025925/36540234-ebdfbf9e-17d9-11e8-9cdf-9fc0815356c4.png"></td>  
    <td><img width = 100% src="https://user-images.githubusercontent.com/9025925/36540235-ec082efc-17d9-11e8-8a74-fa3a4d32e379.png"></td> 
    <td><img width = 100% src="https://user-images.githubusercontent.com/9025925/36540236-ec332c56-17d9-11e8-8442-30f90db53845.png"></td> 
    <td><img width = 100% src="https://user-images.githubusercontent.com/9025925/36540237-ec839eac-17d9-11e8-9e48-d666d25010f3.png"></td> 
  </tr>
  
<tr>
  <td colspan = 5> 2^18 = 262144 bit</td>
</tr>
<tr> 
    <td><img width = 100% src="https://user-images.githubusercontent.com/9025925/36539809-53689994-17d8-11e8-9606-10ec09af5a46.png"></td> 
    <td><img width = 100% src="https://user-images.githubusercontent.com/9025925/36539810-54287340-17d8-11e8-9753-98e98c404863.png"></td>  
    <td><img width = 100% src="https://user-images.githubusercontent.com/9025925/36539811-545dee4e-17d8-11e8-9437-98de5ee62c13.png"></td> 
    <td><img width = 100% src="https://user-images.githubusercontent.com/9025925/36539812-5484601a-17d8-11e8-8f86-5b3ea320e500.png"></td> 
    <td><img width = 100% src="https://user-images.githubusercontent.com/9025925/36539813-54bdd688-17d8-11e8-9b06-6b2a5163498d.png"></td> 
  </tr>
<table>
	
<table>
<tr> <th>Bit</th>  <th>Binary</th> <th>Hex</th></tr>
<tr> <td>64</td>  <td>111100001001000011110000101000011011001111111111010010000000</td> <td>f090f0a1b3ff480</td></tr>	<tr> <tr> <td>128</td>           <td>110011000011110110000011100100000110101000001101000001000</br>
	                        01000000111011100111001101110000000010111110000010111111111</td> <td>cc3d83906a0d04207739b805f05ff</td></tr>	
</table>


### Example Application 
A gui application can be found the in example folder: Be aware that the results will be poor due to only one algorithms be applied at a time.
Only the first 100 google thumbnails are downloaded and usually there are not many true duplicates present. 
<p align="center"><img src="https://user-images.githubusercontent.com/9025925/43670281-2ca48ab6-978a-11e8-822b-fc2414586708.png"/></p>



### Example of hamming distances with 64 bit key

The following examples were found by creating a duplicate detection system to filter reposts on 9gag.com

Distance 5: Slight color change. 
<p></p>
<img align="left" width = 48% src="https://user-images.githubusercontent.com/9025925/36516748-be5f9b8e-177f-11e8-813e-9ff92c6e65a8.jpg"><img width = 48% src="https://user-images.githubusercontent.com/9025925/36516750-c00b942e-177f-11e8-8e42-deadc2c49d79.jpg">


Distance 9: (False positive). Due to difference hash relying on gradient search on a compressed image and the text being swapped out for a similar object this algorithm failed. Rehash with perceptual algorithm.
<p>
<img align="left" width = 48% src="https://user-images.githubusercontent.com/9025925/36517079-2efe056e-1781-11e8-9db7-a182e2727985.jpg"><img width = 48% src="https://user-images.githubusercontent.com/9025925/36517081-30157f72-1781-11e8-9ed1-8ebb8d64aba9.jpg">
</p>

<p>Distance 10: Resized image</p>
<img align="left" width = 48% src="https://user-images.githubusercontent.com/9025925/36517179-a59b47a4-1781-11e8-8f00-a8d47856e6f0.jpg"><img width = 48% src="https://user-images.githubusercontent.com/9025925/36517181-a71751fe-1781-11e8-81d6-56cdfdae614f.jpg">




### Known Bugs
- Perceptive hash relies on JTransform which fails to close a threadpool upon calculating large DCT matrices resulting in the JVM not to terminate. If you want to calculate a perceptive hash with a large bit resolution call `ConcurrencyUtils.shutdownThreadPoolAndAwaitTermination();` if you want to terminate the program.

