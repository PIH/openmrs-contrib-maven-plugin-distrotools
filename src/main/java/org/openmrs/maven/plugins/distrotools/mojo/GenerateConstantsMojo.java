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
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;
import org.openmrs.maven.plugins.distrotools.util.FileUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Goal which generates two things from the distribution's constants configuration
 *  1. One or more java source files containing constants
 *  2. A properties file for filtering of resources containing constant references (constants.properties)
 */
@Mojo(name = "generate-constants", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public class GenerateConstantsMojo extends AbstractMojo {

	// Metadata configuration directory
	@Parameter(property = "metadataDirectory", required = true, defaultValue = "src/main/distro/constants")
	private File metadataDirectory;

	@Parameter(property = "outputDirectory", required = true, defaultValue = "${project.build.directory}/generated-sources/distro")
	private File outputDirectory;

	@Parameter(property = "outputPackage", required = true)
	private String outputPackage;

	@Parameter(property = "outputFilterFile", required = true, defaultValue = "${project.build.directory}/constants.properties")
	private File outputFilterFile;

	/**
	 * Executes the generate goal
	 * @throws MojoExecutionException if an error occurs
	 */
	public void execute() throws MojoExecutionException, MojoFailureException {
		if (!metadataDirectory.exists() || !metadataDirectory.isDirectory()) {
			throw new MojoFailureException("Metadata configuration directory " + metadataDirectory + " doesn't exist or is not a directory");
		}
		try {
			List<ConstantClass> constantClasses = loadFromDirectory(metadataDirectory, getLog());
			generateSourceFiles(constantClasses, outputDirectory, outputPackage);
			generateMetadataFilter(constantClasses, outputFilterFile);
		}
		catch (MojoFailureException ex) {
			throw ex;
		}
		catch (Exception ex) {
			throw new MojoExecutionException("Unexpected error", ex);
		}
	}

	/**
	 * @param directory the directory
	 * @param log the log
	 * @return List of ConstantClass that represent each individual top-level class file to generate
	 */
	public static List<ConstantClass> loadFromDirectory(File directory, Log log) throws MojoFailureException {
		List<ConstantClass> ret = new ArrayList<ConstantClass>();
		try {
			List<File> configFiles = FileUtils.getFilesInDirectory(directory, "json");
			log.info("Found " + configFiles + " constant files to process");
			ObjectMapper mapper = new ObjectMapper();
			for (File configFile : configFiles) {
				log.info("In constant file " + configFile.getName());
				ObjectNode node = mapper.readValue(configFile, ObjectNode.class);
				// Each top level node in a json file becomes a new class with the name of that node
				Iterator<String> fieldNames = node.getFieldNames();
				while (fieldNames.hasNext()) {
					String className = fieldNames.next();
					log.info("Getting information to produce class " + className);
					ret.add(createConstantClass(className, node.get(className)));
				}
			}
		}
		catch (Exception e) {
			throw new MojoFailureException("An error occurred loading constants", e);
		}
		return ret;
	}

	/**
	 * @param className the name of the class to create
	 * @param node the JsonNode representing the details of the class
	 * @return the Constant class that represents the passed information
	 */
	protected static ConstantClass createConstantClass(String className, JsonNode node) {
		ConstantClass cc = new ConstantClass();
		cc.setClassName(className);

		Iterator<String> fieldNameIterator = node.getFieldNames();
		while (fieldNameIterator.hasNext()) {
			String key = fieldNameIterator.next();
			JsonNode jsonNode = node.get(key);

			// This allows us to have nested subclasses
			if (jsonNode.isObject()) {
				cc.getSubclasses().add(createConstantClass(key, jsonNode));
			}
			else {
				Object val = null;
				// This allows us to have list constants
				if (jsonNode.isArray()) {
					ArrayNode arrayNode = (ArrayNode) jsonNode;
					Iterator<JsonNode> listIterator = arrayNode.getElements();
					List l = new ArrayList();
					while (listIterator.hasNext()) {
						JsonNode listElement = listIterator.next();
						l.add(listElement.asText());
					}
					val = l;
				}
				// This allows us to have scalar constants
				else {
					val = jsonNode.asText();
				}
				cc.getConstantValues().put(key, val);  // TODO: Maybe support other data types, like numbers?
			}
		}
		return cc;
	}

	/**
	 * Generates the constant source files
	 * @param constantClasses the List of ConstantClasses representing the source files
	 * @param directory the output directory
	 * @param pkgName the output package name
	 */
	protected void generateSourceFiles(List<ConstantClass> constantClasses, File directory, String pkgName) throws IOException {
		for (ConstantClass cc : constantClasses) {

			String template = IOUtils.toString(getClass().getClassLoader().getResourceAsStream("Constant.java.template"));
			template = template.replace("{PACKAGE}", pkgName);
			StringBuilder sb = new StringBuilder();
			generateAndAppendClassBody(sb, cc, 0);
			template = template.replace("{CLASS_BODY}", sb.toString());

			String outputPath = directory.getPath() + File.separator + pkgName.replace(".", File.separator) + File.separator + cc.getClassName() + ".java";
			File outputFile = new File(outputPath);

			// Make sub-folders if necessary
			if (!outputFile.getParentFile().exists()) {
				outputFile.getParentFile().mkdirs();
			}

			FileWriter writer = new FileWriter(outputFile);
			IOUtils.write(template, writer);
			writer.close();

			getLog().info("Generated " + outputFile.getPath());
		}
	}

	protected void generateAndAppendClassBody(StringBuilder sb, ConstantClass cc, int level) {
		String classIndent = indent(level);
		String memberIndent = classIndent+indent(1);
		sb.append(classIndent).append("public ").append(level == 0 ? "" : "static ").append("class ");
		sb.append(cc.getClassName()).append(" {").append(level == 0 ? newLine() : "");
		for (ConstantClass subclass : cc.getSubclasses()) {
			sb.append(newLine());
			generateAndAppendClassBody(sb, subclass, level + 1);
			sb.append(newLine());
		}
		sb.append(newLine());
		for (Map.Entry<String, Object> constant : cc.getConstantValues().entrySet()) {
			sb.append(memberIndent).append("public static final ");
			if (constant.getValue() instanceof List) {
				List elements = (List)constant.getValue();
				sb.append("String[] ").append(constant.getKey()).append(" = { ");
				for (int i=0; i<elements.size(); i++) {
					sb.append(i == 0 ? "" : ", ");
					sb.append(elements.get(i));
				}
				sb.append(" };");
			}
			else {
				sb.append("String ").append(constant.getKey()).append(" = ").append(quoted(constant.getValue())).append(";");
			}
			sb.append(newLine());
		}
		sb.append(classIndent).append("}");
	}

	protected String quoted(Object input) {
		return "\""+input+"\"";
	}

	protected String indent(int level) {
		StringBuilder sb = new StringBuilder();
		for (int i=0; i<level; i++) {
			sb.append("\t");
		}
		return sb.toString();
	}

	protected String newLine() {
		return "\n";
	}

	/**
	 * Generates the metadata filter file
	 * @param constantClasses the List of ConstantClasses representing the source files
	 * @param file the output filter file
	 */
	protected void generateMetadataFilter(List<ConstantClass> constantClasses, File file) throws IOException {
		FileWriter writer = null;
		try {
			Map<String, String> properties = new LinkedHashMap<String, String>();
			for (ConstantClass cc : constantClasses) {
				loadFilterProperties(properties, cc.getClassName(), cc.getClassName(), cc);
			}
			getLog().info("Loaded " + properties.size() + " constant properties");

			writer = new FileWriter(file);
			for (String key : properties.keySet()) {
				writer.write(key + "=" + properties.get(key) + "\n");
			}
		}
		finally {
			IOUtils.closeQuietly(writer);
		}
	}

	protected void loadFilterProperties(Map<String, String> properties, String topClassName, String currentClassName, ConstantClass constantClass) throws IOException {
		for (ConstantClass subclass : constantClass.getSubclasses()) {
			loadFilterProperties(properties, topClassName, currentClassName + "." + subclass.getClassName(), subclass);
		}
		for (Map.Entry<String, Object> e : constantClass.getConstantValues().entrySet()) {
			String value = e.getValue().toString();
			if (e.getValue() instanceof List) {
				StringBuilder sb = new StringBuilder();
				List l = (List)e.getValue();
				for (Object o : l) {
					String element = o.toString();
					if (properties.containsKey(element)) {
						element = properties.get(element);
					}
					else if (properties.containsKey(topClassName + "." + element)) {
						element = properties.get(topClassName + "." + element);
					}
					sb.append(sb.length() == 0 ? "" : ",").append(element);
				}
				value = sb.toString();
			}
			properties.put(currentClassName + "." + e.getKey(), value);
		}
	}

	private static class ConstantClass {

		private String className;
		private List<ConstantClass> subclasses = new ArrayList<ConstantClass>();
		private Map<String, Object> constantValues = new LinkedHashMap<String, Object>();

		public ConstantClass() {}

		public String getClassName() {
			return className;
		}

		public void setClassName(String className) {
			this.className = className;
		}

		public List<ConstantClass> getSubclasses() {
			return subclasses;
		}

		public Map<String, Object> getConstantValues() {
			return constantValues;
		}
	}
}