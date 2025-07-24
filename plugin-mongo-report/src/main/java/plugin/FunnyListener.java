package plugin;

import io.cucumber.plugin.ConcurrentEventListener;
import io.cucumber.plugin.event.*;
import org.openqa.selenium.WebDriver;

import java.util.HashMap;
import java.util.Map;

public class FunnyListener implements ConcurrentEventListener {

    private final Map<String, String> scenarioErrors = new HashMap<>();

    @Override
    public void setEventPublisher(EventPublisher publisher) {
        publisher.registerHandlerFor(TestStepFinished.class, this::onTestStepFinished);
    }

    private void onTestStepFinished(TestStepFinished event) {
        if (!(event.getTestStep() instanceof PickleStepTestStep)) return;

        Result result = event.getResult();
        if (result.getStatus().is(Status.FAILED)) {
            String scenarioId = event.getTestCase().getId().toString();
            Throwable error = result.getError();
            if (error != null) {
                // En son hata mesajını kaydet
                scenarioErrors.put(scenarioId, error.getMessage());
            }
        }
    }

    // Hata mesajını getirmek için metod
    public String getErrorForScenario(String scenarioId) {
        return scenarioErrors.get(scenarioId);
    }
}
