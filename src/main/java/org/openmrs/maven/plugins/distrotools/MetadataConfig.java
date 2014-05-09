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
import org.openmrs.maven.plugins.distrotools.util.FileUtils;
import org.openmrs.maven.plugins.distrotools.util.XmlUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;

/**
 * Describes a distribution's metadata configuration
 */
public class MetadataConfig {

	protected Map<String, Map<String, String>> referencesByType = new TreeMap<String, Map<String, String>>();

	/**
	 * Creates an empty metadata config
	 */
	public MetadataConfig() {
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

		List<File> configFiles = FileUtils.getFilesInDirectory(directory, "xml");

		for (File configFile : configFiles) {
			int loaded = config.loadReferences(configFile, documentBuilder);
			log.info("Parsed " + loaded + " references from " + configFile.getAbsolutePath());
		}

		return config;
	}

	/**
	 * Loads metadata references from the given file
	 * @param configFile the XML config file
	 * @param documentBuilder the DOM document builder
	 * @return the number of references loaded
	 */
	protected int loadReferences(File configFile, DocumentBuilder documentBuilder) throws IOException, SAXException {
		Document document = documentBuilder.parse(configFile);
		Node refsNode = XmlUtils.findFirstChild(document, "refs");
		String type = XmlUtils.findAttribute(refsNode, "type");
		List<Node> refNodes = XmlUtils.findAllChildren(refsNode, "ref");

		for (Node itemNode : refNodes) {
			Node keyNode = itemNode.getAttributes().getNamedItem("key");
			Node uuidNode = itemNode.getAttributes().getNamedItem("uuid");
			addReference(type, keyNode.getTextContent(), uuidNode.getTextContent());
		}
		return refNodes.size();
	}

	/**
	 * Adds a reference
	 * @param type the type
	 * @param key the key
	 * @param uuid the UUID
	 */
	public void addReference(String type, String key, String uuid) {
		Map<String, String> references = referencesByType.get(type);

		if (references == null) {
			references = new LinkedHashMap<String, String>();
			referencesByType.put(type, references);
		}

		references.put(key, uuid);
	}

	/**
	 * Gets all configured types
	 * @return the types
	 */
	public Collection<String> getConfiguredTypes() {
		return referencesByType.keySet();
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
	 * Returns a flattened properties list of all metadata references
	 * @return the properties
	 */
	public Properties toProperties(String prefix) {
		Properties properties = new Properties();

		for (String type : getConfiguredTypes()) {
			String propertyPrefix = prefix + type.toLowerCase() + ".";

			for (Map.Entry<String, String> typeRef : getReferencesByType(type).entrySet()) {
				properties.put(propertyPrefix + typeRef.getKey(), typeRef.getValue());
			}
		}

		return properties;
	}
}