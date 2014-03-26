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
import java.util.Set;

/**
 * Describes a distribution's metadata configuration
 */
public class MetadataConfig {

	protected Map<String, Map<String, String>> referencesByType = new LinkedHashMap<String, Map<String, String>>();

	protected static final Map<String, String> typeConfigs = new LinkedHashMap<String, String>();

	static {
		typeConfigs.put("Concept", "concepts.xml");
		typeConfigs.put("Form", "forms.xml");
		typeConfigs.put("IdentifierType", "identifierTypes.xml");
		typeConfigs.put("Program", "programs.xml");
	}

	/**
	 * Loads a metadata configuration from the given directory
	 * @param directory the directory
	 * @param documentBuilder the DOM document builder
	 * @param log the log
	 * @return the configuration
	 */
	public static MetadataConfig loadFromDirectory(File directory, DocumentBuilder documentBuilder, Log log) throws IOException, SAXException {
		MetadataConfig config = new MetadataConfig();

		for (Map.Entry<String, String> typeConfig : typeConfigs.entrySet()) {
			String typeName = typeConfig.getKey();
			String fileName = typeConfig.getValue();
			File typeFile = new File(directory.getPath() + File.separator + fileName);

			if (typeFile.exists()) {
				Map<String, String> typeReferences = loadReferences(typeFile, documentBuilder);
				config.referencesByType.put(typeName, typeReferences);

				log.info("Parsed " + typeReferences.size() + " references from " + typeFile.getAbsolutePath());
			}
			else {
				config.referencesByType.put(typeName, new LinkedHashMap<String, String>());

				log.info("File " + typeFile.getAbsolutePath() + " not provided");
			}
		}

		return config;
	}

	/**
	 * Loads metadata references from the given file
	 * @param xmlFile the XML file
	 * @param documentBuilder the DOM document builder
	 * @return the reference map
	 */
	protected static Map<String, String> loadReferences(File xmlFile, DocumentBuilder documentBuilder) throws IOException, SAXException {
		Map<String, String> references = new LinkedHashMap<String, String>();

		Document document = documentBuilder.parse(xmlFile);
		Node itemsNode = XmlUtils.findFirstChild(document, "refs");

		for (Node itemNode : XmlUtils.findAllChildren(itemsNode, "ref")) {
			Node keyNode = itemNode.getAttributes().getNamedItem("key");
			Node uuidNode = itemNode.getAttributes().getNamedItem("uuid");
			references.put(keyNode.getTextContent(), uuidNode.getTextContent());
		}

		return references;
	}

	/**
	 * Gets all of the supported type names
	 * @return the type names
	 */
	public Set<String> getSupportedTypes() {
		return typeConfigs.keySet();
	}

	/**
	 * Gets references for the given type
	 * @param type the type
	 * @return the reference map
	 */
	public Map<String, String> getReferencesByType(String type) {
		return referencesByType.get(type);
	}

	/**
	 * Returns a flattened properties style list of all metadata references
	 * @return the properties
	 */
	public Map<String, String> toProperties() {
		Map<String, String> properties = new LinkedHashMap<String, String>();

		for (String type : getSupportedTypes()) {
			String propertyPrefix = "metadata." + type.toLowerCase() + ".";

			for (Map.Entry<String, String> typeRef : getReferencesByType(type).entrySet()) {
				properties.put(propertyPrefix + typeRef.getKey(), typeRef.getValue());
			}
		}

		return properties;
	}
}