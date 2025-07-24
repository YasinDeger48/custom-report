package plugin;

import io.cucumber.java.Scenario;
import org.openqa.selenium.WebDriver;

public class MongoReportHook {

    private final WebDriver driver;
    private final FunnyListener funnyListener;
    private long startTime;

    public MongoReportHook(WebDriver driver, FunnyListener funnyListener) {
        this.driver = driver;
        this.funnyListener = funnyListener;
    }

    public void startTimer() {
        startTime = System.currentTimeMillis();
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
            // FunnyListener'dan hatayı al
            errorMessage = funnyListener.getErrorForScenario(scenario.getId().toString());
        }

        MongoReporter.logScenario(scenarioName, status, duration, screenshotPath, errorMessage);
    }
}
