package ru.ferra.data;

import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;


public class RssArticle {
    private int id;
    private String externalId;
    private String title;
    private String description;
    private String content;
    private String rubric;
    private int rubricId;
    private URL url;
    private String enclosure;
    private boolean is_new;
    private boolean is_read;
    private String guid;
    private Date publishDate;
    
    private static Comparator<RssArticle> comparator = new Comparator<RssArticle>() {
        @Override
        public int compare(RssArticle rssArticle1, RssArticle rssArticle2) {
        	long dif = rssArticle2.getPublishDate() - rssArticle1.getPublishDate();
        	if(dif > 0) return 1;
        	if(dif < 0) return -1;
        	
        	return 0;
        }
    };

    //is used only for loading and caching, not for display
    private HashMap<Long, String> imagesToCache;
    private HashMap<Long, String> imagesToCacheFullSize;

    static SimpleDateFormat INPUTFORMATTER = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US);
    static SimpleDateFormat OUTPUTFORMATTER = new SimpleDateFormat("dd.MMM");

    public RssArticle(){
        publishDate = new Date(0);
        imagesToCache = new HashMap<Long, String>();
        imagesToCacheFullSize = new HashMap<Long, String>();
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public URL getUrl() {
        return url;
    }

    public void setUrl(String url) {
        try {
            this.url = new URL(url);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getRubricName() {
        return rubric;
    }

    public void setRubricName(String rubric) {
        this.rubric = rubric;
    }

    public long getPublishDate() {
        return publishDate.getTime();
    }

    public void setPublishDate(String publishDate) {
        try {
            this.publishDate = INPUTFORMATTER.parse(publishDate);
        } catch (ParseException e) {
        	this.publishDate = new Date();
            e.printStackTrace();
        }
    }
    
    public String addImageToCache(String url){
    	long id = getNextID();
        imagesToCache.put(id, url);
    	
    	return String.valueOf(id);
    }

    public String addImageToCacheFullSize(String url){
    	long id = getNextID();
        imagesToCacheFullSize.put(id, url);
    	
    	return String.valueOf(id);
    }

    public void setPublishDate(long publishDate) {
            this.publishDate = new Date(publishDate);
    }

    public String getEnclosure() {
        return enclosure;
    }

    public void setEnclosure(String enclosure) {
        this.enclosure = enclosure;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }


    public String getGuid() {
        return guid;
    }

    public void setGuid(String guid) {
        this.guid = guid;
    }

    public boolean isNew() {
        return is_new;
    }

    public void setNew(boolean is_new) {
        this.is_new = is_new;
    }

    public boolean isRead() {
        return is_read;
    }

    public void setRead(boolean is_read) {
        this.is_read = is_read;
    }

	public int getRubricId() {
		return rubricId;
	}

	public void setRubricId(int rubricId) {
		this.rubricId = rubricId;
	}

	public void setExternalId(String externalId) {
		this.externalId = externalId;
	}

	public String getExternalId() {
		return externalId;
	}
	
	public static Comparator<RssArticle> getComparator() {
		return comparator;
	}
	
	public HashMap<Long, String> getImagesInfo(){
		return imagesToCache;
	}

	public HashMap<Long, String> getImagesInfoFullSize(){
		return imagesToCacheFullSize;
	}
	
	private static long nextId;
	static{
		nextId = new Date().getTime();
	}
	private static synchronized long getNextID() {
		nextId++;
		return nextId;
	}
}
