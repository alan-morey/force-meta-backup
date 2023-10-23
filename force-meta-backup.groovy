#!/usr/bin/env groovy
@Grab(group='com.force.api', module='force-partner-api', version='58.0.0')
@Grab(group='com.force.api', module='force-metadata-api', version='58.0.0')

import com.sforce.soap.metadata.FileProperties
import com.sforce.soap.metadata.ListMetadataQuery
import com.sforce.soap.metadata.MetadataConnection
import com.sforce.soap.partner.Connector
import com.sforce.soap.partner.PartnerConnection
import com.sforce.ws.SoapFaultException
import com.sforce.ws.ConnectorConfig
import groovy.io.FileType
import groovy.xml.MarkupBuilder
import groovy.xml.StreamingMarkupBuilder
import groovy.xml.XmlUtil
import java.net.URLEncoder

class ForceServiceConnector {
    static final DEFAULT_API_VERSION = determineDefaultApiVersion()

    ConnectorConfig config
    def apiVersion

    PartnerConnection connection
    MetadataConnection metadataConnection

    ForceServiceConnector(serverUrl, username, password, apiVersion) {
        this.apiVersion = apiVersion

        this.config = new ConnectorConfig().with {
            it.username = username
            it.password = password
            it.authEndpoint = partnerEndpoint(serverUrl)
            it
        }
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
                it.serviceEndpoint = metadataEndpoint(partnerConfig.serviceEndpoint)
                it
            }

            metadataConnection = com.sforce.soap.metadata.Connector.newConnection(metadataConfig)
        }

        metadataConnection
    }

    public getApiVersion() {
        apiVersion
    }

    private String metadataEndpoint(url) {
        endpoint url, 'm', apiVersion
    }

    private String partnerEndpoint(url) {
        endpoint url, 'u', apiVersion
    }

    private endpoint(url, apiType, apiVersion) {
        def host = new URI(url).host
        "https://$host/services/Soap/$apiType/$apiVersion"
    }

    private static determineDefaultApiVersion() {
        // END_POINT is a URI where the last path segment is the API version
        Connector.END_POINT.toURI()
            .getPath()
            .split('/')
            [-1]
    }
}

class ForceService {
    final ForceServiceConnector forceServiceConnector
    def metadata
    def metadataTypes

    ForceService(ForceServiceConnector forceServiceConnector) {
        this.forceServiceConnector = forceServiceConnector
    }

    def getConnection() {
        forceServiceConnector.connection
    }

    def getMetadataConnection() {
        forceServiceConnector.metadataConnection
    }

    def getOrganizationId() {
        forceServiceConnector.connection.userInfo.organizationId
    }

    Double getApiVersion() {
        forceServiceConnector.apiVersion.toDouble()
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

        def numQueries = queries.size
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
    static create(ConfigObject config) {
        def connector = new ForceServiceConnector(
            config.sf.serverurl.toString(),
            config.sf.username.toString(),
            config.sf.password.toString(),
            config.sf.apiVersion.toString()
        )

        new ForceService(connector)
    }
}

class Configuration {
    static ConfigObject build(propFileName, cliOptions) {
        final def getCliOptionOrDefault = { option, defaultValue -> 
            cliOptions[option] ?: defaultValue
        }

        ConfigObject config = new ConfigObject()
        config.putAll([
            buildDir: getCliOptionOrDefault('build-dir', 'build')
        ])

        // Load properties files and CLI property overrides into config
        [
            loadPropertiesFromFile(propFileName),
            cliOverrideProperties(cliOptions)
        ].each {
            config.merge(toConfigObject(it))
        }

        if (!config.sf?.apiVersion) {
            // Set default apiVersion if not provided
            config.sf.apiVersion = ForceServiceConnector.DEFAULT_API_VERSION
        }

        expandExcludeTypes(config)

        config
    }

    private static Properties loadPropertiesFromFile(fileName) {
        def props = new Properties()

        new File(fileName).withInputStream {
            props.load(it)
        }

        props
    }

    private static toConfigObject(Properties p) {
        new ConfigSlurper().parse(p)
    }

    // Options provided with -Dproperty=value to script CLI
    private static cliOverrideProperties(cliOptions) {
        def props = new Properties();

        if (cliOptions?.Ds) {
            cliOptions?.Ds?.toSpreadMap().each { name, value ->
                props[name] = value
            }
        }

        props
    }

