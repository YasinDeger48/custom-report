package dashboard.model;

import java.time.LocalDateTime;

public class Execution {
    private String scenario;
    private String status;
    private long duration;
    private String screenshot;
    private LocalDateTime timestamp;
    private String runId;
    private String error;

    public Execution(String scenario, String status, long duration, String screenshot, LocalDateTime timestamp, String runId, String error) {
        this.scenario = scenario;
        this.status = status;
        this.duration = duration;
        this.screenshot = screenshot;
        this.timestamp = timestamp;
        this.runId = runId;
        this.error = error;
    }

    // getter'ları ekle
    public String getScenario() { return scenario; }
    public String getStatus() { return status; }
    public long getDuration() { return duration; }
    public String getScreenshot() { return screenshot; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public String getRunId() { return runId; }
    public String getError() { return error; }

}
