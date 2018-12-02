<img align=left src = "https://user-images.githubusercontent.com/9025925/48595271-388ba280-e954-11e8-8bc6-8b8afe108682.png" />

# JImageHash

[![Travis](https://travis-ci.org/KilianB/JImageHash.svg?branch=master)](https://travis-ci.org/KilianB/JImageHash)
[![GitHub license](https://img.shields.io/github/license/KilianB/JImageHash.svg)](https://github.com/KilianB/JImageHash/blob/master/LICENSE)
[![Download](https://api.bintray.com/packages/kilianb/maven/JImageHash/images/download.svg) ](https://bintray.com/kilianb/maven/JImageHash/_latestVersion)


JImageHash is a performant perceptual image fingerprinting library entirely written in Java. The library returns a similarity score aiming to indentify entities which are likely modifications of the original source while being robust variouse attack vectors ie. color, rotation and scale transformation.

>  A perceptual hash is a fingerprint of a multimedia file derived from various features from its content. Unlike cryptographic hash functions which rely on the avalanche effect of small changes in input leading to drastic changes in the output, perceptual hashes are "close" to one another if the features are similar.

This library was inspired by _Dr. Neal Krawetz_ blog post "<a href="http://www.hackerfactor.com/blog/index.php?/archives/529-Kind-of-Like-That.html">kind of like that</a>" and incorporates several improvements. A comprehensive overview of perceptual image hashing can be found in this <a href="https://www.phash.org/docs/pubs/thesis_zauner.pdf">paper</a> by Christoph Zauner. 

## Maven - Bintray

The project is hosted on bintray and jcenter. <b>Please be aware that hashes created with versions 1.x.x are not compatible with versions >= 2.0.0.</b> Some method signatures and variable names have changed as well!

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
	<version>2.0.2</version>
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
			<td>Test different algorithm/setting combintations against your images to see which settings give the best result.</td>
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

Multiple types image matchers are available for each situation

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


#### Persistently Store Hashes (New Version 2.0.0)

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

The <b>hamming distance</b> returned by algorithms ranges from [0 - bitKeyResolution] (choosen during algorithm creation)
The <b>normalized hamming distance</b> ranges from [0 - 1].


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

## Use the benchmark utility to see how algorithms react to individual images

Figuring out which algorithm to use with which settings is a bit tricky. Starting with version <i>2.0.0</i> the `AlgorithmBenchmarker` allows you to directly look at your test images and composes statistics on how the individual algorithm are doing.

````Java

//Easy setup. Configure a SingleImageMatcher with the algorithms you want to test.

SingleImageMatcher matcher = new SingleImageMatcher();
	
//Naively add a bunch of algorithms using normalized hamming distance with a 40% threshold
matcher.addHashingAlgorithm(new AverageHash(16), 0.4f);
matcher.addHashingAlgorithm(new AverageHash(64), 0.4f);
matcher.addHashingAlgorithm(new PerceptiveHash(16), 0.4f);
....

//Create the benchmarker 
boolean includeSpeedMicrobenchmark = true;
AlgorithmBenchmarker bm = new AlgorithmBenchmarker(matcher,includeSpeedMicrobenchmark);

//Add test images  Category label, image file
bm.addTestImages(new TestData(0, new File("src/test/resources/ballon.jpg")));
bm.addTestImages(new TestData(1, new File("src/test/resources/copyright.jpg")));
bm.addTestImages(new TestData(1, new File("src/test/resources/highQuality.jpg")));
bm.addTestImages(new TestData(1, new File("src/test/resources/lowQuality.jpg")));
bm.addTestImages(new TestData(1, new File("src/test/resources/thumbnail.jpg")));

//Enjoy your benchmark
//bm.display(); bm.toConsole();
bm.toFile(); ;
````
You will get something like this:

![benchmark](https://user-images.githubusercontent.com/9025925/49185669-c14a0b80-f362-11e8-92fa-d51a20476937.jpg)

<b>Note:</b> The report is generated as a HTML document and may either be directly displayed in a JavaFX webview via the
`display();` call or saved as .html file ('.toFile()') to be opened in a browser of your choice. Due to the webview 
not supporting javascript entirely the chart component will only work if viewed in a browser.

Lets run down the table and see if we can interpret the results:
Each image is tested against each other using every of the supplied hashing algorithm. If two images are carrying the same class label they are expected to be matched. Yellow numbers indicate a deviation from the expected behaviour. The table displays the distances (normalized or not depending on the what was specified in the `addHashingAlgorithm()` method call). Same categories expect numbers below the threshold while distinct categories expect a number above the threshold.

<ul>
<li>The <b>avg match</b> category displays the average distance of all the images in the same category (<b>expected</b> matches). </li>
<li>The <b>avg distinct</b> column shows the average distance of all images which are not in the same category.(<b>expected</b> distinct images)</li>
</ul>
The actual value of these two cells isn't really important. It doesn't matter if the avg match is .2 or .5 as long as the delta between match and distinct is big enough to allow to differentiate between categories.

The perceptive hash is showing a great differentiation between matches and distinct images. We can see that the difference hash (32 tipple precision) is struggling. When using this algorithm you may want alter the threshold to split the two groups of images.

While the average values are a great indication what is really important are the min and max values. If they do not overlap a perfect categorization for your test images is possible if you simply pick any value inbetween these two bounds. At the bottom of the table you can find a confusion matrix allowing you to calculate recall or any other metric as desired.

<b>Precision</b> indicates that if images are considered a match how likely are they matched. It's noted that due to chaining algorithms a weak precision value can be increased.

Lets apply the benchmark to a different set of images:

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


Suddenly a lot of the other algorithms fail, since they are not robust against rotational attacks. If the treshold of the RotPHash algorithm gets adjusted to .1 it would be able to identify all images perfectly.



## Key bit resolution. Bigger isn't "always" better!

When creating an algorithm instance you are asked to supply a bit resolution value. Using the bit resolution setting you define the number of bits present in the resulting hashes. Altering the key resolution offers a trade off between computation time, storage space and feature detection quality. The default algorithms follow the contract to create a hash with at least the supplied bits but may require to return a longer hash if it is necessary due to geometric constraints.

<div align=center>
<img src= "https://placehold.it/15/f03c15/000000?text=+" /> <b>Note:</b> a huge resolution is <b>NOT</b> preferable as image fingerprinting shall be more concerned about broad feature and structure of an image rather than individual details. Resolution ranges between from 32 - 128 bit have proven to be suitable values but highly depend on the algorithm chosen.
</div>
</br>
<p>
To visualize the tradeoff let's move away from images. Assume we have two classes of objects (red apples and green pears) we want to categorize. With 1 bit of information [Color] we can categorize red objects as 0 and green objects as 1. 
The two groups/fruits are perfectly distinct. If we would calculate a normalized hamming distance we would get a distance of 100%. Great. But what happens if we suddenly also need to categorize yellow apples? Due to the way hashing functions work they are more likely to be categorized as pears due to their color. To fix this we could add a few new categories [Color,Shape,Price]. Suddenly we will again be able to separate the groups based on the information.
</p>

<pre>
		[Color, Shape, Price]
Red Apple 	: 0 0 0
Yellow Apple	: 1 0 0
Green Pear	: 1 1 1

Apples   [ [0,0,0] , [1,0,0] ]
Pear     [ [1,1,1] ]
</pre>

Due to adding more bits we get the ability to differentiate more features, on the other hand, the hamming distance between the two groups decreased to <b>0.66</b> instead of 1. Usually the more bits you add, the more groups you will detect but the harder it will be to actually distinguish between these groups. Sadly we can not hand pick our categories and are left at the mercy of our algorithms. 


### Image Preprocessing (Beta)

Sometimes you may wish to alter the images before they are hashed by applying filters like 
gaussian blur, box blur, sharpening ....

````Java
HashingAlgorithm pHash = new PerceptiveHash(32);
//Some examples of filters...
pHash.addFilter(Kernel.gaussianFilter(5, 5, 5));
pHash.addFilter(new MedianFilter(4,4));
pHash.addFilter(new SobelFilter(0.7));
//Or add your custom kernel masks ...
````


<b>Note:</b> Image preprocessing currently is a rough part of the module and may be improved over time. Preprocessing
each image in full resolution carries a major performance penalty, therefore it should be evaluated carefully if this step is really worth the effort. Most of the times filter do not improve the performance of hashing algorithms due to the fact that a lot of the effect is lost during reszing at a later stage.


<p> Alternatively take a look at <a href="/src/main/java/com/github/kilianB/hashAlgorithms/AverageKernelHash.java">AverageKernelHash</a> to see how to apply
kernels to the rescaled image. </p>

Here are some of the available filters:

<table>
	<tr><th>Filter</th> 			<th>Original</th> <th>Output</th> </tr>
	<tr><td>MedianKernel</td>	<td><img widht=300 height=300 src="https://user-images.githubusercontent.com/9025925/49190756-ec892680-f373-11e8-95aa-4ebe8c020e87.png"/></td>	<td><img width=300 height=300 src= "https://user-images.githubusercontent.com/9025925/49190670-a5029a80-f373-11e8-9c8f-3f4347a3ca6d.png"/></td></tr>
	<tr><td>Gaussian</td>	<td><img width=300 height=300 src="https://user-images.githubusercontent.com/9025925/49189543-11c76600-f36f-11e8-838c-b0e284a2a8a1.png"/></td>	<td><img width=300 height=300 src="https://user-images.githubusercontent.com/9025925/49189613-60750000-f36f-11e8-9914-9c385e9b0b4d.png"/></td></tr>
	<tr><td>Sobel</td>	<td><img width=300 height=300 src="https://user-images.githubusercontent.com/9025925/49189543-11c76600-f36f-11e8-838c-b0e284a2a8a1.png"/></td>	<td><img width=300 height=300 src="https://user-images.githubusercontent.com/9025925/49189448-b2695600-f36e-11e8-93a3-e842c1a2e632.png" /></td></tr>
</table>

### Example of hamming distances with 64 bit key

The following examples were found by creating a duplicate detection system to filter reposts on 9gag.com

#### Distance 5: Slight color change. 

<p>
<img align="left" width = 25% src="https://user-images.githubusercontent.com/9025925/36516748-be5f9b8e-177f-11e8-813e-9ff92c6e65a8.jpg"><img width = 25% src="https://user-images.githubusercontent.com/9025925/36516750-c00b942e-177f-11e8-8e42-deadc2c49d79.jpg">
</p>

#### Distance 9: (False positive). 
Due to difference hash relying on gradient search on a compressed image and the text being swapped out for a similar object this algorithm failed. Rehash with perceptual algorithm.
<p>
<img align="left" width = 25% src="https://user-images.githubusercontent.com/9025925/36517079-2efe056e-1781-11e8-9db7-a182e2727985.jpg"><img width = 25% src="https://user-images.githubusercontent.com/9025925/36517081-30157f72-1781-11e8-9ed1-8ebb8d64aba9.jpg">
</p>

#### Distance 10: Resized image
<p>
<img align="left" width = 25% src="https://user-images.githubusercontent.com/9025925/36517179-a59b47a4-1781-11e8-8f00-a8d47856e6f0.jpg"><img width = 25% src="https://user-images.githubusercontent.com/9025925/36517181-a71751fe-1781-11e8-81d6-56cdfdae614f.jpg">
</p>

## Some more information reagarding the differen hashing algorithms

### AverageHash, AverageKernelHash, MedianHash, AverageColorHash

The <b>average hash</b> works on the Y(Luma) component of the YCbCr color model. First the image is rescaled and the luma value calculated.
`Y = R * 0.299 + G + 0.587 + B * 0.114`. if the luma of the current pixel is higher or smaller than the average luminosity the bit of the hash is set.
This is a quick and easy operation but might get you in trouble once the luminosity of the image shifts.

The <b>Average kernel hash</b> simply performs an additional filter pass with an arbitrary kernel. By default a box filter is applied.

The <b>Average Color Hash</b> computes the grayscale value for each pixel `V = (R + G + B) /3`. The same approach as the AverageHash in version 1.x.x. Relying on the actual color values makes this hash vulnerable against color changes abd it usually performs worse than the luminosity based average hash.
The MedianHash compares the luminosity value against the median value. This guarantees each has to be 50% consistent out of 0 and 1 bits but eliminates outliers which may or may not be useable to differentiate images.

<table>
	<tr> <td>Algo / Resolution</td> <td>2^6 = 64 bit</td><td>2^8 = 256 bit</td><td>2^12 = 4096 bit</td><td>2^18 = 262144 bit</td> </tr>
	<tr> <td>AverageHash</td> <td><img src="https://user-images.githubusercontent.com/9025925/49260416-c4fe9080-f43d-11e8-8c67-1c09313227c4.png" width=100%/></td><td><img src="https://user-images.githubusercontent.com/9025925/49260417-c4fe9080-f43d-11e8-9979-5d6fff90415b.png" width=100%/></td><td><img src="https://user-images.githubusercontent.com/9025925/49260418-c4fe9080-f43d-11e8-8074-c110c887efa0.png" width=100%/></td><td><img src="https://user-images.githubusercontent.com/9025925/49260419-c4fe9080-f43d-11e8-93e5-46d067578581.png" width=100%/></td> </tr>
	<tr> <td>AverageKernelHash</td> <td><img src="https://user-images.githubusercontent.com/9025925/49260471-0727d200-f43e-11e8-8193-bcc00c9c46f2.png" width=100%/></td><td><img src="https://user-images.githubusercontent.com/9025925/49260472-0727d200-f43e-11e8-8700-5a5712d1e255.png" width=100%/></td><td><img src="https://user-images.githubusercontent.com/9025925/49260473-0727d200-f43e-11e8-9330-3bb77af886f3.png" width=100%/></td><td><img src="https://user-images.githubusercontent.com/9025925/49260474-07c06880-f43e-11e8-934b-b2d0d2ef0601.png" width=100%/></td> </tr>
	<tr> <td>MedianHash</td> <td><img src="https://user-images.githubusercontent.com/9025925/49260506-232b7380-f43e-11e8-8cb1-55e562c2cd24.png" width=100%/></td><td><img src="https://user-images.githubusercontent.com/9025925/49260503-232b7380-f43e-11e8-8fad-58d10f7b6319.png" width=100%/></td><td><img src="https://user-images.githubusercontent.com/9025925/49260504-232b7380-f43e-11e8-9eb4-141ccea41b59.png" width=100%/></td><td><img src="https://user-images.githubusercontent.com/9025925/49260505-232b7380-f43e-11e8-88f4-0811f21a50b4.png" width=100%/></td> </tr>
	<tr> <td>AverageColorHash</td> <td><img src="https://user-images.githubusercontent.com/9025925/49260555-638af180-f43e-11e8-8f5e-9f0dddc74860.png" width=100%/></td><td><img src="https://user-images.githubusercontent.com/9025925/49260556-638af180-f43e-11e8-8fd0-2a03a96cd300.png" width=100%/></td><td><img src="https://user-images.githubusercontent.com/9025925/49260553-638af180-f43e-11e8-8ecb-1c67dfc52088.png" width=100%/></td><td><img src="https://user-images.githubusercontent.com/9025925/49260554-638af180-f43e-11e8-802f-7db6012591e3.png" width=100%/></td> </tr>
</table>


### DifferenceHash
The difference hash calculates the gradient in the image. As gradients are depended on the direction of the scanning it's a good idea to take additional dimensions into account.

- DoublePrecision will double the resulting hashSize but additionally accounts for top to bottom gradients
- TripplePrecision will triple the resulting hashSize but additionally accounts for diagonal gradients 
While additional precision will increase the amount of information a 64 bit simple precision usually performs better than a 32 bit double precision hash.

<table>
	<tr> <td>Algo / Resolution</td> <td>2^6 = 64 bit</td><td>2^8 = 256 bit</td><td>2^12 = 4096 bit</td><td>2^18 = 262144 bit</td> </tr>
	<tr> <td>Top/Bottom</td> 
	 <td><img width = 100% src="https://user-images.githubusercontent.com/9025925/36540059-3a7dbb2a-17d9-11e8-97e0-36600e0a5446.png"></td>
	 <td><img width = 100% src="https://user-images.githubusercontent.com/9025925/36540120-7aeeba92-17d9-11e8-9044-9fea43be1dd4.png"></td>  
	 <td><img width = 100% src="https://user-images.githubusercontent.com/9025925/36540234-ebdfbf9e-17d9-11e8-9cdf-9fc0815356c4.png"></td> 
	 <td><img width = 100% src="https://user-images.githubusercontent.com/9025925/36539810-54287340-17d8-11e8-9753-98e98c404863.png"></td> 
	</tr>
	<tr> <td>Left/Right</td> 
		<td><img width = 100% src="https://user-images.githubusercontent.com/9025925/36540060-3ac4c39e-17d9-11e8-803b-9230f36c5a4a.png"></td>
		<td><img width = 100% src="https://user-images.githubusercontent.com/9025925/36540121-7b1837dc-17d9-11e8-9e8e-56e828aa3376.png"></td> 
		<td><img width = 100% src="https://user-images.githubusercontent.com/9025925/36540235-ec082efc-17d9-11e8-8a74-fa3a4d32e379.png"></td> 
		<td><img width = 100% src="https://user-images.githubusercontent.com/9025925/36539811-545dee4e-17d8-11e8-9437-98de5ee62c13.png"></td> 
	</tr>
	<tr>
		<td>Diagonal*</td>
			<td><img width = 100% src="https://user-images.githubusercontent.com/9025925/36540061-3b193f00-17d9-11e8-85f2-d0fd38f7ef0b.png"></td>
			<td><img width = 100% src="https://user-images.githubusercontent.com/9025925/36540122-7b4167f6-17d9-11e8-81a5-60fe0c040b69.png"></td> 
			<td><img width = 100% src="https://user-images.githubusercontent.com/9025925/36540236-ec332c56-17d9-11e8-8442-30f90db53845.png"></td> 
			<td><img width = 100% src="https://user-images.githubusercontent.com/9025925/36539812-5484601a-17d8-11e8-8f86-5b3ea320e500.png"></td> 
	</tr>
</table>

* The images are still of version 1.X.X. The diagonal hash got altered to correctly handle the offset at the corner of the image.

### Perceptive Hash

The image is rescaled as usual and the discrete cosine transformation of the lum values are calculated. Bits are assigned depending on if the coefficient of the dct is greater or smaller than the average. Due to the fact that the first few values are outlier and the lower half of the dct matrix represents features which are not visible to the human perception (the same reason why jpg compression works) the hash is calculated only on a subset of the data.

Due to the cosine structured values a great part of the hash is very likely to be similar in the vast majority of the images. The usual range of normalized hamming distances for pHash is smaller than than [0-1] and usually occupies values from [0-.5] (no hard evidence just observations). While the bits are not optimally used the differentiation between different types of images is outstanding and pHash represents a very good hashing algorithm.

Due to computing the dct this algorithm is slower than aHash or dHash.

<table>
	<tr> <td>Algo / Resolution</td> <td>2^6 = 64 bit</td><td>2^8 = 256 bit</td><td>2^12 = 4096 bit</td><td>2^18 = 262144 bit</td> </tr>
	<tr> <td>Perceptive Hash</td> 
	 <td><img width = 100% src="https://user-images.githubusercontent.com/9025925/49261366-1d379180-f442-11e8-915b-d7fd94b86598.png"></td>
	 <td><img width = 100% src="https://user-images.githubusercontent.com/9025925/49261363-1c9efb00-f442-11e8-94a9-c3b7b2a8b545.png"></td>  
	 <td><img width = 100% src="https://user-images.githubusercontent.com/9025925/49261364-1c9efb00-f442-11e8-9a81-d3318b8c1161.png"></td> 
	 <td><img width = 100% src="https://user-images.githubusercontent.com/9025925/49261365-1d379180-f442-11e8-987f-982622fe37b4.png"></td> 
	</tr>
</table>


### RotPHash

Similar to the perceptive hash this algorithm uses a discrete cosine transformation to extract the frequency of the image. Additionally the following steps are used:

1. Precomputation -> resize and extract Y luma component 
2. Ring parition. Rotate pixels around the center and fit it into a bucket
3. Sorting the lum values of each bucket increasingly and compute a 1d dct transformation. Compute hash by comparing the values to the mean of each bucket. (Multiple bits per bucket)

<p align="center"><image src="https://user-images.githubusercontent.com/9025925/47964206-6f99b400-e036-11e8-8843-471242f9943a.png"/></p>

Again only a subset of the dct matrix is used due to the same constraints as the original pHash. RotPHash shows the same behaviour as it's brother. The hamming distance usually is on the lower range.The more pixels are present in a bucket the more bits can be computed and extracted from said collection of pixels.

The sorting step eliminates the order of the pixels and therefore gets robust against rotated images. The gray pixels at the outside currently get discarded but it could be possible to assume missing information or simply fill it with a default value to include all information.

### RotAverageHash

Works identical RotPHash. The values are computed in buckets but now the average of each bucket is computed and compared to the next bucket. This allows for a quick and good computation for small bit resolutions but requires 1 bucket per bit, meaning this hash will scale badly (performance wise) for higher bit resolutions. Additionally outer buckets contain more pixel than their inner counterparts but still only contribute to 1 pixel meaning that the rot average hash suffers from pixels further away from the center being not weighted as much as central pixels.

### HogHash

The HOG (Histogram of gradients) is a feature descriptor traditionally used in machine learning to identify shapes. The concept can be found in the paper <a href="http://lear.inrialpes.fr/people/triggs/pubs/Dalal-cvpr05.pdf">Histograms of Oriented Gradients for Human Detection</a> by Navneet Dalal and Bill Triggs.

The hog works similar to dHash by calculating gradients and mapping these to buckets related to the angle of the gradients. The following image shows the features of normalized hog values calculated for the test image.
<p align="center"><image src="https://user-images.githubusercontent.com/9025925/47957324-0cffd400-dfb4-11e8-93de-76e20ab09a75.png"/></p>
The hog hash still is experimental, while it does allow to differentiate between different images, the computation cost does to justify it's poor performance. The original hog descriptor has 4k+ byte features which are much much more information than our usual 64 bit! hash. 
I'll have to see how the hog features can be encoded into a significant hash value. This is a work in progress hash.

### Example Application 
A gui application can be found the in example folder: Be aware that the results will be poor due to only one algorithms be applied at a time.
Only the first 100 google thumbnails are downloaded and usually there are not many true duplicates present. 
<p align="center"><img src="https://user-images.githubusercontent.com/9025925/43670281-2ca48ab6-978a-11e8-822b-fc2414586708.png"/></p>

### Known Bugs
- Perceptive hash relies on JTransform which fails to close a threadpool upon calculating large DCT matrices resulting in the JVM not to terminate. If you want to calculate a perceptive hash with a large bit resolution call `ConcurrencyUtils.shutdownThreadPoolAndAwaitTermination();` if you want to terminate the program.

