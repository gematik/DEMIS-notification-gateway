package de.gematik.demis.notificationgateway.common.constants;

/*-
 * #%L
 * DEMIS Notification-Gateway
 * %%
 * Copyright (C) 2025 gematik GmbH
 * %%
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
 *
 * *******
 *
 * For additional notes and disclaimer from gematik and in case of changes by gematik find details in the "Readme" file.
 * #L%
 */

import lombok.experimental.UtilityClass;

@UtilityClass
public class FhirConstants {
  public static final String DEMIS_RKI_DE_FHIR = "https://demis.rki.de/fhir/";

  public static final String PROFILE_SUBMITTING_ROLE =
      DEMIS_RKI_DE_FHIR + "StructureDefinition/SubmittingRole";

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

  public static final String QUESTIONAIRE_STATISTIC_QUESTIONS_BED_OCCUPANCY =
      DEMIS_RKI_DE_FHIR + "Questionnaire/StatisticQuestionsBedOccupancy";

  // extensions
  public static final String STRUCTURE_DEFINITION_ADDRESS_USE =
      DEMIS_RKI_DE_FHIR + "StructureDefinition/AddressUse";
  public static final String STRUCTURE_DEFINITION_ADXP_STREET_NAME =
      "http://hl7.org/fhir/StructureDefinition/iso21090-ADXP-streetName";
  public static final String STRUCTURE_DEFINITION_ADXP_HOUSE_NUMBER =
      "http://hl7.org/fhir/StructureDefinition/iso21090-ADXP-houseNumber";

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

  // code systems for bed occupancy
  public static final String CODE_SYSTEM_REPORT_CATEGORY =
      DEMIS_RKI_DE_FHIR + "CodeSystem/reportCategory";
  public static final String CODE_SYSTEM_REPORT_SECTION =
      DEMIS_RKI_DE_FHIR + "CodeSystem/reportSection";

  // general code systems
  public static final String CODE_SYSTEM_CONDITION_VERIFICATION_STATUS =
      "http://terminology.hl7.org/CodeSystem/condition-ver-status";

  public static final String CODE_SYSTEM_NULL_FLAVOR =
      "http://terminology.hl7.org/CodeSystem/v3-NullFlavor";

  public static final String SYSTEM_LOINC = "http://loinc.org";
}
