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

package org.openmrs.maven.plugins.distrotools.util;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * File system utility methods
 */
public class FileUtils {

	/**
	 * Gets all of the files in the given directory with the given extension
	 * @param directory the directory
	 * @param extension the extension
	 * @return the files
	 */
	public static List<File> getFilesInDirectory(File directory, String extension) {
		List<File> files = new ArrayList<File>();
		getFilesInDirectoryRecursive(directory, extension, files);
		return files;
	}

	/**
	 * Recursively fetches form files from the given directory and its sub-directories
	 * @param directory the directory
	 * @param extension the extension
	 * @param files the files found so far
	 */
	private static void getFilesInDirectoryRecursive(File directory, String extension, List<File> files) {
		for (File child : directory.listFiles()) {
			if (child.getName().endsWith(extension)) {
				files.add(child);
			}
			else if (child.isDirectory()) {
				getFilesInDirectoryRecursive(child, extension, files);
			}
		}
	}
}