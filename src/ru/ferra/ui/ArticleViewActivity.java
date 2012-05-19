package ru.ferra.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import ru.ferra.R;
import ru.ferra.common.Constants;
import ru.ferra.data.ArticleDAO;
import ru.ferra.data.RssArticle;
import ru.ferra.data.Rubric;
import ru.ferra.data.RubricDAO;

import java.util.List;

public class ArticleViewActivity extends Activity {
	private List<RssArticle> articles;
	private int currentId;
	private int page = 0;

	private final static int DIALOG_SELECT_FONT = 0;

	private ViewPager viewPager;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);

		if (preferences.getBoolean(Application.FULLSCREEN_ON, false)) {
			setFullscreen();
		}

		setContentView(R.layout.article_page);

		Bundle extras = getIntent().getExtras();
		currentId = (Integer) extras.get("ID");

		int rubricId = (Integer) extras.get("RUBRIC_ID");
		RubricDAO rubricDao = new RubricDAO();
		ArticleDAO articleDao = new ArticleDAO();

		Rubric rubric = rubricDao.getRubric(getBaseContext(), rubricId);
		rubric.loadArticlesFull(getBaseContext());
		articles = rubric.getArticles();

		for (int i = 0; i < articles.size(); i++) {
			if (articles.get(i).getId() == currentId) {
				page = i;
				if (!articles.get(i).isRead()) {
					articles.get(i).setRead(true);

					articleDao.setArticleRead(getBaseContext(), articles.get(i));
				}

			}
		}

		viewPager = (ViewPager) findViewById(R.id.article_view_pager);

		ImageButton shareButton = (ImageButton) findViewById(R.id.share);
		shareButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				int position = ArticleViewActivity.this.viewPager.getCurrentItem();
				Intent intent = new Intent(Intent.ACTION_SEND);
				intent.setType("text/plain");

				intent.putExtra(Intent.EXTRA_SUBJECT, ArticleViewActivity.this.getString(R.string.share_subject));
				intent.putExtra(Intent.EXTRA_TEXT, articles.get(position).getTitle() + " " + articles.get(position).getUrl());

				ArticleViewActivity.this.startActivity(Intent.createChooser(intent, "Share"));
			}
		});
		findViewById(R.id.logo).setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Intent openFerraHome = new Intent(Intent.ACTION_VIEW);
				openFerraHome.setData(Uri.parse(Constants.FERRA_BASE_URL));

				startActivity(openFerraHome);
			}
		});

		ArticlePagerAdapter pagerAdapter = new ArticlePagerAdapter(articles, this, viewPager, rubric.getFullName());

		viewPager.setAdapter(pagerAdapter);
		viewPager.setCurrentItem(page);

		System.gc();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater menuInflater = getMenuInflater();
		menuInflater.inflate(R.menu.article_view_menu, menu);

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.font_size:
			showDialog(DIALOG_SELECT_FONT);
			return true;
		case R.id.refresh_article:
			int childCount = ArticleViewActivity.this.viewPager.getChildCount();
			for (int i = 0; i < childCount; i++) {
				View childView = ArticleViewActivity.this.viewPager.getChildAt(i);
				WebView content = (WebView) childView.findViewById(R.id.web_view);

				RssArticle article = articles.get((Integer)content.getTag(R.id.page_number));
				content.loadDataWithBaseURL("", article.getContent(), "text/html", "UTF-8", null);
			}
			ViewPager viewPager = ArticleViewActivity.this.viewPager;
			View currentChild = viewPager.getChildAt(viewPager.getCurrentItem());
			WebView currentWebView = (WebView) currentChild.findViewById(R.id.web_view);

			((ProgressBar) currentWebView.getTag(R.id.page_progress_tag)).setVisibility(View.VISIBLE);
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		if (id != DIALOG_SELECT_FONT)
			return super.onCreateDialog(id);

		AlertDialog.Builder dialog = null;

		dialog = new AlertDialog.Builder(this);

		final CharSequence[] items = { getString(R.string.font_large), getString(R.string.font_medium), getString(R.string.font_small) };

		dialog.setTitle(getString(R.string.settings_font_size));

		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);

		int defaultItem = 0;

		String fontSize = preferences.getString("font_article_size", "medium");
		if (fontSize.equalsIgnoreCase("medium")) {
			defaultItem = 1;
		}
		if (fontSize.equalsIgnoreCase("small")) {
			defaultItem = 2;
		}

		dialog.setSingleChoiceItems(items, defaultItem, new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				System.out.println("onClick = " + which);

				SharedPreferences.Editor preferencesEditor = PreferenceManager.getDefaultSharedPreferences(getBaseContext()).edit();

				WebSettings.TextSize textSize = WebSettings.TextSize.NORMAL;

				if (which == 0) {
					preferencesEditor.putString("font_article_size", "large");
					textSize = WebSettings.TextSize.LARGER;
				}
				if (which == 1) {
					preferencesEditor.putString("font_article_size", "medium");
					textSize = WebSettings.TextSize.NORMAL;
				}
				if (which == 2) {
					preferencesEditor.putString("font_article_size", "small");
					textSize = WebSettings.TextSize.SMALLER;
				}

				preferencesEditor.commit();
				int childCount = ArticleViewActivity.this.viewPager.getChildCount();
				for (int i = 0; i < childCount; i++) {
					View childView = ArticleViewActivity.this.viewPager.getChildAt(i);
					WebView content = (WebView) childView.findViewById(R.id.web_view);

					content.getSettings().setTextSize(textSize);
				}

				dialog.dismiss();
			}
		});

		return dialog.create();
	}

	@Override
	protected void onPause() {
		super.onPause();

		int childCount = ArticleViewActivity.this.viewPager.getChildCount();
		for (int i = 0; i < childCount; i++) {
			View childView = ArticleViewActivity.this.viewPager.getChildAt(i);
			WebView content = (WebView) childView.findViewById(R.id.web_view);

//			try{
//				Class.forName("android.webkit.WebView").getMethod("onPause", (Class[]) null).invoke(content, (Object[]) null);
//			}catch(Exception e){}
			content.stopLoading();
		}

	}

	private void setFullscreen() {
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
	}
}
