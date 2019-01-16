package com.github.kilianB.examples.nineGagDuplicateDetectionAndMemeCategorizer;

import java.awt.BasicStroke;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.ImageIO;

import com.github.kilianB.StringUtil;
import com.github.kilianB.examples.nineGagDuplicateDetectionAndMemeCategorizer.GagUtility.Section;
import com.github.kilianB.graphics.ColorUtil;
import com.github.kilianB.graphics.FastPixel;
import com.github.kilianB.graphics.ImageUtil;
import com.github.kilianB.hash.FuzzyHash;
import com.github.kilianB.hash.Hash;
import com.github.kilianB.hashAlgorithms.AverageHash;
import com.github.kilianB.hashAlgorithms.DifferenceHash;
import com.github.kilianB.hashAlgorithms.DifferenceHash.Precision;
import com.github.kilianB.hashAlgorithms.HashingAlgorithm;
import com.github.kilianB.hashAlgorithms.PerceptiveHash;
import com.github.kilianB.matcher.categorize.CategoricalMatcher;

import javafx.scene.paint.Color;

/**
 * An extensive example on how to find duplicates of images on a large set of
 * data (~15 000 images) as well as on how to categorize images into similar
 * groups.
 * 
 * <p>
 * The test data set used in this example consists of 9 gag memes which
 * confronts us with additional challenges, namely that memes by definition are
 * similar to each other increasing the complexity of duplicate detection by
 * another layer.
 * 
 * 
 * @author Kilian
 * @since 3.0.0
 */
public class GagExampleMain {

	private static Logger LOGGER = Logger.getLogger(GagExampleMain.class.getSimpleName());

