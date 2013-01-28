force-meta-backup
=================

A tool to help download Salesforce.com Metadata.

The metadata is downloaded in 3 phases:
- All bulk retrievable components are downloaded first, these are statically defined in bulkRetrievable ant target.
- Queries are performed on the Salesforce Org. to determine all folders for Documents, Reports, Email Templates and Reports. Then build.xml and package.xml files are dynamically generated with this information. A bulk retrieve is then invoked for all folders and content.
- The remaining miscellaneous metadata components that can not be retrived by bulk or by wildcard methds are downloaded by building a package.xml file that explicitly lists these items.

It uses the Force.com Migration Tool along with Ant to do most of the heavy lifting. Salesforce SOAP API and Metadata APIs are used to dynamically build up lists of components for downloading.

Inspired by:

http://wiki.developerforce.com/page/Syncing_Salesforce_Org_Metadata_to_Github
https://github.com/danieljpeter/salesforceMetadataBackup

Usage
-----
- Copy _build.sample.properties_ to _build.properties_
- Edit _build.properties_ and specify your Salesforce credentials
- Open command prompt and enter command **ant backupMetadata**

The results will be downloaded to _build/metadata_
