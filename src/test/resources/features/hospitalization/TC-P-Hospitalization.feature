Feature: Positive Tests - Systemtest
  Ziel ist die Überprüfung der vom Gateway herausgehenden Daten, und NICHT den Inhalt der PDF-Quittung, die von NES generiert wird.
  Versenden von Hospitalisierungsmeldungen über die Notification Gateway API

  @customDataDriven
  Scenario: Versende gültigen Request
    Given Testautomat sends hospitalization notification with valid data and format for all fields
    Then I expect positive http-response code
    And I expect my data in the outgoing FHIR-Request