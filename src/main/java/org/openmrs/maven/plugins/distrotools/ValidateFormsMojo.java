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

import org.apache.commons.io.IOUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

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
	public void execute() throws MojoExecutionException {
		if (!formsDirectory.exists()) {
			throw new MojoExecutionException("Forms directory '" + formsDirectory + "' doesn't exist");
		}
		if (!formsDirectory.isDirectory()) {
			throw new MojoExecutionException("Forms directory '" + formsDirectory + "' is not a directory");
		}

		List<File> formfiles = getFormFilesInDirectory(formsDirectory, formsExtension);

		getLog().info("Found " + formfiles.size() + " form files");

		try {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder documentBuilder = dbf.newDocumentBuilder();

			for (File formFile : formfiles) {
				validateFormFile(formFile, documentBuilder);
			}
		}
		catch (ParserConfigurationException ex) {
			throw new MojoExecutionException("Unable to create document builder", ex);
		}
	}

	/**
	 * Validates the given form file
	 * @param formFile the form file
	 * @param documentBuilder the DOM document builder
	 */
	protected void validateFormFile(File formFile, DocumentBuilder documentBuilder) throws MojoExecutionException {
		try {
			String xml = IOUtils.toString(new FileReader(formFile));

			Document document = documentBuilder.parse(new InputSource(new StringReader(xml)));
		}
		catch (IOException ex) {
			throw new MojoExecutionException("Unable to load '" + formFile.getPath() + "'", ex);
		}
		catch (SAXException ex) {
			throw new MojoExecutionException("Unable to parse '" + formFile.getPath() + "'", ex);
		}

		getLog().info("Validated form file '" + formFile.getPath() + "'");
	}

	/**
	 * Gets all of the files in the given directory with the given extension
	 * @param directory the directory
	 * @param extension the extension
	 * @return the files
	 */
	protected List<File> getFormFilesInDirectory(File directory, String extension) {
		List<File> files = new ArrayList<File>();
		getFormFilesInDirectoryRecursive(directory, extension, files);
		return files;
	}

	/**
	 * Recursively fetches form files from the given directory and its sub-directories
	 * @param directory the directory
	 * @param extension the extension
	 * @param files the files found so far
	 */
	protected void getFormFilesInDirectoryRecursive(File directory, String extension, List<File> files) {
		for (File child : directory.listFiles()) {
			if (child.getName().endsWith(extension)) {
				files.add(child);
			}
			else if (child.isDirectory()) {
				getFormFilesInDirectoryRecursive(child, extension, files);
			}
		}
	}
}