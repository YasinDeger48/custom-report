package dashboard;

import com.google.gson.Gson;
import dashboard.model.EmailSettings;
import dashboard.model.Execution;
import dashboard.service.MongoService;
import dashboard.service.SchedulerService;
import dashboard.transformer.JsonTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.ModelAndView;
import spark.template.thymeleaf.ThymeleafTemplateEngine;

import java.util.ArrayList;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.MultipartConfigElement;
import javax.servlet.http.Part;
import java.util.stream.Collectors;

import static spark.Spark.*;

public class DashboardApp {

    private static final Logger logger = LoggerFactory.getLogger(DashboardApp.class);
    private static final Gson gson = new Gson();
    private static MongoService finalMongoService;  // Sınıf seviyesinde tanımlandı

    private static EmailSettings getSettings() {
        try {
            return finalMongoService != null ? finalMongoService.getEmailSettings() : new EmailSettings();
        } catch (Exception e) {
            logger.error("Error getting email settings: ", e);
            return new EmailSettings();
        }
    }

    public static void main(String[] args) {

        logger.info("TestNG Dashboard uygulaması başlatılıyor...");

        // Port ayarla
        port(4567);
        logger.info("Uygulama port 4567'de başlatıldı.");

        // Static dosyalar için klasör ayarla
        staticFiles.location("/static");
        logger.info("Static dosyalar /static klasöründen sunuluyor.");

        // MongoDB bağlantı bilgileri
        String mongoHost = System.getenv("MONGO_HOST") != null ? System.getenv("MONGO_HOST") : "localhost";
        String mongoPort = System.getenv("MONGO_PORT") != null ? System.getenv("MONGO_PORT") : "27017";
        String mongoUser = System.getenv("MONGO_USER") != null ? System.getenv("MONGO_USER") : "admin";
        String mongoPass = System.getenv("MONGO_PASS") != null ? System.getenv("MONGO_PASS") : "admin123";
        String mongoDb = "testng_reports";
        String mongoCollection = "executions";

        String mongoUri;
        if (mongoUser != null && !mongoUser.isEmpty() && mongoPass != null && !mongoPass.isEmpty()) {
            mongoUri = String.format("mongodb://%s:%s@%s:%s/%s?authSource=admin", 
                mongoUser, mongoPass, mongoHost, mongoPort, mongoDb);
        } else {
            mongoUri = String.format("mongodb://%s:%s/%s", mongoHost, mongoPort, mongoDb);
        }

        logger.info("MongoDB URI: {}", mongoUri.replaceAll(":[^:/@]*@", ":***@"));

        // MongoService'i güvenli şekilde başlat
        MongoService mongoService = null;
        try {
            mongoService = new MongoService(mongoUri, mongoDb, mongoCollection);
            logger.info("MongoDB bağlantısı başarılı.");
        } catch (Exception e) {
            logger.error("MongoDB bağlantısı başarısız: ", e);
            logger.warn("Uygulama MongoDB olmadan çalışacak (test modu).");
        }

        finalMongoService = mongoService;  // Sınıf değişkenine atama yapıldı
        SchedulerService schedulerService = new SchedulerService();

        // Başlangıçta e-posta ayarlarını yükle ve zamanlayıcıyı başlat
        try {
            if (finalMongoService != null) {
                EmailSettings initialSettings = finalMongoService.getEmailSettings();
                if (initialSettings != null && initialSettings.isSendEmail()) {
                    schedulerService.start(initialSettings);
                    logger.info("SchedulerService başlatıldı.");
                } else {
                    logger.info("E-posta gönderme kapalı, SchedulerService başlatılmadı.");
                }
            }
        } catch (Exception e) {
            logger.error("SchedulerService başlatma hatası: ", e);
        }

        // Thymeleaf template engine
        ThymeleafTemplateEngine templateEngine = new ThymeleafTemplateEngine();
        logger.info("ThymeleafTemplateEngine başlatıldı.");

        // Routes
        logger.info("HTML rotalar tanımlanıyor...");

        get("/", (req, res) -> {
            logger.info("GET / isteği alındı, /dashboard'a yönlendiriliyor.");
            res.redirect("/dashboard");
            return null;
        });

        get("/dashboard", (req, res) -> {
            try {
                logger.info("GET /dashboard isteği alındı.");
                Map<String, Object> model = new HashMap<>();
                if (finalMongoService != null) {
                    List<Execution> executions = finalMongoService.getExecutions();
                    model.put("executions", executions);
                    logger.info("Dashboard'a {} adet execution yüklendi.", executions.size());
                } else {
                    model.put("executions", new ArrayList<Execution>());
                    logger.warn("MongoDB bağlantısı yok, boş liste gösteriliyor.");
                }
                // Add required model attributes for template
                model.put("settings", getSettings());
                model.put("activePage", "dashboard");
                model.put("sidebarCollapsed", false);
                model.put("timestamp", System.currentTimeMillis());
                return templateEngine.render(new ModelAndView(model, "index"));
            } catch (Exception e) {
                logger.error("GET /dashboard hatası: ", e);
                Map<String, Object> model = new HashMap<>();
                model.put("executions", new ArrayList<Execution>());
                model.put("settings", getSettings());
                model.put("activePage", "dashboard");
                model.put("sidebarCollapsed", false);
                model.put("timestamp", System.currentTimeMillis());
                model.put("error", "Veri yüklenirken hata oluştu: " + e.getMessage());
                return templateEngine.render(new ModelAndView(model, "index"));
            }
        });

        get("/reports", (req, res) -> {
            try {
                logger.info("GET /reports isteği alındı.");
                Map<String, Object> model = new HashMap<>();
                
                if (finalMongoService != null) {
                    List<Execution> allExecutions = finalMongoService.getExecutions();
                    
                    // Son 10 execution'ı al
                    List<Execution> last10 = allExecutions.stream()
                        .sorted((e1, e2) -> e2.getTimestamp().compareTo(e1.getTimestamp()))
                        .limit(10)
                        .collect(Collectors.toList());
                    
                    model.put("executions", last10);
                } else {
                    model.put("executions", new ArrayList<Execution>());
                }
                
                // Add required model attributes for template
                model.put("settings", getSettings());
                model.put("activePage", "reports");
                model.put("sidebarCollapsed", false);
                model.put("timestamp", System.currentTimeMillis());
                return templateEngine.render(new ModelAndView(model, "reports"));
                
            } catch (Exception e) {
                logger.error("GET /reports hatası: ", e);
                Map<String, Object> model = new HashMap<>();
                model.put("executions", new ArrayList<Execution>());
                model.put("settings", getSettings());
                model.put("activePage", "reports");
                model.put("sidebarCollapsed", false);
                model.put("timestamp", System.currentTimeMillis());
                model.put("error", "Raporlar yüklenirken hata oluştu: " + e.getMessage());
                return templateEngine.render(new ModelAndView(model, "reports"));
            }
        });

        get("/reports/:id", (req, res) -> {
            try {
                logger.info("GET /reports/:id isteği alındı: {}", req.params(":id"));
                Map<String, Object> model = new HashMap<>();
                if (finalMongoService != null) {
                    Execution execution = finalMongoService.getExecution(req.params(":id"));
                    model.put("execution", execution);
                    logger.info("Rapor detayı yüklendi: {}", execution != null ? execution.getId() : "null");
                } else {
                    model.put("execution", null);
                    logger.warn("MongoDB bağlantısı yok, rapor detayı gösterilemiyor.");
                }
                // Add required model attributes for template
                model.put("settings", getSettings());
                model.put("activePage", "reports");
                model.put("sidebarCollapsed", false);
                model.put("timestamp", System.currentTimeMillis());
                return templateEngine.render(new ModelAndView(model, "report"));
            } catch (Exception e) {
                logger.error("GET /reports/:id hatası: ", e);
                Map<String, Object> model = new HashMap<>();
                model.put("execution", null);
                model.put("settings", getSettings());
                model.put("activePage", "reports");
                model.put("sidebarCollapsed", false);
                model.put("timestamp", System.currentTimeMillis());
                model.put("error", "Rapor yüklenirken hata oluştu: " + e.getMessage());
                return templateEngine.render(new ModelAndView(model, "report"));
            }
        });

        get("/settings", (req, res) -> {
            Map<String, Object> model = new HashMap<>();
            try {
                logger.info("GET /settings isteği alındı.");
                
                if (finalMongoService == null) {
                    logger.error("MongoService is not initialized!");
                    model.put("settings", new EmailSettings());
                    model.put("activePage", "settings");
                    model.put("sidebarCollapsed", false);
                    model.put("timestamp", System.currentTimeMillis());
                    model.put("error", "Database bağlantısı kurulamadı. Lütfen daha sonra tekrar deneyin.");
                    return templateEngine.render(new ModelAndView(model, "settings"));
                }
                
                logger.info("MongoService bağlantısı başarılı, ayarlar yükleniyor...");
                EmailSettings settings = finalMongoService.getEmailSettings();
                logger.info("Email ayarları alındı: {}", settings != null ? "başarılı" : "null");
                
                if (settings == null) {
                    settings = new EmailSettings();
                    logger.warn("Ayarlar null döndü, yeni bir örnek oluşturuldu.");
                }
                
                model.put("settings", settings);
                model.put("activePage", "settings");
                model.put("sidebarCollapsed", false);
                model.put("timestamp", System.currentTimeMillis());
                logger.debug("Model oluşturuldu, şablon render ediliyor...");
                
                String rendered = templateEngine.render(new ModelAndView(model, "settings"));
                logger.info("Şablon başarıyla render edildi.");
                return rendered;
                
            } catch (Exception e) {
                logger.error("GET /settings işlenirken beklenmeyen hata:", e);
                Map<String, Object> errorModel = new HashMap<>();
                errorModel.put("settings", new EmailSettings());
                errorModel.put("activePage", "settings");
                errorModel.put("sidebarCollapsed", false);
                errorModel.put("timestamp", System.currentTimeMillis());
                errorModel.put("error", "Beklenmeyen bir hata oluştu: " + e.getMessage() + 
                    "\nLütfen konsol çıktısını kontrol edin veya sistem yöneticinize başvurun.");
                try {
                    return templateEngine.render(new ModelAndView(errorModel, "settings"));
                } catch (Exception templateError) {
                    logger.error("Hata sayfası oluşturulurken hata:", templateError);
                    return "<h1>500 Sunucu Hatası</h1><p>" + e.getMessage() + "</p>";
                }
            }
        });

        // API Endpoints
        logger.info("API endpoint'leri tanımlanıyor...");

        get("/api/test", (req, res) -> {
            logger.info("GET /api/test isteği alındı.");
            res.type("application/json");
            Map<String, Object> status = new HashMap<>();
            status.put("status", "OK");
            status.put("message", "Dashboard API çalışıyor");
            status.put("mongoConnected", finalMongoService != null);
            return status;
        }, new JsonTransformer());

        get("/api/executions", (req, res) -> {
            try {
                logger.info("GET /api/executions isteği alındı.");
                res.type("application/json");
                if (finalMongoService != null) {
                    List<Execution> executions = finalMongoService.getExecutions();
                    logger.info("API'den {} adet execution döndürüldü.", executions.size());
                    return executions;
                } else {
                    logger.warn("MongoDB bağlantısı yok, boş liste döndürülüyor.");
                    return new ArrayList<Execution>();
                }
            } catch (Exception e) {
                logger.error("GET /api/executions hatası: ", e);
                res.status(500);
                Map<String, String> error = new HashMap<>();
                error.put("error", e.getMessage());
                return error;
            }
        }, new JsonTransformer());

        get("/api/settings", (req, res) -> {
            try {
                logger.info("GET /api/settings isteği alındı.");
                res.type("application/json");
                if (finalMongoService != null) {
                    EmailSettings settings = finalMongoService.getEmailSettings();
                    return settings != null ? settings : new EmailSettings();
                } else {
                    logger.warn("MongoDB bağlantısı yok, varsayılan ayarlar döndürülüyor.");
                    return new EmailSettings();
                }
            } catch (Exception e) {
                logger.error("GET /api/settings hatası: ", e);
                res.status(500);
                Map<String, String> error = new HashMap<>();
                error.put("error", e.getMessage());
                return error;
            }
        }, new JsonTransformer());

        post("/api/settings", (req, res) -> {
            try {
                logger.info("POST /api/settings isteği alındı.");
                
                if (finalMongoService == null) {
                    throw new RuntimeException("MongoDB bağlantısı yok");
                }
                
                EmailSettings settings;
                
                // Check if this is a multipart form data (file upload)
                if (req.contentType() != null && req.contentType().startsWith("multipart/form-data")) {
                    // Configure multipart handling
                    javax.servlet.MultipartConfigElement multipartConfigElement = 
                        new javax.servlet.MultipartConfigElement(System.getProperty("java.io.tmpdir"));
                    req.raw().setAttribute("org.eclipse.jetty.multipartConfig", multipartConfigElement);
                    
                    // Get the uploaded file part if it exists
                    javax.servlet.http.Part logoPart = null;
                    try {
                        logoPart = req.raw().getPart("logoFile");
                    } catch (Exception e) {
                        logger.debug("No logo file uploaded");
                    }
                    
                    // Create settings from form parameters
                    settings = new EmailSettings();
                    settings.setSmtpHost(req.queryParams("smtpHost"));
                    // Safe parse smtpPort with default 587
                    try {
                        settings.setSmtpPort(Integer.parseInt(req.queryParams("smtpPort")));
                    } catch (Exception ex) {
                        settings.setSmtpPort(587);
                    }
                    settings.setSmtpUser(req.queryParams("smtpUser"));
                    settings.setSmtpPassword(req.queryParams("smtpPassword"));
                    settings.setUseTls(Boolean.parseBoolean(req.queryParams("useTls")));
                    settings.setFromEmail(req.queryParams("fromEmail"));
                    
                    // Handle recipients: support both 'toEmails' and 'toEmail' form fields
                    String toEmailsParam = req.queryParams("toEmails");
                    String toEmailParam = req.queryParams("toEmail");
                    String recipients = (toEmailsParam != null && !toEmailsParam.trim().isEmpty()) ? toEmailsParam : toEmailParam;
                    if (recipients != null && !recipients.trim().isEmpty()) {
                        List<String> emailList = java.util.Arrays.asList(recipients.split("\\s*,\\s*"));
                        settings.setToEmails(emailList);
                    } else {
                        settings.setToEmails(new ArrayList<>());
                    }
                    
                    settings.setSendEmail(Boolean.parseBoolean(req.queryParams("sendEmail")));
                    settings.setSendTime(req.queryParams("sendTime"));
                    
                    // Handle logo dimensions
                    try {
                        settings.setLogoWidth(Integer.parseInt(req.queryParams("logoWidth")));
                    } catch (Exception e) {
                        settings.setLogoWidth(150); // default width
                    }
                    
                    try {
                        settings.setLogoHeight(Integer.parseInt(req.queryParams("logoHeight")));
                    } catch (Exception e) {
                        settings.setLogoHeight(50); // default height
                    }
                    
                    // Handle logo file upload if present
                    if (logoPart != null && logoPart.getSize() > 0) {
                        String fileName = logoPart.getSubmittedFileName();
                        try (InputStream fileContent = logoPart.getInputStream()) {
                            // Save the logo file and update settings
                            finalMongoService.saveEmailSettings(settings, fileContent, fileName);
                        }
                    } else {
                        // No new logo file, just update settings
                        finalMongoService.saveEmailSettings(settings);
                    }
                } else {
                    // Handle JSON request (for backward compatibility)
                    settings = gson.fromJson(req.body(), EmailSettings.class);
                    finalMongoService.saveEmailSettings(settings);
                }
                
                // Restart the scheduler with new settings
                schedulerService.stop();
                if (settings.isSendEmail()) {
                    schedulerService.start(settings);
                    logger.info("Email ayarları güncellendi, SchedulerService yeniden başlatıldı.");
                } else {
                    logger.info("Email ayarları güncellendi, e-posta gönderme kapalı.");
                }

                // Return success response
                res.type("application/json");
                Map<String, String> result = new HashMap<>();
                result.put("status", "OK");
                result.put("message", "Settings saved successfully");
                return result;
                
            } catch (Exception e) {
                logger.error("POST /api/settings hatası: ", e);
                res.status(500);
                Map<String, String> error = new HashMap<>();
                error.put("error", e.getMessage());
                return error;
            }
        }, new JsonTransformer());

        // Graceful shutdown
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Uygulama kapatılıyor, SchedulerService durduruluyor...");
            schedulerService.stop();
            logger.info("SchedulerService durduruldu.");
        }));

        logger.info("Tüm rotalar ve endpoint'ler başarıyla tanımlandı.");
        logger.info("Dashboard hazır:");
        logger.info("- Ana sayfa: http://localhost:4567/dashboard");
        logger.info("- Raporlar: http://localhost:4567/reports");
        logger.info("- Ayarlar: http://localhost:4567/settings");
        logger.info("- API Test: http://localhost:4567/api/test");
    }
}
