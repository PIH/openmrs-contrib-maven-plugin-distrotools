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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.maven.plugins.distrotools.util.XmlUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import static org.hamcrest.CoreMatchers.*;

/**
 * Tests for {@link org.openmrs.maven.plugins.distrotools.util.FileUtils}
 */
public class XmlUtilsTest {

	private DocumentBuilder documentBuilder;

	private Document testDocument;

	/**
	 * Setup each test
	 */
	@Before
	public void setup() throws Exception {
		this.documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		this.testDocument = documentBuilder.parse(getClass().getClassLoader().getResourceAsStream("test-metadata.xml"));
	}

	/**
	 * @see org.openmrs.maven.plugins.distrotools.util.XmlUtils#stringToDocument(String, javax.xml.parsers.DocumentBuilder)
	 */
	@Test
	public void stringToDocument_shouldParseStringIntoDocument() throws Exception {
		String xml = "<items><item key=\"YES\" /><item /></items>";
		Document doc = XmlUtils.stringToDocument(xml, documentBuilder);

		Assert.assertThat(doc.getFirstChild().getNodeName(), is("items"));
		Assert.assertThat(doc.getFirstChild().getFirstChild().getNodeName(), is("item"));
	}

	/**
	 * @see XmlUtils#findFirstChild(org.w3c.dom.Node, String)
	 */
	@Test
	public void findChildNode_shouldReturnFirstNodeWithName() throws Exception {
		Assert.assertThat(XmlUtils.findFirstChild(testDocument, "items").getNodeName(), is("items"));
		Assert.assertThat(XmlUtils.findFirstChild(testDocument, "xxx"), nullValue());
	}

	/**
	 * @see XmlUtils#findAllChildren(org.w3c.dom.Node, String)
	 */
	@Test
	public void findChildNodes_shouldReturnAllNodesWithName() throws Exception {
		Node itemsNode = XmlUtils.findFirstChild(testDocument, "items");
		Assert.assertThat(XmlUtils.findAllChildren(itemsNode, "item").size(), is(2));
		Assert.assertThat(XmlUtils.findAllChildren(itemsNode, "xxx").size(), is(0));
	}
}