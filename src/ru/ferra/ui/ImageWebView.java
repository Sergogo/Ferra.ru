package ru.ferra.ui;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebChromeClient;
import android.webkit.WebView;

public class ImageWebView extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);

		if(preferences.getBoolean(Application.FULLSCREEN_ON, false)) {
			setFullscreen();
		}

		getWindow().requestFeature(Window.FEATURE_PROGRESS);

		Bundle extras = getIntent().getExtras();

		WebView webView = new WebView(this);
		webView.getSettings().setSupportZoom(true);
		webView.getSettings().setBuiltInZoomControls(true);

		//webView.loadUrl(extras.getString("url"));
		webView.loadDataWithBaseURL("", "<img src=\"" + extras.getString("url") + "\"\\>", "text/html", "windows-1252", "");
		setContentView(webView);

		final Activity activity = this;

		webView.setWebChromeClient(new WebChromeClient() {
			public void onProgressChanged(WebView view, int progress) {
				activity.setProgress(progress * 100);
			}
		});
	}

	private void setFullscreen() {
    		getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
    		getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
	}

}
