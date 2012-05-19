package ru.ferra.common.utils;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import android.net.Uri;
import ru.ferra.common.Constants;
import ru.ferra.data.RssArticle;
import ru.ferra.providers.ArticleProvider;

public class ArticleSaxHandler extends DefaultHandler {
	private static final int SHORT_TEXT_SIZE = 150;

	// private Loader.ImageListener listener;

	private RssArticle article;

	private StringBuilder builder;
	private StringBuilder shortText;
	
	private HashMap<String, String> processedImages;

	// public ArticleSaxHandler(Loader.ImageListener imageListener) {
	// listener = imageListener;
	// }

	public void setArticle(RssArticle article) {
		this.article = article;

		builder = new StringBuilder();
		shortText = new StringBuilder();
		
		processedImages = new HashMap<String, String>();
	}

	public String getHtml() {
		return builder.toString();
	}

	public String getShortText() {
		return shortText.toString();
	}

	private String getImageCachedPath(String url) {
		return article.addImageToCache(url);
	}

	private String getImageCachedPathFullSize(String url) {
		return Uri.withAppendedPath(ArticleProvider.Image.CONTENT_URI,
				article.addImageToCacheFullSize(url)).toString();
	}

	@Override
	public void characters(char[] ch, int start, int length) throws SAXException {
		super.characters(ch, start, length);

		builder.append(ch, start, length);

		if (shortText.length() < SHORT_TEXT_SIZE) {
			shortText.append(ch, start, Math.min(SHORT_TEXT_SIZE - shortText.length(), length));
		}
	}

	@Override
	public void endElement(String uri, String localName, String name) throws SAXException {
		super.endElement(uri, localName, name);

		if (localName.equalsIgnoreCase("iframe") || localName.equalsIgnoreCase("embed")
				|| localName.equalsIgnoreCase("table") || localName.equalsIgnoreCase("tr")
				|| localName.equalsIgnoreCase("td")) {
			return;
		}

		builder.append("</");
		builder.append(localName);
		builder.append(">");

		if (localName.equalsIgnoreCase("img")) {
			builder.append("</A>");
		}

	}

	@Override
	public void startElement(String uri, String localName, String name, Attributes attributes)
			throws SAXException {
		super.startElement(uri, localName, name, attributes);
		boolean isImgTag = false;
		boolean isLinkTag = false;

		if (localName.equalsIgnoreCase("iframe") || localName.equalsIgnoreCase("embed")
				|| localName.equalsIgnoreCase("table") || localName.equalsIgnoreCase("tr")
				|| localName.equalsIgnoreCase("td") || localName.equalsIgnoreCase("select")) {
			return;
		}

		if (localName.equalsIgnoreCase("img")) {
			isImgTag = true;

			String imageUrl = getImageFullUrl(attributes.getValue("src"));
			String cachedImagePath = getImageCachedPathFullSize(imageUrl);

			builder.append("<A href=\"");
			builder.append(cachedImagePath);
			builder.append("\">");
			
			processedImages.put(imageUrl, cachedImagePath);
		}
		
		if (localName.equalsIgnoreCase("a")){
			isLinkTag = true;
		}

		builder.append("<");
		builder.append(localName);

		for (int i = 0; i < attributes.getLength(); i++) {
			builder.append(" ");

			if (isImgTag) {
				if (!attributes.getLocalName(i).equalsIgnoreCase("height")
						&& !attributes.getLocalName(i).equalsIgnoreCase("width")
						&& !attributes.getLocalName(i).equalsIgnoreCase("alt")) {
					builder.append(attributes.getLocalName(i));
					builder.append("=\"");

					if (attributes.getLocalName(i).equalsIgnoreCase("src")) {
						String fullUrl = getImageFullUrl(attributes.getValue(i));
						builder.append(getImageCachedPath(fullUrl));
					} else {
						builder.append(attributes.getValue(i));
					}
					builder.append("\"");
				}
			} else if(!isLinkTag){
				//skip width and style attributes
				if (!attributes.getLocalName(i).equalsIgnoreCase("width")
						&& !attributes.getLocalName(i).equalsIgnoreCase("style")) {
					builder.append(attributes.getLocalName(i));
					builder.append("=\"");
					builder.append(attributes.getValue(i));
					builder.append("\"");
				} else if (attributes.getLocalName(i).equalsIgnoreCase("style")) {
					// remove width from style
					StringBuilder newStyle = new StringBuilder();

					int widthStart = attributes.getValue(i).lastIndexOf("width:");
					int widthEnd;
					if (widthStart < 0) {
						newStyle.append(attributes.getValue(i));
					} else {
						widthEnd = attributes.getValue(i).indexOf(";", widthStart + 1);
						newStyle.append(attributes.getValue(i).substring(0, widthStart));
						newStyle.append(attributes.getValue(i).substring(widthEnd + 1));
					}

					builder.append("style=\"");
					builder.append(newStyle);
					builder.append("\"");
				}

			} else{
				//process links separately
				if(attributes.getLocalName(i).equalsIgnoreCase("href") && processedImages.containsKey(attributes.getValue(i))){
					builder.append("href=\"").append(processedImages.get(attributes.getValue(i))).append("\"");
				} else {
					builder.append(attributes.getLocalName(i));
					builder.append("=\"");
					builder.append(attributes.getValue(i));
					builder.append("\"");
				}
			}
		}
		builder.append(">");
	}

	private String getImageFullUrl(String urlString) {
		boolean isRelativeUrl = false;

		StringBuilder result = new StringBuilder();
		try {
			new URL(urlString);
		} catch (MalformedURLException e) {
			isRelativeUrl = true;
		}

		if (urlString.contains(Constants.FERRA_BASE_URL) || isRelativeUrl) {
			int imagesStartPath = urlString.lastIndexOf("/images/");

			result.append(Constants.FERRA_BASE_URL);
			result.append(urlString.substring(imagesStartPath));

			if(result.toString().equals("http://www.ferra.ru/images/")){
				System.out.println("!!!!!!!!!! ERROR!!!! " + urlString);
			}
			return result.toString();
		} else {
			if(urlString.equals("http://www.ferra.ru/images/")){
				System.out.println("!!!!!!!!!! ERROR!!!! " + urlString);
			}
			return urlString;
		}
	}
}
