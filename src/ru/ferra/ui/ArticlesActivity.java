package ru.ferra.ui;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.*;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import ru.ferra.R;
import ru.ferra.common.Constants;
import ru.ferra.common.utils.ConnectionChecker;
import ru.ferra.common.utils.loaders.ArticleFeed;
import ru.ferra.common.utils.loaders.Loader;
import ru.ferra.data.RssArticle;
import ru.ferra.data.Rubric;

import java.util.*;

public class ArticlesActivity extends RssViewActivity implements OnClickListener {

	public final static String LAST_UPDATE_DATE = "articles_update_date";
	static String feedUrl = Constants.ARTICLES_URL;

	// UI elements
	private TextView emptyMessage;
	private LinearLayout rubricsList;

	private int currentPosition = 0;
	
	private HashMap<Integer, View> rubricsViews;

	private boolean isEmptyOnStart = false;

	private ArticleFeed articleFeed;
	
	private LinkedList<View> listArticlesViews;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		articleFeed = new ArticleFeed(this.getBaseContext());
		setFeed(articleFeed);

		articleFeed.loadCache();

		ThumbnailCache.getInstance().setContext(getBaseContext());
		
		setContentView(R.layout.articles_list);
		rubricsList = (LinearLayout) findViewById(R.id.articles_list);
		emptyMessage = (TextView) findViewById(R.id.empty);

		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
		String fontSize = preferences.getString("font_list_size", "medium");

		float imageSize = 12;
		if(fontSize.equalsIgnoreCase("small")){
			imageSize = getResources().getDimension(R.dimen.image_size_small);
		} else if(fontSize.equalsIgnoreCase("medium")){
			imageSize = getResources().getDimension(R.dimen.image_size_medium);
		} else if(fontSize.equalsIgnoreCase("large")){
			imageSize = getResources().getDimension(R.dimen.image_size_large);
		}

		Display display = ((WindowManager) getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
		int width = display.getWidth();

		int maxImageCount = (int)(width/imageSize + 1);
		Rubric.setDefaultViewCount(maxImageCount);
		System.gc();
		
		listArticlesViews = new LinkedList<View>();
		generateRubricsList();
	}

	/**
	 * It is used to create/recreate the list of rubrics It should be called in
	 * onResume method
	 */
	private void generateRubricsList() {
		LayoutInflater layoutInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View rubricView;
		HorizontalScrollView rubricScroll;

		rubricsViews = new HashMap<Integer, View>();

		for (Rubric rubric : articleFeed.getRubrics()) {
			rubricView = layoutInflater.inflate(R.layout.articles_row, null);
			rubricsViews.put(rubric.getId(), rubricView);
			
			rubricScroll = (HorizontalScrollView)rubricView.findViewById(R.id.rubric_scroll);
			
			rubricScroll.setTag(rubric);
			rubricScroll.setOnTouchListener(touchListener);

			for (RssArticle article: rubric.getArticles()) {
				insertArticleView(article);
			}

			rubricsList.addView(rubricView);

			updateRubricTitle(rubric);
		}
	}

