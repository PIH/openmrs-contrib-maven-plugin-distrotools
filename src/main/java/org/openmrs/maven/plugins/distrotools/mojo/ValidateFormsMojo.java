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
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Goal which validates HFE form files
 */
@Mojo(name = "validate-forms", defaultPhase = LifecyclePhase.VALIDATE)
public class ValidateFormsMojo extends AbstractMojo {

	// Directory of form files
	@Parameter(property = "formsDirectory", required = true)
	private File formsDirectory;

	// File extension of form files
	@Parameter(property = "formsExtension", required = true, defaultValue = "html")
	private String formsExtension;

	/**
	 * Executes the validate goal
	 * @throws MojoExecutionException if an error occurs
	 */
	public void execute() throws MojoExecutionException, MojoFailureException {
		if (!formsDirectory.exists()) {
			throw new MojoFailureException("Forms directory '" + formsDirectory + "' doesn't exist");
		}
		if (!formsDirectory.isDirectory()) {
			throw new MojoFailureException("Forms directory '" + formsDirectory + "' is not a directory");
		}

		List<File> formfiles = DistroToolsUtils.getFormFilesInDirectory(formsDirectory, formsExtension);

		getLog().info("Found " + formfiles.size() + " form files");

		try {
			// Instantiate some required DOM tools
			DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			Transformer documentTransformer = TransformerFactory.newInstance().newTransformer();
			documentTransformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
			documentTransformer.setOutputProperty(OutputKeys.INDENT, "yes");
			documentTransformer.setOutputProperty(OutputKeys.METHOD, "xml");
			documentTransformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");

			for (File formFile : formfiles) {
				validateFormFile(formFile, documentBuilder, documentTransformer);
			}
		}
		catch (MojoFailureException ex) {
			throw ex;
		}
		catch (Exception ex) {
			throw new MojoExecutionException("Unexpected error", ex);
		}
	}

	/**
	 * Validates the given form file
	 * @param formFile the form file
	 * @param documentBuilder the DOM document builder
	 * @param documentTransformer the DOM document transformer
	 */
	protected void validateFormFile(File formFile, DocumentBuilder documentBuilder, Transformer documentTransformer) throws MojoFailureException {
		try {
			String xml = IOUtils.toString(new FileReader(formFile));

			// Validate basic structure
			Document form = DistroToolsUtils.stringToDocument(xml, documentBuilder);
			Node htmlformNode = DistroToolsUtils.findChildNode(form, "htmlform");
			if (htmlformNode == null) {
				throw new MojoFailureException("Form file '" + formFile.getPath() + "' has no root <htmlform> node");
			}

			xml = stripComments(xml);

			try {
				xml = applyMacros(xml, documentBuilder, documentTransformer);
			}
			catch (Exception ex) {
				throw new MojoFailureException("Unable to apply macros in '" + formFile.getPath() + "'", ex);
			}
		}
		catch (IOException ex) {
			throw new MojoFailureException("Unable to load '" + formFile.getPath() + "'", ex);
		}
		catch (SAXException ex) {
			throw new MojoFailureException("Unable to parse '" + formFile.getPath() + "'", ex);
		}

		getLog().info("Validated form file '" + formFile.getPath() + "'");
	}

	/**
	 * Strips comments from the given form XML
	 * @param xml the form XML
	 * @return the form XML with comments stripped
	 */
	public static String stripComments(String xml) {
		String regex = "<!\\s*--.*?--\\s*>"; // this is the regEx for html comment tag <!-- .* -->
		Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
		Matcher matcher = pattern.matcher(xml);
		xml = matcher.replaceAll("");
		return xml;
	}

	/**
	 * Applies macros in the given form XML (if there are any)
	 * @param xml the form XML
	 * @param documentBuilder the DOM document builder
	 * @param documentTransformer the DOM document transformer
	 * @return the form XML with macros applied
	 */
	protected static String applyMacros(String xml, DocumentBuilder documentBuilder, Transformer documentTransformer) throws IOException, TransformerException, SAXException {
		Document form = DistroToolsUtils.stringToDocument(xml, documentBuilder);
		Node htmlformNode = DistroToolsUtils.findChildNode(form, "htmlform");
		Node macrosNode = DistroToolsUtils.findChildNode(htmlformNode, "macros");

		// If there are no macros defined, we just return the original document
		if (macrosNode == null) {
			return xml;
		}

		// Parse macros
		Properties macros = new Properties();
		String macrosText = macrosNode.getTextContent();
		if (macrosText != null) {
			macros.load(new ByteArrayInputStream(macrosText.getBytes()));
		}

		// Remove the macros node
		htmlformNode.removeChild(macrosNode);

		// Switch back to string so we can use string utilities to substitute
		xml = DistroToolsUtils.documentToString(form, documentTransformer);

		// substitute any macros we found
		for (Object temp : macros.keySet()) {
			String key = (String) temp;
			String value = macros.getProperty(key, "");
			xml = xml.replace("$" + key, value);
		}

		return xml;
	}
}