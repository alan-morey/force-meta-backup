package src

/**
 * Salesforce StandardValueSet Names
 *
 * https://developer.salesforce.com/docs/atlas.en-us.api_meta.meta/api_meta/standardvalueset_names.htm
 */
class StandardValueSetRegistry {
    private final names = [
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
        'AFSIBuiltUpAreaUnit',
        'AFSIConstructionStage',
        'AFSIIntendedUse',
        'AFSIItemCondition',
        'AFSIType',
        'AllergyIntoleranceCategory',
        'AllergyIntoleranceSeverity',
        'AllergyIntoleranceStatus',
        'AllergyIntoleranceType',
        'AllergyVerificationStatus',
        'AppealRequestReasonType',
        'AppFormProductDisbursement',
        'ApplicantBusinessEntityType',
        'ApplicantPhoneType',
        'ApplicantRole',
        'ApplicantSalutation',
        'ApplicantStage',
        'ApplicationActionItemStatus',
        'ApplicationActionItemType',
        'ApplicationFormIntakeChannelType',
        'ApplicationFormProductApplicantVisibleStatus',
        'ApplicationFormProductFeeType',
        'ApplicationFormProductLoanPurpose',
        'ApplicationFormProductLoanType',
        'ApplicationFormProductOwnership',
        'ApplicationFormProductPartnerVisibleStatus',
        'ApplicationFormProductProposalInterestRateType',
        'ApplicationFormProductProposalSelectedBy',
        'ApplicationFormProductProposalStage',
        'ApplicationFormProductStage',
        'ApplicationFormStage',
        'ApplicationFormTierType',
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
        'BenefitProcessType',
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
        'CaseEpisodeSubType',
        'CaseEpisodeType',
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
        'CCRDPriority',
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
        'ChangeRequestBusinessReason',
        'ChangeRequestCategory',
        'ChangeRequestImpact',
        'ChangeRequestPriority',
        'ChangeRequestRelatedItemImpactLevel',
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
        'CPPRole',
        'CProgramProductAvailability',
        'CPTemplateProblemPriority',
        'CRDDrugAdministrationSetting',
        'CRDNameType',
        'CRDRequestType',
        'CRDStatus',
        'CRDStatusReason',
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
        'DEInclSumDiversityType',
        'DEInclSumEmployeeType',
        'DEInclSumEmploymentType',
        'DEInclSumGender',
        'DEISumDiversityCategory',
        'DeniedLevelOfCare',
        'DiagnosisCodeType',
        'DiagnosticSummaryCategory',
        'DiagnosticSummaryStatus',
        'DigitalAssetStatus',
        'DischargeDiagnosisCodeType',
        'DIssueDetailType',
        'DivrsEquityInclSumLocation',
        'DivrsEquityInclSumRace',
        'DrugClinicalDetermination',
        'DSDDocumentRelationType',
        'DSDocumentStage',
        'DSummaryDetailDetailType',
        'DSummaryUsageType',
        'EBSEmployeeBenefitType',
        'EBSPercentageCalcType',
        'EBSummaryBenefitUsage',
        'EBSummaryEmploymentType',
        'ECTypeContactPointType',
        'EDemographicSumAgeGroup',
        'EDemographicSumGender',
        'EDemographicSumRegion',
        'EDemographicSumReportType',
        'EDemographicSumWorkType',
        'EDevelopmentSumGender',
        'EDSumEmployeeType',
        'EDSumEmploymentType',
        'EDSumProgramCategory',
        'EducationLevel',
        'EEligibilityCriteriaStatus',
        'EmploymentOccupation',
        'EmploymentStatus',
        'EngagementAttendeeRole',
        'EngagementSentimentEnum',
        'EngagementStatusEnum',
        'EngagementTypeEnum',
        'EnrolleeOptOutReasonType',
        'EnrollmentStatus',
        'EntitlementType',
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
        'LPEVerificationStatus',
        'LPIApplnCategory',
        'LPIApplnStatus',
        'LPIIncomeFrequency',
        'LPIIncomeStatus',
        'LPIIncomeType',
        'LPIVerificationStatus',
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
        'PartyExpenseStatus',
        'PartyExpenseType',
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
        'PaymentMandateAccountType',
        'PaymentMandateAuthorizationType',
        'PaymentMandateMandateFrequency',
        'PaymentMandateMandateStatus',
        'PaymentMandateMandateType',
        'PEFEFctrDataSourceType',
        'PERecurrenceInterval',
        'PersonEmploymentType',
        'PersonLanguageLanguage',
        'PersonLanguageSpeakingProficiencyLevel',
        'PersonLanguageWritingProficiencyLevel',
        'PersonNameNameUsageType',
        'PersonVerificationStatus',
        'PFAOwnershipTypeEnum',
        'PFAStatusEnum',
        'PFATypeEnum',
        'PFAVerificationStatusEnum',
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
        'ProdRequestLineItemStatus',
        'Product2Family',
        'ProductFeeFrequency',
        'ProductFeeType',
        'ProductLineEnum',
        'ProductRequestStatus',
        'ProgressionCriteriaMet',
        'PScreeningStepResultCode',
        'PScreeningStepStatus',
        'PSSResultOverrideReason',
        'PSStepMatchedFieldList',
        'PSSummaryScreenedBy',
        'PSSummaryScreeningDecision',
        'PtyFinclLiabilityShareType',
        'PtyFinclLiabilityStatus',
        'PtyFinclLiabilityType',
        'PtyFinclLiabilityVerficationStatus',
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
        'ResourceAbsenceType',
        'ReturnOrderLineItemProcessPlan',
        'ReturnOrderLineItemReasonForRejection',
        'ReturnOrderLineItemReasonForReturn',
        'ReturnOrderLineItemRepaymentMethod',
        'ReturnOrderShipmentType',
        'ReturnOrderStatus',
        'RLAAmortType',
        'RLAAppliedFor',
        'RLAAppStatus',
        'RLAEstateType',
        'RLALoanPurpose',
        'RLAMortLienType',
        'RLANativeTenure',
        'RLAProjectType',
        'RLARefinanceType',
        'RLARefProgType',
        'RLATitleType',
        'RLATrustType',
        'RoleInTerritory2',
        'SalesTeamRole',
        'Salutation',
        'SAppointmentGroupStatus',
        'ScienceBasedTargetStatus',
        'SContributionSumCategory',
        'Scope3CrbnFtprntStage',
        'ScorecardMetricCategory',
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
        'StnryAssetWaterFtprntStage',
        'StnryAstCrbnFtAllocStatus',
        'StnryAstCrbnFtDataGapSts',
        'StnryAstEvSrcStnryAstTyp',
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
        'WasteDisposalType',
        'WasteFootprintStage',
        'WasteType',
        'WorkOrderLineItemPriority',
        'WorkOrderLineItemStatus',
        'WorkOrderPriority',
        'WorkOrderStatus',
        'WorkStepStatus',
        'WorkTypeDefApptType',
        'WorkTypeGroupAddInfo',
    ].asImmutable()

    def getValueSetNames() {
        names
    }
}
