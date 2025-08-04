package plugin;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

import java.io.InputStream;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Properties;

public class MongoReporter {

    private static MongoCollection<Document> collection;
    private static String screenshotMode;
    private static String currentRunId;

    static {
        try {
            Properties properties = new Properties();
            InputStream input = MongoReporter.class.getClassLoader().getResourceAsStream("config.properties");

            if (input == null) {
                throw new FileNotFoundException("config.properties bulunamadı!");
            }

            properties.load(input);

            String mongoUri = properties.getProperty("mongo.uri");
            String dbName = properties.getProperty("mongo.db");
            String collectionName = properties.getProperty("mongo.collection");
            screenshotMode = properties.getProperty("screenshot.mode");

            MongoClient mongoClient = MongoClients.create(mongoUri);
            MongoDatabase database = mongoClient.getDatabase(dbName);
            collection = database.getCollection(collectionName);

            LocalDateTime now = LocalDateTime.now();
            currentRunId = now.format(DateTimeFormatter.ofPattern("HH:mm:ss - dd.MM.yyyy"));

        } catch (IOException e) {
            throw new RuntimeException("MongoReporter başlatılamadı: " + e.getMessage());
        }
    }

    public static void setCurrentRunId(String runId) {
        currentRunId = runId;
    }

    public static String getCurrentRunId() {
        return currentRunId;
    }

    public static void logScenario(String name, String status, long durationMillis, String screenshotPath, String errorMessage) {
        Document doc = new Document();
        doc.append("scenario", name);
        doc.append("status", status);
        doc.append("duration", durationMillis);
        doc.append("timestamp", System.currentTimeMillis());
        doc.append("runId", currentRunId);

        if (screenshotPath != null && !screenshotPath.isEmpty()) {
            doc.append("screenshot", screenshotPath);
        }

        if (errorMessage != null && !errorMessage.isEmpty()) {
            doc.append("error", errorMessage);
        }

        collection.insertOne(doc);
    }

    public static String getScreenshotMode() {
        return screenshotMode;
    }
}
