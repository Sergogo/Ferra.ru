package ru.ferra.ui;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedList;

import ru.ferra.R;
import ru.ferra.providers.ArticleProvider;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.widget.ImageView;

public class ThumbnailCache {

	private static ThumbnailCache instance = new ThumbnailCache();

	private LinkedList<ImageInfo> tasks = new LinkedList<ImageInfo>();

	private ThumbnailLoaderTask task;
	private Context context;
	private HashMap<String, Bitmap> cache = new HashMap<String, Bitmap>();

	private ThumbnailCache() {}

	public static ThumbnailCache getInstance() {
		return instance;
	}

	public void setContext(Context context) {
		this.context = context;
	}

	public Bitmap getBitmap(String url) {
		synchronized (cache) {
			return cache.get(url);
		}
	}

	public void addBitmap(String url, Bitmap bitmap) {
		synchronized (cache) {
			cache.put(url, bitmap);
		}
	}

	public void load(ImageView image, String uri) {
		synchronized (instance) {
			tasks.add(new ImageInfo(uri, image));
		}

		startNext();
	}

	private void startNext() {
		synchronized (ThumbnailCache.class) {
			if (task == null || task.getStatus() == AsyncTask.Status.FINISHED) {
				task = new ThumbnailLoaderTask(context);

				task.execute();
			}
		}
	}

	public ImageInfo getNext() {
		synchronized (instance) {
			if (tasks.size() == 0)
				return null;

			return tasks.remove();
		}
	}
	
	public void stopLoad(){
		tasks.clear();
	}
	
	public void clearCache(){
		cache = new HashMap<String, Bitmap>();
	}

	private static class ThumbnailLoaderTask extends AsyncTask<Void, Void, Void> {

		private Context context;
		private LinkedList<ImageInfo> finishedTasks = new LinkedList<ImageInfo>();

		public ThumbnailLoaderTask(Context context) {
			this.context = context;
		}

		@Override
		protected Void doInBackground(Void... params) {
			ImageInfo imageInfo;

			while ((imageInfo = ThumbnailCache.instance.getNext()) != null) {
				InputStream imageStream = null;
				try {
					imageStream = context.getContentResolver().openInputStream(Uri.parse(imageInfo.uri));

					if(imageStream == null){
						imageInfo.image.setTag(R.bool.image_empty, true);
						
						return null;
					}

					synchronized (this) {
						finishedTasks.add(imageInfo);
					}

					imageInfo.bitmap = BitmapFactory.decodeStream(imageStream);
					imageStream.close();

					ThumbnailCache.instance.addBitmap(imageInfo.uri, imageInfo.bitmap);

					publishProgress();
					imageInfo.image.setTag(R.bool.image_empty, false);
				}catch(IOException e){
					imageInfo.image.setTag(R.bool.image_empty, true);
				}
			}
			return null;
		}

		@Override
		protected void onProgressUpdate(Void... values) {
			super.onProgressUpdate(values);

			ImageInfo info;
			int size = finishedTasks.size();
			synchronized (this) {
				for(int i=0; i < size; i++) {
					info = finishedTasks.remove();

					info.image.setImageBitmap(info.bitmap);
				}
			}
		}
	}
}

class ImageInfo {
	String uri;
	ImageView image;
	Bitmap bitmap;

	public ImageInfo(String uri, ImageView image) {
		this.uri = uri;
		this.image = image;
	}
}