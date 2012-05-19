package ru.ferra.ui;

import ru.ferra.R;
import ru.ferra.common.utils.ConnectionChecker;
import ru.ferra.common.utils.loaders.Feed;
import android.app.Activity;
import android.view.MenuItem;

public abstract class RssViewActivity extends Activity {

	private Feed feed;
	
	public abstract void reloadFeed();

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_refresh:
			ConnectionChecker.resetLastError();
			reloadFeed();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

    public void setFeed(Feed feed){
    	this.feed = feed;
    }
    
    public Feed getFeed() {
    	return feed;
    }
}
