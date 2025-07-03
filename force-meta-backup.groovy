@Grab(group='com.force.api', module='force-partner-api', version='64.0.3')
@Grab(group='com.force.api', module='force-metadata-api', version='64.0.3')

import com.sforce.soap.metadata.FileProperties
import com.sforce.soap.metadata.ListMetadataQuery
import com.sforce.soap.metadata.MetadataConnection
import com.sforce.soap.partner.Connector
import com.sforce.soap.partner.PartnerConnection
import com.sforce.ws.ConnectorConfig
import com.sforce.ws.SoapFaultException
import groovy.io.FileType
import groovy.xml.MarkupBuilder
import groovy.xml.StreamingMarkupBuilder
import groovy.xml.XmlUtil
import org.apache.tools.ant.Project
import src.StandardValueSetRegistry
import src.BulkRetrieveMetadataTypeRegistry

class ForceServiceConnector {
    ConnectorConfig config
    def apiVersion

    PartnerConnection connection
    MetadataConnection metadataConnection

    ForceServiceConnector(ConnectorConfig config, String apiVersion) {
        this.config = config;
        this.apiVersion = apiVersion;
    }

    PartnerConnection getConnection() {
        if (connection == null) {
            connection = Connector.newConnection(config)
        }

        connection
    }

    MetadataConnection getMetadataConnection() {
        if (metadataConnection == null) {
            def partnerConfig = getConnection().config

            def metadataConfig = new ConnectorConfig().with {
                it.sessionId = partnerConfig.sessionId
                it.serviceEndpoint = metadataEndpoint(partnerConfig.serviceEndpoint, apiVersion)
                it
            }

            metadataConnection = com.sforce.soap.metadata.Connector.newConnection(metadataConfig)
        }

        metadataConnection
    }

    public getApiVersion() {
        apiVersion
    }

    private static String metadataEndpoint(url, apiVersion) {
        endpoint url, 'm', apiVersion
    }

    public static String partnerEndpoint(url, apiVersion) {
        endpoint url, 'u', apiVersion
    }

    private static String endpoint(url, apiType, apiVersion) {
        def host = new URI(url).host
        "https://$host/services/Soap/$apiType/$apiVersion"
    }
}

class ForceService {
    final ForceServiceConnector connector
    def metadata
    def metadataTypes

    ForceService(ForceServiceConnector connector) {
        this.connector = connector
    }

    def getConnection() {
        connector.connection
    }

    def getMetadataConnection() {
        connector.metadataConnection
    }

    def getOrganizationId() {
        connector.connection.userInfo.organizationId
    }

    Double getApiVersion() {
        connector.apiVersion.toDouble()
    }

    def isValidMetadataType(type) {
        if (metadata == null) {
            metadata = basicMetadata()

            metadataTypes = []
            metadataTypes << metadata.keySet()

            metadata.each { k, v ->
                v.childNames.each {
                    if (it) {
                        metadataTypes << it
                    }
                }
            }
            metadataTypes = metadataTypes.flatten() as Set
        }

        metadataTypes.contains(type)
    }

    def withValidMetadataType(type, Closure closure) {
        if (isValidMetadataType(type)) {
            closure(type)
        } else {
            System.err.println "WARNING: $type is an invalid metadata type for this Organisation"
            null
        }
    }

    def query(soql) {
        def result = []

        def queryResult = connection.query soql

        if (queryResult.size > 0) {
            for (;;) {
                queryResult.records.each { result << it }

                if (queryResult.isDone()) {
                    break;
                }

                queryResult = connection.queryMore queryResult.queryLocator
            }
        }

        result
    }

    def basicMetadata() {
        def metadata = [:]

        def result = metadataConnection.describeMetadata(apiVersion)
        if (result) {
            result.metadataObjects.each { obj ->
                def name = obj.xmlName

                metadata[name] = [
                    name: name,
                    childNames: obj.childXmlNames.collect { it } as Set
                ]
            }
        }

        metadata
    }

    def listMetadata(String type) {
        listMetadata(type, null)
    }

    def listMetadata(String type, String folder) {
        def query = new ListMetadataQuery()
        query.type = type
        query.folder = folder

        listMetadata([query])
    }

