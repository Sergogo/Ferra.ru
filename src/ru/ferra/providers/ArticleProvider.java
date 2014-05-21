package ru.ferra.providers;

import android.database.Cursor;
import android.content.*;
import android.content.res.AssetManager;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import ru.ferra.R;
import ru.ferra.common.Constants;
import ru.ferra.common.utils.ConnectionChecker;

import java.io.*;
import java.net.URL;

public class ArticleProvider extends ContentProvider {

	private static final String NO_IMAGE_FILE = Environment.getExternalStorageDirectory()
			+ Constants.CACHE_DIR + "no_image.png";

	private DBHelper dbHelper;
	private byte buffer[] = new byte[4096];

	private static final int RUBRIC = 1;
	private static final int RUBRIC_ID = 2;
	private static final int ARTICLE = 3;
	private static final int ARTICLE_ID = 4;
	private static final int IMAGES = 5;
	private static final UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

	static {
		uriMatcher.addURI("ru.ferra.provider.articleprovider", "rubric", RUBRIC);
		uriMatcher.addURI("ru.ferra.provider.articleprovider", "rubric/#", RUBRIC_ID);
		uriMatcher.addURI("ru.ferra.provider.articleprovider", "article", ARTICLE);
		uriMatcher.addURI("ru.ferra.provider.articleprovider", "article/#", ARTICLE_ID);
		uriMatcher.addURI("ru.ferra.provider.articleprovider", "image", IMAGES);
	}

	public static class Article {
		public static final String ID = "_id";
		public static final String TITLE = "title";
		public static final String DESCRIPTION = "description";
		public static final String URL = "url";
		public static final String CONTENT = "content";
		public static final String ENCLOSURE = "enclosure";
		public static final String TIME_PUBLISHED = "time_published";
		public static final String GUID = "guid";
		public static final String RUBRIC_ID = "rubric_id";
		public static final String IS_READ = "is_read";
		public static final String IS_NEW = "is_new";

		public static final Uri CONTENT_URI = Uri
				.parse("content://ru.ferra.provider.articleprovider/article");
	}

	public static class Rubric {
		public static final String ID = "_id";
		public static final String NAME = "name";
		public static final String FULL_NAME = "full_name";

		public static final Uri CONTENT_URI = Uri
				.parse("content://ru.ferra.provider.articleprovider/rubric");
	}

	public static class Image {
		public static final String ID = "_id";
		public static final String ARTICLE_ID = "article_id";
		public static final String URL = "url";
		public static final String LOCAL_PATH = "local_path";
		public static final Uri CONTENT_URI = Uri.parse("content://ru.ferra.provider.articleprovider/image");
		public static final Uri EMPTY_IMAGE_URI = Uri.parse("content://ru.ferra.provider.articleprovider/image/empty");
	}

	@Override
	public boolean onCreate() {
		dbHelper = new DBHelper(getContext());
		return true;
	}

	@Override
	public Cursor query(Uri uri, String[] strings, String s, String[] strings1, String s1) {
		SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();

		Log.i(Constants.TAG, "DEBUG QUERY URI:" + uri);
		String sortOrder;
		switch (uriMatcher.match(uri)) {
		case RUBRIC_ID:
			queryBuilder.appendWhere("_id=" + uri.getLastPathSegment());
		case RUBRIC:
			queryBuilder.setTables(DBHelper.RUBRIC_TABLE);
			sortOrder = s1;
			break;
		case ARTICLE_ID:
			queryBuilder.appendWhere("_id=" + uri.getLastPathSegment());
		case ARTICLE:
			queryBuilder.setTables(DBHelper.ARTICLE_TABLE);
			sortOrder = (s1 != null) ? s1 : " time_published desc";
			break;
		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}
		Cursor c = queryBuilder.query(dbHelper.getReadableDatabase(), strings, s, strings1, null,
				null, sortOrder);
		c.setNotificationUri(getContext().getContentResolver(), uri);
		return c;
	}

	@Override
	public String getType(Uri uri) {
		return null;
	}

