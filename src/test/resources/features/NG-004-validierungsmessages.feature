Feature: Validierungmessages in der ErrorResponse  (DSC2-2352)
# Das Feld "message" der ErrorResponse soll nur eine Zusammenfassung des Fehlers beschreiben.
# Eine Liste aller Validierungsfehler ist in "validationErrors" beschrieben.
  Scenario Outline: Empfangen einer invaliden Meldung vom Portal
    Given Ich erhalte eine <Meldung> vom Portal mit <Verstoss gegen Datenmodell>
    When Ich die Meldung validiere
    Then Erzeugt das Gateway eine ErrorResponse mit einer <Message> und ValidationErrors, die an das Portal zurückgeschickt wird
    And Enthält der ValidationError <ValidationErrorField> und <ValidationErrorMessage>

    Examples:
      | Meldung                  | Verstoss gegen Datenmodell                                   | Message                   | ValidationErrorField                                                        | ValidationErrorMessage
      | Hospitalisierungsmeldung | keine Impfinformationen angegeben                            | Validation error occurred | disease.diseaseInfoCVDD.vaccinationQuestion.vaccinations                    | size must be between 1 and 2147483647
      | Hospitalisierungsmeldung | kein Impfdatum angegeben                                     | Validation error occurred | disease.diseaseInfoCVDD.vaccinationQuestion.vaccinations[0].vaccinationDate | must not be null
      | Labormeldung             | keine Kontaktmoeglichkeit in der Meldereinrichtung angegeben | Validation error occurred | notifierFacility.contacts                                                   | size must be between 1 and 2147483647
      | Labormeldung             | keine Stadt fuer Meldereinrichtung angegeben                 | Validation error occurred | notifierFacility.address.city                                               | size must be between 1 and 2147483647
      | Labormeldung             | keine Strasse fuer Meldereinrichtung angegeben               | Validation error occurred | notifierFacility.address.street                                             | size must be between 1 and 2147483647
      | Labormeldung             | keine Hausnummer fuer Meldereinrichtung angegeben            | Validation error occurred | notifierFacility.address.houseNumber                                        | size must be between 1 and 2147483647
      | Bettenbelegung           | keine LocationID angegeben                                   | Validation error occurred | notifierFacility.locationID                                                 | must not be null
      | Bettenbelegung           | leere LocationID angegeben                                   | Validation error occurred | notifierFacility.locationID                                                 | size must be between 1 and 2147483647