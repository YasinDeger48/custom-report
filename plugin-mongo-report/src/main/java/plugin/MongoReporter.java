package plugin;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoException;
import com.mongodb.MongoWriteException;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

/**
 * JUnit 5, TestNG ve Cucumber test sonuçlarını MongoDB'ye kaydetmek için yardımcı sınıf
 */
public class MongoReporter {
    // Instance properties
    private final Object lock = new Object();
    private MongoCollection<Document> collection;
    private String screenshotMode = "on_failure";
    private String currentRunId;
    private boolean initialized = false;
    private boolean initializationInProgress = false;
    private Throwable initializationError = null;
    private MongoClient mongoClient;
    
    /**
     * Creates a new instance of MongoReporter
     */
    public MongoReporter() {
        this.currentRunId = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        System.out.println("[MongoReporter] MongoReporter instance created. Connection will be initialized on first use.");
    }
    
    /**
     * Creates a new instance of MongoReporter with the specified configuration
     */
    public MongoReporter(String mongoUri, String dbName, String collectionName) {
        this();
        if (mongoUri != null && dbName != null) {
            initialize(mongoUri, dbName, 
                      (collectionName != null) ? collectionName : "test_results", 
                      "on_failure");
        }
    }
    
    /**
     * Creates a new instance of MongoReporter with the specified configuration
     */
    public MongoReporter(String mongoUri, String dbName, String collectionName, String screenshotMode) {
        this();
        if (mongoUri != null && dbName != null) {
            initialize(mongoUri, 
                      dbName, 
                      (collectionName != null) ? collectionName : "test_results",
                      (screenshotMode != null) ? screenshotMode : "on_failure");
        }
    }
    
    /**
     * Initializes the MongoDB connection with the specified configuration
     */
    private void initialize(String mongoUri, String dbName, String collectionName, String screenshotMode) {
        if (initialized) {
            return;
        }
        
        synchronized (lock) {
            if (initialized) {
                return;
            }
            
            initializationInProgress = true;

            try {
                this.screenshotMode = (screenshotMode != null) ? screenshotMode : "on_failure";
                
                if (mongoUri == null || dbName == null || collectionName == null) {
                    throw new IllegalArgumentException("MongoDB URI, database name, and collection name cannot be null");
                }
                
                String finalCollectionName = (collectionName != null && !collectionName.isEmpty()) ? 
                    collectionName : "test_results";
                
                ConnectionString connectionString = new ConnectionString(mongoUri);
                MongoClientSettings settings = MongoClientSettings.builder()
                        .applyConnectionString(connectionString)
                        .applyToConnectionPoolSettings(builder -> 
                            builder.maxConnectionIdleTime(10, TimeUnit.SECONDS))
                        .build();
                
                this.mongoClient = MongoClients.create(settings);
                
                try {
                    mongoClient.getDatabase(dbName).runCommand(new Document("ping", 1));
                } catch (MongoException e) {
                    String errorMsg = "[MongoReporter] Failed to connect to MongoDB: " + e.getMessage();
                    System.err.println(errorMsg);
                    mongoClient.close();
                    throw new RuntimeException(errorMsg, e);
                }
                
                MongoDatabase database = mongoClient.getDatabase(dbName);
                this.collection = database.getCollection(finalCollectionName);
                
                try {
                    this.collection.createIndex(new Document("testName", 1));
                    this.collection.createIndex(new Document("status", 1));
                    this.collection.createIndex(new Document("timestamp", -1));
                } catch (Exception e) {
                    System.err.println("[MongoReporter] Warning: Failed to create indexes: " + e.getMessage());
                }
                
                initialized = true;
                
            } catch (Exception e) {
                String errorMsg = "[MongoReporter] Error during initialization: " + e.getMessage();
                System.err.println(errorMsg);
                e.printStackTrace();
                initializationError = e;
                throw new RuntimeException(errorMsg, e);
            } finally {
                initializationInProgress = false;
            }
        }
    }
    