    def listMetadata(List<ListMetadataQuery> queries) {
        final MAX_QUERIES_PER_REQUEST = 3

        def numQueries = queries.size()
        def isLastQuery =  false
        def index = 0

        def fileProperties = []
        while (numQueries > 0 && !isLastQuery) {
            def start = index * MAX_QUERIES_PER_REQUEST

            def end = start + MAX_QUERIES_PER_REQUEST
            if (end > numQueries) {
                end = numQueries
            }

            def requestQueries = queries.subList(start, end) as ListMetadataQuery[]
            def result = null
            try {
                result = metadataConnection.listMetadata(requestQueries, apiVersion)
            } catch (SoapFaultException e) {
                if (e.faultCode.localPart == 'INVALID_TYPE') {
                    System.err.println "WARNING: ${e.message}"
                } else {
                    throw e
                }
            }

            if (result != null) {
                fileProperties.addAll(result.toList())
            }

            isLastQuery = (numQueries - (++index * MAX_QUERIES_PER_REQUEST)) < 1
        }

        fileProperties
    }

    def listMetadataForTypes(types) {
        def queries = types.collect { type ->
            withValidMetadataType(type) {
                def query = new ListMetadataQuery()
                query.type = it
                query
            }
        }
        queries.removeAll([null])
        
        listMetadata(queries)
    }
}

class ForceServiceFactory {
    static final USERNAME_PASSWORD_OR_SESSION_ID = 'You must specify the sf.username/sf.password property pair, or the sf.sessionId property but not both.'

    static create(ConfigObject config) {
        final flatConfig = config.flatten();

        def (username, password, sessionId, serverUrl, apiVersion) = [
            'sf.username',
            'sf.password',
            'sf.sessionId',
            'sf.serverurl',
            'sf.apiVersion'
        ].collect { flatConfig.get(it)?.toString() }

        def connectorConfig = new ConnectorConfig().with {
            it.authEndpoint = ForceServiceConnector.partnerEndpoint(serverUrl, apiVersion)
            it
        }

        if (sessionId) {
            // Session ID flow
            connectorConfig.sessionId = sessionId
            connectorConfig.serviceEndpoint = connectorConfig.authEndpoint
        } else if (username || password) {
            // Username Password flow

            if (!username || !password) {
                throw new IllegalArgumentException('You must specify values for both sf.username and sf.password properties')
            }

            connectorConfig.username = username 
            connectorConfig.password = password 
        } else {
            throw new IllegalArgumentException(USERNAME_PASSWORD_OR_SESSION_ID)
        }

        new ForceService(new ForceServiceConnector(connectorConfig, apiVersion))
    }
}

class Configuration {
    static ConfigObject fromAntProject(Project project) {
        def properties = project.getProperties()

        ConfigObject config = new ConfigObject()
        config.buildDir = properties.get('build.dir')
        config.excludeTypes = expandExcludeTypes(properties.remove('sf.excludeTypes'))

        // We only want the sessionId or username/password but not both
        if (properties.get('sf.sessionId') == null) {
            properties.remove('sf.sessionId')
        } else {
            properties.remove('sf.username')
            properties.remove('sf.password')
        }

        // Copy sf properties to config
        final prefix = 'sf.'
        properties.keySet()
            .findAll { it.startsWith(prefix) }
            .collect { it.replace(prefix, "") }
            .each { property -> 
                config.sf[property] = properties.get(prefix + property)
            }

        config
    }

    private static Set<String> expandExcludeTypes(String commaSeparatedExcludeTypes) {
        def excludeTypes = [] as Set

        if (commaSeparatedExcludeTypes) {
            excludeTypes.addAll(commaSeparatedExcludeTypes.split(',').collect {
                it.trim().toLowerCase()
            })
        }

        excludeTypes
    }
}

class FileWriterFactory {
    static create(filePath) {
        def file = new File(filePath)
        def parentFile = file.parentFile

        if (parentFile) {
            parentFile.mkdirs()
        }

        new FileWriter(file)
    }
}

abstract class ManifestBuilder {
    protected def forceService
    protected def config

    protected ManifestBuilder(forceService, config) {
        this.forceService = forceService
        this.config = config
    }

    def isExcluded(metadataType) {
        config.excludeTypes.contains(metadataType.toLowerCase())
    }

    abstract void writeManifest() 

    def bulkThreadCount() {
      config.sf.concurrency
    }
}

