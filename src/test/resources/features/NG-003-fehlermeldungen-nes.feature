Feature: Verpacken der NES Fehler in validationErrors (DSC2-2384)
# Alle Fehlermeldungen aus dem NES werden vom Gateway vollständig, ohne Filterung oder Veränderung, weitergereicht.
  Scenario Outline: Schicken einer invaliden Meldung an NES
    Given Ich habe einen Token für eine <FHIR-Meldung>
    When Ich eine <FHIR-Meldung> mit <Anzahl Fehler in der Meldung> Fehlern an den NES schicke
    And  die Antwort vom NES <Anzahl NES-Fehler> Fehler enthält
    Then Erzeugt das Gateway <Anzahl ValidationErrors> ValidationErrors, die an das Portal weitergereicht werden

    Examples:
      | FHIR-Meldung             | Anzahl Fehler in der Meldung | Anzahl NES-Fehler | Anzahl ValidationErrors
      | Labormeldung             | 2                            | 3                 | 3
      | Hospitalisierungsmeldung | 2                            | 3                 | 3
