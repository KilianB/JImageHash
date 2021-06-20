package dev.brachtendorf.jimagehash.matcher.categorize.supervised;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

/**
 * A labeled image used to benchmark hashing algorithms.
 * <p>
 * Labeled images bind an image to a category. All images in the same category
 * are expected to produce a match if an image matcher is queried.
 * 
 * @author Kilian
 * @since 2.0.0
 * @since 2.1.1 moved to new file
 * @since 2.2.0 renamed to LabeledImage previously TestData
 */
public class LabeledImage implements Comparable<LabeledImage> {

	/** A character representation of the file for easy feedback */
	protected String name;

	/** The category of the image. Same categories equals similar images */
	protected int category;

	/** The image to test */
	protected BufferedImage bImage;

	/**
	 * 
	 * @param category The image category. Images with the same category are
	 *                 expected to be classified as similar images
	 * @param f        The Fie pointing to the image
	 */
	public LabeledImage(int category, File f) {
		try {
			this.bImage = ImageIO.read(f);
		} catch (IOException e) {
			e.printStackTrace();
		}
		this.name = f.getName().substring(0, f.getName().lastIndexOf("."));
		this.category = category;
	}

	@Override
	public int compareTo(LabeledImage o) {
		return Integer.compare(category, o.category);
	}

	@Override
	public String toString() {
		return "LabeledImage [name=" + name + ", category=" + category + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((bImage == null) ? 0 : bImage.hashCode());
		result = prime * result + category;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		LabeledImage other = (LabeledImage) obj;
		if (bImage == null) {
			if (other.bImage != null)
				return false;
		} else if (!bImage.equals(other.bImage))
			return false;
		if (category != other.category)
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return the category
	 */
	public int getCategory() {
		return category;
	}

	/**
	 * @return the bImage
	 */
	public BufferedImage getbImage() {
		return bImage;
	}

}