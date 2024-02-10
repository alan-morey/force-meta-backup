package src

/**
 * Metadata types to be retrieved with Ant task `sf:bulkRetrieve`
 */
class BulkRetrieveMetadataTypeRegistry {
    private final types = [
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
        'BotBlock',
        'BotTemplate',
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
        'ConversationChannelDefinition',
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
        'ExtlClntAppGlobalOauthSettings',
        'ExtlClntAppOauthConfigurablePolicies',
        'ExtlClntAppOauthSettings',
        'FeatureParameterBoolean',
        'FeatureParameterDate',
        'FeatureParameterInteger',
        'FieldRestrictionRule',
        'FlexiPage',
        'Flow',
        'FlowCategory',
        'FlowDefinition',
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
        'ManagedEventSubscription',
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
        'OauthTokenExchangeHandler',
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
    ].asImmutable()

    def getTypes() {
        types
    }
}
