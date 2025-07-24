package plugin;

import org.apache.commons.io.FileUtils;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

import java.io.File;

public class ScreenshotUtil {

    public static String captureScreenshot(WebDriver driver, String name) {
        try {
            File srcFile = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
            String filename = "screenshots/" + name.replaceAll(" ", "_") + "_" + System.currentTimeMillis() + ".png";
            File destFile = new File(filename);
            FileUtils.copyFile(srcFile, destFile);
            return filename;
        } catch (Exception e) {
            return null;
        }
    }
}