	public static void main(String[] args) {

		// Setup a folder to use for our example
		File workingDirectory = new File("G:\\9Gag");
		workingDirectory.mkdir();

		// Recursion depth for nextToken retrival.
		int scrapDepth = 1000;

		boolean downloadMaxResolution = true;

		// Create a parser which handles data download and is backed by a SQL database
		try (GagUtility parser = new GagUtility(workingDirectory, scrapDepth)) {

			/*
			 * 1. Scrap the metadata from 9Gag.
			 * 
			 * This step has to only be done once. The results are cached a database. If you
			 * want to run the example repetative times you may uncomment the following
			 * code.
			 */
			parser.scrapMetadata(Section.HOT);
			parser.scrapMetadata(Section.TRENDING);
			parser.scrapMetadata(Section.NEW);

			/*
			 * 2. Download the images from 9Gag
			 * 
			 * Again this step only has to be performed once.
			 */
			parser.downloadImages(downloadMaxResolution);

			/*
			 * 3. Download blank meme templates which we can later use sort memes into
			 * categories.
			 */
			TemplateImageDownloader.download(workingDirectory);

			/*
			 * 4. Categorize the images
			 */

			// The distance images may have in order for them to be categorized into one
			// group
			double firstClusterThreshold = .3;
			// Double check the cluster fuzzy hash with a second hash
			double clusterComplianceThreshold = .4;

			// The first hasher used to create clusters
			HashingAlgorithm hasher = new PerceptiveHash(128);
			// The second hasher used to challenge the cluster
			HashingAlgorithm secondaryHasher = new DifferenceHash(64, Precision.Double);

			// Matcher used to cluster images
			CategoricalMatcher cMatcher = new CategoricalMatcher(firstClusterThreshold);
			// Add all the hashers we want. We got with 1 algorithm for now. Its well tested
			// and much quicker.
			cMatcher.addHashingAlgorithm(hasher);

			// our meme images
			File[] imageFiles = new File(workingDirectory.getAbsolutePath() + "/gagImages").listFiles();

			/*
			 * Categorize images. This will take some time due to the fact that we need to
			 * reach every single file from disk
			 */

			System.out.println("Add images to matcher. This is a 1 time action. The fuzzy hash can be saved to disk.");
			String progressFormat = "%" + StringUtil.charsNeeded(imageFiles.length) + "d / " + imageFiles.length + "%n";

			for (int i = 0; i < imageFiles.length; i++) {
				cMatcher.categorizeImageAndAdd(imageFiles[i]);
				if (i % 250 == 0) {
					System.out.printf(progressFormat, i);
				}
			}

			// This operation may take a few second
			LOGGER.info("Done clustering. Recompute categories.");

			// Recompute after adding all the images. careful this is an expensive
			// operation.
			cMatcher.recomputeCategories();

			/*
			 * 5. Match the fuzzy clusters to template categories. Lets do it manually to
			 * not needing to save the fuzzy hashes to a file.
			 */
			File[] memeTemplates = new File(workingDirectory.getAbsolutePath() + "/memeTemplate").listFiles();

			// Hash all blank template images we have downloaded earlier
			Hash[] memeHashes = hasher.hash(memeTemplates);

			// Remember which cluster corresponds to a meme
			Set<Integer> memeCategories = new HashSet<>();

			// For now lets collect all images which are really likely duplicates before we
			// expand on this
			Set<Integer> veryLiklyDuplicates = new HashSet<>();
			for (int category : cMatcher.getCategories()) {
				
				//the centeroid of the cluster
				FuzzyHash fuzzy = cMatcher.getClusterAverageHash(hasher, category);
				
				
				/*
				 *  TODO next blog post. Clean up the category hash.
				 *  If we don't use the perceptive hash  we might collect hashes
				 *  which are not really part of the cluster but snuck in there due to 
				 *  some features being identical.
				 *  Now we should use a second hasher to filter those.
				 */
				

				//Find the closest meme category to a center
				int bestCategory = 0;
				double bestDistance = Double.MAX_VALUE;
				for (int memeCat = 0; memeCat < memeTemplates.length; memeCat++) {
					double distance = fuzzy.weightedDistance(memeHashes[memeCat]);
					if (distance < bestDistance) {
						bestDistance = distance;
						bestCategory = memeCat;
					}

				}

				//All images that got matched into this category
				List<String> imagesInCategory = cMatcher.getImagesInCategory(category);

				// We found a category which matches to a template image
				if (bestDistance < .3) {
					
					/*
					 * Lets challenge the fuzzy hash by checking if the cluster really is close to
					 *  the images by asking a second hashing algorithm which looks at differnt image
					 *  features.
					 */
					Hash categoryHash = secondaryHasher.hash(memeTemplates[bestCategory]);

					// The centeroid of the cluster using the second hashing algorithm
					// Not used right now but we could reuse it after application restart to
					// not requuring to repeat all the stats for a second hashing algorithm.
					FuzzyHash secondaryFuzzyHash = new FuzzyHash();

					double avgDistance = 0;
					for (String imgInCategory : imagesInCategory) {
						Hash secondaryHash = secondaryHasher.hash(new File(imgInCategory));
						avgDistance += (categoryHash.normalizedHammingDistanceFast(secondaryHash))
								/ imagesInCategory.size();
						secondaryFuzzyHash.mergeFast(secondaryHash);
					}

					// Here we could also check the inner cluster distance using the secondary
					// fuzzy hash as a benchmark to exclude images which are not liekly to be part

					/*
					 * The images inside the cluster are far apart the centeroid according to the second 
					 * hashing algorithm.
					 */
					if (avgDistance >= clusterComplianceThreshold) {
						System.out.printf("Category: challanged: Secondary hasher did not agree with cluster %.4f %s%n",
								avgDistance, memeTemplates[bestCategory]);
						continue;
					}
					
					// Lets move all matching memes to a new folder so we can inspect them further
					File pooledMemes = new File(workingDirectory,
							"custeredMeme/" + getEscapedCategoryName(memeTemplates[bestCategory]));
					pooledMemes.mkdirs();
					String dirPath = pooledMemes.getAbsolutePath();

					// Copy images into folder
					for (String pathToImage : imagesInCategory) {
						Path source = Paths.get(pathToImage);
						Path target = Paths.get(dirPath, new File(pathToImage).getName());
						if (target.toFile().exists()) {
							continue;
						}
						Files.copy(source, target);
					}

					// Save the fuzzy hash for future reference
					cMatcher.getClusterAverageHash(hasher, category).toFile(Paths
							.get(dirPath, memeTemplates[bestCategory].getName() + category + "Fuzzy.ser").toFile());

					//And finally remember that the images are memes
					memeCategories.add(category);

					/*
					 * Now it's up to use to do a very strict duplicate search.
					 * This will be part of the second tutorial. Alterntivly we could load
					 * a config file indicating how this specific category should be handled if we require
					 * more specific means of intervention.
					 */

				} else {
					
					/*
					 * Do a relaxed duplicate detection since we are not working with meme images.
					 * 
					 * Catch:! the biggest clusters might actually be memes we have no template image
					 * for. (very likely.) We should manually inspect them and filter them before
					 * detecting duplicates or we will be up for a bad time.
					 * 
					 * TODO Part 2:
					 */

					//Just to get some useable results right now take shortcut
					if (imagesInCategory.size() > 1 && cMatcher.getAverageDistanceWithinCluster(category) < 0.05) {
						// This is not how you would do it! check the images within a cluster with a
						// different algorithm.
						veryLiklyDuplicates.add(category);
					}
					// Example
					/*
					 * CumulativeMatcher inClusterMatcher = new CumulativeMatcher(true,.2);
					 * inClusterMatcher.addImage(....) for(Image img : images){ //Get images within
					 * distance ... they are duplicates }
					 *
					 */
				}
			}

			// Print all categories which are not memes
			//Show if you are interested
			//printImagesInCategories(cMatcher, memeCategories);

			// Print all images which are very very likely to be duplicates
			System.out.println("Incredibly likly duplicates");
			Set<Integer> allCategories = new HashSet<>(cMatcher.getCategories());
			allCategories.removeAll(veryLiklyDuplicates);
			// veryLikuplicates = new HashSet<>();
			printImagesInCategories(cMatcher, allCategories);

			/*
			 * Some statistics 
			 */
			LinkedHashMap<Integer, Integer> categories = cMatcher.getCategoriesSortedByImageCount();
			int nonMemeClusters = 0;
			int duplicates = 0;
			int singleton = 0;

			for (Entry<Integer, Integer> entry : categories.entrySet()) {
				int category = entry.getKey();
				int imagesInCategory = entry.getValue();

				if (!memeCategories.contains(category)) {
					nonMemeClusters++;

					if (imagesInCategory > 1) {
						duplicates++;
					} else {
						singleton++;
					}
				}
			}

			System.out.println("Clusters: " + categories.size() + " NonMemes: " + nonMemeClusters
					+ " Mote than 2 Elems: " + duplicates + " Pure cluster: " + singleton);


			//Build animation used in the blog post
//			// 27 confession bear
//			buildAnim(cMatcher, 27, "ConfessionBear");
//			buildAnim(cMatcher2, 27, "ConfessionBear2");
////			// 529 penguin
//			buildAnim(cMatcher, 529, "Penguin");
//			buildAnim(cMatcher2, 529, "Penguin2");
////			// 376 farmer
//			buildAnim(cMatcher, 376, "Farmer");
//			buildAnim(cMatcher2, 376, "Farmer2");
//			// 262 philo
//			buildAnim(cMatcher, 262, "Philo");
		} catch (SQLException sqlE) {
			LOGGER.log(Level.SEVERE, "Database exception", sqlE);
		} catch (Exception e) {
			LOGGER.severe("This block should never get executed. All known exceptions are handled.");
			e.printStackTrace();
		}
	}

