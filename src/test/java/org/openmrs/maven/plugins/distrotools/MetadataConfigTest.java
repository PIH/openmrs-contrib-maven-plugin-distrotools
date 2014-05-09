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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;
import java.util.Properties;

import static org.hamcrest.Matchers.*;

/**
 * Tests for {@link MetadataConfig}
 */
public class MetadataConfigTest {

	private MetadataConfig config;

	/**
	 * Setup each test
	 */
	@Before
	public void setup() {
		config = new MetadataConfig();
		config.addReference("Program", "HIV", "AAAA");
		config.addReference("Program", "TB", "BBBB");
		config.addReference("Concept", "YES", "CCCC");
		config.addReference("EncounterType", "INIT", "DDDD");
	}

	/**
	 * @see MetadataConfig#getConfiguredTypes()
	 */
	@Test
	public void getConfiguredTypes_shouldGetConfiguredTypesAlphabetically() {
		Assert.assertThat(config.getConfiguredTypes(), contains("Concept", "EncounterType", "Program"));
	}

	/**
	 * @see MetadataConfig#getReferencesByType(String)
	 */
	@Test
	public void getReferencesByType_shouldGetReferencesForType() {
		Map<String, String> refs = config.getReferencesByType("Program");
		Assert.assertThat(refs, hasEntry("HIV", "AAAA"));
		Assert.assertThat(refs, hasEntry("TB", "BBBB"));
	}

	/**
	 * @see MetadataConfig#toProperties(String)
	 */
	@Test
	public void toProperties_shouldGetReferencesAsProperties() {
		Properties properties = config.toProperties("test.");
		Assert.assertThat(properties, hasEntry((Object) "test.program.HIV", (Object) "AAAA"));
		Assert.assertThat(properties, hasEntry((Object) "test.program.TB", (Object) "BBBB"));
		Assert.assertThat(properties, hasEntry((Object) "test.concept.YES", (Object) "CCCC"));
		Assert.assertThat(properties, hasEntry((Object) "test.encountertype.INIT", (Object) "DDDD"));
	}
}