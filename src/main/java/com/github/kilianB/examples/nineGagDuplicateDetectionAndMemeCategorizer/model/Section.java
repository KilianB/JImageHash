package com.github.kilianB.examples.nineGagDuplicateDetectionAndMemeCategorizer.model;

import java.util.HashMap;

import org.json.JSONObject;

/**
 * Each post can be included in one section (funny, animals ...) they are not
 * equivalent to hot trending fresh.
 * 
 * @author Kilian
 *
 */
public class Section {

	private static final HashMap<String, Section> CACHE = new HashMap<>();

	private final String name;
	private final String url;
	private final String imageUrl;

	/**
	 * @param name   of the section
	 * @param url    to the section page used on 9gag
	 * @param imgUrl thumbnail image
	 */
	public Section(String name, String url, String imgUrl) {
		super();
		this.name = name;
		this.url = url;
		this.imageUrl = imgUrl;
		CACHE.put(name, this);
	}

	public static Section getSection(JSONObject section) {
		JSONObject postSection = (JSONObject) section.get("postSection");
		String name = postSection.getString("name");
		if (CACHE.containsKey(name)) {
			return CACHE.get(name);
		} else {
			return new Section(name, postSection.getString("url"), postSection.getString("imageUrl"));
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((imageUrl == null) ? 0 : imageUrl.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((url == null) ? 0 : url.hashCode());
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
		Section other = (Section) obj;
		if (imageUrl == null) {
			if (other.imageUrl != null)
				return false;
		} else if (!imageUrl.equals(other.imageUrl))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (url == null) {
			if (other.url != null)
				return false;
		} else if (!url.equals(other.url))
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
	 * @return the url
	 */
	public String getUrl() {
		return url;
	}

	/**
	 * @return the imageUrl
	 */
	public String getImageUrl() {
		return imageUrl;
	}

}
