Feature: Immunization Creator Test
# Story: https://service.gematik.de/browse/DSC2-3025
# Hier sollen verschiedene Impfstoffe validiert werden
  Scenario Outline: Ueberpruefe die Impfstoffe
    Given Token fuer "Gateway" wurde geholt
    When Eine Portalmeldung aus der Datei <datei> an Notification-Gateway gesendet wird
    Then Ueberprueft Notification-Gateway den Impfstoffcode <impfstoffCode> mit dem Impfstoffname <impfstoffDisplay> fuer das System <system>
    And Wird eine Antwort mit dem Http-Statuscode 200 erwartet

    Examples:
      | datei                                                                         | system                                                           | impfstoffCode  | impfstoffDisplay                                       |
      | "portal/disease/immunization/notification_content_vaccine_moderna.json"       | "https://ec.europa.eu/health/documents/community-register/html/" | "EU/1/20/1507" | "Spikevax (COVID-19 Vaccine Moderna)"                  |
      | "portal/disease/immunization/notification_content_vaccine_janssen.json"       | "https://ec.europa.eu/health/documents/community-register/html/" | "EU/1/20/1525" | "COVID-19 Vaccine Janssen"                             |
      | "portal/disease/immunization/notification_content_vaccine_comirnaty.json"     | "https://ec.europa.eu/health/documents/community-register/html/" | "EU/1/20/1528" | "Comirnaty"                                            |
      | "portal/disease/immunization/notification_content_vaccine_vaxzevria.json"     | "https://ec.europa.eu/health/documents/community-register/html/" | "EU/1/21/1529" | "Vaxzevria (COVID-19 Vaccine AstraZeneca)"             |
      | "portal/disease/immunization/notification_content_vaccine_nuvaxovid.json"     | "https://ec.europa.eu/health/documents/community-register/html/" | "EU/1/21/1618" | "Nuvaxovid (NVX-CoV2373)"                              |
      | "portal/disease/immunization/notification_content_vaccine_valneva.json"       | "https://ec.europa.eu/health/documents/community-register/html/" | "EU/1/21/1624" | "COVID-19 Vaccine (inactivated, adjuvanted) Valneva")" |
      | "portal/disease/immunization/notification_content_vaccine_other.json"         | "https://demis.rki.de/fhir/CodeSystem/vaccine"                   | "otherVaccine" | "Anderer Impfstoff"                                    |
      | "portal/disease/immunization/notification_content_vaccine_indeterminate.json" | "http://terminology.hl7.org/CodeSystem/v3-NullFlavor"            | "ASKU"         | "asked but unknown"                                    |
      | "portal/disease/immunization/notification_content_vaccine_unknown.json"       | "http://terminology.hl7.org/CodeSystem/v3-NullFlavor"            | "NASK"         | "not asked"                                            |
