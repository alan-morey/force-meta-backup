# force-meta-backup

A tool to help download Salesforce.com Metadata.

Using the Salesforce Metadata API, this tool will dynamically generate Ant `build.xml` files and Salesforce Metadata `package.xml` files for downloading the Metadata from your Org.

The metadata is downloaded in 3 phases:
- All bulk retrievable components are downloaded first, the `bulkRetrievable` Ant target is dynamically generated and executed.
- Queries are performed on the Salesforce Org. to determine all folders for Dashboards, Documents, Email Templates and Reports. Then `build.xml` and `package.xml` files are dynamically generated with this information. A bulk retrieve is then invoked for all folders and content.
- The remaining miscellaneous metadata components that can not be retrived by bulk or by wildcard methds are downloaded by building a `package.xml` file that explicitly lists these items.

It uses the [Ant Migration Tool](https://developer.salesforce.com/docs/atlas.en-us.daas.meta/daas/) along with Ant to retrieve metadata from the Org, Salesforce's [SOAP API](https://developer.salesforce.com/docs/atlas.en-us.api.meta/api/) and [Metadata API](https://developer.salesforce.com/docs/atlas.en-us.api_meta.meta/api_meta/) are used in a [Groovy](https://groovy-lang.org/) script to dynamically build up lists of components for downloading.

Inspired by:

https://github.com/danieljpeter/salesforceMetadataBackup

## Prerequisites
The following tools should be installed before use:
- JDK
- [Ant](http://ant.apache.org/)

Ensure you are able to execute `ant` on the command line:
e.g.

```
$> ant -version
Apache Ant(TM) version 1.10.13 compiled on January 4 2023
```


## Usage

To backup metadata from your Saleforce Org, you execute the `backupMetadata`
ant target:

```bash
ant backupMetadata
```

However, you will need to provide login details before this will succeed. The
login details are provided to ant using properties which can be configured in
a `build.properties` file or provided as commandline options using `-Dproperty=value`.

To login, you can provide a username/password pair, or an active Salesforce session.

> [!WARNING]
> You can specify either the `sf.username` and `sf.password` property pair, or the `sf.sessionId` property but not both.

To use a username/password for login, specify your username and password using the
`sf.username` and `sf.password` properties.

To use an active Salesforce session for login, specify a valid session ID or OAuth
token using the `sf.sessionId` property.

> [!IMPORTANT]
The login used should be for an Administrator user, to ensure the correct access permissions for retrieving all metadata components.


In addition to the login properties, username/password or session ID, you will need to provide 
the URL for the Salesforce Org to retrieve the metadata from. Specify this using the `sf.serverurl` property.

> [!IMPORTANT]
> The `sf.serverurl` property should always be configured with the instance URL and never https://login.salesforce.com or https://test.salesforce.com.


For more details on these properties consult the [Ant Migration Tool](https://developer.salesforce.com/docs/atlas.en-us.daas.meta/daas/) documentation.

### Already have a Session ID and server instance URL
For example to download the metadata 

```bash
ant backupMetadata -Dsf.sessionId="<SESSION_ID_OR_OAUTH_TOKEN>" -Dsf.serverUrl="https://na17.my.salesforce.com"
```

Results will be downlaoded to `build/metadata`

### Configuring `build.properties`
Properties can be configured in the a `build.properties` file instead of passing
via commandline arguments.

- Copy `build.sample.properties` to `build.properties`
- Edit `build.properties` then specify valid `sf.username`, `sf.password` and `sf.serverurl`
  properties for the Org you wish to backup.
- Open command prompt/shell, change directory to this project base directory and run command `ant backupMetadata`


Results will be downloaded to `build/metadata`


## How to exclude types from backup
To exclude specific metadata types from backup you can specify a comma separated list of 
metadata types using the `sf.excludedTypes` property. This can be configured on the commandline
using `-Dsf.excludeTypes` or in the `build.properties` 

For example to exclude Reports and Documents from backup, add the following line
to `build.properties`

```properties
sf.excludeTypes=Report,Document
```

Then run backup with `ant backupMetadata`.
