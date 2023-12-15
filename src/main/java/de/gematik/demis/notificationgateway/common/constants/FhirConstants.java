/*
 * Copyright [2023], gematik GmbH
 *
 * Licensed under the EUPL, Version 1.2 or - as soon they will be approved by the
 * European Commission – subsequent versions of the EUPL (the "Licence").
 * You may not use this work except in compliance with the Licence.
 *
 * You find a copy of the Licence in the "Licence" file or at
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either expressed or implied.
 * In case of changes by gematik find details in the "Readme" file.
 *
 * See the Licence for the specific language governing permissions and limitations under the Licence.
 */

package de.gematik.demis.notificationgateway.common.constants;

import lombok.experimental.UtilityClass;

@UtilityClass
public class FhirConstants {
  public static final String DEMIS_RKI_DE_FHIR = "https://demis.rki.de/fhir/";

  // profiles for laboratory notification (Labormeldung)
  public static final String PROFILE_NOTIFICATION_BUNDLE_LABORATORY =
      DEMIS_RKI_DE_FHIR + "StructureDefinition/NotificationBundleLaboratory";
  public static final String PROFILE_NOTIFICATION_LABORATORY =
      DEMIS_RKI_DE_FHIR + "StructureDefinition/NotificationLaboratory";
  public static final String PROFILE_SUBMITTING_FACILITY =
      DEMIS_RKI_DE_FHIR + "StructureDefinition/SubmittingFacility";
  public static final String PROFILE_SUBMITTING_ROLE =
      DEMIS_RKI_DE_FHIR + "StructureDefinition/SubmittingRole";
  public static final String PROFILE_LABORATORY_REPORT =
      DEMIS_RKI_DE_FHIR + "StructureDefinition/LaboratoryReport";
  public static final String PROFILE_LABORATORY_REPORT_CVDP = PROFILE_LABORATORY_REPORT + "CVDP";
  public static final String PROFILE_PATHOGEN_DETECTION =
      DEMIS_RKI_DE_FHIR + "StructureDefinition/PathogenDetection";
  public static final String PROFILE_PATHOGEN_DETECTION_CVDP = PROFILE_PATHOGEN_DETECTION + "CVDP";
  public static final String PROFILE_SPECIMEN = DEMIS_RKI_DE_FHIR + "StructureDefinition/Specimen";
  public static final String PROFILE_SPECIMEN_CVDP = PROFILE_SPECIMEN + "CVDP";

  // profiles for disease notification (Arztmeldung)
  public static final String PROFILE_NOTIFICATION_BUNDLE_DISEASE =
      DEMIS_RKI_DE_FHIR + "StructureDefinition/NotificationBundleDisease";
  public static final String PROFILE_NOTIFICATION_DISEASE =
      DEMIS_RKI_DE_FHIR + "StructureDefinition/NotificationDiseaseCVDD";
  public static final String PROFILE_DISEASE_CVDD =
      DEMIS_RKI_DE_FHIR + "StructureDefinition/DiseaseCVDD";
  public static final String PROFILE_HOSPITALIZATION =
      DEMIS_RKI_DE_FHIR + "StructureDefinition/Hospitalization";
  public static final String PROFILE_IMMUNIZATION_INFORMATION_CVDD =
      DEMIS_RKI_DE_FHIR + "StructureDefinition/ImmunizationInformationCVDD";
  public static final String PROFILE_DISEASE_INFORMATION_COMMON =
      DEMIS_RKI_DE_FHIR + "StructureDefinition/DiseaseInformationCommon";
  public static final String PROFILE_DISEASE_INFORMATION_CVDD =
      DEMIS_RKI_DE_FHIR + "StructureDefinition/DiseaseInformationCVDD";

  // profiles for bed occupancy (Bettenbelegungsmeldung)
  public static final String PROFILE_REPORT_BUNLDE =
      DEMIS_RKI_DE_FHIR + "StructureDefinition/ReportBundle";
  public static final String PROFILE_REPORT_BED_OCCUPANCY =
      DEMIS_RKI_DE_FHIR + "StructureDefinition/ReportBedOccupancy";
  public static final String PROFILE_STATISTIC_INFORMATION_BED_OCCUPANCY =
      DEMIS_RKI_DE_FHIR + "StructureDefinition/StatisticInformationBedOccupancy";

  // profiles for laboratory and disease notification
  public static final String PROFILE_NOTIFIED_PERSON =
      DEMIS_RKI_DE_FHIR + "StructureDefinition/NotifiedPerson";
  public static final String PROFILE_NOTIFIER_FACILITY =
      DEMIS_RKI_DE_FHIR + "StructureDefinition/NotifierFacility";
  public static final String PROFILE_NOTIFIED_PERSON_FACILITY =
      DEMIS_RKI_DE_FHIR + "StructureDefinition/NotifiedPersonFacility";
  public static final String PROFILE_NOTIFIER_ROLE =
      DEMIS_RKI_DE_FHIR + "StructureDefinition/NotifierRole";

  public static final String PROFILE_ORGANIZATION =
      "http://hl7.org/fhir/StructureDefinition/Organization";

  // questionnaires
  public static final String QUESTIONAIRE_DISEASE_QUESTIONS_COMMON =
      DEMIS_RKI_DE_FHIR + "Questionnaire/DiseaseQuestionsCommon";
  public static final String QUESTIONAIRE_DISEASE_QUESTIONS_CVDD =
      DEMIS_RKI_DE_FHIR + "Questionnaire/DiseaseQuestionsCVDD";
  public static final String QUESTIONAIRE_STATISTIC_QUESTIONS_BED_OCCUPANCY =
      DEMIS_RKI_DE_FHIR + "Questionnaire/StatisticQuestionsBedOccupancy";