	private static String getEscapedCategoryName(File pathToMemeTemplate) {
		String name = pathToMemeTemplate.getName();
		return name.substring(0, name.lastIndexOf("."));
	}

	/**
	 * Print all images contained in the cluster
	 * @param cMatcher The matcher to retrieve the data from
	 * @param categoriesToIgnore the categories we omit
	 */
	private static void printImagesInCategories(CategoricalMatcher cMatcher, Collection<Integer> categoriesToIgnore) {
		LinkedHashMap<Integer, Integer> categories = cMatcher.getCategoriesSortedByImageCount();
		//
		for (Entry<Integer, Integer> entry : categories.entrySet()) {

			int category = entry.getKey();
			if (categoriesToIgnore.contains(category)) {
				continue;
			}

			int numImages = entry.getValue();
			System.out.printf("Category: %4d IClustDistance: %.3f Images: %d %s %n", category,
					cMatcher.getAverageDistanceWithinCluster(category), numImages,
					cMatcher.getImagesInCategory(category));
		}
	}

	@SuppressWarnings("unused")
	private static void buildAnim(CategoricalMatcher cMatcher, int category, String name) throws IOException {

		//Build animation for blog post
		
		HashingAlgorithm hasher = new AverageHash(512);

		List<String> images = cMatcher.getImagesInCategory(category);

		String sL = "" + name + "fuzzy/";
		File saveLocation = new File(sL);
		saveLocation.mkdir();

		FuzzyHash fHash = new FuzzyHash();
		int i = 0;
		int numNeeded = StringUtil.charsNeeded(images.size()) + 1;
		for (String imgUrl : images) {

			BufferedImage originalImage = ImageIO.read(new File(imgUrl));

			fHash.merge(hasher.hash(originalImage));
			String imageNumber = StringUtil.fillStringBeginning("0", numNeeded, Integer.toString(i));
			BufferedImage hashAsImage = fHash.toImage(10);

			// Hash certainty
			double certainty = (fHash.getMaximalError());
			System.out.println(name + " " + certainty);

			i++;

			int legendOffsetX = 20;
			int inputImageOffset = 50;

			// Lets bundle the images together;
			BufferedImage legend = createLegendImage();

			// Load the original image

			int centerYHashOffset = 0;
			int centerYLegendOffset = 0;
			int desiredHeight = Math.max(legend.getHeight(), hashAsImage.getHeight());

			int footerHeight = 15;

			int totalWidth = legend.getWidth() + hashAsImage.getWidth() + legendOffsetX + desiredHeight
					+ inputImageOffset;

			BufferedImage rescaledOriginal;
			if (originalImage.getHeight() < desiredHeight || originalImage.getWidth() < desiredHeight) {
				rescaledOriginal = ImageUtil.getScaledInstance(originalImage, desiredHeight, desiredHeight);
			} else {
				rescaledOriginal = ImageUtil.createThumbnail(originalImage, desiredHeight, desiredHeight);
			}

			if (legend.getHeight() > hashAsImage.getHeight()) {
				centerYHashOffset = (legend.getHeight() - hashAsImage.getHeight()) / 2;
			} else {
				centerYLegendOffset = (hashAsImage.getHeight() - legend.getHeight()) / 2;
			}

			BufferedImage out = new BufferedImage(totalWidth, desiredHeight + footerHeight, 0x01);
			Graphics2D gc = (Graphics2D) out.getGraphics();

			gc.setBackground(java.awt.Color.white);
			gc.clearRect(0, 0, out.getWidth(), out.getHeight());

			gc.drawImage(rescaledOriginal, 0, 0, desiredHeight, desiredHeight, null);
			gc.drawImage(hashAsImage, desiredHeight + inputImageOffset, centerYHashOffset, hashAsImage.getWidth(),
					hashAsImage.getHeight(), null);

			gc.drawImage(legend, desiredHeight + inputImageOffset + hashAsImage.getWidth() + legendOffsetX,
					centerYLegendOffset, legend.getWidth(), legend.getHeight(), null);

			DecimalFormat df = new DecimalFormat("###.00%");

			gc.setColor(java.awt.Color.BLACK);
			gc.drawString(i + "/" + images.size() + "    Total Certainty: " + df.format(certainty),
					out.getWidth() * 6 / 12, out.getHeight() - 10);

			gc.setStroke(new BasicStroke(3));
			drawArrowLine(gc, desiredHeight + 5, desiredHeight / 2, desiredHeight + 5 + legendOffsetX + 20,
					desiredHeight / 2, 8, 8);

			gc.dispose();
			ImageIO.write(out, "png", new File(sL + imageNumber + ".png"));

		}
	}

