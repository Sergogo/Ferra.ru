package ru.ferra.ui;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.JsResult;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.TextView;
import ru.ferra.R;
import ru.ferra.common.Constants;
import ru.ferra.data.ArticleDAO;
import ru.ferra.data.RssArticle;
import ru.ferra.providers.ArticleProvider;

import java.util.HashMap;
import java.util.List;

public class ArticlePagerAdapter extends PagerAdapter implements ArticleArranger.OnArticleReady {
	
	private final static String LOAD_FINISHED_SIGNAL = "LoadFinished";
	
	private List<RssArticle> articles = null;
	private String rubricName;
	
	private Context context;
	private ViewPager viewPager;
	
	private boolean isCurrenLoaded = false;
	
	private HashMap<RssArticle, WebView> webViews = new HashMap<RssArticle, WebView>();
	
	private final static String SCRIPT_INJECT = "javascript:(" +
	"function () {" +
	"	var imgs = document.getElementsByTagName('img');" +
	"	var N = imgs.length;" +
	"	var imgWidth;" +
	"	for (var i = 0; i < N; i++){" +
	"		imgs[i].src = '" + ArticleProvider.Image.CONTENT_URI +"/' + imgs[i].src;" +
	"		imgs[i].onload = function() {" +
	"			N = N-1;" +
	"			if(N == 0){" +
	"				alert('" + LOAD_FINISHED_SIGNAL + "');" +
	"			}" +
	"		}" +
	"	}" +
	"	if(N == 0){" +
	"		alert('" + LOAD_FINISHED_SIGNAL + "');" +
	"	}"+
	"})()";

	public ArticlePagerAdapter(List<RssArticle> articels, Context context, ViewPager viewPager, String rubricName) {
		this.articles = articels;
		this.context = context;
		this.viewPager = viewPager;
		this.rubricName = rubricName;

		viewPager.setOnPageChangeListener(onPageChangeListener);
		ArticleArranger.getInstance().setContext(context);
	}

	@Override
	public int getCount() {
		return articles.size();
	}

	@Override
	public boolean isViewFromObject(View view, Object o) {
		return view.equals(o);
	}

	@Override
	public void OnReady(RssArticle article) {
		WebView content = webViews.get(article);
		if(content != null) {
			content.loadDataWithBaseURL("", article.getContent(), "text/html", "UTF-8", null);
			
			webViews.remove(article);
		}
	}

	public Object instantiateItem(View collection, int position) {

		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
		LayoutInflater inflater = LayoutInflater.from(context);
		View page = inflater.inflate(R.layout.article_page_content, null);

		RssArticle article = articles.get(position);

		Log.i(Constants.TAG, "view article " + article.getTitle());

		TextView rubric = (TextView) page.findViewById(R.id.category);
		TextView pageNumber = (TextView) page.findViewById(R.id.page);

		ProgressBar pageProgress = (ProgressBar) page.findViewById(R.id.page_progress);

		rubric.setText(rubricName);

		StringBuilder pageNumString = new StringBuilder();
		pageNumString.append(position + 1).append('/').append(articles.size());
		pageNumber.setText(pageNumString.toString());

		WebView content = (WebView) page.findViewById(R.id.web_view);

		content.setTag(R.bool.page_fully_load, Boolean.FALSE);
		content.setTag(R.id.page_number, Integer.valueOf(position));

		content.getSettings().setJavaScriptEnabled(true);
		content.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
		
		content.getSettings().setSupportZoom(false);
		content.getSettings().setBuiltInZoomControls(false);

		String fontSize = preferences.getString("font_article_size", "medium");

		if(fontSize.equals("small")){
			content.getSettings().setTextSize(WebSettings.TextSize.SMALLER);
		}
		if(fontSize.equals("medium")){
			content.getSettings().setTextSize(WebSettings.TextSize.NORMAL);
		}
		if(fontSize.equals("large")){
			content.getSettings().setTextSize(WebSettings.TextSize.LARGER);
		}

		content.setTag(R.id.page_progress_tag, pageProgress);
		content.setWebChromeClient(new WebChromeClient() {

			@Override
			public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
				if(message.equalsIgnoreCase(LOAD_FINISHED_SIGNAL)) {
					((ProgressBar) view.getTag(R.id.page_progress_tag)).setVisibility(View.INVISIBLE);
				}
				result.confirm();
				return true;
			}

		});
		content.setWebViewClient(new WebViewClient() {

			@Override
			public void onPageFinished(WebView view, String url) {
				if((Integer)view.getTag(R.id.page_number) == viewPager.getCurrentItem()) {
					view.loadUrl(SCRIPT_INJECT);
				}
			}

			@Override
			public void onPageStarted(WebView view, String url, Bitmap favicon) {
				((ProgressBar) view.getTag(R.id.page_progress_tag)).setVisibility(View.VISIBLE);
			}

			@Override
			public boolean shouldOverrideUrlLoading(WebView view, String url) {
				Intent openUrlIntent = new Intent();

				if (url.startsWith("content")) {
					openUrlIntent.setClass(ArticlePagerAdapter.this.context, ImageWebView.class);
					openUrlIntent.putExtra("url", url);
				} else {
				    openUrlIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
				}
				ArticlePagerAdapter.this.context.startActivity(openUrlIntent);
				return true;
			}

		});

		webViews.put(article, content);

		if(article.isNew()){
			ArticleArranger arranger = ArticleArranger.getInstance();
			arranger.add(article, this);
			
			if(position == viewPager.getCurrentItem()){
				arranger.setPriorotyArticle(article);
				
				arranger.start();
				isCurrenLoaded = true;
			} else {
				if(isCurrenLoaded) {
					arranger.start();
				}
			}
		} else {
	
			OnReady(article);
		}

		((ViewPager) collection).addView(page, 0);
		return page;
	}

	@Override
	public void destroyItem(View collection, int position, Object view) {
		((ViewPager) collection).removeView((View) view);
		
		ArticleArranger.getInstance().remove(articles.get(position));
		webViews.remove(articles.get(position));
	}

	private ViewPager.SimpleOnPageChangeListener onPageChangeListener = new ViewPager.SimpleOnPageChangeListener() {

		@Override
		public void onPageScrollStateChanged(int state) {
			super.onPageScrollStateChanged(state);

			ArticleDAO articleDao = new ArticleDAO();

			if (state == 0) {
				RssArticle article = articles.get(ArticlePagerAdapter.this.viewPager.getCurrentItem());
				if (!article.isRead()) {
					article.setRead(true);

					articleDao.setArticleRead(ArticlePagerAdapter.this.context, article);
				}

				WebView content = (WebView) viewPager.findViewById(R.id.web_view);
				content.loadUrl(SCRIPT_INJECT);
				
				ArticleArranger.getInstance().start();
			}
		}

	};

}
