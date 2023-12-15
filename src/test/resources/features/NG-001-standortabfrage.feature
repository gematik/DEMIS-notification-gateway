Feature: Standortabfrage DSC2-2646
# Story DSC2-2646 Gateway erfragt Standorte beim Core/Hospital-Location-Service
  Scenario Outline: Verarbeitung einer Standortabfrage
    Given Ich habe ein Token für Bettenbelegung
    And  mein Token enthält eine IK-Nummer <IK_in_token>
    When Ich eine Standortanfrage an den HLS versende
    And  der HLS eine <HLS-Antwort> zurückliefert
    Then erwarte ich eine Antwort mit Http Status <statuscode> und <Ergebnis>
    Examples:
      | IK_in_token | HLS-Antwort                        | statuscode | Ergebnis      |
      | true        | standortliste                      | 200        | Standortliste |
      | true        | leere liste                        | 200        | leere Liste   |
      | true        | in einen Felhler läuft  (http 500) | 500        | keine Liste   |
      | false       | null                               | 400        | keine Liste   |