	private static BufferedImage createLegendImage() {
		// Create legend
		int legendHeight = 230;
		int legendWidth = 30;
		int rightXOffset = 30;

		int legendXOffset = 40;

		int legendYOffset = 30;

		Color[] lowerCol = ColorUtil.ColorPalette.getPalette(230 / 2, Color.web("#ff642b"), Color.web("#ffff7c"));
		Color[] higherCol = ColorUtil.ColorPalette.getPalette(230 / 2, Color.web("#ffff7c"), Color.GREEN);

		Color[] colors = new Color[lowerCol.length + higherCol.length];
		System.arraycopy(lowerCol, 0, colors, 0, lowerCol.length);
		System.arraycopy(higherCol, 0, colors, lowerCol.length, higherCol.length);

		BufferedImage bi = new BufferedImage(legendWidth + legendXOffset + rightXOffset, legendHeight + legendYOffset,
				0x1);

		Graphics2D gc = (Graphics2D) bi.getGraphics();
		gc.setBackground(java.awt.Color.white);
		gc.clearRect(0, 0, bi.getWidth(), bi.getHeight());

		FastPixel fp = FastPixel.create(bi);

		for (int y = legendYOffset; y < legendHeight + legendYOffset; y++) {
			Color curColor = colors[y - legendYOffset];
			for (int x = legendXOffset; x < legendWidth + legendXOffset; x++) {
				fp.setRed(x, y, (int) (curColor.getRed() * 255));
				fp.setGreen(x, y, (int) (curColor.getGreen() * 255));
				fp.setBlue(x, y, (int) (curColor.getBlue() * 255));
			}
		}

		// Draw a black border
		gc.setColor(java.awt.Color.black);
		gc.setStroke(new BasicStroke(1));
		gc.drawRect(legendXOffset, legendYOffset, legendWidth - 1, legendHeight - 1);

		// Print some text on the left side
		gc.setColor(java.awt.Color.black);

		// We want 10 labels
		int labels = 11;
		int lHalf = labels / 2;
		int yIncrement = (int) Math.round((bi.getHeight() - legendYOffset - 6) / (labels - 1));

		for (int j = 0, y = legendYOffset + 6; y <= bi.getHeight(); y = y + yIncrement, j++) {

			int probability = 0;
			if (j < lHalf + 1) {
				probability = 100 / lHalf * (lHalf - j);
			} else {
				probability = -(100 / lHalf * (lHalf - j));
			}

			gc.drawString(probability + "%", 0, y);
		}

		gc.drawString("Certainty", 0, 12);
		gc.drawString("0 Bit", legendXOffset + legendWidth + 5, legendYOffset + 6);
		gc.drawString("1 Bit", legendXOffset + legendWidth + 5, bi.getHeight() - 5);

		gc.dispose();

		return bi;
	}

