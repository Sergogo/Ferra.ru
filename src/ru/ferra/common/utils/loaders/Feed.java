package ru.ferra.common.utils.loaders;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.content.Context;

import ru.ferra.common.utils.ConnectionChecker;
import ru.ferra.data.RssArticle;
import ru.ferra.data.Rubric;
import ru.ferra.data.RubricDAO;

public abstract class Feed implements Loader.RssListener {
	Context context;
	private HashMap<String, Rubric> rubrics;
	private String[] rubricsNames;

	private Loader.RssListener rssListener;
	private Loader contentLoader;

	public Feed(Context context, String[] rubricsNames) {
		rubrics = new HashMap<String, Rubric>();

		this.rubricsNames = rubricsNames;
		this.context = context;
		
		contentLoader = Loader.getNewInstance();
	}

	public Feed(Context context) {
		rubrics = new HashMap<String, Rubric>();

		this.context = context;

		contentLoader = Loader.getNewInstance();
	}

	/**
	 * Loads/reloads RSS feed. It skips article if it was cached
	 * 
	 */
	public abstract void reloadRss();

	public void loadCache() {
		RubricDAO rubricDao = new RubricDAO();

		for (String name : rubricsNames) {
			Rubric rubric = rubricDao.getRubric(context, name);

			if (rubric != null) {
				rubrics.put(rubric.getName(), rubric);
				rubric.loadArticles(context);
			}
		}
	}

	public List<Rubric> getRubrics() {
		return new ArrayList<Rubric>(rubrics.values());
	}

	public Rubric getRubric(String name) {
		return rubrics.get(name);
	}

	public Rubric getRubric(int id) {
		for(Rubric rubric: rubrics.values()){
			if(rubric.getId() == id) return rubric;
		}
		return null;
	}

	protected HashMap<String, Rubric> getRubricMap(){
		return rubrics;
	}

	public boolean isArticlesPresent(RssArticle article) {

		Rubric rubric = rubrics.get(article.getRubricName());
		if (rubric != null && rubric.isArticlesPresent(article.getGuid())) {
			return true;
		} else {
			return false;
		}
	}
	
	public abstract void addArticle(RssArticle article);

	@Override
	public void finish() {
		if (rssListener != null) {
			rssListener.finish();
		}
	}

	protected Loader getLoader(){
		return contentLoader;
	}
	
	protected void loadArticlesFromFeed(String url) {
		if (!ConnectionChecker.isNetworkAvailable(context)) {
			return;
		}

		contentLoader.load(url, this);
	}

	protected void loadArticlesFromFeed(String url, boolean fullLoad) {
		contentLoader.load(url, this, fullLoad);
	}

	public void setRssListener(Loader.RssListener listener) {
		rssListener = listener;
	}

	public Loader.RssListener getListener(){
		return rssListener;
	}
	
	public void setRubrics(String[] rubricsNames) {
		this.rubricsNames = rubricsNames;
	}

	public void pauseLoad(){
		contentLoader.pause();
	}

	public void resume(){
		contentLoader.resume();
	}
	
	public void stopLoad() {
		contentLoader.stop();
	}
}