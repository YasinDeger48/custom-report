package dashboard;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dashboard.model.Execution;
import dashboard.util.LocalDateTimeAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static spark.Spark.*;

import java.io.FileInputStream;
import java.time.LocalDateTime;
import java.util.List;

public class DashboardApp {

    private static final Logger logger = LoggerFactory.getLogger(DashboardApp.class);

    public static void main(String[] args) {
        try {
            port(4567);
            staticFiles.location("/templates");

            MongoService service = new MongoService();
            Gson gson = new GsonBuilder()
                    .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
                    .create();

            // Global exception handler (Spark)
            exception(Exception.class, (e, req, res) -> {
                logger.error("Global hata yakalandı", e);
                res.status(500);
                res.body("Sunucu hatası: " + e.getMessage());
            });

            // GET /executions
            get("/executions", (req, res) -> {
                try {
                    String start = req.queryParams("from");
                    String end = req.queryParams("to");
                    String filter = req.queryParams("filter");
                    String runId = req.queryParams("runId");

                    LocalDateTime from = start != null ? LocalDateTime.parse(start) : null;
                    LocalDateTime to = end != null ? LocalDateTime.parse(end) : null;

                    List<Execution> list = service.getExecutions(from, to, filter, runId);
                    res.type("application/json");
                    return gson.toJson(list);
                } catch (Exception e) {
                    logger.error("GET /executions endpoint'inde hata oluştu", e);
                    res.status(200); // frontend hataya bozulmasın diye boş array
                    res.type("application/json");
                    return gson.toJson(List.of());
                }
            });

            // GET /pdf
            get("/pdf", (req, res) -> {
                try {
                    String runId = req.queryParams("runId");
                    String filePath = "test-report.pdf";
                    List<Execution> list = service.getExecutions(null, null, null, runId);

                    PdfExporter.export(list, filePath);

                    res.header("Content-Disposition", "attachment; filename=test-report.pdf");
                    res.type("application/pdf");
                    return new FileInputStream(filePath);
                } catch (Exception e) {
                    logger.error("GET /pdf endpoint'inde PDF oluşturulamadı", e);
                    halt(500, "PDF oluşturulamadı: " + e.getMessage());
                    return null;
                }
            });

            logger.info("DashboardApp başarıyla başlatıldı.");
        } catch (Exception e) {
            logger.error("DashboardApp başlatılırken kritik hata oluştu!", e);
            System.exit(100); // ❗ İşte burada exit code 100 gönderilir
        }
    }
}
