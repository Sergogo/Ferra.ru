package ru.ferra.data;

import java.util.ArrayList;
import java.util.Collections;

import android.content.Context;

public class Rubric {
	private String name;
	private String fullName;
	private int id;
	private ArrayList<RssArticle> articles;
	
	private static int defaultViewCount = 5;
	private int maximumViewCount = defaultViewCount;
	private int viewCount = 0;
	
	public Rubric(String name){
		this.name = name;
	}
	
	public void loadArticles(Context context){
		ArticleDAO articleDAO = new ArticleDAO();
		
		articles = articleDAO.getArticlesShort(context, id);
	}

	public void loadArticlesFull(Context context){
		ArticleDAO articleDAO = new ArticleDAO();
		
		articles = articleDAO.getArticlesFull(context, id);
	}
	
	public void addArticle(RssArticle article) {
		articles.add(article);
		Collections.sort(articles, RssArticle.getComparator());
		
		//if(articles.size() <= defaultViewCount) viewCount = articles.size();
	}
	
	public ArrayList<RssArticle> getArticles(){
		return articles;
	}

	public boolean isArticlesPresent(String artcleGuid) {
		for(RssArticle article: articles){
			if(article.getGuid().equals(artcleGuid)) return true;
		}
		
		return false;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	public String getName(){
		return name;
	}
	
	public void setId(int id) {
		this.id = id;
	}

	public int getId(){
		return id;		
	}
	
	public int getSize(){
		return articles.size();
	}

	public String getFullName() {
		return fullName;
	}

	public void setFullName(String fullName) {
		this.fullName = fullName;
	}
	
	protected void setArticles(ArrayList<RssArticle> articles) {
		this.articles = articles;
	}
	
	public int getPosition(RssArticle article){
		return articles.indexOf(article);
	}

	public void increaseViewCount(int delta){
		setViewCount(viewCount + delta);
	}
	
	public void setViewCount(int viewCount){
		if(viewCount <= articles.size()) {
			this.viewCount = viewCount;
		} else {
			this.viewCount = articles.size();
		}
	}

	public int getViewCount(){
		return viewCount;
	}

	public static void setDefaultViewCount(int viewCount){
		defaultViewCount = viewCount;
	}
	
	public static int getDefaultViewCount() {
		return defaultViewCount;
	}
	
	public int getMaximumViewCount(){
		return maximumViewCount;
	}
	
	public void setMaximumViewCount(int maximumViewCount){
		this.maximumViewCount = maximumViewCount;
	}
}
