/**
 * The contents of this file are subject to the OpenMRS Public License
 * Version 1.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://license.openmrs.org
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 *
 * Copyright (C) OpenMRS, LLC.  All Rights Reserved.
 */

package org.openmrs.maven.plugins.distrotools;

import org.apache.maven.plugin.logging.Log;
import org.openmrs.maven.plugins.distrotools.util.XmlUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Describes a distribution's metadata configuration
 */
public class DistroMetadataConfig {

	protected Map<String, String> concepts;

	protected Map<String, String> forms;

	/**
	 * Loads a metadata configuration from the given directory
	 * @param directory the directory
	 * @param documentBuilder the DOM document builder
	 * @param log the log
	 * @return the configuration
	 */
	public static DistroMetadataConfig loadFromDirectory(File directory, DocumentBuilder documentBuilder, Log log) throws IOException, SAXException {
		DistroMetadataConfig config = new DistroMetadataConfig();

		config.concepts = loadMetadataItems(new File(directory.getPath() + File.separator + "concepts.xml"), documentBuilder, log);
		config.forms = loadMetadataItems(new File(directory.getPath() + File.separator + "forms.xml"), documentBuilder, log);

		return config;
	}

	/**
	 * Loads metadata items from the given file. If file doesn't exist, then nothing is loaded
	 * @param xmlFile the XML file
	 * @param documentBuilder the DOM document builder
	 * @param log the log
	 * @return the metadata items
	 */
	protected static Map<String, String> loadMetadataItems(File xmlFile, DocumentBuilder documentBuilder, Log log) throws IOException, SAXException {
		Map<String, String> constants = new LinkedHashMap<String, String>();

		if (xmlFile.exists()) {
			Document document = documentBuilder.parse(xmlFile);
			Node itemsNode = XmlUtils.findFirstChild(document, "items");

			for (Node itemNode : XmlUtils.findAllChildren(itemsNode, "item")) {
				Node keyNode = itemNode.getAttributes().getNamedItem("key");
				Node uuidNode = itemNode.getAttributes().getNamedItem("uuid");
				constants.put(keyNode.getTextContent(), uuidNode.getTextContent());
			}

			log.info("Parsed " + constants.size() + " items from " + xmlFile.getAbsolutePath());
		}
		else {
			log.info("File " + xmlFile.getAbsolutePath() + " not provided");
		}

		return constants;
	}

	/**
	 * Gets the configured concepts (key -> uuid)
	 * @return the concepts
	 */
	public Map<String, String> getConcepts() {
		return concepts;
	}

	/**
	 * Gets the configured forms (key -> uuid)
	 * @return the forms
	 */
	public Map<String, String> getForms() {
		return forms;
	}
}