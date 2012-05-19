package ru.ferra.ui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.WindowManager;
import ru.ferra.R;
import ru.ferra.common.Constants;
import ru.ferra.providers.ArticleProvider;

import java.io.File;

public class Settings extends PreferenceActivity {

	public final static String SETTINGS_WIFI_USE = "settings_wifi_use";

	private final static int DIALOG_ARTICLES_DELETE = 0;
	private final static int DIALOG_IMAGES_DELETE = 1;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		addPreferencesFromResource(R.xml.settings);

		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);

		if (preferences.getBoolean(Application.FULLSCREEN_ON, false)) {
			setFullscreen();
		}

		findPreference("cache_articles_del").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				showDialog(DIALOG_ARTICLES_DELETE);
				return true;
			}
		});

		findPreference("cache_images_del").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				showDialog(DIALOG_IMAGES_DELETE);
				return true;
			}
		});

	}

	@Override
	protected Dialog onCreateDialog(int id) {
		AlertDialog.Builder dialog = null;

		dialog = new AlertDialog.Builder(this);
		dialog.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialogInterface, int i) {
				dialogInterface.cancel();
			}
		});

		switch (id) {
		case DIALOG_ARTICLES_DELETE:
			dialog.setMessage(R.string.cache_delete_articles_confirm);
			dialog.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialogInterface, int i) {

					ThumbnailCache.getInstance().clearCache();
					new DeleteTask(DeleteTask.DELETE_ARTICLES).execute();

					SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(Settings.this);
					SharedPreferences.Editor editor = preferences.edit();

					editor.putLong("ARTICLES_LAST_DATE", 0);
					editor.putLong("NEWS_LAST_DATE", 0);
					editor.putBoolean("CLEAN_UP", true);
					editor.commit();

				}
			});

			break;
		case DIALOG_IMAGES_DELETE:
			dialog.setMessage(R.string.cache_delete_images_confirm);
			dialog.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialogInterface, int i) {
					ThumbnailCache.getInstance().clearCache();
					new DeleteTask(DeleteTask.DELETE_IMAGES).execute();
				}
			});
			break;

		default:
			dialog = null;
		}

		return dialog.create();
	}

	private void deleteFiles() {
		File file;
		if (!Environment.getExternalStorageState().equals("mounted"))
			file = getBaseContext().getCacheDir();
		else
			file = new File(Environment.getExternalStorageDirectory(), Constants.CACHE_DIR);
		String[] fileList = null;
		if ((file.exists()) && (file.isDirectory()))
			fileList = file.list();
		for (int i = 0;; i++) {
			if (fileList == null || i >= fileList.length)
				return;
			new File(file, fileList[i]).delete();
			Log.d(Constants.TAG, "Remove file: " + fileList[i]);
		}
	}

	private void setFullscreen() {
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
	}
	
	class DeleteTask extends AsyncTask<Void, Void, Void>{

		public static final int DELETE_IMAGES = 0;
		public static final int DELETE_ARTICLES = 1;
		
		int type;
		ProgressDialog progressDialog;

		public DeleteTask(int type){
			this.type = type;
		}

		
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			
			progressDialog = new ProgressDialog(Settings.this);
			progressDialog.setMessage(getString(R.string.wait_message));

			progressDialog.show();

		}


		@Override
		protected Void doInBackground(Void... params) {
			
			if(type == DELETE_IMAGES){
				deleteFiles();
			}

			if(type == DELETE_ARTICLES){
				getContentResolver().delete(ArticleProvider.Article.CONTENT_URI, null, null);
				getContentResolver().delete(ArticleProvider.Image.CONTENT_URI, null, null);
			}
			
			return null;
		}


		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);
			try{
				progressDialog.dismiss();
			} catch(Exception e){}
		}
		
		
	}
}
