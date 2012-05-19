package ru.ferra.common;

public final class RubricsTitles {
	private final static String[] rubrics = new String[]{
			"notebooks",
			"mobile",
			"digiphoto",
			"multimedia",
			"system",
			"video",
			"storage",
			"casecool",
			"periphery",
			"networks",
			"soft",
			"techlife",
			"digihome",
			"epads",
			"3d",
			"byt"
	};
	
	public static String[] getTitles() {
		return rubrics;
	}
	
	public static String getTitlesCommaSeparated() {
		if(rubrics.length == 0) return "";

		StringBuilder result = new StringBuilder(rubrics[0]);

		for(int i = 1; i < rubrics.length; i++) {
			result.append(',');
			result.append(rubrics[i]);
		}
		
		return result.toString();
	}
}
