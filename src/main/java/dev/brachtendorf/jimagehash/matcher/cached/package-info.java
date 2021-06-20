/**
 * Cached image matchers allow to check images against a batch of previously
 * added images keeping the buffered image object and hashes in memory.
 * 
 * <p>
 * Due to the source data being being available hashing algorithms can be added and
 * removed on the fly as well as the buffered image object of matches can be
 * returned.. On the flip side this approach requires much more memory and is
 * unsuited for large collection of images.
 * 
 * @author Kilian
 */
package dev.brachtendorf.jimagehash.matcher.cached;