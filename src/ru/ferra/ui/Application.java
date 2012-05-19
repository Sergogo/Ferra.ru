package ru.ferra.ui;

import android.app.TabActivity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;
import ru.ferra.R;
import ru.ferra.common.Constants;

public class Application extends TabActivity implements TabHost.OnTabChangeListener {

	private static ProgressBar progressBar;
	private static int showProgressCount = 0;
	
	private MenuItem menuFullscreenOn;
	private MenuItem menuFullscreenOff;
	private final static int NEWS_TAB = 0;
	private final static int ARTICLES_TAB = 1;

	public final static String LAST_ACTIVE_TAB = "last_active_tab";
	public final static String FULLSCREEN_ON = "fullscreen_on";
	
	private static int lastErrorMessageId = 0;
	private static Context context = null;


	public static void showProgressBar(){
		progressBar.setVisibility(View.VISIBLE);
		showProgressCount ++;

	}
	
	public static void hideProgressBar() {
		showProgressCount --;

		if(showProgressCount < 0) {
			showProgressCount = 0;
		}

		if(showProgressCount == 0){
			progressBar.setVisibility(View.INVISIBLE);
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);

		if (preferences.getBoolean(FULLSCREEN_ON, false)) {
			setFullscreen(true);
		}

		setContentView(R.layout.main);

		progressBar = (ProgressBar) findViewById(R.id.load_progress);
		context = getBaseContext();

		findViewById(R.id.logo).setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Intent openFerraHome = new Intent(Intent.ACTION_VIEW);
				openFerraHome.setData(Uri.parse(Constants.FERRA_BASE_URL));

				startActivity(openFerraHome);
			}
		});

		setupTab(new TextView(this), getString(R.string.tab_news), new Intent().setClass(this, NewsActivity.class));
		setupTab(new TextView(this), getString(R.string.tab_articles), new Intent().setClass(this, ArticlesActivity.class));

		getTabHost().setOnTabChangedListener(this);

		switch (preferences.getInt(LAST_ACTIVE_TAB, NEWS_TAB)) {
		case NEWS_TAB:
			getTabHost().setCurrentTab(0);
			break;
		case ARTICLES_TAB:
			getTabHost().setCurrentTab(1);
		}

	}

	private static View createTabView(final Context context, final String text) {
		View view = LayoutInflater.from(context).inflate(R.layout.tabs_bg, null);
		TextView tv = (TextView) view.findViewById(R.id.tabsText);
		tv.setText(text);
		return view;
	}

	private void setupTab(final View view, final String tag, Intent intent) {
		View tabView = createTabView(getTabHost().getContext(), tag);
		TabHost.TabSpec setContent = getTabHost().newTabSpec(tag).setIndicator(tabView).setContent(intent);

		getTabHost().addTab(setContent);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater menuInflater = getMenuInflater();
		menuInflater.inflate(R.menu.main_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_about:
			Intent localIntent = new Intent(getBaseContext(), AboutActivity.class);
			startActivity(localIntent);
			return true;
		case R.id.menu_preferences:
			Intent settingsActivity = new Intent(getBaseContext(), Settings.class);
			startActivity(settingsActivity);
			return true;
		case R.id.menu_fullscreen_on:
			setFullscreen(true);
			enableFullscreenMenu(false);

			return true;

		case R.id.menu_fullscreen_off:
			setFullscreen(false);
			enableFullscreenMenu(true);

			return true;
		}

		return super.onOptionsItemSelected(item);
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		menuFullscreenOn = menu.findItem(R.id.menu_fullscreen_on);
		menuFullscreenOff = menu.findItem(R.id.menu_fullscreen_off);

		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);

		if (preferences.getBoolean(FULLSCREEN_ON, false)) {
			enableFullscreenMenu(false);
		} else {
			enableFullscreenMenu(true);
		}

		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public void onTabChanged(String tabId) {
		SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
		if (tabId.equals(getString(R.string.tab_news))) {
			editor.putInt(LAST_ACTIVE_TAB, NEWS_TAB);
		}
		if (tabId.equals(getString(R.string.tab_articles))) {
			editor.putInt(LAST_ACTIVE_TAB, ARTICLES_TAB);
		}

		editor.commit();
	}

	private void setFullscreen(boolean isFullscreen) {
		SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
		editor.putBoolean(FULLSCREEN_ON, isFullscreen);
		editor.commit();

		if (isFullscreen) {
			getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
			getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
		} else {
			getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
			getWindow().addFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
		}
	}

	private void enableFullscreenMenu(boolean show) {
		if (show) {
			menuFullscreenOn.setVisible(true);
			menuFullscreenOff.setVisible(false);
		} else {
			menuFullscreenOn.setVisible(false);
			menuFullscreenOff.setVisible(true);
		}
	}

	public static void resetLastError() {
		lastErrorMessageId = 0;
	}
	
	public static void showError(int idErrorMessage){
		if(lastErrorMessageId != idErrorMessage) {
			lastErrorMessageId = idErrorMessage;
			
			if(context != null) {
				try{
					Toast.makeText(context, context.getString(idErrorMessage), Toast.LENGTH_SHORT).show();
				}catch(Exception e) {
					Log.e(Constants.TAG, "Error Toast creation.");
				}
			}
		}
	}
}
