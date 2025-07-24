package sample;

import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import org.openqa.selenium.WebDriver;
import plugin.MongoReportHook;
import plugin.FunnyListener;

public class Hooks {

    private WebDriver driver;
    private MongoReportHook mongoReportHook;
    private FunnyListener funnyListener;

    @Before
    public void setUp(Scenario scenario) {
        driver = DriverFactory.getDriver();

        funnyListener = new FunnyListener(); // FunnyListener objesi
        mongoReportHook = new MongoReportHook(driver, funnyListener);

        mongoReportHook.startTimer();
    }

    @After
    public void tearDown(Scenario scenario) {
        mongoReportHook.afterScenario(scenario);
        DriverFactory.quitDriver();
    }
}
