package com.github.kilianB.examples.nineGagDuplicateDetectionAndMemeCategorizer.model;

import org.json.JSONObject;

/**
 * @author Kilian
 *
 */
public class ArticlePost extends PostItem{

	protected JSONObject articleData;
	
	public ArticlePost() {
		setType(Type.Article);
	}
	
	@Override
	protected void parse(JSONObject item) {
		JSONObject articleData = (JSONObject) item.get("article");
		this.articleData = articleData;
		//TODO extract more info
		System.out.println("ArticleData: " + articleData);
	}

}
