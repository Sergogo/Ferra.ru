package ru.ferra.common.utils.loaders;

import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.URL;
import java.util.ArrayList;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import ru.ferra.common.Constants;
import ru.ferra.common.utils.ArticleSaxHandler;
import ru.ferra.common.utils.RssSaxHandler;
import ru.ferra.common.utils.loaders.Loader.RssListener;
import ru.ferra.data.RssArticle;
import android.os.AsyncTask;
import android.util.Log;

public class ArticleDownloaderTask extends AsyncTask<Void, Void, Void> implements RssListener {

	private Loader.TaskInfo tasksInfo;
	private ArrayList<RssArticle> processedItems = new ArrayList<RssArticle>();
	private ArrayList<RssListener> processedListeners = new ArrayList<RssListener>();

	private RssSaxHandler handler;
	private SAXParser parser;

	private Loader loader;
	
	private ArticleSaxHandler articleHandler = new ArticleSaxHandler();

	public ArticleDownloaderTask(Loader loader) {

		this.loader = loader;

		handler = new RssSaxHandler(this);
		SAXParserFactory spf = SAXParserFactory.newInstance();
		try {
			parser = spf.newSAXParser();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		}
	}

	@Override
	protected Void doInBackground(Void... voids) {
		Loader.TaskInfo info;
		while ((info = loader.getTask()) != null) {
			tasksInfo = info;
			Log.e(Constants.TAG, "Load Rss in AsyncTask");
			try {
				URL url = new URL(tasksInfo.url);
				InputStreamReader xmlReader = new InputStreamReader(url.openStream(),
						"windows-1251");

				try {
					parser.parse(new InputSource(xmlReader), handler);
				} catch (SAXException e) {
					Log.e(Constants.TAG, e.getMessage());
					e.printStackTrace();
				}

			} catch (Exception e) {
				Log.e(Constants.TAG, e.getMessage());
				e.printStackTrace();
			}
			
			publishProgress();
		}
		return null;
	}

	@Override
	protected void onProgressUpdate(Void... values) {
		super.onProgressUpdate(values);

		synchronized (this) {
			int size = processedItems.size();
			for (int i = 0; i < size; i++) {
				tasksInfo.listener.addArticle(processedItems.remove(0));
			}

			size = processedListeners.size();
			for (int i = 0; i < size; i++) {
				processedListeners.get(i).finish();
			}
		}
	}

	@Override
	protected void onPostExecute(Void result) {
		super.onPostExecute(result);
		loader.taskFinished();
//		tasksInfo.listener.finish();
	}

	private void arrangeArticle(RssArticle article) {

//		final RssArticle articleToHandler = article;
//		ArticleSaxHandler handler = new ArticleSaxHandler(new ImageListener() {
//			@Override
//			public String addImage(String url) {
//				return articleToHandler.addImageToCache(url);
//			}
//		});
		
		articleHandler.setArticle(article);

		if (article.getDescription() != null) {
			if (tasksInfo.fullLoad) {
				article.setNew(false);

				InputSource i = new InputSource(new StringReader(article.getDescription()));
				HTMLSAXParser parser = new HTMLSAXParser();
				parser.setContentHandler(articleHandler);
				
				try {
					parser.parse(i);
					StringBuilder articleContent = new StringBuilder();
					articleContent.append("<h3>").append(article.getTitle()).append("</h3>").append(articleHandler.getHtml());

					article.setContent(articleContent.toString());

					article.setDescription(articleHandler.getShortText());
				} catch (Exception e) {
					e.printStackTrace();
				}
			} else {
				article.setNew(true);
				try {
					article.setContent(article.getDescription());

					article.setDescription("");
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

	@Override
	public void addArticle(RssArticle article) {
		arrangeArticle(article);
		synchronized (this) {
			processedItems.add(article);
		}
		publishProgress();
	}

	@Override
	public void finish() {
		processedListeners.add(tasksInfo.listener);
	}
}