    /**
     * Initializes the MongoDB connection using configuration from config.properties or environment variables
     */
    public void ensureInitialized() {
        if (initialized) return;
        
        synchronized (lock) {
            if (initialized) return;
            
            try {
                System.out.println("[MongoReporter] ensureInitialized() called");
                
                // First try environment variables
                String mongoUri = System.getenv("MONGO_URI");
                String dbName = System.getenv("MONGO_DATABASE");
                String collectionName = System.getenv("MONGO_COLLECTION");
                String screenshotModeValue = System.getenv("SCREENSHOT_MODE");
                
                System.out.println("[MongoReporter] Environment variables - " +
                    "URI: " + (mongoUri != null && mongoUri.contains("@") ? "[REDACTED]" : mongoUri) + 
                    ", DB: " + dbName + ", Collection: " + collectionName);
                
                // If environment variables not set, try system properties
                if (mongoUri == null || dbName == null) {
                    System.out.println("[MongoReporter] Trying system properties...");
                    mongoUri = System.getProperty("mongo.uri");
                    dbName = System.getProperty("mongo.database");
                    collectionName = System.getProperty("mongo.collection", "test_results");
                    screenshotModeValue = System.getProperty("screenshot.mode", "on_failure");
                    
                    System.out.println("[MongoReporter] System properties - " +
                        "URI: " + (mongoUri != null && mongoUri.contains("@") ? "[REDACTED]" : mongoUri) + 
                        ", DB: " + dbName + ", Collection: " + collectionName);
                }
                
                // If still not set, try ConfigLoader
                if (mongoUri == null || dbName == null) {
                    System.out.println("[MongoReporter] Trying ConfigLoader...");
                    mongoUri = ConfigLoader.getProperty("mongo.uri");
                    dbName = ConfigLoader.getProperty("mongo.database");
                    collectionName = ConfigLoader.getProperty("mongo.collection", "test_results");
                    screenshotModeValue = ConfigLoader.getProperty("screenshot.mode", "on_failure");
                    
                    System.out.println("[MongoReporter] ConfigLoader - " +
                        "URI: " + (mongoUri != null && mongoUri.contains("@") ? "[REDACTED]" : mongoUri) + 
                        ", DB: " + dbName + ", Collection: " + collectionName);
                }
                
                // Validate required properties
                if (mongoUri == null || mongoUri.trim().isEmpty()) {
                    throw new IllegalStateException("MongoDB URI is not configured. " +
                        "Please set MONGO_URI environment variable, mongo.uri system property, or in config.properties");
                }
                
                if (dbName == null || dbName.trim().isEmpty()) {
                    throw new IllegalStateException("Database name is not configured. " +
                        "Please set MONGO_DATABASE environment variable, mongo.database system property, or in config.properties");
                }
                
                // Set defaults for optional properties
                if (collectionName == null || collectionName.trim().isEmpty()) {
                    collectionName = "test_results";
                }
                
                if (screenshotModeValue == null || screenshotModeValue.trim().isEmpty()) {
                    screenshotModeValue = "on_failure";
                }
                
                System.out.println("[MongoReporter] Final configuration - " +
                    "DB: " + dbName + ", Collection: " + collectionName + ", Screenshot mode: " + screenshotModeValue);
                
                // Initialize with the loaded configuration
                initialize(mongoUri, dbName, collectionName, screenshotModeValue);
                
            } catch (Exception e) {
                String errorMsg = "[MongoReporter] Error during configuration loading: " + e.getMessage();
                System.err.println(errorMsg);
                e.printStackTrace();
                initializationError = e;
                throw new RuntimeException(errorMsg, e);
            }
        }
    }
    
    /**
     * Checks if the reporter is properly initialized
     */
    public boolean isInitialized() {
        return initialized && collection != null;
    }
    
    /**
     * Closes the MongoDB connection
     */
    public void close() {
        if (mongoClient != null) {
            try {
                mongoClient.close();
                System.out.println("[MongoReporter] MongoDB connection closed");
            } catch (Exception e) {
                System.err.println("[MongoReporter] Error closing MongoDB connection: " + e.getMessage());
            }
        }
        initialized = false;
        collection = null;
    }
    
    @Override
    protected void finalize() throws Throwable {
        try {
            close();
        } finally {
            super.finalize();
        }
    }
    
    /**
     * Get the screenshot mode (on_failure, always, never)
     */
    public String getScreenshotMode() {
        return screenshotMode;
    }

