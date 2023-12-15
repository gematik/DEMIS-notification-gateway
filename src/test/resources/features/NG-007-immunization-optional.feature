Feature: Impfstoffe sind optional DSC2-2948
  Scenario Outline: Verarbeitung einer Immunization ohne Impfstoffe
    Given Token fuer "Gateway" wurde geholt
    When Eine Portalmeldung aus der Datei <datei> an Notification-Gateway gesendet wird
    Then Ueberprueft Notification-Gateway die Meldung
    And Wird eine Antwort mit dem <Http-StatusCode> 200 erwartet

    Examples:
      | datei                                                               | Http-StatusCode|
      | "notification-not-community-register-vaccination-with-date.json"    | 200            |
      | "notification-missing-vaccinations-use-case.json"                   | 200            |
      | "notification-not-community-register-vaccination-without-date.json" | 200            |