class BulkMetadataManifestBuilder extends ManifestBuilder {
    def buildXmlPath

    static final BUILD_XML = 'bulk-retrievable-target.xml'
    static final TYPES = new BulkRetrieveMetadataTypeRegistry().getTypes()

    BulkMetadataManifestBuilder(ForceService forceService, config) {
        super(forceService, config)

        buildXmlPath = "$config.buildDir/$BUILD_XML"
    }

    void writeManifest() {
        def writer = FileWriterFactory.create(buildXmlPath)
        def builder = new MarkupBuilder(writer)

        builder.project('xmlns:sf': 'antlib:com.salesforce', 'default': 'bulkRetrievable') {
            'import'(file: '../ant-includes/setup-target.xml')

            target(name: 'bulkRetrievable', depends: '-setUpMetadataDir') {
                parallel(threadCount: bulkThreadCount()) {
                    TYPES.findAll {
                        !isExcluded(it)
                    }.each { type ->
                        forceService.withValidMetadataType(type) {
                            'sfBulkRetrieve'(metadataType: type)
                        }
                    }
                }
            }
        }
    }
}

class Folders extends ManifestBuilder {
    def packageXmlPath
    def buildXmlPath
    
    static final PACKAGE_XML = 'folders-package.xml'
    static final BUILD_XML = 'folders-build.xml'

    def folderMetaTypeByFolderType = [
        Dashboard: 'Dashboard',
        Document: 'Document',
        Email: 'EmailTemplate',
        Report: 'Report',
        Insights: 'Report'
    ]

    Folders(ForceService forceService, config) {
        super(forceService, config)

        packageXmlPath = "$config.buildDir/$PACKAGE_XML"
        buildXmlPath = "$config.buildDir/$BUILD_XML"
    }

    void writeManifest() {
        def allFolders = fetchAllFolders()
        def foldersAndUnfiled = [:] + allFolders

        def unfiledFetchMap = [
            Email: 'fetchUnfiledPublicEmailTemplates',
            Report: 'fetchUnfiledPublicReports'
        ]
        
        unfiledFetchMap.each { folderType, fetchMethod ->
            def metadataType = folderMetaTypeByFolderType[folderType]

            if (!isExcluded(metadataType)) {
                foldersAndUnfiled[folderType] = foldersAndUnfiled[folderType] ?: []
                foldersAndUnfiled[folderType] += "$fetchMethod"()
            }
        }

        writeFolderBulkRetriveXml(allFolders)
        writeFoldersPackageXml(foldersAndUnfiled)
    }

    private void writeFoldersPackageXml(foldersAndUnfiled) {
        def builder = new StreamingMarkupBuilder()

        builder.encoding = 'UTF-8'
        def xml = builder.bind {
            mkp.xmlDeclaration()
            Package(xmlns: 'http://soap.sforce.com/2006/04/metadata') {
                foldersAndUnfiled.each { folderType, folders ->
                    types {

                        folders.each { folderName ->
                            members folderName 
                        }

                        name folderMetaTypeByFolderType[folderType]
                    }
                }

                version forceService.apiVersion
            }
        }

        def writer = FileWriterFactory.create(packageXmlPath)
        XmlUtil.serialize(xml, writer)
    }

    private void writeFolderBulkRetriveXml(allFolders) {
        def writer = FileWriterFactory.create(buildXmlPath)
        def builder = new MarkupBuilder(writer)

        builder.project('xmlns:sf': 'antlib:com.salesforce', 'default': 'bulkRetrieveFolders') {
            'import'(file: '../ant-includes/setup-target.xml')

            target(name: 'bulkRetrieveFolders', depends: '-setUpMetadataDir') {
                parallel(threadCount: bulkThreadCount()) {
                    'sfRetrieve'(unpackaged: packageXmlPath)

                    allFolders.each { folderType, folders ->
                        folders.each { folderName ->
                            def type = folderMetaTypeByFolderType[folderType]
                            'sfBulkRetrieve'(metadataType: type, containingFolder: folderName)
                        }
                    }
                }
            }
        }
    }