	@Override
	public Uri insert(Uri uri, ContentValues contentValues) {
		int uriType = uriMatcher.match(uri);

		Log.i(Constants.TAG, "DEBUG INSERT URI:" + uri);

		ContentValues values;
		if (contentValues == null) {
			values = new ContentValues();
		} else {
			values = new ContentValues(contentValues);
		}

		if (uriType == ARTICLE) {
			long l = dbHelper.getWritableDatabase().insert(DBHelper.ARTICLE_TABLE, null, values);
			if (l <= 0L) {
				Log.i(Constants.TAG, "Failed to insert row into " + uri);
			}

			return ContentUris.withAppendedId(Article.CONTENT_URI, l);
		}
		if (uriType == IMAGES) {
			long l = dbHelper.getWritableDatabase().insert(DBHelper.IMAGES_TABLE, null, values);
			if (l <= 0L)
				Log.i(Constants.TAG, "Failed to insert row into " + uri);

			Log.i(Constants.TAG, "INSERT IMAGE RES: " + l);
			return ContentUris.withAppendedId(Image.CONTENT_URI, l);

		}
		throw new IllegalArgumentException("Unknown URI " + uri);
	}

	@Override
	public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
		String img = null;
		File f = null;
		Cursor cursor = null;
		URL imageUrl;
		String imageLocalPath;
		if(uri.getLastPathSegment().equals("empty")){
			return ParcelFileDescriptor.open(getEmptyImage(), ParcelFileDescriptor.MODE_READ_ONLY);
		}