  // extensions
  public static final String STRUCTURE_DEFINITION_ADDRESS_USE =
      DEMIS_RKI_DE_FHIR + "StructureDefinition/AddressUse";
  public static final String STRUCTURE_DEFINITION_FACILITY_ADDRESS_NOTIFIED_PERSON =
      DEMIS_RKI_DE_FHIR + "StructureDefinition/FacilityAddressNotifiedPerson";
  public static final String STRUCTURE_DEFINITION_ADXP_STREET_NAME =
      "http://hl7.org/fhir/StructureDefinition/iso21090-ADXP-streetName";
  public static final String STRUCTURE_DEFINITION_ADXP_HOUSE_NUMBER =
      "http://hl7.org/fhir/StructureDefinition/iso21090-ADXP-houseNumber";
  public static final String STRUCTURE_DEFINITION_HOSPITALIZATION_NOTE =
      DEMIS_RKI_DE_FHIR + "StructureDefinition/HospitalizationNote";
  public static final String STRUCTURE_DEFINITION_RECEIVED_NOTIFICATION =
      DEMIS_RKI_DE_FHIR + "StructureDefinition/ReceivedNotification";

  // naming systems
  public static final String NAMING_SYSTEM_NOTIFICATION_BUNDLE_ID =
      DEMIS_RKI_DE_FHIR + "NamingSystem/NotificationBundleId";
  public static final String NAMING_SYSTEM_NOTIFICATION_ID =
      DEMIS_RKI_DE_FHIR + "NamingSystem/NotificationId";
  public static final String NAMING_SYSTEM_BSNR =
      "https://fhir.kbv.de/NamingSystem/KBV_NS_Base_BSNR";
  public static final String NAMING_SYSTEM_INEK_STANDORT_ID =
      DEMIS_RKI_DE_FHIR + "NamingSystem/InekStandortId";

  // code systems for laboratory notification
  public static final String CODE_SYSTEM_ADDRESS_USE = DEMIS_RKI_DE_FHIR + "CodeSystem/addressUse";
  public static final String CODE_SYSTEM_ORGANIZATION_TYPE =
      DEMIS_RKI_DE_FHIR + "CodeSystem/organizationType";
  public static final String CODE_SYSTEM_NOTIFICATION_CATEGORY =
      DEMIS_RKI_DE_FHIR + "CodeSystem/notificationCategory";
  public static final String CODE_SYSTEM_CONCLUSION_CODE =
      DEMIS_RKI_DE_FHIR + "CodeSystem/conclusionCode";

  // code systems for disease notification
  public static final String CODE_SYSTEM_NOTIFICATION_TYPE =
      DEMIS_RKI_DE_FHIR + "CodeSystem/notificationType";
  public static final String CODE_SYSTEM_SECTION_CODE =
      DEMIS_RKI_DE_FHIR + "CodeSystem/sectionCode";
  public static final String CODE_SYSTEM_NOTIFICATION_DISEASE_CATEGORY =
      DEMIS_RKI_DE_FHIR + "CodeSystem/notificationDiseaseCategory";
  public static final String CODE_SYSTEM_HOSPITALIZATION_SERVICE_TYPE =
      DEMIS_RKI_DE_FHIR + "CodeSystem/hospitalizationServiceType";
  public static final String CODE_SYSTEM_VACCINE = DEMIS_RKI_DE_FHIR + "CodeSystem/vaccine";
  public static final String CODE_SYSTEM_MILITARY_AFFILIATION =
      DEMIS_RKI_DE_FHIR + "CodeSystem/militaryAffiliation";
  public static final String CODE_SYSTEM_YES_OR_NO_ANSWER =
      DEMIS_RKI_DE_FHIR + "CodeSystem/yesOrNoAnswer";
  public static final String CODE_SYSTEM_ORGANIZATION_ASSOCIATION =
      DEMIS_RKI_DE_FHIR + "CodeSystem/organizationAssociation";
  public static final String CODE_SYSTEM_GEOGRAPHIC_REGION =
      DEMIS_RKI_DE_FHIR + "CodeSystem/geographicRegion";
  public static final String CODE_SYSTEM_INFECTION_ENVIRONMENT_SETTING =
      DEMIS_RKI_DE_FHIR + "CodeSystem/infectionEnvironmentSetting";

  // code systems for bed occupancy
  public static final String CODE_SYSTEM_REPORT_CATEGORY =
      DEMIS_RKI_DE_FHIR + "CodeSystem/reportCategory";
  public static final String CODE_SYSTEM_REPORT_SECTION =
      DEMIS_RKI_DE_FHIR + "CodeSystem/reportSection";

  // general code systems
  public static final String CODE_SYSTEM_OBSERVATION_INTERPRETATION =
      "http://terminology.hl7.org/CodeSystem/v3-ObservationInterpretation";
  public static final String CODE_SYSTEM_OBSERVATION_CATEGORY =
      "http://terminology.hl7.org/CodeSystem/observation-category";
  public static final String CODE_SYSTEM_CONDITION_VERIFICATION_STATUS =
      "http://terminology.hl7.org/CodeSystem/condition-ver-status";
  public static final String CODE_SYSTEM_ACT_CODE =
      "http://terminology.hl7.org/CodeSystem/v3-ActCode";
  public static final String CODE_SYSTEM_NULL_FLAVOR =
      "http://terminology.hl7.org/CodeSystem/v3-NullFlavor";

  public static final String SYSTEM_LOINC = "http://loinc.org";
  public static final String SYSTEM_SNOMED = "http://snomed.info/sct";

  public static final String COMMUNITY_REGISTER =
      "https://ec.europa.eu/health/documents/community-register/html/";
}
