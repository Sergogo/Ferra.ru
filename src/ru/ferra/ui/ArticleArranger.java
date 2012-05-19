package ru.ferra.ui;

import java.io.StringReader;
import java.util.HashMap;
import java.util.LinkedList;

import org.xml.sax.InputSource;

import ru.ferra.common.utils.ArticleSaxHandler;
import ru.ferra.common.utils.loaders.HTMLSAXParser;
import ru.ferra.data.ArticleDAO;
import ru.ferra.data.RssArticle;

import android.content.Context;
import android.os.AsyncTask;

public class ArticleArranger {
	interface OnArticleReady {
		public void OnReady(RssArticle article);
	}

	private HashMap<RssArticle, OnArticleReady> cache = new HashMap<RssArticle, OnArticleReady>();
	private HashMap<RssArticle, OnArticleReady> processed = new HashMap<RssArticle, OnArticleReady>();

	private Context context;
	private ArticleDAO articleDao = new ArticleDAO();

	private ArticleParserTask task;

	private RssArticle priorityArticle = null;

	private static ArticleArranger instance = new ArticleArranger();

	private ArticleArranger() {
	}

	public static ArticleArranger getInstance() {
		return instance;
	}

	public void add(RssArticle article, OnArticleReady readyListener) {
		synchronized (instance) {
			cache.put(article, readyListener);
		}
	}

	public void remove(RssArticle article) {
		synchronized (instance) {
			cache.remove(article);
		}
		if (priorityArticle == article)
			priorityArticle = null;
	}

	public void setPriorotyArticle(RssArticle article) {
		priorityArticle = article;
	}

	public void start() {
		if (task == null || task.getStatus() == AsyncTask.Status.FINISHED) {
			task = new ArticleParserTask();

			task.execute();
		}
	}

	protected RssArticle getNextArticle() {
		RssArticle result;
		synchronized (instance) {

			if (cache.size() == 0)
				return null;

			if (priorityArticle != null) {
				processed.put(priorityArticle, cache.remove(priorityArticle));

				result = priorityArticle;
				priorityArticle = null;
			} else {
				result = cache.keySet().iterator().next();

				processed.put(result, cache.remove(result));
			}

		}
		return result;
	}

	private void articleReady(RssArticle article) {
		OnArticleReady listener = processed.get(article);

		processed.remove(article);

		//articleDao.setArticleNew(context, article);
		article.setNew(false);
		articleDao.saveArticle(context, article);

		if (listener != null) {
			listener.OnReady(article);
		}
	}

	public void setContext(Context context) {
		this.context = context;
	}

	private static class ArticleParserTask extends AsyncTask<Void, Void, Void> {

		ArticleSaxHandler handler = new ArticleSaxHandler();
		LinkedList<RssArticle> finishedTasks = new LinkedList<RssArticle>();

		@Override
		protected Void doInBackground(Void... params) {
			RssArticle article;
			while ((article = ArticleArranger.instance.getNextArticle()) != null) {

				handler.setArticle(article);

				InputSource i = new InputSource(new StringReader(article.getContent()));
				HTMLSAXParser parser = new HTMLSAXParser();
				parser.setContentHandler(handler);
				try {
					parser.parse(i);

					StringBuilder articleContent = new StringBuilder();
					articleContent.append("<h3>").append(article.getTitle()).append("</h3>").append(handler.getHtml());

					article.setContent(articleContent.toString());

					article.setDescription(handler.getShortText());
				} catch (Exception e) {
					e.printStackTrace();
				}

				synchronized (this) {
					finishedTasks.add(article);
				}
				publishProgress();
			}
			return null;
		}

		@Override
		protected void onProgressUpdate(Void... values) {
			super.onProgressUpdate(values);

			RssArticle article;
			int size = finishedTasks.size();
			synchronized (this) {
				for (int i = 0; i < size; i++) {
					article = finishedTasks.remove();

					ArticleArranger.instance.articleReady(article);
				}
			}
		}
	}

}