	/**
	 * Draw an arrow line between two points.
	 * 
	 * Credit: https://stackoverflow.com/a/27461352/3244464
	 * 
	 * @param g  the graphics component.
	 * @param x1 x-position of first point.
	 * @param y1 y-position of first point.
	 * @param x2 x-position of second point.
	 * @param y2 y-position of second point.
	 * @param d  the width of the arrow.
	 * @param h  the height of the arrow.
	 */
	private static void drawArrowLine(Graphics g, int x1, int y1, int x2, int y2, int d, int h) {
		int dx = x2 - x1;
		int dy = y2 - y1;
		double D = Math.sqrt(dx * dx + dy * dy);
		double xm = D - d;
		double xn = xm;
		double ym = h;
		double yn = -h;
		double x;
		double sin = dy / D;
		double cos = dx / D;

		x = xm * cos - ym * sin + x1;
		ym = xm * sin + ym * cos + y1;
		xm = x;

		x = xn * cos - yn * sin + x1;
		yn = xn * sin + yn * cos + y1;
		xn = x;

		int[] xpoints = { x2, (int) xm, (int) xn };
		int[] ypoints = { y2, (int) ym, (int) yn };

		g.drawLine(x1, y1, x2, y2);
		g.fillPolygon(xpoints, ypoints, 3);
	}

}
