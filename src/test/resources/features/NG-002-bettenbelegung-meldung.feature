Feature: DEMISDSC2-2519 Verarbeitung des Bettenbelegungs-Reports im Gateway
# Das Gateway kann die Meldung zur Bettenbelegung vom Meldeportal annehmen
# Das Gateway bereitet die Meldung entsprechend auf und schickt sie an den DEMIS Core
# Die Tokenverarbeitung erfolgt wie bei einer Hospitalisierungsmeldung
  Scenario: Verarbeitung einer validen Bettenbelegungsmeldung
    Given Ich habe ein Token für Bettenbelegung
    When Ich eine valide Bettenbelegungsmeldung vom Portal empfange
    Then versendet das Gateway eine Anfrage an den RPS mit einer aufbereiteten Bettenbelegungs-FHIR-Meldung, die alle gemeldeten Werte enthält

  Scenario Outline: Verarbeitung einer nicht validen Bettenbelegungsmeldung
    Given Ich habe ein Token für Bettenbelegung
    When Ich eine Bettenbelegungsmeldung mit <standortID> Anzahl belegter Betten von Erwachsenen <adultsNumberOfBeds> vom Portal empfange
    Then sendet das Gateway eine Antwort mit Http-Status 400 und message <message>
    Then versendet das Gateway keine Anfrage an den RPS
    Examples:
      | standortID | adultsNumberOfBeds | message                                                   |
      | null       | 10                 | locationID must not be null                               |
      | leer       | 10                 | locationID size must be between 1 and 2147483647          |