	/**
	 * Add article thumbnail to the appropriate rubric
	 * 
	 * @param article
	 */
	private void insertArticleView(RssArticle article) {
		LayoutInflater layoutInflater;
		View articleView;
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

		LinearLayout rubricLayout;

		Bitmap thumbnailBitmap;

		TextView articleTitle;
		ImageView thumbnailImageView;
		
		int rubricId = article.getRubricId();
		Rubric rubric = articleFeed.getRubric(rubricId);
		int articlePosition = rubric.getPosition(article);
		
		updateRubricTitle(rubric);

		if(articlePosition >= rubric.getViewCount() && articlePosition > rubric.getMaximumViewCount()) {
			return;
		}

		rubric.increaseViewCount(1);
		
		layoutInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		articleView = layoutInflater.inflate(R.layout.article_thumbnail, null);

		thumbnailImageView = (ImageView) articleView.findViewById(R.id.article_image);
		articleTitle = (TextView) articleView.findViewById(R.id.article_title);
		articleTitle.setText(article.getTitle());

		thumbnailBitmap = ThumbnailCache.getInstance().getBitmap(article.getEnclosure());
		if (thumbnailBitmap != null) {
			thumbnailImageView.setImageBitmap(thumbnailBitmap);
		} else {
			ThumbnailCache.getInstance().load(thumbnailImageView, article.getEnclosure());
		}
		listArticlesViews.add(articleView);
		
		if(article.isRead()) {
			thumbnailImageView.setColorFilter(0x99ffffff, PorterDuff.Mode.MULTIPLY);
			articleTitle.setTextColor(0xffb7b7b7);
		}

		String fontSize = preferences.getString("font_list_size", "medium");
		float imageSize = 12;
		float density = getResources().getDisplayMetrics().density;
		if(fontSize.equalsIgnoreCase("small")){
			articleTitle.setTextSize(getResources().getDimension(R.dimen.tile_text_small) / density);
			imageSize = getResources().getDimension(R.dimen.image_size_small);
		} else if(fontSize.equalsIgnoreCase("medium")){
			articleTitle.setTextSize(getResources().getDimension(R.dimen.tile_text_medium) / density);
			imageSize = getResources().getDimension(R.dimen.image_size_medium);
		} else if(fontSize.equalsIgnoreCase("large")){
			articleTitle.setTextSize(getResources().getDimension(R.dimen.tile_text_large) / density);
			imageSize = getResources().getDimension(R.dimen.image_size_large);
		}
		thumbnailImageView.setLayoutParams(new RelativeLayout.LayoutParams((int)imageSize, (int)imageSize));

		articleView.setOnClickListener(this);
		articleView.setTag(article);

		rubricLayout = (LinearLayout) rubricsViews.get(rubricId).findViewById(R.id.rubric_layout);

		rubricLayout.addView(articleView, articlePosition);
	}

	/**
	 * Update rubric's title: set number of articles in the rubric
	 * 
	 * @param rubric
	 */
	private void updateRubricTitle(Rubric rubric) {
		TextView rubricTitle = (TextView) rubricsViews.get(rubric.getId()).findViewById(R.id.rubric_title);
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

		StringBuilder title = new StringBuilder(rubric.getFullName());
		title.append(" (");
		title.append(rubric.getArticles().size());
		title.append(")");
		rubricTitle.setText(title.toString());

		float density = getResources().getDisplayMetrics().density;
		
		String fontSize = preferences.getString("font_list_size", "medium");
		if(fontSize.equalsIgnoreCase("small")){
			rubricTitle.setTextSize(getResources().getDimension(R.dimen.rubric_text_small) / density);
		} else if(fontSize.equalsIgnoreCase("medium")){
			rubricTitle.setTextSize(getResources().getDimension(R.dimen.rubric_text_medium) / density);
		} else if(fontSize.equalsIgnoreCase("large")){
			rubricTitle.setTextSize(getResources().getDimension(R.dimen.rubric_text_large) / density);
		}

	}

	@Override
	protected void onPostResume() {
		super.onPostResume();
		articleFeed.resume();

		if (!isUpdatedToday()) {
			reloadFeed();
		}
		
		SharedPreferences.Editor settings = PreferenceManager.getDefaultSharedPreferences(this).edit();
		Date lastUpdateDate = new Date();
		settings.putLong(LAST_UPDATE_DATE, lastUpdateDate.getTime());
		settings.commit();

		refreshImages();
	}

	@Override
	protected void onResume() {
		super.onResume();
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
		SharedPreferences.Editor editor = settings.edit();

		if(settings.getBoolean("CLEAN_UP", false)) {
			editor.putBoolean("CLEAN_UP", false);
			editor.commit();
			setContentView(R.layout.articles_list);

			rubricsList = (LinearLayout) findViewById(R.id.articles_list);
//			emptMessage = (TextView) findViewById(R.id.empty);

			articleFeed.loadCache();

			generateRubricsList();
		}

//		final ScrollView scrollView = (ScrollView)findViewById(R.id.article_scroll_view); 
//		scrollView.post(new Runnable() {
//			
//			@Override
//			public void run() {
//				scrollView.scrollTo(0, currentPosition);
//			}
//		});
		
		if (articleFeed.getNonemptyRubrics().size() == 0) {
			emptyMessage.setVisibility(View.INVISIBLE);
		} else {
			emptyMessage.setVisibility(View.INVISIBLE);
		}

		articleFeed.setRssListener(new Loader.RssListener() {

			@Override
			public void finish() {
				Application.hideProgressBar();
			}

			@Override
			public void addArticle(RssArticle article) {
				insertArticleView(article);
			}
		});
	}