    private fetchAllFolders() {
        def folderTypes = folderMetaTypeByFolderType.keySet()

        def soql = "SELECT NamespacePrefix, DeveloperName, Type FROM Folder WHERE DeveloperName != '' AND Type IN ('${folderTypes.join("', '")}') ORDER BY Type, NamespacePrefix, DeveloperName"
        def sObjects = forceService.query soql

        def folders = [:]

        sObjects.each {
            def prefix = it.getField('NamespacePrefix')
            prefix = (prefix == null) ? '' : prefix + '__'

            def name = it.getField('DeveloperName')
            def type = it.getField('Type')

            if (!folders.containsKey(type)) {
                folders[type] = []
            }

            folders[type] << "$prefix$name"
        }

        folderMetaTypeByFolderType.each { folderType, metadataType ->
            if (isExcluded(metadataType)) {
                folders.remove(folderType)
            }
        }

        folders
    }

    private fetchUnfiledPublicEmailTemplates() {
        def soql = "SELECT DeveloperName FROM EmailTemplate WHERE FolderId = '$forceService.organizationId'"

        fetchUnfiled soql
    }    

    private fetchUnfiledPublicReports() {
        def soql = "SELECT DeveloperName FROM Report WHERE OwnerId = '$forceService.organizationId'"

        fetchUnfiled soql
    }

    private fetchUnfiled(soql) {
        def sObjects = forceService.query soql

        // Unfiled Public folders are not real folders in that there is no
        // folder object in the Folders table, instead the OrganisationId is
        // used as the Folder/Owner and this is what makes it unfiled.
        // There is no direct metadata approach to get this so building this from
        // queries to get list of unfiled components.
        def folder = 'unfiled$public';
        def unfiled = [folder]

        sObjects.each {
            def name = it.getField('DeveloperName')
            unfiled << "$folder/$name"
        }

        unfiled
    }
}

class MiscMetadataManifestBuilder extends ManifestBuilder {
    def packageXmlPath
    
    static final PACKAGE_XML = 'misc-package.xml'

    static final TYPES = [
        'StandardValueSet',
    ]

    static final WILDCARD_TYPES = [
        'ApexComponent',
        'ApexTrigger',
        'AuraDefinitionBundle', 
        'LightningComponentBundle',
        'StandardValueSetTranslation',
    ]

    static final STANDARD_VALUE_SET_NAMES = new StandardValueSetRegistry().getValueSetNames();

    MiscMetadataManifestBuilder(ForceService forceService, config) {
        super(forceService, config)

        packageXmlPath = "$config.buildDir/$PACKAGE_XML"
    }

    private getGroupedFileProperties() {
        def grouped = new GroupedFileProperties(
            forceService.listMetadataForTypes(TYPES)
        )

        // XXX - Hack to always retrieve the CaseComment SObject & Workflow.
        //
        // For some reason CaseComment is not returned in listMetadata()
        // calls for CustomObject and Workflow but if we explicitly put
        // these in package.xml for retrieve we can download them.
        grouped.addIfMissingStandard('Workflow', 'CaseComment')

        // XXX - Salesforce does not return names for type StandardValueSet
        // when we call listMetdata(). Adding hardcoded list of names
        // as workaround
        STANDARD_VALUE_SET_NAMES.each {
            grouped.addIfMissingStandard('StandardValueSet', it)
        }

        grouped.sort()

        grouped.filePropertiesByType.findAll {
            !isExcluded(it.key)
        }
    }

    void writeManifest() {
        def builder = new StreamingMarkupBuilder()
        builder.encoding = 'UTF-8'

        def xml = builder.bind {
            mkp.xmlDeclaration()
            Package(xmlns: 'http://soap.sforce.com/2006/04/metadata') {

                groupedFileProperties.each { type, fileProperties ->
                    types {
                        fileProperties.each { fp ->
                            members fp.fullName
                        }

                        name type
                    }
                }

                WILDCARD_TYPES.findAll {
                    !isExcluded(it)
                }.each { type ->
                    types {
                        members '*'
                        name type
                    }
                }

                version forceService.apiVersion
            }
        }

        def writer = FileWriterFactory.create(packageXmlPath)
        XmlUtil.serialize(xml, writer)
    }
}

class GroupedFileProperties {
    static final String NO_NAMESPACE = null;
    static final String STANDARD_NAMESPACE = '';

    def filePropertiesByType = [:] as TreeMap

    GroupedFileProperties() {
    }

