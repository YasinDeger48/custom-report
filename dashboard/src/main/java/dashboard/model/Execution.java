package dashboard.model;

import java.time.LocalDateTime;
import java.util.List;

public class Execution {
    private String id; // MongoDB _id field
    private String scenario;
    private String status;
    private long duration;
    private String screenshot;
    private List<String> screenshots;
    private LocalDateTime timestamp;
    private String runId;
    private String error;

    // Default constructor for MongoDB deserialization
    public Execution() {}

    public Execution(String scenario, String status, long duration, String screenshot, LocalDateTime timestamp, String runId, String error) {
        this.scenario = scenario;
        this.status = status;
        this.duration = duration;
        this.screenshot = screenshot;
        this.timestamp = timestamp;
        this.runId = runId;
        this.error = error;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getScenario() { return scenario; }
    public void setScenario(String scenario) { this.scenario = scenario; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public long getDuration() { return duration; }
    public void setDuration(long duration) { this.duration = duration; }
    
    public String getScreenshot() { return screenshot; }
    public void setScreenshot(String screenshot) { this.screenshot = screenshot; }
    
    public List<String> getScreenshots() { return screenshots; }
    public void setScreenshots(List<String> screenshots) { this.screenshots = screenshots; }
    
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    
    public String getRunId() { return runId; }
    public void setRunId(String runId) { this.runId = runId; }
    
    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
}
