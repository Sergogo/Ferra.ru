package ru.ferra.common.utils.loaders;

import org.apache.xerces.parsers.AbstractSAXParser;
import org.cyberneko.html.HTMLConfiguration;

public class HTMLSAXParser extends AbstractSAXParser {

	public HTMLSAXParser() {
		super(new HTMLConfiguration());
	}
}