    private static expandExcludeTypes(config) {
        final propertyName = 'excludeTypes'
        def excludeTypes = [] as Set

        if (config.sf.excludeTypes) {
            excludeTypes.addAll(config.sf.excludeTypes.split(',').collect {
                it.trim().toLowerCase()
            })

            config.sf.remove(propertyName)
        }

        config.put(propertyName, excludeTypes)
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
}

class BulkMetadataManifestBuilder extends ManifestBuilder {
    def buildXmlPath

    static final BUILD_XML = 'bulk-retrievable-target.xml'

    static TYPES = [
        'AccountingFieldMapping',
        'AccountingModelConfig',
        'AccountRelationshipShareRule',
        'ActionLinkGroupTemplate',
        'ActionPlanTemplate',
        'ActivationPlatform',
        'ActivationPlatformField',
        'ActvPfrmDataConnectorS3',
        'ActvPlatformAdncIdentifier',
        'ActvPlatformFieldValue',
        'AIApplication',
        'AIApplicationConfig',
        'AnalyticSnapshot',
        'AnimationRule',
        'ApexEmailNotifications',
        'ApexTestSuite',
        'AppMenu',
        'AppointmentSchedulingPolicy',
        'ApprovalProcess',
        'ArticleType',
        'AssignmentRules',
        'Audience',
        'AuthProvider',
        'AutoResponseRules',
        'BatchCalcJobDefinition',
        'BatchProcessJobDefinition',
        'BlacklistedConsumer',
        'Bot',
        'BotVersion',
        'BrandingSet',
        'BriefcaseDefinition',
        'BusinessProcessGroup',
        'CallCenter',
        'CallCoachingMediaProvider',
        'CampaignInfluenceModel',
        'CanvasMetadata',
        'CaseSubjectParticle',
        'Certificate',
        'ChannelLayout',
        'ChatterExtension',
        'ClauseCatgConfiguration',
        'CleanDataService',
        'CMSConnectSource',
        'Community',
        'CommunityTemplateDefinition',
        'CommunityThemeDefinition',
        'ConnectedApp',
        'ContentAsset',
        'ContractType',
        'CorsWhitelistOrigin',
        'CspTrustedSite',
        'CustomApplicationComponent',
        'CustomFeedFilter',
        'CustomHelpMenuSection',
        'CustomIndex',
        'CustomLabels',
        'CustomMetadata',
        'CustomNotificationType',
        'CustomPageWebLink',
        'CustomSite',
        'DataCategoryGroup',
        'DataConnectorIngestApi',
        'DataPackageKitDefinition',
        'DataPackageKitObject',
        'DataSource',
        'DataSourceBundleDefinition',
        'DataSourceField',
        'DataSourceObject',
        'DataSrcDataModelFieldMap',
        'DataStreamDefinition',
        'DataStreamTemplate',
        'DataWeaveResource',
        'DecisionTable',
        'DecisionTableDatasetLink',
        'DelegateGroup',
        'DigitalExperienceBundle',
        'DigitalExperienceConfig',
        'DuplicateRule',
        'EclairGeoData',
        'EmailServicesFunction',
        'EmbeddedServiceBranding',
        'EmbeddedServiceConfig',
        'EmbeddedServiceFieldService',
        'EmbeddedServiceFlowConfig',
        'EmbeddedServiceLiveAgent',
        'EmbeddedServiceMenuSettings',
        'EntitlementProcess',
        'EntitlementTemplate',
        'EntityImplements',
        'EscalationRules',
        'ESignatureConfig',
        'ESignatureEnvelopConfig',
        'EventDelivery',
        'EventRelayConfig',
        'EventSubscription',
        'ExperienceBundle',
        'ExperiencePropertyTypeBundle',
        'ExternalCredential',
        'ExternalDocStorageConfig',
        'ExternalServiceRegistration',
        'FeatureParameterBoolean',
        'FeatureParameterDate',
        'FeatureParameterInteger',
        'FieldRestrictionRule',
        'FlexiPage',
        'Flow',
        'FlowCategory',
        'FlowDefinition',
        'FlowOrchestratedStage',
        'FlowStageStep',
        'FlowStageStepAssignee',
        'FlowTest',
        'ForecastingFilter',
        'ForecastingFilterCondition',
        'ForecastingSourceDefinition',
        'ForecastingType',
        'ForecastingTypeSource',
        'FuelType',
        'FuelTypeSustnUom',
        'FunctionReference',
        'GlobalValueSet',
        'GlobalValueSetTranslation',
        'Group',
        'HomePageComponent',
        'HomePageLayout',
        'InboundNetworkConnection',
        'InstalledPackage',
        'IPAddressRange',
        'KeywordList',
        'LeadConvertSettings', 
        'Letterhead',
        'LightningBolt',
        'LightningExperienceTheme',
        'LightningMessageChannel',
        'LightningOnboardingConfig',
        'LiveChatAgentConfig',
        'LiveChatButton',
        'LiveChatDeployment',
        'LiveChatSensitiveDataRule',
        'ManagedContentType',
        'ManagedContentTypeBundle',
        'ManagedTopics',
        'MarketingAppExtAction',
        'MatchingRules',
        'MessagingChannel',
        'MilestoneType',
        'MktCalcInsightObjectDef',
        'MktDataTranObject ',
        'MLDataDefinition',
        'MlDomain',
        'MLPredictionDefinition',
        'MLRecommendationDefinition',
        'MobileApplicationDetail',
        'ModerationRule',
        'MutingPermissionSet',
        'MyDomainDiscoverableLogin',
        'NamedCredential',
        'NavigationMenu',
        'Network',
        'NetworkBranding',
        'NotificationTypeConfig',
        'OauthCustomScope',
        'OcrSampleDocument',
        'OcrTemplate',
        'OmniInteractionAccessConfig',
        'OmniInteractionConfig',
        'OmniSupervisorConfig',
        'OrchestrationContext',
        'OrchestrationContextEvents',
        'OutboundNetworkConnection',
        'PathAssistant',
        'PaymentGatewayProvider',
        'PermissionSet',
        'PermissionSetGroup',
        'PersonAccountOwnerPowerUser',
        'PipelineInspMetricConfig',
        'PlatformCachePartition',
        'PlatformEventChannel',
        'PlatformEventChannelMember',
        'PlatformEventSubscriberConfig',
        'Portal',
        'PortalDelegablePermissionSet',
        'PostTemplate',
        'PresenceDeclineReason',
        'PresenceUserConfig',
        'ProcessFlowMigration',
        'ProfilePasswordPolicy',
        'ProfileSessionSetting',
        'Prompt',
        'Queue',
        'QueueRoutingConfig',
        'QuickAction',
        'RecommendationStrategy',
        'RecordActionDeployment',
        'RedirectWhitelistUrl',
        'RemoteSiteSetting',
        'ReportType',
        'RestrictionRule',
        'Role',
        'RoleOrTerritory',
        'SalesWorkQueueSettings',
        'SamlSsoConfig',
        'SchedulingObjective',
        'SchedulingRule',
        'Scontrol',
        'ServiceAISetupDefinition',
        'ServiceAISetupField',
        'ServiceChannel',
        'ServicePresenceStatus',
        'Settings',
        'SharingRules',
        'SharingSet',
        'SiteDotCom',
        'Skill',
        'SlackApp',
        'StandardValueSetTranslation',
        'StaticResource',
        'StreamingAppDataConnector',
        'SustnUomConversion',
        'SvcCatalogCategory',
        'SvcCatalogFulfillmentFlow',
        'SvcCatalogItemDef',
        'SynonymDictionary',
        'Territory',
        'Territory2',
        'Territory2Model',
        'Territory2Rule',
        'Territory2Type',
        'TimeSheetTemplate',
        'TopicsForObjects',
        'TransactionSecurityPolicy',
        'Translations',
        'UserAccessPolicy',
        'UserCriteria',
        'UserProfileSearchScope',
        'UserProvisioningConfig',
        'WaveApplication',
        'WaveDashboard',
        'WaveDataflow',
        'WaveDataset',
        'WaveLens',
        'WaveRecipe',
        'WaveTemplateBundle',
        'WaveXmd',
        'WebStoreTemplate',
        'WorkDotComSettings',
        'Workflow',
        'WorkSkillRouting',
        'XOrgHub',
        'XOrgHubSharedObject',
    ]

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
                parallel(threadCount: 4) {
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
                parallel(threadCount: 4) {
                    'sfRetrieve'(unpackaged: packageXmlPath)

                    allFolders.each { folderType, folders ->
                        folders.each { folderName ->
                            def type = folderMetaTypeByFolderType[folderType]
                            'sfBulkRetrieveFolder'(metadataType: type, containingFolder: folderName)
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
        'CustomPermission',
        'StandardValueSet',
    ]

    static final WILDCARD_TYPES = [
        'ApexComponent',
        'ApexTrigger',
        'AuraDefinitionBundle', 
        'LightningComponentBundle',
        'StandardValueSetTranslation',
    ]

    static final STANDARD_VALUE_SET_NAMES = [
        'AAccreditationRating',
        'AccountContactMultiRoles',
        'AccountContactRole',
        'AccountOwnership',
        'AccountRating',
        'AccountType',
        'AccreditationAccreditingBody',
        'AccreditationStatus',
        'AccreditationSubType',
        'AccreditationType',
        'ACInitSumEmployeeType',
        'ACInitSumInitiativeType',
        'ACISumRecipientCategory',
        'ACorruptionInitSumCountry',
        'ACorruptionInitSumRegion',
        'ActivityTimeEnum',
        'AdmissionSource',
        'AdmissionType',
        'AllergyIntoleranceCategory',
        'AllergyIntoleranceSeverity',
        'AllergyIntoleranceStatus',
        'AllergyIntoleranceType',
        'AllergyVerificationStatus',
        'AppealRequestReasonType',
        'ApprovedLevelOfCare',
        'AQuestionQuestionCategory',
        'AReasonAppointmentReason',
        'AssessmentRating',
        'AssessmentStatus',
        'AssetActionCategory',
        'AssetRelationshipType',
        'AssetStatus',
        'AssociatedLocationType',
        'AuthorNoteRecipientType',
        'BarrierCodeType',
        'BCCertificationType',
        'BLicenseJurisdictionType',
        'BLicenseVerificationStatus',
        'BoardCertificationStatus',
        'BusinessLicenseStatus',
        'CampaignMemberStatus',
        'CampaignStatus',
        'CampaignType',
        'CardType',
        'CareAmbulanceTransReason',
        'CareAmbulanceTransType',
        'CareBarrierPriority',
        'CareBarrierStatus',
        'CareBenefitVerifyRequestStatus',
        'CareDeterminantPriority',
        'CareDeterminantTypeDomain',
        'CareDeterminantTypeType',
        'CareEpisodeStatus',
        'CareEpisodeType',
        'CareItemStatus',
        'CareItemStatusReason',
        'CareMetricTargetType',
        'CareObservationCategory',
        'CareObservationStatus',
        'CarePlanActivityStatus',
        'CarePlanAuthorizationType',
        'CarePlanDetailDetailType',
        'CarePreauthItemLaterality',
        'CarePreauthStatus',
        'CareProgramEnrolleeStatus',
        'CareProgramGoalPriority',
        'CareProgramGoalStatus',
        'CareProgramProductStatus',
        'CareProgramProviderRole',
        'CareProgramProviderStatus',
        'CareProgramStatus',
        'CareProgramTeamMemberRole',
        'CareQuantityType',
        'CareRegisteredDeviceStatus',
        'CareRequestExtensionAmbulanceTransportReason',
        'CareRequestExtensionAmbulanceTransportType',
        'CareRequestExtensionNursingHomeResidentialStatus',
        'CareRequestExtensionRequestType',
        'CareRequestExtensionServiceLevel',
        'CareRequestMemberGender',
        'CareRequestMemberPrognosis',
        'CareRequestQuantityType',
        'CareRequestReviewerStatus',
        'CareSpecialtySpecialtyType',
        'CareSpecialtySpecialtyUsage',
        'CareTaxonomyTaxonomyType',
        'CareTeamStatus',
        'CaseContactRole',
        'CaseOrigin',
        'CasePriority',
        'CaseReason',
        'CaseServicePlanStatus',
        'CaseStatus',
        'CaseType',
        'CBCoverageType',
        'CBenefitItemLimitTermType',
        'CBItemLimitCoverageLevel',
        'CBItemLimitNetworkType',
        'CCFALineOfBusiness',
        'CCPAdditionalBenefits',
        'CCProjectMitigationType',
        'CCPStandardsAgencyName',
        'CCreditProjectProjectType',
        'CDPresentOnAdmission',
        'CEIdentifierIdUsageType',
        'CEncounterAdmissionSource',
        'CEncounterCategory',
        'CEncounterDietPreference',
        'CEncounterFacilityStatus',
        'CEncounterServiceType',
        'CEncounterSpecialCourtesy',
        'CEncounterStatus',
        'CEpisodeDetailDetailType',
        'ChangeRequestRelatedItemImpactLevel',
        'ChangeRequestBusinessReason',
        'ChangeRequestCategory',
        'ChangeRequestImpact',
        'ChangeRequestPriority',
        'ChangeRequestRiskLevel',
        'ChangeRequestStatus',
        'ClassRankReportingFormat',
        'ClassRankWeightingType',
        'ClinicalAlertCategories',
        'ClinicalAlertStatus',
        'ClinicalCaseType',
        'ClinicalDetectedIssueSeverityLevel',
        'ClinicalDetectedIssueStatus',
        'COComponentValueType',
        'COCValueInterpretation',
        'CodeSetCodeSetType',
        'CommunicationChannel',
        'CompanyRelationshipType',
        'ConsequenceOfFailure',
        'ContactPointAddressType',
        'ContactPointUsageType',
        'ContactRequestReason',
        'ContactRequestStatus',
        'ContactRole',
        'ContractContactRole',
        'ContractLineItemStatus',
        'ContractStatus',
        'COProcessingResult',
        'COValueInterpretation',
        'CPAActivityType',
        'CPADetailDetailType',
        'CPADetailDetailType',
        'CPAdverseActionActionType',
        'CPAdverseActionStatus',
        'CPAgreementAgreementType',
        'CPAgreementLineofBusiness',
        'CPAProhibitedActivity',
        'CPDProblemPriority',
        'CPEligibilityRuleStatus',
        'CPEnrolleeProductStatus',
        'CPEnrollmentCardStatus',
        'CPFSpecialtySpecialtyRole',
        'CProgramProductAvailability',
        'CPTemplateProblemPriority',
        'CRDDrugAdministrationSetting',
        'CRDNameType',
        'CCRDPriority',
        'CCRDPriority',
        'CRDRequestType',
        'CRDStatus',
        'CRDStatusReason',
        'CRECaseSubStatus',
        'CRECaseSubStatus',
        'CREDocumentAttachmentStatus',
        'CREIndependentReviewDetermination',
        'CREPriorAuthRequestIdentifier',
        'CREPriorDischargeStatus',
        'CREReopenRequestOutcome',
        'CREReopenRequestType',
        'CRERequestOutcome',
        'CRIApprovedLevelOfCare',
        'CRIClinicalDetermination',
        'CRICurrentLevelOfCare',
        'CRIDeniedLevelOfCare',
        'CRIModifiedLevelOfCare',
        'CRIPriority',
        'CRIRequestedLevelOfCare',
        'CRIRequestType',
        'CRReviewerReviewerType',
        'CSBundleUsageType',
        'CServiceRequestIntent',
        'CServiceRequestPriority',
        'CServiceRequestStatus',
        'CSRequestDetailDetailType',
        'CurrentLevelOfCare',
        'DChecklistItemStatus',
        'DecisionReason',
        'DeniedLevelOfCare',
        'DiagnosisCodeType',
        'DiagnosticSummaryCategory',
        'DiagnosticSummaryStatus',
        'DigitalAssetStatus',
        'DEInclSumDiversityType',
        'DEInclSumEmployeeType',
        'DEInclSumEmploymentType',
        'DEInclSumGender',
        'DEISumDiversityCategory',
        'DischargeDiagnosisCodeType',
        'DIssueDetailType',
        'DivrsEquityInclSumLocation',
        'DivrsEquityInclSumRace',
        'DrugClinicalDetermination',
        'DSDDocumentRelationType',
        'DSDocumentStage',
        'DSummaryDetailDetailType',
        'DSummaryUsageType',
        'ECTypeContactPointType',
        'EducationLevel',
        'EEligibilityCriteriaStatus',
        'EmploymentOccupation',
        'EmploymentStatus',
        'EngagementAttendeeRole',
        'EngagementSentimentEnum',
        'EngagementStatusEnum',
        'EngagementTypeEnum',
        'EnrolleeOptOutReasonType',
        'EntitlementType',
        'EBSEmployeeBenefitType',
        'EBSPercentageCalcType',
        'EBSummaryBenefitUsage',
        'EBSummaryEmploymentType',
        'EBSEmployeeBenefitType',
        'EBSPercentageCalcType',
        'EDemographicSumAgeGroup',
        'EDemographicSumGender',
        'EDemographicSumRegion',
        'EDemographicSumReportType',
        'EDemographicSumWorkType',
        'EDevelopmentSumGender',
        'EDSumEmployeeType',
        'EDSumEmploymentType',
        'EDSumProgramCategory',
        'EPSumMarket',
        'EPSumPerformanceCategory',
        'EPSumPerformanceType',
        'EPSumRegion',
        'ERCompanyBusinessRegion',
        'ERCompanySector',
        'EReductionTargetTargetType',
        'ERTargetOtherTargetKpi',
        'ERTTargetSettingMethod',
        'EventSubject',
        'EventType',
        'FacilityRoomBedType',
        'FinalLevelOfCare',
        'FinanceEventAction',
        'FinanceEventType',
        'FiscalYearPeriodName',
        'FiscalYearPeriodPrefix',
        'FiscalYearQuarterName',
        'FiscalYearQuarterPrefix',
        'ForecastingItemCategory',
        'FreightHaulingMode',
        'FtprntAuditApprovalStatus',
        'FulfillmentStatus',
        'FulfillmentType',
        'GADetailDetailType',
        'GoalAssignmentProgressionStatus',
        'GoalAssignmentStatus',
        'GoalDefinitionCategory',
        'GoalDefinitionUsageType',
        'GovtFinancialAsstSumType',
        'GpaWeightingType',
        'GrievanceType',
        'HCFacilityLocationType',
        'HcpCategory',
        'HcpCodeType',
        'HealthCareDiagnosisCategory',
        'HealthCareDiagnosisCodeType',
        'HealthCareDiagnosisGender',
        'HealthcareProviderStatus',
        'HealthConditionDetailType',
        'HealthConditionSeverity',
        'HealthConditionStatus',
        'HealthConditionType',
        'HealthDiagnosticStatus',
        'HFNetworkGenderRestriction',
        'HFNetworkPanelStatus',
        'HPayerNetworkNetworkType',
        'HPayerNwkLineOfBusiness',
        'HPFGenderRestriction',
        'HPFTerminationReason',
        'HProviderNpiNpiType',
        'HProviderProviderClass',
        'HProviderProviderType',
        'HPSpecialtySpecialtyRole',
        'HSActionLogActionStatus',
        'IaApplnStatus',
        'IaAuthCategory',
        'IaInternalStatus',
        'IAItemStatus',
        'IARejectionReason',
        'IAServiceType',
        'IdeaCategory',
        'IdeaMultiCategory',
        'IdeaStatus',
        'IdeaThemeStatus',
        'IdentifierIdUsageType',
        'IFnolChannel',
        'IncidentCategory',
        'IncidentImpact',
        'IncidentPriority',
        'IncidentRelatedItemImpactLevel',
        'IncidentRelatedItemImpactType',
        'IncidentReportedMethod',
        'IncidentStatus',
        'IncidentSubCategory',
        'IncidentType',
        'IncidentUrgency',
        'Industry',
        'InterventionCodeType',
        'IPCancelationReasonType',
        'IPCBenefitPaymentFrequency',
        'IPCCategory',
        'IPCCategoryGroup',
        'IPCDeathBenefitOptionType',
        'IPCIncomeOptionType',
        'IPCLimitRange',
        'IPolicyAuditTerm',
        'IPolicyChangeSubType',
        'IPolicyChangeType',
        'IPolicyChannel',
        'IPolicyPlanTier',
        'IPolicyPlanType',
        'IPolicyPolicyType',
        'IPolicyPremiumCalcMethod',
        'IPolicyPremiumFrequency',
        'IPolicyPremiumPaymentType',
        'IPolicyStatus',
        'IPolicySubStatusCode',
        'IPolicyTerm',
        'IPolicyTransactionStatus',
        'IPolicyTransactionType',
        'IPOwnerPOwnerType',
        'IPParticipantRole',
        'IPPRelationshipToInsured',
        'LeadSource',
        'LeadStatus',
        'LicenseClassType',
        'LineOfAuthorityType',
        'LocationType',
        'LPIApplnCategory',
        'LPIApplnStatus',
        'MedicationCategoryEnum',
        'MedicationDispenseMedAdministrationSettingCategory',
        'MedicationDispenseStatus',
        'MedicationDispenseSubstitutionReason',
        'MedicationDispenseSubstitutionType',
        'MedicationStatementStatus',
        'MedicationStatus',
        'MedReviewRepresentativeType',
        'MedTherapyReviewSubtype',
        'MemberPlanPrimarySecondaryTertiary',
        'MemberPlanRelToSub',
        'MemberPlanStatus',
        'MemberPlanVerificStatus',
        'MilitaryService',
        'ModifiedCareCodeType',
        'ModifiedDiagnosisCodeType',
        'ModifiedDrugCodeType',
        'ModifiedLevelOfCare',
        'MRequestPriority',
        'MRequestStatus',
        'MRequestTherapyDuration',
        'MRequestType',
        'MStatementDeliverySetting',
        'MStatementDetailType',
        'OcrService',
        'OcrStatus',
        'OIncidentSummaryHazardType',
        'OISCorrectiveActionType',
        'OISummaryIncidentSubtype',
        'OISummaryIncidentType',
        'OISummaryPenaltyType',
        'OpportunityCompetitor',
        'OpportunityStage',
        'OpportunityType',
        'OrderItemSummaryChgRsn',
        'OrderStatus',
        'OrderSummaryRoutingSchdRsn',
        'OrderSummaryStatus',
        'OrderType',
        'ParProvider',
        'PartnerRole',
        'PartyProfileCountryofBirth',
        'PartyProfileEmploymentType',
        'PartyProfileFundSource',
        'PartyProfileGender',
        'PartyProfileResidentType',
        'PartyProfileReviewDecision',
        'PartyProfileRiskType',
        'PartyProfileStage',
        'PartyScreeningStepType',
        'PartyScreeningSummaryStatus',
        'PatientImmunizationStatus',
        'PEFEFctrDataSourceType',
        'PersonEmploymentType',
        'PersonLanguageLanguage',
        'PersonLanguageSpeakingProficiencyLevel',
        'PersonLanguageWritingProficiencyLevel',
        'PersonNameNameUsageType',
        'PersonVerificationStatus',
        'PHealthReactionSeverity',
        'PIdentityVerificationResult',
        'PIdentityVerificationStatus',
        'PIVerificationStepStatus',
        'PIVerificationStepType',
        'PIVerificationVerifiedBy',
        'PIVOverriddenResult',
        'PIVResultOverrideReason',
        'PIVSVerificationDecision',
        'PlaceOfService',
        'PlanBenefitStatus',
        'PMDDosageDefinitionType',
        'PMDosageDosageAmountType',
        'PMDosageRateType',
        'PMPDetailDetailType',
        'PMPOutcome',
        'PMPStatus',
        'PPCreditScoreProvider',
        'PPPrimaryIdentifierType',
        'PProfileAddressAddressType',
        'PProfileCountryOfDomicile',
        'PProfileEmploymentIndustry',
        'PProfileNationality',
        'PProfileOffBoardingReason',
        'PProfileRiskRiskCategory',
        'PPROverridenRiskCategory',
        'PPTaxIdentificationType',
        'ProblemCategory',
        'ProblemDefinitionCategory',
        'ProblemDefinitionPriority',
        'ProblemDefinitionUsageTypeEnum',
        'ProblemImpact',
        'ProblemPriority',
        'ProblemRelatedItemImpactLevel',
        'ProblemRelatedItemImpactType',
        'ProblemStatus',
        'ProblemSubCategory',
        'ProblemUrgency',
        'ProcessExceptionCategory',
        'ProcessExceptionPriority',
        'ProcessExceptionSeverity',
        'ProcessExceptionStatus',
        'Product2Family',
        'ProdRequestLineItemStatus',
        'ProductLineEnum',
        'ProductRequestStatus',
        'ProgressionCriteriaMet',
        'PScreeningStepResultCode',
        'PScreeningStepStatus',
        'PSSResultOverrideReason',
        'PSStepMatchedFieldList',
        'PSSummaryScreenedBy',
        'PSSummaryScreeningDecision',
        'PurchaserPlanAffiliation',
        'PurchaserPlanStatus',
        'PurchaserPlanType',
        'QuantityUnitOfMeasure',
        'QuestionOrigin',
        'QuickTextCategory',
        'QuickTextChannel',
        'QuoteStatus',
        'ReceivedDocumentDirection',
        'ReceivedDocumentOcrStatus',
        'ReceivedDocumentPriority',
        'ReceivedDocumentStatus',
        'RegAuthCategory',
        'RegulatoryBodyType',
        'ReopenReason',
        'RequestedCareCodeType',
        'RequestedDrugCodeType',
        'RequestedLevelOfCare',
        'RequesterType',
        'RequestingPractitionerLicense',
        'RequestingPractitionerSpecialty',
        'ResidenceStatusType',
        'RoleInTerritory2',
        'ResourceAbsenceType ',
        'ReturnOrderLineItemProcessPlan',
        'ReturnOrderLineItemReasonForRejection',
        'ReturnOrderLineItemReasonForReturn',
        'ReturnOrderLineItemRepaymentMethod',
        'ReturnOrderShipmentType',
        'ReturnOrderStatus',
        'SalesTeamRole',
        'Salutation',
        'SAppointmentGroupStatus',
        'ScorecardMetricCategory',
        'ScienceBasedTargetStatus',
        'SContributionSumCategory',
        'Scope3CrbnFtprntStage',
        'ServiceAppointmentStatus',
        'ServiceContractApprovalStatus',
        'ServicePlanTemplateStatus',
        'ServicingPractitionerLicense',
        'ServicingPractitionerSpecialty',
        'ServTerrMemRoleType',
        'ShiftStatus',
        'SocialContributionSumType',
        'SocialPostClassification',
        'SocialPostEngagementLevel',
        'SocialPostReviewedStatus',
        'SolutionStatus',
        'SourceBusinessRegion',
        'StatusReason',
        'StnryAssetCrbnFtprntStage',
        'StnryAstCrbnFtAllocStatus',
        'StnryAstCrbnFtDataGapSts',
        'StnryAstCrbnFtAllocStatus',
        'StnryAstCrbnFtDataGapSts',
        'StnryAstEvSrcStnryAstTyp',
        'StnryAssetWaterFtprntStage',
        'SupplierClassification',
        'SupplierEmssnRdctnCmtTypev',
        'SupplierReportingScope',
        'SupplierTier',
        'SustainabilityScorecardStatus',
        'TaskPriority',
        'TaskStatus',
        'TaskSubject',
        'TaskType',
        'TCDDetailType',
        'TCPriority',
        'TCStatus',
        'TCStatusReason',
        'TopicFailureReasonEnum',
        'TopicProcessStatusEnum',
        'TrackedCommunicationType',
        'TypesOfIntervention',
        'UnitOfMeasure',
        'UnitOfMeasureType',
        'VehicleAstCrbnFtprntStage',
        'VehicleType',
        'WasteFootprintStage',
        'WasteDisposalType',
        'WasteType',
        'WorkOrderLineItemPriority',
        'WorkOrderLineItemStatus',
        'WorkOrderPriority',
        'WorkOrderStatus',
        'WorkStepStatus',
        'WorkTypeDefApptType',
        'WorkTypeGroupAddInfo',
    ]


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
        'CustomObjectTranslation',
        'CustomTab',
        'ExternalDataSource',
        'Layout'
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
                parallel(threadCount: 4) {
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

                parallel(threadCount: 1) {
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

static void main(args) {
    def cli = new CliBuilder(usage: 'force-meta-backup.groovy [options]')
    cli.with {
        b longOpt: 'build-dir', args: 1, 'build directory'
        h longOpt: 'help', 'usage information'
        D(args: 2, valueSeparator: '=', argName: 'property=value', 'set property to override properties defined in build.properties file')
        _ longOpt: 'build-xml-merge-target', 'Builds XML Merge target for Profile XML files'
    }

    def options = cli.parse(args)
    if (!options) {
        return
    }

    if (options.h) {
        cli.usage()
        return
    }

    def config = Configuration.build("build.properties", options)


    def forceService = ForceServiceFactory.create(config)

    if (options.'build-xml-merge-target') {
        def xmlMerge = new XmlMergeTargetBuilder(config)
        xmlMerge.writeBuildXml()
        return
    }

    // Default Action
    [
        new BulkMetadataManifestBuilder(forceService, config),
        new Folders(forceService, config),
        new MiscMetadataManifestBuilder(forceService, config),
        new ProfilesMetadataManifestBuilder(forceService, config)
    ].each { it.writeManifest() }
}
