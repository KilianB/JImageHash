package com.github.kilianB.examples.nineGagDuplicateDetectionAndMemeCategorizer.model;

import java.awt.Dimension;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * 
 * Either a video or a gif
 * 
 * @author Kilian
 *
 */
public class AnimatedPost extends PostItem {

	private static final Logger LOGGER = Logger.getLogger(AnimatedPost.class.getSimpleName());

	// Thumbnail
	protected List<Dimension> availableDimensionsThumbnail = new ArrayList<>();
	protected List<URL> imageUrlThumbnail = new ArrayList<>();
	protected List<URL> imageUrlWebpThumbnail = new ArrayList<>();

	// Video | Gif

	protected URL vp9;
	protected Dimension vp9Dim;

	protected URL h265;
	protected Dimension h265Dim;

	// image460svwm
	protected URL url;
	protected Dimension svwmDimension;

	protected int duration;

	// Gif

	// Video specific

	// Photo specific
	protected int width;
	protected int height;

	public AnimatedPost() {
		setType(null);
	}

	public boolean isVideo() {
		return duration > 0;
	}

	public boolean isGif() {
		return !isVideo();
	}

	@Override
	public Type getType() {
		if (type == null) {
			type = isVideo() ? Type.Video : Type.Gif;
		}
		return type;
	}

	/**
	 * @return the availableDimensionsThumbnail
	 */
	public List<Dimension> getAvailableDimensionsThumbnail() {
		return availableDimensionsThumbnail;
	}

	/**
	 * @return the imageUrlThumbnail
	 */
	public List<URL> getImageUrlThumbnail() {
		return imageUrlThumbnail;
	}

	/**
	 * @return the imageUrlWebpThumbnail
	 */
	public List<URL> getImageUrlWebpThumbnail() {
		return imageUrlWebpThumbnail;
	}

	@Override
	protected void parse(JSONObject item) {
		JSONObject imageData = item.getJSONObject("images");

		for (String key : imageData.keySet()) {
			JSONObject obj = imageData.getJSONObject(key);
			Dimension dim = new Dimension(obj.getInt("width"), obj.getInt("height"));

			/*
			 * Usually we get 2 returned formats to work with image 400 and image 700. While
			 * the 700 is bigger than the 400 if available sometimes it's identical in this
			 * case no reason to save it.
			 */
			// Video
			// hard coded
			try {
				if ("image460sv".equals(key)) {

					// TODO a tiny fraction of videos does not have this vp9 url but a normal
					// field with url instead we might want to flag it.

					if (obj.has("vp9Url")) {
						vp9 = new URL(obj.getString("vp9Url"));
						vp9Dim = dim;
					} else {
						vp9 = new URL(obj.getString("url"));
						vp9Dim = dim;
						LOGGER.warning("no vp9 found. fallback to url");
					}

					h265 = new URL(obj.getString("h265Url"));
					h265Dim = dim;

					duration = obj.getInt("duration");

				} else if ("image460svwm".equals(key)) {

					url = new URL(obj.getString("url"));
					svwmDimension = dim;

				} else {
					// Thumbnail
					if (!availableDimensionsThumbnail.contains(dim)) {
						URL url = new URL(obj.getString("url"));
						URL webP = new URL(obj.getString("webpUrl"));
						availableDimensionsThumbnail.add(dim);
						imageUrlThumbnail.add(url);
						imageUrlWebpThumbnail.add(webP);
					}
				}
			} catch (MalformedURLException | JSONException e) {
				e.printStackTrace();

			}

		}
	}

	/**
	 * @return the width
	 */
	public int getWidth() {
		return width;
	}

	/**
	 * @param width the width to set
	 */
	public void setWidth(int width) {
		this.width = width;
	}

	/**
	 * @return the height
	 */
	public int getHeight() {
		return height;
	}

	/**
	 * @param height the height to set
	 */
	public void setHeight(int height) {
		this.height = height;
	}

}
