package ru.ferra.common.utils.loaders;

import ru.ferra.common.Constants;
import ru.ferra.common.utils.loaders.Loader.RssListener;
import ru.ferra.data.ArticleDAO;
import ru.ferra.data.RssArticle;
import ru.ferra.data.Rubric;
import android.content.Context;

public class NewsFeed extends Feed{
	
	public static String newsRubric = "news";
	
	@Override
	public void reloadRss() {
		this.loadArticlesFromFeed(Constants.NEWS_RSS_URL, true);
	}

	public NewsFeed(Context context) {
		super(context, new String[]{newsRubric});
	}

	public Rubric getNewsRubric(){
		return getRubric(newsRubric);
	}
	
	public void addArticle(RssArticle article) {
		Rubric news = getRubric(newsRubric);
		article.setRubricId(news.getId());
		article.setRubricName(newsRubric);
		
		if (!isArticlesPresent(article)) {
			
			ArticleDAO articleDao = new ArticleDAO();
			Rubric rubric = getRubric(newsRubric);
			
			article.setRubricId(rubric.getId());
			
			articleDao.createAndUpdateArticle(context, article, rubric.getId());
			rubric.addArticle(article);

			RssListener listener = getListener();
			if(listener != null) listener.addArticle(article);
		}
	}
}
