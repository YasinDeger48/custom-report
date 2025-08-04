package plugin;

import io.cucumber.plugin.ConcurrentEventListener;
import io.cucumber.plugin.event.*;
import java.util.HashMap;
import java.util.Map;

public class FunnyListener implements ConcurrentEventListener {
    public static String finalResult;

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
                String errorMessage = error.getMessage();
                scenarioErrors.put(scenarioId, errorMessage);
                finalResult = errorMessage;
                System.out.println("[FunnyListener] finalResult set: " + finalResult);
            }
        }
    }

    public String getErrorForScenario(String scenarioId) {
        return scenarioErrors.get(scenarioId);
    }
}
