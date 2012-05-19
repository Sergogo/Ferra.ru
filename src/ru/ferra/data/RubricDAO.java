package ru.ferra.data;

import ru.ferra.providers.ArticleProvider;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;

public class RubricDAO {

	public Rubric getRubric(Context context, int id) {
		return getRubric(context, ArticleProvider.Rubric.ID, String.valueOf(id));
	}

	public Rubric getRubric(Context context, String name) {
		return getRubric(context, ArticleProvider.Rubric.NAME, name);
	}


	public Rubric createRubric(Context context) {
		throw new UnsupportedOperationException("Method createRubric is udefined");
	}

	public void updateRubric(Context context, Rubric rubric) {
		throw new UnsupportedOperationException("Method updateRubric is udefined");
	}


	private Rubric getRubric(Context context, String condition, String conditionValue) {
		ContentResolver contentResolver = context.getContentResolver();
		Rubric rubric;

		String[] columns = new String[] { ArticleProvider.Rubric.ID, ArticleProvider.Rubric.NAME,
				ArticleProvider.Rubric.FULL_NAME };

		String selectClause = condition + " = ?";
		String[] selectArg = { conditionValue };

		Cursor cursor = contentResolver.query(ArticleProvider.Rubric.CONTENT_URI, columns, selectClause, selectArg, null);
		cursor.moveToFirst();
		if (cursor.isAfterLast()) {
			return null;
		}

		rubric = new Rubric(cursor.getString(1));
		rubric.setFullName(cursor.getString(2));
		rubric.setId(cursor.getInt(0));

		cursor.close();

		return rubric;
	}

}