    GroupedFileProperties(List<FileProperties> fileProperties) {
        addAll(fileProperties)
    }

    GroupedFileProperties addAll(List<FileProperties> fileProperties) {
        fileProperties.each { fp ->
            add(fp)
        }

        this
    }

    GroupedFileProperties add(FileProperties fp) {
        if (!containsGroup(fp.type)) {
            filePropertiesByType[fp.type] = []
        }

        filePropertiesByType[fp.type] << fp

        this
    }
    
    GroupedFileProperties addIfMissingStandard(String type, String fullName) {
        addIfMissing(type, fullName, STANDARD_NAMESPACE)
    }
    
    GroupedFileProperties addIfMissingCustom(String type, String fullName) {
        addIfMissing(type, fullName, NO_NAMESPACE)
    }

    GroupedFileProperties addIfMissing(String type, String fullName, String namespacePrefix) {
        addIfMissing([
            type: type,
            fullName: fullName,
            namespacePrefix: namespacePrefix
        ] as FileProperties)
    }

    GroupedFileProperties addIfMissing(FileProperties fp) {
        if (!contains(fp.type, fp.fullName, fp.namespacePrefix)) {
            add(fp)
        }

        this
    }

    Boolean containsGroup(String type) {
        filePropertiesByType.containsKey(type)
    }

    Boolean contains(String type, String fullName, String namespacePrefix) {
        containsGroup(type) && filePropertiesByType[type].find {
            it.fullName == fullName &&
            it.namespacePrefix == namespacePrefix
        }
    }

    void sort() {
        filePropertiesByType.each { k, v ->
            v.sort { FileProperties a, FileProperties b ->
                a.namespacePrefix <=> b.namespacePrefix ?: a.fullName <=> b.fullName
            }
        }
    }
}

class ProfilesMetadataManifestBuilder extends ManifestBuilder {
    def groupedFileProps

    static final TYPES = [
        'ApexClass',
        'ApexPage',
        'CustomApplication',
        'CustomObject',
        'CustomPermission',
        'CustomTab',
        'ExternalDataSource',
        'Layout',
      ]

    ProfilesMetadataManifestBuilder(ForceService forceService, config) {
        super(forceService, config)
    }

    private getGroupedFileProperties() {
        if (groupedFileProps == null) {
            def listMetadata = forceService.listMetadataForTypes(TYPES)
            listMetadata.removeAll { fileProperties ->
                // Filter out ApexClasses from managed packages, these are all (hidden) anyway
                // and in large Orgs can be more than 10000 components causing LIMIT_EXCEPTION
                // on retrieval
                fileProperties.type == 'ApexClass' && fileProperties.namespacePrefix != null
            }
    
            def grouped = new GroupedFileProperties(listMetadata)

            // XXX - Hack to always retrieve the CaseComment SObject & Workflow.
            //
            // For some reason CaseComment is not returned in listMetadata()
            // calls for CustomObject and Workflow but if we explicitly put
            // these in package.xml for retrieve we can download them.
            grouped.addIfMissingStandard('CustomObject', 'CaseComment')

            grouped.sort()

            groupedFileProps = grouped.filePropertiesByType.findAll {
                !isExcluded(it.key)
            }

        }

        groupedFileProps
    }

    void writeManifest() {
        groupedFileProperties.each { type, fileProperties ->
            writePackageXmlForType type, fileProperties
        }

        writeBuildXml()
    }

    private void writePackageXmlForType(type, fileProperties) {
        def builder = new StreamingMarkupBuilder()
        builder.encoding = 'UTF-8'

        def resolveName = { FileProperties fp ->
            fp.fullName
        }

        def WILDCARD_TYPES = ['Profile']

        if (type == 'Layout') {
            // Note: Page Layout assignments require Layouts & RecordType to be retrieved with Profile 
            WILDCARD_TYPES << 'RecordType'

            // Layouts in managed pacakges must have namespace prefix
            resolveName = { FileProperties fp ->
                if (fp.namespacePrefix) {
                    def namespace = fp.namespacePrefix + '__'
                    def seperator = '-'

                    return fp.fullName.replace(seperator, seperator + namespace)
                }

                fp.fullName
            }
        }

        def xml = builder.bind {
            mkp.xmlDeclaration()
            Package(xmlns: 'http://soap.sforce.com/2006/04/metadata') {
                types {
                    fileProperties.each { fp ->
                        members resolveName(fp)
                    }

                    name type
                }

                WILDCARD_TYPES.each { metadataType ->
                    types {
                        members '*'
                        name metadataType
                    }
                }

                version { mkp.yield forceService.apiVersion }
            }
        }

        def writer = FileWriterFactory.create(profilePackageXmlPath(type))
        XmlUtil.serialize(xml, writer)
    }

