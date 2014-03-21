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

package org.openmrs.maven.plugins.distrotools.mojo;

import org.apache.commons.io.IOUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.openmrs.maven.plugins.distrotools.DistroToolsUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Goal which generates a metadata source file of constants from XML resources
 */
@Mojo(name = "generate-metadata-sources", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public class GenerateMetadataSourcesMojo extends AbstractMojo {

	// Directory of form files
	@Parameter(property = "metadataDirectory", required = true, defaultValue = "src/main/metadata")
	private File metadataDirectory;

	@Parameter(property = "outputDirectory", required = true, defaultValue = "${project.build.directory}/generated-sources/metadata")
	private File outputDirectory;

	@Parameter(property = "outputPackage", required = true)
	private String outputPackage;

	/**
	 * Executes the generate goal
	 * @throws org.apache.maven.plugin.MojoExecutionException if an error occurs
	 */
	public void execute() throws MojoExecutionException, MojoFailureException {
		if (!metadataDirectory.exists() || !metadataDirectory.isDirectory()) {
			throw new MojoFailureException("Metadata directory '" + metadataDirectory + "' doesn't exist or is not a directory");
		}

		try {
			// Instantiate some required DOM tools
			DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();

			// Load template for M.java
			String template = IOUtils.toString(getClass().getClassLoader().getResourceAsStream("M.java.template"));

			template = template.replace("{PACKAGE}", outputPackage);

			File conceptsFile = new File(metadataDirectory.getPath() + File.separator + "concepts.xml");

			if (conceptsFile.exists()) {
				Map<String, String> constants = loadMetadataItems(conceptsFile, documentBuilder);
				template = template.replace("{CONCEPTS}", renderItemsAsConstants(constants));
				getLog().info("Processed '" + conceptsFile.getPath() + "'");
			}

			String outputPath = outputDirectory.getPath() + File.separator + outputPackage.replace(".", File.separator) + File.separator + "M.java";
			File outputFile = new File(outputPath);

			// Make sub-folders if necessary
			if (!outputFile.getParentFile().exists()) {
				outputFile.getParentFile().mkdirs();
			}

			FileWriter writer = new FileWriter(outputFile);
			IOUtils.write(template, writer);
			writer.close();

			getLog().info("Generated '" + outputFile.getPath() + "'");
		}
		catch (Exception ex) {
			throw new MojoExecutionException("Unexpected error", ex);
		}
	}

	/**
	 * Loads metadata items from the given file
	 * @param xmlFile the XML file
	 * @param documentBuilder the DOM document builder
	 * @return the metadata items
	 */
	protected Map<String, String> loadMetadataItems(File xmlFile, DocumentBuilder documentBuilder) throws IOException, SAXException {
		Map<String, String> constants = new LinkedHashMap<String, String>();
		Document document = documentBuilder.parse(xmlFile);
		Node itemsNode = DistroToolsUtils.findChildNode(document, "items");

		for (Node itemNode : DistroToolsUtils.findChildNodes(itemsNode, "item")) {
			Node keyNode = itemNode.getAttributes().getNamedItem("key");
			Node uuidNode = itemNode.getAttributes().getNamedItem("uuid");
			constants.put(keyNode.getTextContent(), uuidNode.getTextContent());
		}

		return constants;
	}

	/**
	 * Renders metadata items as code constants
	 * @param constants the constants
	 * @return the code
	 */
	protected String renderItemsAsConstants(Map<String, String> constants) {
		StringBuilder sb = new StringBuilder();

		for (Map.Entry<String, String> entry : constants.entrySet()) {
			sb.append("\t\tpublic static final String ");
			sb.append(entry.getKey());
			sb.append(" = \"");
			sb.append(entry.getValue());
			sb.append("\";\n");
		}

		return sb.toString();
	}
}