		try {

			SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
			queryBuilder.setTables(DBHelper.IMAGES_TABLE);
			cursor = queryBuilder.query(dbHelper.getWritableDatabase(), new String[] { "url",
					"local_path" }, "_id=?", new String[] { uri.getLastPathSegment() }, null, null,
					null);

			cursor.moveToFirst();
			Log.i(Constants.TAG, "DEBUG OPENFILE URI:" + uri);
			f = new File(cursor.getString(1));

			imageUrl = new URL(cursor.getString(0));
			imageLocalPath = cursor.getString(1);
			cursor.close();

			if (!f.exists()) {
				if (!ConnectionChecker.isNetworkAvailable(getContext())) {
					// AssetManager assetManager = getContext().getAssets();
					// AssetFileDescriptor d =
					// assetManager.openFd("no_picture.png");
					// return d.getParcelFileDescriptor();
					// f = new File("file:///android_asset/no_picture.png");
//					f = getEmptyImage();
//					throw new FileNotFoundException(uri.toString());
					return null;
				} else {
					Log.i(Constants.TAG, "Downloading img " + imageUrl);
					img = cacheImage(imageUrl, imageLocalPath);
					f = new File(img);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			if (!cursor.isClosed()) {
				cursor.close();
			}
		}

		ParcelFileDescriptor fileDescriptor = ParcelFileDescriptor.open(f,
				ParcelFileDescriptor.MODE_READ_ONLY);
		return fileDescriptor;
	}

	private String cacheImage(URL url, String localName) {
		InputStream input = null;
		OutputStream output = null;
		String filePath = "";

		try {
			String sstate = Environment.getExternalStorageState();
			Log.i(Constants.TAG, "DEBUG STORAGE STATE:" + sstate);
			Log.i(Constants.TAG, "DEBUG LOCALNAME:" + localName);

			//File cacheFolder = new File(Environment.getExternalStorageDirectory() + Constants.CACHE_DIR);
			//File cacheFolder = new File(Environment.getDownloadCacheDirectory() + Constants.CACHE_DIR);
			//File cacheFolder = new File(getContext().getExternalFilesDir(null) + Constants.CACHE_DIR);
			File cacheFolder = new File(getContext().getFilesDir() + Constants.CACHE_DIR);
//			File cacheFolder = new File(Environment.getDataDirectory() + Constants.CACHE_DIR);
			if (!cacheFolder.exists()) {
				cacheFolder.mkdirs();
			}

			File file = new File(localName);
			input = url.openStream();
			output = new FileOutputStream(file);

			int bytesRead = 0;
			while ((bytesRead = input.read(buffer, 0, buffer.length)) >= 0) {
				output.write(buffer, 0, bytesRead);
			}

			filePath = file.getPath();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				input.close();
				output.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		return filePath;
	}

	private File getEmptyImage() {
		AssetManager assets = getContext().getAssets();
		File file = new File(NO_IMAGE_FILE);

		OutputStream out = null;
		InputStream in = null;

		if (!file.exists()) {
			try {
				if(!file.getParentFile().exists()) {
					file.getParentFile().mkdirs();
				}
				file.createNewFile();
				out = new FileOutputStream(file);
				out = new BufferedOutputStream(out);

//				in = assets.open("file:///android_asset/no_picture.png");
				in = assets.open("no_picture.png");
				in = new BufferedInputStream(in);
				while (true) {
					int b = in.read();
					if (b == -1) {
						break;
					}
					out.write(b);
				}
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				try {
					out.flush();
					out.close();
					in.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

		}

		return file;
	}

	@Override
	public int delete(Uri uri, String s, String[] strings) {
		SQLiteDatabase database = dbHelper.getWritableDatabase();
		int res;
		switch (uriMatcher.match(uri)) {
		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		case IMAGES:
			res = database.delete(DBHelper.IMAGES_TABLE, s, strings);
			break;
		case ARTICLE:
			res = database.delete(DBHelper.ARTICLE_TABLE, s, strings);
			break;
		case RUBRIC:
			res = database.delete(DBHelper.RUBRIC_TABLE, s, strings);
			break;

		}
		return res;
	}

	@Override
	public int update(Uri uri, ContentValues contentValues, String s, String[] strings) {
		SQLiteDatabase database = dbHelper.getWritableDatabase();
		StringBuilder where = new StringBuilder();

		if (s != null)
			where.append(s);

		switch (uriMatcher.match(uri)) {
		case ARTICLE_ID:
			if (where.length() != 0)
				where.append(" and");
			where.append(" _id =  ");
			where.append(uri.getLastPathSegment());
		case ARTICLE:
			break;
		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}
		return database.update(DBHelper.ARTICLE_TABLE, contentValues, where.toString(), strings);
	}

	private static class DBHelper extends SQLiteOpenHelper {
		final static int DB_VERSION = Constants.INTERNAL_DATABASE_VERSION;
		private static final String DB_NAME = "ferra.db";

		private static final String RUBRIC_TABLE = "rubric";
		private static final String DROP_RUBRIC_TABLE_QUERY = "drop table if exists "
				+ RUBRIC_TABLE;
		private static final String CREATE_RUBRIC_TABLE_QUERY = "create table " + RUBRIC_TABLE
				+ " (" + "_id integer primary key autoincrement," + "name text,"
				+ "full_name text);";

		private static final String ARTICLE_TABLE = "article";
		private static final String DROP_ARTICLE_TABLE_QUERY = "drop table if exists "
				+ ARTICLE_TABLE;
		private static final String CREATE_ARTICLE_TABLE_QUERY = "create table " + ARTICLE_TABLE
				+ " (" + "_id integer primary key autoincrement," + "title text,"
				+ "description text," + "url text," + "content text," + "enclosure text,"
				+ "time_published integer," + "guid text," + "is_read integer default 0,"
				+ "is_new integet default 1," + "rubric_id integer REFERENCES " + RUBRIC_TABLE
				+ "(_id) ON DELETE CASCADE" + ");";

		private static final String IMAGES_TABLE = "images";
		private static final String DROP_IMAGES_TABLE_QUERY = "drop table if exists "
				+ IMAGES_TABLE;
		private static final String CREATE_IMAGES_TABLE_QUERY = "create table " + IMAGES_TABLE
				+ " (" 
				+ "_id integer primary key," 
				+ "article_id integer,"
				+ "url text," 
				+ "local_path text);";

		private Context context;

		public DBHelper(Context context) {
			super(context, DB_NAME, null, DB_VERSION);
			this.context = context;
		}

		@Override
		public void onCreate(SQLiteDatabase paramSQLiteDatabase) {
			paramSQLiteDatabase.execSQL(CREATE_RUBRIC_TABLE_QUERY);
			paramSQLiteDatabase.execSQL(CREATE_ARTICLE_TABLE_QUERY);
			paramSQLiteDatabase.execSQL(CREATE_IMAGES_TABLE_QUERY);

			String[] rubricsNames, rubricsNativeNames;

			rubricsNames = context.getResources().getStringArray(R.array.rubrics);
			rubricsNativeNames = context.getResources().getStringArray(R.array.rubrics_native);

			ContentValues values = new ContentValues();
			for (int i = 0; i < rubricsNames.length; i++) {
				values.put(Rubric.NAME, rubricsNames[i]);
				values.put(Rubric.FULL_NAME, rubricsNativeNames[i]);

				paramSQLiteDatabase.insert(RUBRIC_TABLE, null, values);
			}
		}

		@Override
		public void onUpgrade(SQLiteDatabase paramSQLiteDatabase, int paramInt1, int paramInt2) {
			paramSQLiteDatabase.execSQL(DROP_RUBRIC_TABLE_QUERY);
			paramSQLiteDatabase.execSQL(DROP_ARTICLE_TABLE_QUERY);
			paramSQLiteDatabase.execSQL(DROP_IMAGES_TABLE_QUERY);

			onCreate(paramSQLiteDatabase);
		}
	}
}
