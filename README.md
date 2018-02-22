# JImageHash

JImageHash is a performant perceptual image fingerprinting library written entirely in Java. The library returns a similarity score aiming to indentify entities which are likely modifications of the original source while being robust to color and scale transformation.

>  A perceptual hash is a fingerprint of a multimedia file derived from various features from its content. Unlike cryptographic hash functions which rely on the avalanche effect of small changes in input leading to drastic changes in the output, perceptual hashes are "close" to one another if the features are similar.

This library was inspired by _Dr. Neal Krawetz_ blog post <a href="http://www.hackerfactor.com/blog/index.php?/archives/529-Kind-of-Like-That.html">kind of like that</a> and incorporates several improvements like adjustable hash resolution and diagonal gradient detection. A comprehensive overview of perceptual image hashing can be found in this <a href="https://www.phash.org/docs/pubs/thesis_zauner.pdf">paper</a> by Christoph Zauner. 

## Example 


````java
public static void main(String[] args){

  //Load images
  BufferedImage img1 = ImageIO.read(new File("image1.jpg"));
  BufferedImage img2 = ImageIO.read(new File("image2.jpg"));
  
  ImageMatcher matcher = ImageMatcher.createDefaultMatcher(true);
	
  if(matcher.checkSimilarity(img1, img2)){
    //likely duplicate found
  }
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




Hashes stay persistent between invocations of the same algorithms. Therefore if you want to compare huge batches of images an useful approach is to save the precompiled hashes to a database (e.g. <a href="http://www.h2database.com/html/main.html">h2 embedded</a>) allowing for a quick lookup.  With the hash already known checking for duplicates between 10000 images took only 4 seconds on a standard laptop.

````java
 //Retrieving hashes for persistent storage
 BigInteger hash = hasher.hash(img1);
 String hexHash = hash.toString(16);
 String binaryHash = hash.toString(2);
 
 //Re import hash from db
 BigInteger hash = new BigInteger(hashAsString,radix);
````

## Hashing algorithm

Each algorithm comes with individual properties
<table>
  <tr><th>Algorithm</th>  <th>Feature</th><th>Notes</th> </tr>
  <tr><td>AverageHash</td>  <td>Color based</td> <td>Slow. Not robust against hue/sat changes</td> </tr>
  <tr><td>DifferenceHash</td> <td>Gradient based</td> <td>Generally prefered algorithm. Fast and accurate</td> </tr>
  <tr><td>Perceptive Hash</td> <td>Frequency based</td> <td>In some cases more accurate than dHash. Best accuracy / bitResolution </td> </tr>  
</table>


````java
//Approximate final hash resolution
int bitResolution = 64;

public static void main(String[] args){

  //Load images
  BufferedImage img1 = ImageIO.read(new File("image1.jpg"));
  BufferedImage img2 = ImageIO.read(new File("image2.jpg"));
  
  //Pick an algorithm
  HashingAlgorithm hasher = new AverageHash(bitResolution);
  
  //Calculate similarity score
  double normDistance = ImageHash.normalizedHammingDistance(hasher.hash(img1),hasher.hash(img2));
  double distance = ImageHash.hammingDistance(hasher.hash(img1),hasher.hash(img2));
  
  if(normDistance < 0.1){
    //Duplicate found. Most likely an altered image 
  }
  
  //Or 
  if(distance < 10){
    //Likely duplicate found
  }
}
  
````
Only hashes produced by the same algorithm with the same bit resolution can be compared.

### DifferenceHash
The dHash algorithm is the fastest algorithm while mostly being on par on detection quality with the perceptive hash. Usually this algorithm is the choice to go with. Difference hash calculates the gradient in the image. As gradients are depended on the direction of the scanning it's a good idea to take additional dimensions into account.

- DoublePrecision will double the resulting hashSize but additionally accounts for top to bottom gradients
- TripplePrecision will triple the resulting hashSize but additionally accounts for diagonal gradients 

## Algorithm chaning & fine tuning
In some situations it might be useful to chain multiple detection algorithms back to back to utilize the different features they are based on. 
A promising approach is to first filter images using the fast difference hash with a low resolution key and if a potential match is found checking again with the perceptive hash function.

The 'ImageMatcher matcher = ImageMatcher.createDefaultMatcher(boolean);' does exactly this for you. The boolean lets you choose between different implementations.

Depending on the image domains you may want to try out different treshold values at which point an image is considered a match. The most granular control you achieve by calculating the hammingDistance on 2 hashes. A small hamming distance corresponds to closer related images. If two images are equal their hamming distance will be 0. 
Be aware that unlike the normalized hamming distance the hamming distance ranges from 0 -> bitKeyResolution.

<p align= "center">
<img src="https://user-images.githubusercontent.com/9025925/36545875-3805f32e-17ea-11e8-9b28-96e25ba0ea67.png">
	</p>
<p align= "center"><a href="https://www.phash.org/docs/pubs/thesis_zauner.pdf">Source</a>
</p>
Describes the tradeoff between false positives and false negatives. Also see his comments on Equal Error Rate which are applicable if you want to minimze the amount of falsely categorized images.


## Key bit resolution

Using the bit resolution setting you may alter the resulting hash length as well as the hash resolution. Altering the key resolution offers a trade off between computation time, storage space and feature detection quality. 

Note a huge resolution is **not** always preferable as image fingerprinting shall be more concerned about broad feature and structure of an image rather than individual details. Resolution ranges between from 32 - 128 bit have proven to be suitable values.

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



### Example of hamming distances with 64 bit key

Distance 0: Identical image

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


### TODO
 - [ ] implement rotaional invariant hash

