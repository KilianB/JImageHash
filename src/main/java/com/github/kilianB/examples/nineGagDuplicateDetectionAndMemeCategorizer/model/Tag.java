package com.github.kilianB.examples.nineGagDuplicateDetectionAndMemeCategorizer.model;

/**
 * Tags associated with a post
 * @author Kilian
 *
 */
public class Tag{
	private String key;
	private String url;	//May link to something?
	
	public Tag(String key, String url){
		this.setKey(key);
		this.setUrl(url);
	}

	/**
	 * @return the key
	 */
	public String getKey() {
		return key;
	}

	/**
	 * @param key the key to set
	 */
	public void setKey(String key) {
		this.key = key;
	}

	/**
	 * @return the url
	 */
	public String getUrl() {
		return url;
	}

	/**
	 * @param url the url to set
	 */
	public void setUrl(String url) {
		this.url = url;
	}
}