package ru.ferra.ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import ru.ferra.R;
import ru.ferra.common.utils.ConnectionChecker;
import ru.ferra.common.utils.loaders.Loader;
import ru.ferra.common.utils.loaders.NewsFeed;
import ru.ferra.data.RssArticle;
import ru.ferra.data.Rubric;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class NewsActivity extends RssViewActivity implements AdapterView.OnItemClickListener {

	public final static String LAST_UPDATE_DATE = "news_update_date";

	// UI elements
	private TextView emptyMessage;
	private ListView listView;

	private NewsArrayAdapter adapter;

	private NewsFeed newsFeed;
	private Rubric newsRubric;

	private int currentPosition = 0;

	private boolean isEmptyOnStart = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.news_list);
		emptyMessage = (TextView) findViewById(R.id.empty);

		newsFeed = new NewsFeed(this.getBaseContext());
		setFeed(newsFeed);

		newsFeed.loadCache();
		newsRubric = newsFeed.getNewsRubric();

		listView = (ListView) findViewById(R.id.news_list);

		listView.setOnItemClickListener(this);
		if (newsRubric.getArticles().size() == 0) {
			isEmptyOnStart = true;
		}
		
		System.gc();
	}

	public void reloadFeed() {
		if (!ConnectionChecker.isNetworkAvailable(getBaseContext())) {
			return;
		}

		Application.showProgressBar();
		newsFeed.reloadRss();

		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
		SharedPreferences.Editor editor = preferences.edit();

		editor.putLong(LAST_UPDATE_DATE, new Date().getTime());
		editor.commit();
		isEmptyOnStart = false;
	}

	@Override
	public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
		Intent viewArticle = new Intent();
		viewArticle.setClass(this, ArticleViewActivity.class);
		viewArticle.putExtra("RUBRIC_ID", adapter.getItem(i).getRubricId());
		viewArticle.putExtra("ID", adapter.getItem(i).getId());

		startActivity(viewArticle);
	}

	class NewsArrayAdapter extends ArrayAdapter<RssArticle> {

		private int resourceId;

		ArrayList<RssArticle> addedElements;

		public NewsArrayAdapter(Context context, int resourceId, List<RssArticle> messages) {
			super(context, resourceId, messages);

			this.resourceId = resourceId;
			addedElements = new ArrayList<RssArticle>(messages);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View v = convertView;
			SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
			if (v == null) {
				LayoutInflater li = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				v = li.inflate(resourceId, null);
			}
			
			float density = getResources().getDisplayMetrics().density;

			RssArticle currentArticle = super.getItem(position);
			if (currentArticle != null) {
				TextView title = ((TextView) v.findViewById(R.id.newsTitle));
				TextView description = ((TextView) v.findViewById(R.id.newsDescription));

				title.setText(currentArticle.getTitle());
				description.setText(currentArticle.getDescription());

				if (currentArticle.isRead()) {
					title.setTypeface(Typeface.defaultFromStyle(Typeface.NORMAL));
					description.setTextColor(R.color.read_article);
				} else {
					title.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
					description.setTextColor(R.color.unread_article);
				}

				if (!addedElements.contains(currentArticle)) {
					Animation myFadeInAnimation = AnimationUtils.loadAnimation(this.getContext(), android.R.anim.fade_in);
					v.startAnimation(myFadeInAnimation);

					addedElements.add(currentArticle);
				}
				
				String fontSize = preferences.getString("font_list_size", "medium");
				float dimFontSize = 12;
				if(fontSize.equalsIgnoreCase("small")){
					dimFontSize = getResources().getDimension(R.dimen.news_list_text_small);
				} else if(fontSize.equalsIgnoreCase("medium")){
					dimFontSize = getResources().getDimension(R.dimen.news_list_text_medium);
				} else if(fontSize.equalsIgnoreCase("large")){
					dimFontSize = getResources().getDimension(R.dimen.news_list_text_large);
				}
				title.setTextSize(dimFontSize / density);
				description.setTextSize(dimFontSize);
			}
			return v;
		}

		@Override
		public void add(RssArticle object) {
			super.add(object);

			NewsActivity.this.emptyMessage.setVisibility(View.INVISIBLE);
		}
	}

	@Override
	protected void onPostResume() {
		super.onPostResume();

		if (!isUpdatedToday()) {
			reloadFeed();
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		currentPosition = listView.getFirstVisiblePosition();

		newsFeed.setRssListener(null);
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
	}

	@Override
	protected void onResume() {
		super.onResume();
		newsFeed.loadCache();
		newsRubric = newsFeed.getNewsRubric();

		adapter = new NewsArrayAdapter(this, R.layout.news_row, newsRubric.getArticles());
		listView.setAdapter(adapter);
		listView.setSelection(currentPosition);

		if (newsRubric.getArticles().size() == 0) {
			emptyMessage.setVisibility(View.INVISIBLE);
		} else {
			emptyMessage.setVisibility(View.INVISIBLE);
		}

		newsFeed.setRssListener(new Loader.RssListener() {

			@Override
			public void finish() {
				System.out.println("NewsActivity hideProgressBar");
				Application.hideProgressBar();
			}

			@Override
			public void addArticle(RssArticle article) {
				Collections.sort(newsRubric.getArticles(), RssArticle.getComparator());
				adapter.notifyDataSetChanged();
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
	protected void onDestroy() {
		super.onDestroy();

		newsFeed.stopLoad();
	}
}
