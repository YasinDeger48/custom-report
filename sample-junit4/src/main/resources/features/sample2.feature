Feature: Web site kontrolü

  Scenario: Kullanıcı sayfayı ziyaret eder ve başlığı kontrol eder
    Given user navigates to "https://example.com"
    Then page should contain title "example"
    Then page should contain text "domain"


  Scenario: Test Amacıyla yazılmış bir diğer senaryo
    Given user navigates to "https://example.com"
    Then page should contain title "example"
    Then page should contain text "domain"