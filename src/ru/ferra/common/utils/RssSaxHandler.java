package ru.ferra.common.utils;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import ru.ferra.common.utils.loaders.Loader;
import ru.ferra.data.RssArticle;

public class RssSaxHandler extends DefaultHandler {
    private final String CHANNEL = "channel";
    private final String ITEM = "item";
    private final String TITLE = "title";
    private final String LINK = "link";
    private final String DESCRIPTION = "description";
    private final String PUBDATE = "pubDate";
    private final String CATEGORY = "category";
    private final String ENCLOSURE = "enclosure";
    private final String GUID = "guid";
    private final String DOC_ID = "doc_id";

    private enum states {
        none,
        chanel,
        item,
        item_title,
        item_link,
        item_description,
        item_pubDate,
        item_category,
        item_guid,
        item_doc_id
    }


    private Loader.RssListener listener;

    private states currentState;
    private RssArticle article;
    private StringBuilder accumulator;


    public RssSaxHandler(Loader.RssListener listener) {
        this.listener = listener;
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        super.characters(ch, start, length);
        accumulator.append(String.copyValueOf(ch, start, length));
    }

    @Override
    public void endElement(String uri, String localName, String name) throws SAXException {
        super.endElement(uri, localName, name);

        switch (currentState) {
            case item:
                if (localName.equalsIgnoreCase(ITEM)) {
                	listener.addArticle(article);
                    currentState = states.chanel;
                }
                break;

            case item_description:
                article.setDescription(accumulator.toString());
                currentState = states.item;
                break;

            case item_link:
                article.setUrl(accumulator.toString());
                currentState = states.item;
                break;

            case item_title:
                article.setTitle(accumulator.toString());
                currentState = states.item;
                break;

            case item_category:
            	article.setRubricName(accumulator.toString());
                currentState = states.item;
                break;
            case item_guid:
            	article.setGuid(accumulator.toString());
                currentState = states.item;
                break;

            case item_pubDate:
                article.setPublishDate(accumulator.toString());
                currentState = states.item;
                break;
                
            case item_doc_id:
                article.setExternalId(accumulator.toString());
                currentState = states.item;
                break;
        }
    }


    @Override
    public void startElement(String uri, String localName, String name, Attributes attributes) throws SAXException {
        super.startElement(uri, localName, name, attributes);

        accumulator = new StringBuilder();

        switch (currentState) {
            case none:
                if (localName.equalsIgnoreCase(CHANNEL)) {
                    currentState = states.chanel;
                }
                break;

            case chanel:
                if (localName.equalsIgnoreCase(ITEM)) {
                    currentState = states.item;
                    article = new RssArticle();
                }
                break;

            case item:
                if (localName.equalsIgnoreCase(TITLE)) {
                    currentState = states.item_title;
                }

                if (localName.equalsIgnoreCase(LINK)) {
                    currentState = states.item_link;
                }

                if (localName.equalsIgnoreCase(DESCRIPTION)) {
                    currentState = states.item_description;
                }

                if (localName.equalsIgnoreCase(PUBDATE)) {
                    currentState = states.item_pubDate;
                }

                if (localName.equalsIgnoreCase(CATEGORY)) {
                    currentState = states.item_category;
                }

                if (localName.equalsIgnoreCase(ENCLOSURE)) {
                    article.setEnclosure(attributes.getValue("url"));
                }

                if (localName.equalsIgnoreCase(GUID)) {
                    currentState = states.item_guid;
                }

                if (localName.equalsIgnoreCase(DOC_ID)) {
                    currentState = states.item_doc_id;
                }
                break;
        }
    }

	@Override
	public void endDocument() throws SAXException {
		listener.finish();
	}

	@Override
	public void startDocument() throws SAXException {
		super.startDocument();
		currentState = states.none;
	}
	
}
