#!/usr/bin/env groovy
@Grab(group='com.force.sdk', module='force-connector', version='22.0.9-BETA')
@Grab(group='commons-lang', module='commons-lang', version='2.6')

import com.force.sdk.connector.ForceConnectorConfig
import com.force.sdk.connector.ForceServiceConnector

import com.sforce.soap.partner.PartnerConnection
import com.sforce.soap.metadata.FileProperties
import com.sforce.soap.metadata.ListMetadataQuery

import com.sforce.ws.SoapFaultException
import java.net.URLEncoder

import groovy.io.FileType
import groovy.xml.MarkupBuilder
import groovy.xml.StreamingMarkupBuilder
import groovy.xml.XmlUtil


class ForceService {
    def forceServiceConnector
    def metadata
    def metadataTypes
    def apiVersion

    ForceService(serverUrl, username, password, apiVersion) {
        def config = new ForceConnectorConfig()
        config.connectionUrl = buildConnectionUrl serverUrl, username, password

        forceServiceConnector = new ForceServiceConnector(config)

        this.apiVersion = apiVersion
    }

    def getConnection() {
        forceServiceConnector.connection
    }

    def getMetadataConnection() {
        forceServiceConnector.metadataConnection
    }

    def getSessionId() {
        forceServiceConnector.connection.sessionId
    }

