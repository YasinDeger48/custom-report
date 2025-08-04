package plugin;

import io.cucumber.java.Scenario;
import org.openqa.selenium.WebDriver;

public class MongoReportHook {

    private final WebDriver driver;
    private long startTime;

    public MongoReportHook(WebDriver driver) {
        this.driver = driver;
    }

    public void startTimer() {
        startTime = System.currentTimeMillis();
    }

    // Kısaltma fonksiyonu buraya kopyala (aynı MongoReporter'dakinin aynısı)
    private String extractSimpleErrorMessage(String errorMessage) {
        if (errorMessage == null || errorMessage.isEmpty()) return null;

        if (errorMessage.length() > 150) {
            return errorMessage.substring(0, 147) + "...";
        }
        return errorMessage;
    }

    public void afterScenario(Scenario scenario) {
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        String status = scenario.isFailed() ? "FAILED" : "PASSED";
        String scenarioName = scenario.getName();

        String screenshotPath = null;
        if (scenario.isFailed()) {
            screenshotPath = ScreenshotUtil.captureScreenshot(driver, scenarioName);
        }

        String errorMessage = null;
        if (scenario.isFailed()) {
            errorMessage = FunnyListener.finalResult;
            if (errorMessage == null || errorMessage.isEmpty()) {
                errorMessage = "Hata mesajı alınamadı.";
            } else {
                errorMessage = extractSimpleErrorMessage(errorMessage);
            }
            System.out.println("[MongoReportHook] Kısaltılmış errorMessage: " + errorMessage);
        }

        MongoReporter.logScenario(scenarioName, status, duration, screenshotPath, errorMessage);
    }
}
