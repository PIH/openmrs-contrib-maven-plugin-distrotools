OpenMRS Distro Tools Maven Plugin
=================================

Overview
--------
Useful build tasks for OpenMRS distributions. So far that is just one forms-related goal, but it's early days.

Usage
-----
Add the plugin to your module's main pom.xml:

    <pluginManagement>
      <plugins>
        <plugin>
          <groupId>org.openmrs.maven.plugins</groupId>
          <artifactId>distrotools-maven-plugin</artifactId>
          <version>0.1</version>
        </plugin>
      </plugins>
    </pluginManagement>

Goals
-----
### validate-forms
Performs basic validation (DOM validation, macro application) all HFE form files in a specified directory.

 * _formsDirectory_ the directory containing the form files
     * Required: yes
     * Type: File
 * _formsExtension_ the file extension used for form files
     * Required: no
     * Type: String
     * Default: html

Example:

    <plugin>
      <groupId>org.openmrs.maven.plugins</groupId>
      <artifactId>distrotools-maven-plugin</artifactId>
      <executions>
        <execution>
          <phase>validate</phase>
          <goals>
            <goal>validate-forms</goal>
          </goals>
          <configuration>
            <formsDirectory>src/main/webapp/resources/htmlforms</formsDirectory>
          </configuration>
        </execution>
      </executions>
    </plugin>

