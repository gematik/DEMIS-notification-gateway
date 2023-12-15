Feature: Aussagekraeftige Fehlermeldung bei invaliden Angaben laut Datenmodell (DSC2-2848)
# Die Fehlermeldung gibt Auskunft darüber, für welches Feld ein unerwarteter Wert angegeben wurde
  Scenario Outline: Empfangen einer invaliden Meldung vom Portal
    Given Ich erhalte eine <Meldung> vom Portal mit <falscher Wert>
    When Ich die Meldung validiere
    Then Erzeugt das Gateway eine ErrorResponse mit einer <Message> und ValidationErrors, die an das Portal zurückgeschickt wird
    And Enthält der ValidationError <ValidationErrorField> und <ValidationErrorMessage>

    Examples:
      | falscher Wert                                              | Message                      | ValidationErrorField                      | ValidationErrorMessage
      | leerer addressType in Adresse der Meldereinrichtung        | Instantiation error occurred | notifierFacility.address.addressType      | Unexpected value ''
      | leerer addressType in Adresse der Betroffenen Person       | Instantiation error occurred | notifiedPerson.currentAddress.addressType | Unexpected value ''
      | "INVALID" als addessType in Addresse der Meldereinrichtung | Instantiation error occurred | notifierFacility.address.addressType      | Unexpected value 'INVALID'
      | leere Anrede in Kontaktperson                              | Instantiation error occurred | notifierFacility.contact.salutation       | Unexpected value ''
