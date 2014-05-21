package ru.ferra.data;

import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import ru.ferra.common.Constants;
import ru.ferra.providers.ArticleProvider;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowManager;
import android.util.Log;


public class ArticleDAO {
	public RssArticle createArticle(Context context, int rubricId) {
		ContentResolver contentResolver = context.getContentResolver();

		ContentValues values = new ContentValues();

		values.put(ArticleProvider.Article.RUBRIC_ID, rubricId);

		Uri result = contentResolver.insert(ArticleProvider.Article.CONTENT_URI, values);

		RssArticle article = new RssArticle();

		article.setRubricId(rubricId);
		article.setId(Integer.parseInt(result.getLastPathSegment()));

		return article;
	}

	public RssArticle createAndUpdateArticle(Context context, RssArticle article, int rubricId) {
		ContentResolver contentResolver = context.getContentResolver();
		Log.i(Constants.TAG,"LETS ADD ARTICLE ");

		ContentValues articleAttributes = new ContentValues();

		if (article.getTitle() != null) {
			articleAttributes.put(ArticleProvider.Article.TITLE, article.getTitle());
		}
		if (article.getDescription() != null) {
			articleAttributes.put(ArticleProvider.Article.DESCRIPTION, article.getDescription());
		}
		if (article.getUrl() != null) {
			articleAttributes.put(ArticleProvider.Article.URL, article.getUrl().toString());
		}
		if (article.getContent() != null) {
			articleAttributes.put(ArticleProvider.Article.CONTENT, article.getContent());
		}
		if (article.getPublishDate() != 0) {
			articleAttributes.put(ArticleProvider.Article.TIME_PUBLISHED, article.getPublishDate());
		}
		if (article.getGuid() != null) {
			articleAttributes.put(ArticleProvider.Article.GUID, article.getGuid());
		}
		if (article.getRubricId() != 0) {
			articleAttributes.put(ArticleProvider.Article.RUBRIC_ID, article.getRubricId());
		}
		articleAttributes.put(ArticleProvider.Article.IS_NEW, article.isNew());

		Uri result;

		ContentValues imageAttributes = new ContentValues();
		imageAttributes.put(ArticleProvider.Image.ARTICLE_ID, article.getId());

		try {
			URL url = new URL(article.getEnclosure());

			long id = new Date().getTime();

			StringBuilder newImgUrl = new StringBuilder();
			newImgUrl.append(url.getFile());

			url = new URL(url.getProtocol(), url.getHost(), newImgUrl.toString());

			StringBuilder path = new StringBuilder();
			path.append(context.getFilesDir());
			path.append(Constants.CACHE_DIR);
			path.append("/");
			path.append(id);

			imageAttributes.put(ArticleProvider.Image.ID, id);
			imageAttributes.put(ArticleProvider.Image.ARTICLE_ID, article.getId());
			imageAttributes.put(ArticleProvider.Image.URL, url.toString());
			imageAttributes.put(ArticleProvider.Image.LOCAL_PATH, path.toString());

			result = contentResolver.insert(ArticleProvider.Image.CONTENT_URI, imageAttributes);

			article.setEnclosure(result.toString());
		} catch (Exception e) {
			e.printStackTrace();
		}

		if (article.getEnclosure() != null) {
			articleAttributes.put(ArticleProvider.Article.ENCLOSURE, article.getEnclosure());
		}
		result = contentResolver.insert(ArticleProvider.Article.CONTENT_URI, articleAttributes);

		article.setId(Integer.parseInt(result.getLastPathSegment()));

		// save images
		HashMap<Long, String> imagesCache = article.getImagesInfo();
		HashMap<Long, String> imagesCacheFullSize = article.getImagesInfoFullSize();

		Display display = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE))
				.getDefaultDisplay();
		DisplayMetrics metrics = new DisplayMetrics();
		display.getMetrics(metrics);

		int width = (int) (metrics.widthPixels / metrics.scaledDensity);
		int height = (int) (metrics.heightPixels / metrics.scaledDensity);

		int size = Math.min(width - 20, height - 20);

		StringBuilder baseImageUrlPath = new StringBuilder();
		StringBuilder baseImagePath = new StringBuilder();

		baseImagePath.append(context.getFilesDir());
		baseImagePath.append(Constants.CACHE_DIR);

		baseImageUrlPath.append("/");
		baseImageUrlPath.append(size);
		baseImageUrlPath.append("x");
		baseImageUrlPath.append(size);

		Log.i(Constants.TAG, "CREATE_AND_UPDATE_ARTICLE " + article.getTitle());
		for (long id : imagesCache.keySet()) {
			try {
				URL url = new URL(imagesCache.get(id));

				StringBuilder imagePath = new StringBuilder(baseImagePath).append(id);

				StringBuilder imageUrlPath = new StringBuilder(baseImageUrlPath).append(url
						.getFile());

				url = new URL(url.getProtocol(), url.getHost(), imageUrlPath.toString());

				imageAttributes.put(ArticleProvider.Image.ID, id);
				imageAttributes.put(ArticleProvider.Image.ARTICLE_ID, article.getId());
				imageAttributes.put(ArticleProvider.Image.URL, url.toString());
				imageAttributes.put(ArticleProvider.Image.LOCAL_PATH, imagePath.toString());

				contentResolver.insert(ArticleProvider.Image.CONTENT_URI, imageAttributes);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		for (long id2 : imagesCacheFullSize.keySet()) {
			try {
				URL url = new URL(imagesCacheFullSize.get(id2));

				StringBuilder imagePath = new StringBuilder(baseImagePath).append(id2);

				url = new URL(url.getProtocol(), url.getHost(), url.getFile());

				imageAttributes.put(ArticleProvider.Image.ID, id2);
				imageAttributes.put(ArticleProvider.Image.ARTICLE_ID, article.getId());
				imageAttributes.put(ArticleProvider.Image.URL, url.toString());
				imageAttributes.put(ArticleProvider.Image.LOCAL_PATH, imagePath.toString());

				contentResolver.insert(ArticleProvider.Image.CONTENT_URI, imageAttributes);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		return article;
	}

	public RssArticle getArticleShort(Context context, int articleId) {
		ContentResolver contentResolver = context.getContentResolver();
		RssArticle article = new RssArticle();

		String[] columns = new String[] {
				ArticleProvider.Article.RUBRIC_ID,
				ArticleProvider.Article.TITLE,
				ArticleProvider.Article.GUID,
				ArticleProvider.Article.ENCLOSURE,
				ArticleProvider.Article.IS_NEW };

		String selectClause = ArticleProvider.Article.ID + " = ?";
		String[] selectArg = { String.valueOf(articleId) };

		Cursor cursor = contentResolver.query(ArticleProvider.Article.CONTENT_URI, columns,
				selectClause, selectArg, null);
		cursor.moveToFirst();

		if (!cursor.isAfterLast()) {
			article.setId(cursor.getInt(0));
			article.setTitle(cursor.getString(1));
			article.setGuid(cursor.getString(2));
			article.setEnclosure(cursor.getString(3));
			article.setNew(cursor.getInt(4) == 1);
			cursor.close();

			return article;
		}

		return null;

	}

	public ArrayList<RssArticle> getArticlesShort(Context context, int rubricId) {
		ContentResolver contentResolver = context.getContentResolver();
		ArrayList<RssArticle> articles = new ArrayList<RssArticle>();

		String[] columns = new String[] {
				ArticleProvider.Article.RUBRIC_ID,
				ArticleProvider.Article.TITLE,
				ArticleProvider.Article.GUID,
				ArticleProvider.Article.ENCLOSURE,
				ArticleProvider.Article.DESCRIPTION,
				ArticleProvider.Article.ID,
				ArticleProvider.Article.IS_READ,
				ArticleProvider.Article.IS_NEW };

		String selectClause = ArticleProvider.Article.RUBRIC_ID + " = ?";
		String[] selectArg = { String.valueOf(rubricId) };

		Cursor cursor = contentResolver.query(ArticleProvider.Article.CONTENT_URI, columns,
				selectClause, selectArg, null);
		cursor.moveToFirst();

		while (!cursor.isAfterLast()) {
			RssArticle article = new RssArticle();
			articles.add(article);

			article.setRubricId(cursor.getInt(0));
			article.setTitle(cursor.getString(1));
			article.setGuid(cursor.getString(2));
			article.setEnclosure(cursor.getString(3));
			article.setDescription(cursor.getString(4));
			article.setId(cursor.getInt(5));
			article.setRead(cursor.getInt(6) == 1);
			article.setNew(cursor.getInt(7) == 1);

			cursor.moveToNext();
		}
		cursor.close();

		return articles;
	}

	public ArrayList<RssArticle> getArticlesFull(Context context, int rubricId) {
		ContentResolver contentResolver = context.getContentResolver();
		ArrayList<RssArticle> articles = new ArrayList<RssArticle>();

		String[] columns = new String[] {
				ArticleProvider.Article.RUBRIC_ID,
				ArticleProvider.Article.TITLE,
				ArticleProvider.Article.GUID,
				ArticleProvider.Article.ENCLOSURE,
				ArticleProvider.Article.DESCRIPTION,
				ArticleProvider.Article.ID,
				ArticleProvider.Article.IS_READ,
				ArticleProvider.Article.IS_NEW,
				ArticleProvider.Article.CONTENT,
				ArticleProvider.Article.URL};

		String selectClause = ArticleProvider.Article.RUBRIC_ID + " = ?";
		String[] selectArg = { String.valueOf(rubricId) };

		Log.i(Constants.TAG, "GET ARTICLES FULL");


		Cursor cursor = contentResolver.query(ArticleProvider.Article.CONTENT_URI, columns,
				selectClause, selectArg, null);
		cursor.moveToFirst();

		while (!cursor.isAfterLast()) {
			RssArticle article = new RssArticle();
			articles.add(article);

			article.setRubricId(cursor.getInt(0));
			article.setTitle(cursor.getString(1));
			article.setGuid(cursor.getString(2));
			article.setEnclosure(cursor.getString(3));
			article.setDescription(cursor.getString(4));
			article.setId(cursor.getInt(5));
			article.setRead(cursor.getInt(6) == 1);
			article.setNew(cursor.getInt(7) == 1);
			article.setContent(cursor.getString(8));
			article.setUrl(cursor.getString(9));

			cursor.moveToNext();
		}
		cursor.close();

		return articles;
	}

	public void setArticleRead(Context context, RssArticle article) {
		ContentValues values = new ContentValues();

		values.put(ArticleProvider.Article.IS_READ, true);

		updateArticlePartially(context, article, values);
	}

	public void setArticleNew(Context context, RssArticle article) {
		ContentValues values = new ContentValues();

		values.put(ArticleProvider.Article.IS_NEW, false);

		updateArticlePartially(context, article, values);
	}

	public void saveArticle(Context context, RssArticle article) {
		ContentResolver contentResolver = context.getContentResolver();

		ContentValues values = new ContentValues();

		if (article.getTitle() != null) {
			values.put(ArticleProvider.Article.TITLE, article.getTitle());
		}
		if (article.getDescription() != null) {
			values.put(ArticleProvider.Article.DESCRIPTION, article.getDescription());
		}
		if (article.getUrl() != null) {
			values.put(ArticleProvider.Article.URL, article.getUrl().toString());
		}
		if (article.getContent() != null) {
			values.put(ArticleProvider.Article.CONTENT, article.getContent());
		}
		if (article.getPublishDate() != 0) {
			values.put(ArticleProvider.Article.TIME_PUBLISHED, article.getPublishDate());
		}
		if (article.getGuid() != null) {
			values.put(ArticleProvider.Article.GUID, article.getGuid());
		}
		if (article.getRubricId() != 0) {
			values.put(ArticleProvider.Article.RUBRIC_ID, article.getRubricId());
		}
		values.put(ArticleProvider.Article.IS_NEW, article.isNew());

		updateArticlePartially(context, article, values);

		// save images
		HashMap<Long, String> imagesCache = article.getImagesInfo();
		HashMap<Long, String> imagesCacheFullSize = article.getImagesInfoFullSize();

		Display display = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE))
				.getDefaultDisplay();
		DisplayMetrics metrics = new DisplayMetrics();
		display.getMetrics(metrics);

		int width = (int) (metrics.widthPixels / metrics.scaledDensity);
		int height = (int) (metrics.heightPixels / metrics.scaledDensity);

		int size = Math.min(width - 20, height - 20);

		StringBuilder baseImageUrlPath = new StringBuilder();
		StringBuilder baseImagePath = new StringBuilder();

		baseImagePath.append(context.getFilesDir());
		baseImagePath.append(Constants.CACHE_DIR);

		baseImageUrlPath.append("/");
		baseImageUrlPath.append(size);
		baseImageUrlPath.append("x");
		baseImageUrlPath.append(size);

		ContentValues imageAttributes = new ContentValues();
		Log.i(Constants.TAG,"SAVE_ARTICLE " + article.getTitle());

		for (long id : imagesCache.keySet()) {
			try {
				URL url = new URL(imagesCache.get(id));

				StringBuilder imagePath = new StringBuilder(baseImagePath).append(id);

				StringBuilder imageUrlPath = new StringBuilder(baseImageUrlPath).append(url
						.getFile());

				url = new URL(url.getProtocol(), url.getHost(), imageUrlPath.toString());

				imageAttributes.put(ArticleProvider.Image.ID, id);
				imageAttributes.put(ArticleProvider.Image.ARTICLE_ID, article.getId());
				imageAttributes.put(ArticleProvider.Image.URL, url.toString());
				imageAttributes.put(ArticleProvider.Image.LOCAL_PATH, imagePath.toString());

				contentResolver.insert(ArticleProvider.Image.CONTENT_URI, imageAttributes);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		for (long id2 : imagesCacheFullSize.keySet()) {
			try {
				URL url = new URL(imagesCacheFullSize.get(id2));

				StringBuilder imagePath = new StringBuilder(baseImagePath).append(id2);

				url = new URL(url.getProtocol(), url.getHost(), url.getFile());

				imageAttributes.put(ArticleProvider.Image.ID, id2);
				imageAttributes.put(ArticleProvider.Image.ARTICLE_ID, article.getId());
				imageAttributes.put(ArticleProvider.Image.URL, url.toString());
				imageAttributes.put(ArticleProvider.Image.LOCAL_PATH, imagePath.toString());

				contentResolver.insert(ArticleProvider.Image.CONTENT_URI, imageAttributes);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

	}

	private void updateArticlePartially(Context context, RssArticle article, ContentValues values) {
		ContentResolver contentResolver = context.getContentResolver();

		Uri uri = Uri.withAppendedPath(ArticleProvider.Article.CONTENT_URI,
				String.valueOf(article.getId()));

		contentResolver.update(uri, values, null, null);
	}

}