    /**
     * Log a test scenario result to MongoDB
     * @param scenarioName Name of the test scenario
     * @param status Test status (PASSED, FAILED, etc.)
     * @param duration Test duration in milliseconds
     * @param screenshotPath Path to the screenshot (if any)
     * @param errorMessage Error message (if any)
     */
    public void logScenario(String scenarioName, String status, long duration, String screenshotPath, String errorMessage) {
        if (!initialized) {
            System.err.println("[MongoReporter] MongoDB reporter is not initialized. Skipping logging for scenario: " + scenarioName);
            return;
        }
        
        if (collection == null) {
            System.err.println("[MongoReporter] MongoDB collection is null. Skipping logging for scenario: " + scenarioName);
            return;
        }
        
        try {
            // Create a document with test results
            Document doc = new Document()
                .append("testName", scenarioName)
                .append("status", status)
                .append("duration", duration)
                .append("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .append("runId", this.currentRunId)
                .append("screenshotPath", screenshotPath != null ? screenshotPath : "");
            
            // Add error message if present
            if (errorMessage != null && !errorMessage.isEmpty()) {
                doc.append("errorMessage", errorMessage);
            }
            
            // Add system information
            doc.append("systemInfo", new Document()
                .append("os.name", System.getProperty("os.name"))
                .append("os.version", System.getProperty("os.version"))
                .append("java.version", System.getProperty("java.version"))
                .append("user.name", System.getProperty("user.name"))
                .append("hostname", getHostname()));
            
            // Insert the document into the collection
            collection.insertOne(doc);
            
            System.out.println("[MongoReporter] Test result logged to MongoDB: " + doc.toJson());
            
        } catch (MongoWriteException e) {
            // Handle MongoDB write errors specifically
            System.err.println("[MongoReporter] Failed to write to MongoDB: " + e.getMessage());
            if (e.getError() != null) {
                System.err.println("Error code: " + e.getError().getCode() + ", Category: " + e.getError().getCategory());
            }
            e.printStackTrace();
        } catch (MongoException e) {
            // Handle other MongoDB-specific errors
            System.err.println("[MongoReporter] MongoDB error: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            // Handle all other exceptions
            System.err.println("[MongoReporter] Error logging test result to MongoDB: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * JUnit5 ve TestNG testleri için test sonucunu MongoDB'ye kaydet
     */
    public void logTest(String testName, String status, long duration, String errorMessage, 
                       String framework, String screenshotPath, String timestamp) {
        if (!isInitialized()) {
            System.err.println("[MongoReporter] MongoReporter is not initialized. Cannot log test result.");
            return;
        }

        try {
            Document doc = new Document()
                .append("testName", testName)
                .append("status", status)
                .append("duration", duration)
                .append("timestamp", timestamp != null ? timestamp : LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .append("runId", this.currentRunId)
                .append("framework", framework)
                .append("screenshotPath", screenshotPath != null ? screenshotPath : "");
            
            // Add error message if present
            if (errorMessage != null && !errorMessage.isEmpty()) {
                doc.append("errorMessage", errorMessage);
            }
            
            // Add system information
            doc.append("systemInfo", new Document()
                .append("os.name", System.getProperty("os.name"))
                .append("os.version", System.getProperty("os.version"))
                .append("java.version", System.getProperty("java.version"))
                .append("user.name", System.getProperty("user.name"))
                .append("hostname", getHostname()));
            
            // Insert the document into the collection
            collection.insertOne(doc);
            
            System.out.println("[MongoReporter] " + framework + " test result logged to MongoDB: " + testName + " - " + status);
            
        } catch (MongoWriteException e) {
            System.err.println("[MongoReporter] Failed to write to MongoDB: " + e.getMessage());
            if (e.getError() != null) {
                System.err.println("Error code: " + e.getError().getCode() + ", Category: " + e.getError().getCategory());
            }
            e.printStackTrace();
        } catch (MongoException e) {
            System.err.println("[MongoReporter] MongoDB error: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("[MongoReporter] Error logging test result to MongoDB: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static String getHostname() {
        try {
            return java.net.InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            return "Unknown";
        }
    }
}
