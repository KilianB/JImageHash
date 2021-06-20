/**
 * Persistent image matchers allow to check images against a batch of previously
 * added images keeping only the created hashes coupled with a unique identifier
 * of the image.
 * <p>
 * Due to keeping no hard reference to the buffered image object matchers in
 * this package are suited to work with large collection of images. Contrary to
 * cached matchers matchers in this package can not be reconfigured once the
 * first hash has been created due to the fact that old hashes can not be
 * updated anymore.
 * <p>
 * Additionally the matcher objects and hashes can be saved to disk for later
 * reuse.
 * 
 * @author Kilian
 */
package dev.brachtendorf.jimagehash.matcher.persistent;