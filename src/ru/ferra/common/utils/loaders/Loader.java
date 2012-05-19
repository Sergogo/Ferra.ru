package ru.ferra.common.utils.loaders;

import java.util.LinkedList;

import android.os.AsyncTask;
import android.util.Log;

import ru.ferra.common.Constants;
import ru.ferra.data.RssArticle;

public class Loader {
	private final int MAX_TASKS = 2;

	private ArticleDownloaderTask[] tasks = new ArticleDownloaderTask[MAX_TASKS];
	private LinkedList<TaskInfo> tasksInfo = new LinkedList<TaskInfo>();

	//private Loader instance = new Loader();
	private int tasksInProgress = 0;
	private boolean paused = false;

	public interface RssListener {
		public void addArticle(RssArticle article);

		public void finish();
	}

//	public interface ImageListener {
//		public String addImage(String url);
//	}

	private Loader() {}

	public static Loader getNewInstance() {
		return new Loader();
	}

	public void load(String url, RssListener listener) {
		synchronized (this) {
			tasksInfo.add(new TaskInfo(listener, url));
		}

		startNextTasks();
	}

	public void load(String url, RssListener listener, boolean fullLoad) {
		synchronized (this) {
			tasksInfo.add(new TaskInfo(listener, url, fullLoad));
		}

		startNextTasks();
	}

	public TaskInfo getTask(){
		if (paused)
			return null;

		synchronized (this) {
			if(tasksInfo.size() == 0) return null;
			return tasksInfo.remove();
		}
	}

	public void pause() {
		paused = true;
	}

	public void resume() {
		paused = false;

		startNextTasks();
	}

	public void stop() {
		tasksInfo.clear();
	}
	
	public boolean isStopped(){
		if(tasksInfo.size() > 0) return false;

//		for (int i=0; i< MAX_TASKS; i++) {
//			if(tasks[i] != null && tasks[i].getStatus() != AsyncTask.Status.FINISHED) {
//				result = false;
//			}
//		}
//		
		return true;
	}

	private void startNextTasks() {
		if (paused)
			return;

		synchronized (this) {
			if (tasksInfo.size() > 0) {
				for (int i=0; i< MAX_TASKS; i++) {
					if(tasks[i] == null || tasks[i].getStatus() == AsyncTask.Status.FINISHED) {
						tasks[i] = new ArticleDownloaderTask(this);
						tasks[i].execute();
						
						tasksInProgress++;
						Log.d(Constants.TAG, "tasksInProgress = " + tasksInProgress);

						return;
					}
				}
			} else {
				return;
			}
		}
	}

	public void taskFinished() {
		synchronized (Loader.class) {

			tasksInProgress--;
			startNextTasks();
			Log.d(Constants.TAG, "tasksInProgress = " + tasksInProgress);
		}
	}

	public class TaskInfo {
		RssListener listener;
		String url;
		boolean fullLoad = false;

		public TaskInfo(RssListener listener, String url) {
			this.listener = listener;
			this.url = url;
		}

		public TaskInfo(RssListener listener, String url, boolean fullLoad) {
			this.listener = listener;
			this.url = url;
			this.fullLoad = fullLoad;
		}
	}
}

