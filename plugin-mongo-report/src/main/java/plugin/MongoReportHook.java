package plugin;

import io.cucumber.java.Scenario;
import org.openqa.selenium.WebDriver;
import org.opentest4j.TestAbortedException;

/**
 * JUnit 5, TestNG ve Cucumber için MongoDB raporlama hook'u
 */
public class MongoReportHook {

    private final WebDriver driver;
    private MongoReporter mongoReporter = null;
    private long startTime;
    private String testName;
    private boolean initializationFailed = false;

    public MongoReportHook(WebDriver driver) {
        this.driver = driver;
        
        try {
            System.out.println("[MongoReportHook] MongoReporter başlatılıyor...");
            
            // Config dosyasından ayarları al
            String mongoUri = ConfigLoader.getProperty("mongo.uri");
            String dbName = ConfigLoader.getProperty("mongo.database");
            String collectionName = ConfigLoader.getProperty("mongo.collection");
            
            System.out.println("[MongoReportHook] Config dosyasından okunan değerler - " +
                "URI: " + (mongoUri != null ? "[GİZLİ]" : "ayarlanmamış") + 
                ", DB: " + (dbName != null ? dbName : "ayarlanmamış") + 
                ", Collection: " + (collectionName != null ? collectionName : "ayarlanmamış"));
            
            // Eğer config dosyasında yoksa environment variables'ı dene
            if (mongoUri == null || dbName == null) {
                System.out.println("[MongoReportHook] Environment variables deneniyor...");
                if (mongoUri == null) mongoUri = System.getenv("MONGO_URI");
                if (dbName == null) dbName = System.getenv("MONGO_DATABASE");
                if (collectionName == null) collectionName = System.getenv("MONGO_COLLECTION");
            }
            
            // Varsayılan değerleri ayarla
            if (collectionName == null || collectionName.trim().isEmpty()) {
                collectionName = "test_results";
            }
            
            // Gerekli konfigürasyon varsa initialize et
            if (mongoUri != null && dbName != null) {
                System.out.println("[MongoReportHook] MongoReporter konfigürasyonla oluşturuluyor");
                this.mongoReporter = new MongoReporter(mongoUri, dbName, collectionName);
                this.mongoReporter.ensureInitialized();
                
                if (this.mongoReporter.isInitialized()) {
                    System.out.println("[MongoReportHook] MongoReporter başarıyla başlatıldı");
                } else {
                    System.err.println("[MongoReportHook] UYARI: MongoReporter başlatılamadı. MongoDB raporlama devre dışı.");
                    this.mongoReporter = null;
                    initializationFailed = true;
                }
            } else {
                System.err.println("[MongoReportHook] UYARI: MongoDB konfigürasyonu bulunamadı. " +
                    "Lütfen config.properties dosyasında mongo.uri ve mongo.database ayarlarını yapın. " +
                    "MongoDB raporlama devre dışı.");
                this.mongoReporter = null;
                initializationFailed = true;
            }
        } catch (Exception e) {
            System.err.println("[MongoReportHook] HATA: MongoReporter başlatılamadı: " + e.getMessage());
            e.printStackTrace();
            this.mongoReporter = null;
            initializationFailed = true;
        }
    }

    public void startTimer() {
        this.startTime = System.currentTimeMillis();
    }

    public void setTestName(String testName) {
        this.testName = testName;
    }

    /**
     * Hata mesajını kısaltır
     */
    private String extractSimpleErrorMessage(String errorMessage) {
        if (errorMessage == null || errorMessage.isEmpty()) return null;
        return errorMessage.length() > 150 ? errorMessage.substring(0, 147) + "..." : errorMessage;
    }

    /**
     * Cucumber senaryosu için raporlama
     */
    public void afterScenario(Scenario scenario) {
        String status = scenario.isFailed() ? "FAILED" : "PASSED";
        String errorMessage = null;
        
        if (scenario.isFailed()) {
            errorMessage = "Test başarısız oldu";
            // Cucumber'da doğrudan exception bilgisine erişim yok,
            // bu yüzden sadece durum bilgisini kullanıyoruz
        }
        
        reportTestResult(
            scenario.getName(),
            status,
            errorMessage
        );
    }

    /**
     * JUnit 5 ve TestNG için raporlama
     */
    public void reportTestResult(TestAbortedException testAbortedException) {
        String status = testAbortedException != null && testAbortedException.getMessage() != null && 
                       testAbortedException.getMessage().contains("FAILED") ? "FAILED" : "PASSED";
        
        reportTestResult(
            testName != null ? testName : "Bilinmeyen Test",
            status,
            status.equals("FAILED") ? testAbortedException.getMessage() : null
        );
    }

    /**
     * Genel test sonucu raporlama metodu
     */
    public void reportTestResult(String testName, String status, String errorMessage) {
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        String screenshotPath = null;
        if (status.equals("FAILED") && driver != null) {
            screenshotPath = ScreenshotUtil.captureScreenshot(driver, testName);
        }

        // Hata mesajını işle
        String finalErrorMessage = null;
        if (errorMessage != null && !errorMessage.isEmpty()) {
            finalErrorMessage = extractSimpleErrorMessage(errorMessage);
            System.out.println("[MongoReportHook] Hata mesajı: " + finalErrorMessage);
        }

        // Skip if initialization failed or mongoReporter is not available
        if (initializationFailed || mongoReporter == null) {
            System.err.println("[MongoReportHook] WARNING: MongoReporter is not available. Test results will not be saved to MongoDB.");
            return;
        }
        
        // Skip if not initialized
        if (!mongoReporter.isInitialized()) {
            System.err.println("[MongoReportHook] WARNING: MongoReporter is not initialized. Test results will not be saved to MongoDB.");
            return;
        }
        
        // Log the test result
        try {
            mongoReporter.logScenario(
                testName,
                status,
                duration,
                screenshotPath,
                finalErrorMessage
            );
            System.out.println(String.format("[MongoReportHook] %s - %s (Süre: %dms)", 
                testName, status, duration));
        } catch (Exception e) {
            System.err.println("[MongoReportHook] Error logging test result to MongoDB: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