    private profilePackageXmlPath(type) {
        "${config.buildDir}/profile-packages/${type}.xml"
    }

    private writeBuildXml() {
        def writer = FileWriterFactory.create("${config.buildDir}/profile-packages-target.xml")
        def builder = new MarkupBuilder(writer)

        def targetName = 'profilesPackageRetrieve'

        builder.project('xmlns:sf': 'antlib:com.salesforce', 'default': targetName) {
            'import'(file: '../ant-includes/setup-target.xml')

            target(name: targetName, depends: '-setUpMetadataDir') {
                parallel(threadCount: bulkThreadCount()) {
                    groupedFileProperties.each { type, fileProperties ->
                        def retrieveTarget = "${config.buildDir}/profile-packages-metadata/$type"

                        forceService.withValidMetadataType(type) {
                            'sfRetrieveToFolder'(
                                unpackaged: profilePackageXmlPath(type),
                                retrieveTarget: retrieveTarget
                            )
                        }
                    }
                }
            }
        }
    }
}

class XmlMergeTargetBuilder {
    def config
    def srcDir

    XmlMergeTargetBuilder(config) {
        this.config = config
        srcDir = "${config.buildDir}/profile-packages-metadata"
    }

    private getProfiles() {
        def profiles = new TreeSet()

        def dir = new File(srcDir)

        dir.eachFileRecurse (FileType.FILES) { file ->
            if (file.name ==~ /.+\.profile$/) {
                profiles << file.name
            }
        }

        profiles
    }

    private writeBuildXml() {
        def writer = FileWriterFactory.create("${config.buildDir}/profile-packages-merge-target.xml")
        def builder = new MarkupBuilder(writer)

        def targetName = 'profilesPackageXmlMerge'
        def metadataDir = "${config.buildDir}/metadata"

        builder.project('default': targetName) {
            'import'(file: '../ant-includes/setup-target.xml')

            target(name: targetName) {
                def destDir = "$metadataDir/profiles"
                mkdir(dir: destDir)

                profiles.each { filename ->
                    sequential {
                        echo "Xml Merging: $filename"
                        xmlmerge(dest: "$destDir/$filename", conf: 'xmlmerge.properties') {
                            fileset(dir: srcDir) {
                                include(name: "**/$filename")
                            }
                        }
                    }
                }

                // TODO maybe we can dynamically build this list of folders/files to be copied
                copy(todir: metadataDir) {
                    fileset(dir: srcDir) {
                        include(name: '**/classes/*')
                        include(name: '**/pages/*')
                        include(name: '**/applications/*')
                        include(name: '**/objects/*')
                        include(name: '**/objectTranslations/*')
                        include(name: '**/customPermissions/*');
                        include(name: '**/tabs/*')
                        include(name: '**/layouts/*')
                        include(name: '**/dataSources/*')
                    }

                    cutdirsmapper(dirs: 1)
                }
            }
        }
    }
}

////////////////////////////////////////////////////////////////////////////////

class MetadataBackupTool {
    final ForceService forceService;
    final ConfigObject config;

    static MetadataBackupTool forProject(Project p) {
        new MetadataBackupTool(Configuration.fromAntProject(p))
    }

    MetadataBackupTool(ConfigObject config) {
        this.config = config
        forceService = ForceServiceFactory.create(config)
    }

    String getSalesforceSessionId() {
        "${forceService.connection.config.sessionId}"
    }

    void generateXmlMergeTarget() {
        new XmlMergeTargetBuilder(config).writeBuildXml()
    }

    void generateMetadataRetrievalManifests() {
        [
            new BulkMetadataManifestBuilder(forceService, config),
            new Folders(forceService, config),
            new MiscMetadataManifestBuilder(forceService, config),
            new ProfilesMetadataManifestBuilder(forceService, config)
        ].each { it.writeManifest() }
    }
}
