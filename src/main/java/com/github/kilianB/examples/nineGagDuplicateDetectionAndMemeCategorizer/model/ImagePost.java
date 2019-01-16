package com.github.kilianB.examples.nineGagDuplicateDetectionAndMemeCategorizer.model;

import java.awt.Dimension;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * @author Kilian
 *
 */
public class ImagePost extends PostItem {

	// Photo specific

	protected List<Dimension> availableDimensions = new ArrayList<>();
	protected List<URL> imageUrl = new ArrayList<>();
	protected List<URL> imageUrlWebp = new ArrayList<>();

	public ImagePost() {
		this.type = Type.Photo;
	}

	public ImagePost(boolean video) {
		this.type = Type.Video;
	}
	
	/**
	 * @return the availableDimensions
	 */
	public List<Dimension> getAvailableDimensions() {
		return availableDimensions;
	}

	/**
	 * @return the imageUrl
	 */
	public List<URL> getImageUrl() {
		return imageUrl;
	}

	/**
	 * @return the imageUrlWebp
	 */
	public List<URL> getImageUrlWebp() {
		return imageUrlWebp;
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

			if (!availableDimensions.contains(dim)) {
				try {
					URL url = new URL(obj.getString("url"));
					URL webP = new URL(obj.getString("webpUrl"));
					availableDimensions.add(dim);
					imageUrl.add(url);
					imageUrlWebp.add(webP);
				} catch (MalformedURLException | JSONException e) {
					e.printStackTrace();
				}
			}
		}
	}

}