    def getOrganizationId() {
        forceServiceConnector.connection.userInfo.organizationId
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
            println "WARNING: $type is an invalid metadata type for this Organisation"
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

        def result = metadataConnection.describeMetadata(apiVersion.toDouble())
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

        def numQueries = queries.size
        def isLastQuery =  false
        def index = 0
        def apiVersion = this.apiVersion.toDouble()

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
                    println "WARNING: ${e.message}"
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

    private buildConnectionUrl = { serverUrl, username, password ->
        def encode = { URLEncoder.encode(it, 'UTF-8') }

        def host = new URI(serverUrl).host;
        def query = [
            user: username,
            password: password
        ].collect { k, v -> "${encode k}=${encode v}" }.join('&')

        "force://$host?$query"
    }
}

class ForceServiceFactory {
    static create(propFileName) {
        def loadProperties = {
            def props = new Properties()
            def inputStream = getClass().getResourceAsStream(it)

            props.load(inputStream)
            inputStream.close()

            new ConfigSlurper().parse(props)
        }

        def config = loadProperties 'ant-includes/default.properties'
        config = config.merge(loadProperties(propFileName))

        new ForceService(
            config.sf.serverurl,
            config.sf.username,
            config.sf.password,
            config.sf.antlib.version
        )
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

class BulkMetadataManifestBuilder {
    def forceService
    def config
    def buildXmlPath

    static final BUILD_XML = 'bulk-retrievable-target.xml'

    static TYPES = [
        'AccountSharingRules',
        'AccountTerritorySharingRules',
        'AnalyticSnapshot',
        'AppMenu',
        'ApexComponent',
        'ApexTrigger',
        'ApprovalProcess',
        'ArticleType',
        'AssignmentRules',
        'AuthProvider',
        'AutoResponseRules',
        'CallCenter',
        'CampaignSharingRules',
        'CaseSharingRules',
        'Community',
        'ContactSharingRules',
        'CustomApplicationComponent',
        'CustomLabels',
        'CustomPageWebLink',
        'CustomSite',
        'DataCategoryGroup',
        'EntitlementProcess',
        'EntitlementTemplate',
        'EscalationRules',
        'FlexiPage',
        'Flow',
        'Group',
        'HomePageComponent',
        'HomePageLayout',
        'InstalledPackage',
        'LiveChatAgentConfig',
        'LiveChatButton',
        'LiveChatDeployment',
        'MilestoneType',
        'Network',
        'OpportunitySharingRules',
        'Portal',
        'PostTemplate',
        'Queue',
        'QuickAction',
        'RemoteSiteSetting',
        'ReportType',
        'Role',
        'SamlSsoConfig',
        'Scontrol',
        'Settings',
        'SharingSet',
        'Skill',
        'StaticResource',
        'SiteDotCom',
        'Territory',
        'Workflow'
    ]

    BulkMetadataManifestBuilder(ForceService forceService, config) {
        this.forceService = forceService
        this.config = config
        buildXmlPath = "${config['build.dir']}/${BUILD_XML}"
    }

    def writeBuildXml() {
        def writer = FileWriterFactory.create(buildXmlPath)
        def builder = new MarkupBuilder(writer)

        builder.project('xmlns:sf': 'antlib:com.salesforce', 'default': 'bulkRetrievable') {
            'import'(file: '../ant-includes/setup-target.xml')

            target(name: 'bulkRetrievable', depends: '-setUpMetadataDir') {
                TYPES.each { type ->
                    forceService.withValidMetadataType(type) {
                        'sf:bulkRetrieve'(
                            metadataType: it,
                            retrieveTarget: '${build.metadata.dir}',
                            username: '${sf.username}',
                            password: '${sf.password}',
                            serverurl: '${sf.serverurl}',
                            pollWaitMillis: '${sf.pollWaitMillis}',
                            maxPoll: '${sf.maxPoll}'
                        )
                    }
                }
            }
        }
    }
}

class Folders {
    def forceService
    def config
    def packageXmlPath
    def buildXmlPath
    
    static final PACKAGE_XML = 'folders-package.xml'
    static final BUILD_XML = 'folders-build.xml'

    def folderMetaTypeByFolderType = [
        Dashboard: 'Dashboard',
        Document: 'Document',
        Email: 'EmailTemplate',
        Report: 'Report'
    ]

    Folders(ForceService forceService, config) {
        this.forceService = forceService
        this.config = config
        packageXmlPath = "${config['build.dir']}/${PACKAGE_XML}"
        buildXmlPath = "${config['build.dir']}/${BUILD_XML}"
    }

    def writeFoldersPackageXml() {
        def builder = new StreamingMarkupBuilder()

        def foldersAndUnfiled = allFolders

        foldersAndUnfiled['Email'] = (foldersAndUnfiled['Email']) ?: []
        foldersAndUnfiled['Email'] += fetchUnfiledPublicEmailTemplates()

        foldersAndUnfiled['Report'] = (foldersAndUnfiled['Report']) ?: []
        foldersAndUnfiled['Report'] += fetchUnfiledPublicReports()

        builder.encoding = 'UTF-8'
        def xml = builder.bind {
            mkp.xmlDeclaration()
            Package(xmlns: 'http://soap.sforce.com/2006/04/metadata') {
                foldersAndUnfiled.each { folderType, folders ->
                    types {

                        folders.each { folderName ->
                            members {
                                mkp.yield folderName
                            }
                        }

                        name() { mkp.yield folderMetaTypeByFolderType[folderType] }
                    }
                }

                version { mkp.yield forceService.apiVersion }
            }
        }

        def writer = FileWriterFactory.create(packageXmlPath)
        XmlUtil.serialize(xml, writer)
    }

    def writeFolderBulkRetriveXml() {
        def writer = FileWriterFactory.create(buildXmlPath)
        def builder = new MarkupBuilder(writer)

        builder.project('xmlns:sf': 'antlib:com.salesforce', 'default': 'bulkRetrieveFolders') {
            'import'(file: '../ant-includes/setup-target.xml')

            target(name: 'bulkRetrieveFolders', depends: '-setUpMetadataDir') {
                'sf:retrieve'(
                    unpackaged: packageXmlPath,
                    retrieveTarget: '${build.metadata.dir}',
                    username: '${sf.username}',
                    password: '${sf.password}',
                    serverurl: '${sf.serverurl}',
                    pollWaitMillis: '${sf.pollWaitMillis}',
                    maxPoll: '${sf.maxPoll}'
                )

                allFolders.each { folderType, folders ->
                    folders.each { folderName ->
                        'sf:bulkRetrieve'(
                            metadataType: folderMetaTypeByFolderType[folderType],
                            containingFolder: folderName,
                            retrieveTarget: '${build.metadata.dir}',
                            username: '${sf.username}',
                            password: '${sf.password}',
                            serverurl: '${sf.serverurl}',
                            pollWaitMillis: '${sf.pollWaitMillis}',
                            maxPoll: '${sf.maxPoll}'
                        )
                    }
                }
            }
        }
    }

    def getAllFolders() {
        fetchAllFolders()
    }

    private fetchAllFolders() {
        def soql = "SELECT NamespacePrefix, DeveloperName, Type FROM Folder WHERE DeveloperName != '' ORDER BY Type, NamespacePrefix, DeveloperName"
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

        folders
    }

    private fetchUnfiledPublicEmailTemplates() {
        def soql = "SELECT DeveloperName FROM EmailTemplate WHERE FolderId = '${forceService.organizationId}'"

        fetchUnfiled soql
    }
        

    private fetchUnfiledPublicReports() {
        def soql = "SELECT DeveloperName FROM Report WHERE OwnerId = '${forceService.organizationId}'"

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

class MiscMetadataManifestBuilder {
    def forceService
    def config
    def packageXmlPath
    
    static final PACKAGE_XML = 'misc-package.xml'

    static final TYPES = [
        'Letterhead',
        'AccountCriteriaBasedSharingRule',
        'AccountOwnerSharingRule',
        'AccountSharingRules',
        'CampaignCriteriaBasedSharingRule',
        'CampaignOwnerSharingRule',
        'CampaignSharingRules',
        'CaseCriteriaBasedSharingRule',
        'CaseOwnerSharingRule',
        'CaseSharingRules',
        'ContactCriteriaBasedSharingRule',
        'ContactOwnerSharingRule',
        'ContactSharingRules',
        'CustomObjectCriteriaBasedSharingRule',
        'CustomObjectOwnerSharingRule',
        'CustomObjectSharingRules',
        'LeadCriteriaBasedSharingRule',
        'LeadOwnerSharingRule',
        'LeadSharingRules',
        'OpportunityCriteriaBasedSharingRule',
        'OpportunityOwnerSharingRule',
        'OpportunitySharingRules',
        'UserCriteriaBasedSharingRule',
        'UserMembershipSharingRule'
    ]

    MiscMetadataManifestBuilder(ForceService forceService, config) {
        this.forceService = forceService
        this.config = config
        packageXmlPath = "${config['build.dir']}/${PACKAGE_XML}"
    }


    private getGroupedFileProperties() {
        def queries = TYPES.collect { type ->
            forceService.withValidMetadataType(type) {
                def query = new ListMetadataQuery()
                query.type = it
                query
            }
        }
        queries.removeAll([null])

        def grouped = [:]

        forceService.listMetadata(queries).each { fileProperties ->
            def type = fileProperties.type

            if (!grouped.containsKey(type)) {
                grouped[type] = []
            }

            grouped[type] << fileProperties
        }

        grouped
    }

    def writePackageXml() {
        def builder = new StreamingMarkupBuilder()
        builder.encoding = 'UTF-8'

        def xml = builder.bind {
            mkp.xmlDeclaration()
            Package(xmlns: 'http://soap.sforce.com/2006/04/metadata') {

                groupedFileProperties.each { type, fileProperties ->
                    types {
                        fileProperties.each { fp ->
                            members { mkp.yield fp.fullName }
                        }

                         name() { mkp.yield type}
                    }
                }

                version { mkp.yield forceService.apiVersion }
            }
        }

        def writer = FileWriterFactory.create(packageXmlPath)
        XmlUtil.serialize(xml, writer)
    }
}

class ProfilesMetadataManifestBuilder {
    def forceService
    def config

    static final TYPES = [
        'ApexClass',
        'ApexPage',
        'CustomApplication',
        'CustomObject',
        'CustomObjectTranslation',
        'CustomTab',
        'ExternalDataSource',
        'Layout'
    ]

    static final PERMISSON_TYPES = [
        'Profile',
        'PermissionSet'
    ]

    ProfilesMetadataManifestBuilder(ForceService forceService, config) {
        this.forceService = forceService
        this.config = config
    }

    private getGroupedFileProperties() {
        def queries = TYPES.collect { type ->
            forceService.withValidMetadataType(type) {
                def query = new ListMetadataQuery()
                query.type = it
                query
            }
        }
        queries.removeAll([null])

        def grouped = [:]

        forceService.listMetadata(queries).each { fileProperties ->
            def type = fileProperties.type

            if (!grouped.containsKey(type)) {
                grouped[type] = []
            }

            grouped[type] << fileProperties
        }

        grouped.each { k, v ->
            v.sort { a, b ->
                a.namespacePrefix <=> b.namespacePrefix ?: a.fullName <=> b.fullName
            }
        }
    }

    def writePackageXmlForType(type, fileProperties) {
        def builder = new StreamingMarkupBuilder()
        builder.encoding = 'UTF-8'

        def xml = builder.bind {
            mkp.xmlDeclaration()
            Package(xmlns: 'http://soap.sforce.com/2006/04/metadata') {
                types {
                    fileProperties.each { fp ->
                        members { mkp.yield fp.fullName }
                    }

                    name() { mkp.yield type}
                }

                PERMISSON_TYPES.each { metadataType ->
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

    def writePackageXml() {
        groupedFileProperties.each { type, fileProperties ->
            writePackageXmlForType type, fileProperties
        }

        writeBuildXml()
    }

    private profilePackageXmlPath(type) {
        "${config['build.dir']}/profile-packages/${type}.xml"
    }

    private writeBuildXml() {
        def writer = FileWriterFactory.create("${config['build.dir']}/profile-packages-target.xml")
        def builder = new MarkupBuilder(writer)

        def targetName = 'profilesPackageRetrieve'

        builder.project('xmlns:sf': 'antlib:com.salesforce', 'default': targetName) {
            'import'(file: '../ant-includes/setup-target.xml')

            target(name: targetName, depends: '-setUpMetadataDir') {
                TYPES.each { type ->
                    def retrieveTarget = "${config['build.dir']}/profile-packages-metadata/$type"

                    forceService.withValidMetadataType(type) {
                        mkdir(dir: retrieveTarget)

                        'sf:retrieve'(
                            unpackaged: profilePackageXmlPath(type),
                            retrieveTarget: retrieveTarget,
                            username: '${sf.username}',
                            password: '${sf.password}',
                            serverurl: '${sf.serverurl}',
                            pollWaitMillis: '${sf.pollWaitMillis}',
                            maxPoll: '${sf.maxPoll}'
                        )
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
        srcDir = "${config['build.dir']}/profile-packages-metadata"
    }

    private getData() {
        def data = [
            profiles: new TreeSet(),
            permissionsets: new TreeSet()
        ]

        def dir = new File(srcDir)

        dir.eachFileRecurse (FileType.FILES) { file ->
            if (file.name ==~ /.+\.profile$/) {
                data.profiles << file.name
            } else if (file.name ==~ /.+\.permissionset/) {
                data.permissionsets <<  file.name
            }
        }

        data
    }

    private writeBuildXml() {
        def writer = FileWriterFactory.create("${config['build.dir']}/profile-packages-merge-target.xml")
        def builder = new MarkupBuilder(writer)

        def targetName = 'profilesPackageXmlMerge'
        def metadataDir = "${config['build.dir']}/metadata"

        builder.project('default': targetName) {
            'import'(file: '../ant-includes/setup-target.xml')

            target(name: targetName) {
                data.each { type, filenames ->
                    def destDir = "$metadataDir/$type"
                    mkdir(dir: destDir)

                    filenames.each { filename ->
                        echo "Xml Merging: $filename"
                        xmlmerge(dest: "$destDir/$filename", conf: 'xmlmerge.properties'
                        ) {
                            fileset(dir: srcDir) {
                                include(name: "**/$filename")
                            }
                        }
                    }
                }

                copy(todir: metadataDir) {
                    fileset(dir: srcDir) {
                        include(name: '**/classes/*')
                        include(name: '**/pages/*')
                        include(name: '**/applications/*')
                        include(name: '**/objects/*')
                        include(name: '**/objectTranslations/*')
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

static void main(args) {
    def config = ['build.dir': 'build'];

    def cli = new CliBuilder(usage: 'force-meta-backup.groovy [options]')
    cli.with {
        b longOpt: 'build-dir', args: 1, 'build directory'
        h longOpt: 'help', 'usage information'
        _ longOpt: 'build-xml-merge-target', 'Builds XML Merge target for Profile and PermissionSets XML files'
    }

    def options = cli.parse(args)
    if (!options) {
        return
    }

    if (options.h) {
        cli.usage()
        return
    }

    if (options.b) {
        config['build.dir'] = options.b
    }

    def forceService = ForceServiceFactory.create('build.properties')

    if (options.'build-xml-merge-target') {
        def xmlMerge = new XmlMergeTargetBuilder(config)
        xmlMerge.writeBuildXml()
        return
    }

    // Default Action

    def bulk = new BulkMetadataManifestBuilder(forceService, config)
    bulk.writeBuildXml()
    
    def folders = new Folders(forceService, config)
    folders.writeFolderBulkRetriveXml()
    folders.writeFoldersPackageXml()

    def misc = new MiscMetadataManifestBuilder(forceService, config)
    misc.writePackageXml()

    def profiles = new ProfilesMetadataManifestBuilder(forceService, config)
    profiles.writePackageXml()
}