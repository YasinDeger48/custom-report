package sample;

import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;
import org.junit.runner.RunWith;

@RunWith(Cucumber.class)
@CucumberOptions(
        features = "src/main/resources/features",
        glue = {"sample"},
        plugin = {
                "pretty",
                "plugin.FunnyListener",
                "html:target/cucumber-html-report.html"
        },
        monochrome = true
)
public class TestRunner {
}
