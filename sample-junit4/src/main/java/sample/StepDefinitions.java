package sample;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

import static org.junit.Assert.assertTrue;

public class StepDefinitions {

    WebDriver driver = DriverFactory.getDriver();

    @Given("user navigates to {string}")
    public void user_navigates_to(String url) {
        driver.get(url);
    }

    @Then("page should contain title {string}")
    public void page_should_contain_title(String keyword) {
        String title = driver.getTitle().toLowerCase();
        assertTrue("Title does not contain expected keyword!", title.contains(keyword.toLowerCase()));
    }

    @Then("page should contain text {string}")
    public void page_should_contain_text(String expectedText) {
        String pageSource = driver.getPageSource().toLowerCase();
        assertTrue("Page does not contain expected text!", pageSource.contains(expectedText.toLowerCase()));
    }
}
