package ru.ferra.common.utils.loaders;

import java.util.ArrayList;

import ru.ferra.R;
import ru.ferra.common.Constants;
import ru.ferra.common.utils.loaders.Loader.RssListener;
import ru.ferra.data.ArticleDAO;
import ru.ferra.data.RssArticle;
import ru.ferra.data.Rubric;

import android.content.Context;

public class ArticleFeed extends Feed{

	String[] rubricsNames;
	
	public void reloadRss() {
        for(String rubricName: rubricsNames){
    		this.loadArticlesFromFeed(Constants.ARTICLE_HEADERS_URL + rubricName);
        }
	}

	public ArticleFeed(Context context) {
		super(context);
		
		String[] rubricsFull = context.getResources().getStringArray(R.array.rubrics);
		rubricsNames = new String[rubricsFull.length - 1];
		
		int i=0;
		for(String rubric: rubricsFull){
			if(!rubric.equals(NewsFeed.newsRubric)) {
				rubricsNames[i++] = rubric;
			}
		}
		
		setRubrics(rubricsNames);
	}

	public void addArticle(RssArticle article) {
		if (!isArticlesPresent(article)) {
			
			//load content
			Rubric rubric = getRubricMap().get(article.getRubricName());

			if(rubric == null) return;

			if(article.getExternalId() == null){
				System.out.println("!!!! " + rubric.getFullName() + " " + article.getTitle());
			}

			getLoader().load(Constants.ARTICLE_BY_ID_URL + article.getExternalId(), new Loader.RssListener() {
				@Override
				public void addArticle(RssArticle article) {
					ArticleDAO articleDao = new ArticleDAO();
					Rubric rubric = getRubricMap().get(article.getRubricName());

					article.setRubricId(rubric.getId());

					articleDao.createAndUpdateArticle(context, article, rubric.getId());
					rubric.addArticle(article);
					
					//recycle content
					article.setContent(null);

					RssListener listener = getListener();
					if(listener != null) listener.addArticle(article);
				}

				@Override
				public void finish() {
					ArticleFeed.this.finish();
				}
			});
		}
	}
	
	public ArrayList<Rubric> getNonemptyRubrics() {
		ArrayList<Rubric> rubrics = new ArrayList<Rubric>();
		
		for(Rubric rubric: getRubrics()) {
			if(rubric.getArticles().size() > 0) {
				rubrics.add(rubric);
			}
		}
		return rubrics;
	}
	
	@Override
	public void finish() {
		if (getListener() != null){
			if(getLoader().isStopped()) {
				getListener().finish();
			}
		}
	}

}
