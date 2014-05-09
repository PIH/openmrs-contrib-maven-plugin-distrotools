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
import org.openmrs.maven.plugins.distrotools.MetadataConfig;
import org.openmrs.maven.plugins.distrotools.util.XmlUtils;

import javax.xml.parsers.DocumentBuilder;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

/**
 * Goal which generates two things from the distribution's metadata configuration
 *  1. A Java source file of constants (Metadata.java)
 *  2. A properties file for filtering of resources containing metadata references (metadata.properties)
 */
@Mojo(name = "generate-metadata-sources", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public class GenerateMetadataSourcesMojo extends AbstractMojo {

	// Metadata configuration directory
	@Parameter(property = "metadataDirectory", required = true, defaultValue = "src/main/distro/metadata")
	private File metadataDirectory;

	@Parameter(property = "outputDirectory", required = true, defaultValue = "${project.build.directory}/generated-sources/distro")
	private File outputDirectory;

	@Parameter(property = "outputPackage", required = true)
	private String outputPackage;

	@Parameter(property = "outputFilterFile", required = true, defaultValue = "${project.build.directory}/metadata.properties")
	private File outputFilterFile;

	// Name of the generated source file
	private static final String GEN_SOURCE_NAME = "Metadata.java";

	/**
	 * Executes the generate goal
	 * @throws org.apache.maven.plugin.MojoExecutionException if an error occurs
	 */
	public void execute() throws MojoExecutionException, MojoFailureException {
		if (!metadataDirectory.exists() || !metadataDirectory.isDirectory()) {
			throw new MojoFailureException("Metadata configuration directory " + metadataDirectory + " doesn't exist or is not a directory");
		}

		try {
			// Instantiate some required DOM tools
			DocumentBuilder documentBuilder = XmlUtils.createBuilder();

			// Load provided distribution configuration
			MetadataConfig distroConfig = MetadataConfig.loadFromDirectory(metadataDirectory, documentBuilder, getLog());

			generateMetadataSource(distroConfig, outputDirectory, outputPackage);

			generateMetadataFilter(distroConfig, outputFilterFile);
		}
		catch (Exception ex) {
			throw new MojoExecutionException("Unexpected error", ex);
		}
	}

	/**
	 * Generates the metadata source file
	 * @param config the metadata configuration
	 * @param directory the output directory
	 * @param pkgName the output package name
	 */
	protected void generateMetadataSource(MetadataConfig config, File directory, String pkgName) throws IOException {
		// Load template for M.java
		String template = IOUtils.toString(getClass().getClassLoader().getResourceAsStream(GEN_SOURCE_NAME + ".template"));
		template = template.replace("{PACKAGE}", pkgName);

		StringBuilder sb = new StringBuilder();
		renderReferencesAsClasses(sb, config);
		template = template.replace("{REFERENCES}", sb.toString());

		String outputPath = directory.getPath() + File.separator + pkgName.replace(".", File.separator) + File.separator + GEN_SOURCE_NAME;
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

	/**
	 * Generates the metadata filter file
	 * @param config the metadata configuration
	 * @param file the output filter file
	 */
	protected void generateMetadataFilter(MetadataConfig config, File file) throws IOException {
		FileWriter writer = new FileWriter(file);

		for (Map.Entry<Object, Object> property : config.toProperties("metadata.").entrySet()) {
			writer.write(property.getKey() + "=" + property.getValue() + "\n");
		}

		writer.close();

		getLog().info("Generated " + file.getPath());
	}

	/**
	 * Renders metadata references as constant classes organized by type
	 * @param sb the string builder
	 * @param config the metadata configuration
	 * @return the code
	 */
	protected void renderReferencesAsClasses(StringBuilder sb, MetadataConfig config) {
		for (String type : config.getConfiguredTypes()) {
			renderTypeReferencesAsClass(sb, type, config.getReferencesByType(type));
		}
	}

	/**
	 * Renders all references for the given type as class of constants
	 * @param sb the string builder
	 * @param type the type name
	 * @param references the type reference map
	 */
	protected void renderTypeReferencesAsClass(StringBuilder sb, String type, Map<String, String> references) {
		sb.append("\n\tpublic static class ");
		sb.append(type);
		sb.append(" {\n");

		for (Map.Entry<String, String> entry : references.entrySet()) {
			renderReferenceAsConstant(sb, entry.getKey(), entry.getValue());
		}

		sb.append("\t}\n");
	}

	/**
	 * Renders a single reference as a constant
	 * @param sb the string builder
	 * @param key the reference key
	 * @param uuid the reference UUID
	 */
	protected void renderReferenceAsConstant(StringBuilder sb, String key, String uuid) {
		sb.append("\t\tpublic static final String ");
		sb.append(key);
		sb.append(" = \"");
		sb.append(uuid);
		sb.append("\";\n");
	}
}