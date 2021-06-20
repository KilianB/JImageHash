package dev.brachtendorf.jimagehash.matcher.persistent;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.PriorityQueue;
import java.util.logging.Logger;

import javax.imageio.ImageIO;

import dev.brachtendorf.jimagehash.datastructures.tree.Result;
import dev.brachtendorf.jimagehash.hash.Hash;
import dev.brachtendorf.jimagehash.hashAlgorithms.HashingAlgorithm;
import dev.brachtendorf.jimagehash.matcher.TypedImageMatcher;

/**
 * Persistent image matchers are a subset of
 * {@link dev.brachtendorf.jimagehash.matcher.TypedImageMatcher TypedImageMatcher} which
 * can be saved to disk to be later reconstructed. They expose the method
 * {@link #serializeState(File)} and {@link #reconstructState(File, boolean)}.
 * 
 * <p>
 * Since serialized hashes can not be updated or recreated due to the source of
 * the hash not being available anymore the persistent matcher needs to ensure
 * that the binary tree stays in a valid state. This means that the matcher
 * forbids adding new hashing algorithms as soon as a single hash was created.
 * <p>
 * <b> Implnote:</b> classes extending persistent image matcher need to ensure
 * that their internal state can be serialized.
 * 
 * @author Kilian
 * @since 3.0.0
 */
public abstract class PersistentImageMatcher extends TypedImageMatcher implements Serializable {

	private static final long serialVersionUID = 4656669336898685462L;

	private static final Logger LOGGER = Logger.getLogger(ConsecutiveMatcher.class.getSimpleName());

	protected boolean lockedState = false;

	protected HashMap<String, Hash> addedImageMap;

	/**
	 * Non args constructor for serialization
	 */
	protected PersistentImageMatcher() {
	}

	@Override
	public void addHashingAlgorithm(HashingAlgorithm algo, double threshold) {
		checkLockedState();
		super.addHashingAlgorithm(algo, threshold);
	}

	@Override
	public void addHashingAlgorithm(HashingAlgorithm algo, double threshold, boolean normalized) {
		checkLockedState();
		super.addHashingAlgorithm(algo, threshold, normalized);
	}

	@Override
	public void clearHashingAlgorithms() {
		checkLockedState();
		super.clearHashingAlgorithms();
	}

	/**
	 * Index the image. This enables the image matcher to find the image in future
	 * searches. The database image matcher does not store the image data itself but
	 * indexes the hash bound to the absolute path of the image.
	 * 
	 * <p>
	 * The path of the file has to be unique in order for this operation to return
	 * deterministic results.
	 * 
	 * @param imageFile The image whose hash will be added to the matcher
	 * @throws IOException if an error exists reading the file
	 */
	public void addImage(File imageFile) throws IOException {
		addImage(imageFile.getAbsolutePath(), imageFile);
	}

	/**
	 * Add the images to the matcher allowing the image to be found in future
	 * searches.
	 * 
	 * @param imagesToAdd The images whose hash will be added to the matcher
	 * @throws IOException if an error exists reading the image files
	 */
	public void addImages(File... imagesToAdd) throws IOException {
		for (File img : imagesToAdd) {
			this.addImage(img);
		}
	}

	/**
	 * Index the image. This enables the image matcher to find the image in future
	 * searches. The database image matcher does not store the image data itself but
	 * indexes the hash bound to the absolute path of the image.
	 * 
	 * <p>
	 * The uniqueId has to be globally unique in order for this operation to return
	 * deterministic results.
	 * 
	 * @param uniqueId  a unique identifier returned if querying for the image
	 * @param imageFile The image whose hash will be added to the matcher
	 * @throws IOException if an error exists reading the file
	 * @since 2.0.2
	 */
	public void addImage(String uniqueId, File imageFile) throws IOException {
		if (steps.isEmpty())
			throw new IllegalStateException(
					"Please supply at least one hashing algorithm prior to invoking the match method");

		if (!imageFile.isFile()) {
			throw new IllegalArgumentException(
					"Please make sure you add an image to the matcher. Directories are not supported");
		}

		addImage(uniqueId, ImageIO.read(imageFile));
	}