	private boolean isUpdatedToday() {
		if (isEmptyOnStart)
			return false;

		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
		Date lastUpdateDate = new Date(settings.getLong(LAST_UPDATE_DATE, 0));
		Date today = new Date();

		return today.getYear() == lastUpdateDate.getYear() && today.getMonth() == lastUpdateDate.getMonth()
				&& today.getDate() == lastUpdateDate.getDate();
	}

	@Override
	public void reloadFeed() {
		if(ConnectionChecker.isNetworkAvailable(getBaseContext())){
//			ThumbnailCache.getInstance().clearCache();
//			setContentView(R.layout.articles_list);

//			rubricsList = (LinearLayout) findViewById(R.id.articles_list);
//			emptyMessage = (TextView) findViewById(R.id.empty);

//			generateRubricsList();

			refreshImages();

//			final ScrollView scrollView = (ScrollView)findViewById(R.id.article_scroll_view); 
//			scrollView.post(new Runnable() {
//				
//				@Override
//				public void run() {
//					scrollView.scrollTo(0, currentPosition);
//				}
//			});

			Application.showProgressBar();

			articleFeed.reloadRss();
		}
	}

	@Override
	protected void onPause() {
		super.onPause();

		articleFeed.pauseLoad();
	}

	@Override
	public void onClick(View v) {
		Intent viewArticle = new Intent();
		viewArticle.setClass(this, ArticleViewActivity.class);
		RssArticle article = (RssArticle) v.getTag();
		viewArticle.putExtra("RUBRIC_ID", article.getRubricId());
		viewArticle.putExtra("ID", article.getId());

		startActivity(viewArticle);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);

		articleFeed.setRssListener(null);
		currentPosition = findViewById(R.id.article_scroll_view).getScrollY();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		articleFeed.stopLoad();
		ThumbnailCache.getInstance().stopLoad();
	}

	OnTouchListener touchListener = new OnTouchListener() {
		@Override
		public boolean onTouch(View view, MotionEvent event) {
			HorizontalScrollView rubricLayout = (HorizontalScrollView) view;
			Rubric rubric = (Rubric) rubricLayout.getTag();
			Rect visibleRect = new Rect();

			rubricLayout.getLocalVisibleRect(visibleRect);

			if (rubricLayout.getChildAt(0).getWidth() - visibleRect.right < 120) {
				int articlesViewNumber = rubric.getViewCount();
				int articlesNumber = rubric.getArticles().size();

				if (articlesNumber > articlesViewNumber) {

					rubric.setMaximumViewCount(rubric.getMaximumViewCount() + 1);
					RssArticle article = rubric.getArticles().get(articlesViewNumber);

					insertArticleView(article);
				}
			}

			return false;
		}

	};

	private void refreshImages(){
		Bitmap thumbnailBitmap;
		RssArticle article;
		for(View articleView: listArticlesViews){
			ImageView thumbnailImageView = (ImageView) articleView.findViewById(R.id.article_image);
			article = (RssArticle)articleView.getTag();
			
			Boolean tag = (Boolean)thumbnailImageView.getTag(R.bool.image_empty);
			if( tag != null && tag == true){
				thumbnailBitmap = ThumbnailCache.getInstance().getBitmap(article.getEnclosure());
				if (thumbnailBitmap != null) {
					thumbnailImageView.setImageBitmap(thumbnailBitmap);
				} else {
					ThumbnailCache.getInstance().load(thumbnailImageView, article.getEnclosure());
				}
			}
		}
	}
}
