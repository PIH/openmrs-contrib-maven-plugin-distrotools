OpenMRS Distro Tools Maven Plugin
=======================================

Overview
--------
Useful build tasks for OpenMRS distributions.

Goals
-----
 * __validate-forms__ validates all HFE form files in a specified directory
     * Required parameters
         * _formsDirectory_ the directory containing the form files
             * Type: File
     * Optional parameters
         * _formsExtension_ the file extension used for form files
             * Type: String
             * Default: html