	/**
	 * 
	 * 
	 * <p>
	 * <b>Implnote:</b> if this method is overwritten the class has to make sure
	 * that the field {@link #lockedState} is correctly updated when ever an image
	 * was added to the matcher.
	 * 
	 * @param uniqueId a unique identifier describing the image
	 * @param image    The image whose hash will be added to the matcher
	 */
	public void addImage(String uniqueId, BufferedImage image) {
		addImageInternal(uniqueId, image);
		lockedState = true;
	}

	/**
	 * Add this image to the image matcher.
	 * 
	 * @param uniqueId the unique id to refer to during lookup
	 * @param image    the image to add
	 */
	protected abstract void addImageInternal(String uniqueId, BufferedImage image);

	/**
	 * Return a list of images that are considered matching by the definition of
	 * this matcher.
	 * 
	 * @param image the image to check all saved images against
	 * @return a list of unique id's identifying the previously matched images
	 *         sorted by distance.
	 * @throws IOException if an error occurs reading the file
	 */
	public PriorityQueue<Result<String>> getMatchingImages(File image) throws IOException {
		return getMatchingImages(ImageIO.read(image));
	}

	/**
	 * Search for all similar images passing the algorithm filters supplied to this
	 * matcher. If the image itself was added to the tree it will be returned with a
	 * distance of 0
	 * 
	 * @param image the image to check all saved images against
	 * @return a list of unique id's identifying the previously matched images
	 *         sorted by distance of the last applied algorithm
	 */
	public abstract PriorityQueue<Result<String>> getMatchingImages(BufferedImage image);

	/**
	 * Serialize this image matcher to a file. Serialized matchers keep their
	 * internal state and can be reconstructed at a later stage without needing to
	 * rehash all images.
	 * 
	 * <p>
	 * To reconstruct the matcher take a look at
	 * {@link #reconstructState(File, boolean)};
	 * 
	 * @param saveLocation the location to save the matcher object to
	 * @throws IOException if an io error occurs during serialzation.
	 */
	public void serializeState(File saveLocation) throws IOException {

		if (saveLocation.exists()) {
			LOGGER.warning("Output file already exists. Overwritting serizable file file");
		}

		try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(saveLocation))) {
			oos.writeObject(this);
		}
	}

	/**
	 * Construct a persistent image matcher from a serialization file and rebuild
	 * it's internal state.
	 * 
	 * @param saveLocation  the serialization file as specified in
	 *                      {@link #serializeState(File)}
	 * @param deleteSerFile If true deletes the serialization file after reading the
	 *                      object
	 * @return the matcher saved to the file
	 * @throws ClassNotFoundException Class of a serialized object cannot be found
	 * @throws IOException            if an IOerror occurs during reading the file
	 */
	public static PersistentImageMatcher reconstructState(File saveLocation, boolean deleteSerFile)
			throws ClassNotFoundException, IOException {
		PersistentImageMatcher pImageMatcher;
		try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(saveLocation))) {
			pImageMatcher = (PersistentImageMatcher) ois.readObject();
		}
		if (deleteSerFile) {
			saveLocation.delete();
		}
		return pImageMatcher;
	}

	protected void checkLockedState() {
		if (lockedState) {
			throw new IllegalStateException(
					"Images have already been added to the matcher. Changing hashing algorithms would invalidate the internal state.");
		}
	}

	// Serialization
	private void writeObject(ObjectOutputStream oos) throws IOException {
		oos.defaultWriteObject();
		oos.writeObject(this.steps);
	}

	@SuppressWarnings("unchecked")
	private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException {
		ois.defaultReadObject();
		this.steps = (LinkedHashMap<HashingAlgorithm, AlgoSettings>) ois.readObject();
	}

}
