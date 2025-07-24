package dashboard;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dashboard.model.Execution;
import static spark.Spark.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class DashboardApp {

    public static void main(String[] args) {
        port(4567);
        staticFiles.location("/templates");

        MongoService service = new MongoService();
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(LocalDateTime.class, new dashboard.util.LocalDateTimeAdapter())
                .create();

        // GET /executions?runId=...&filter=...&from=...&to=...
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
                return gson.toJson(list); // ✅ DİKKAT: array dönüyoruz
            } catch (Exception e) {
                e.printStackTrace();
                res.status(200); // ❗ Sunucu HATAYI JSON ARRAY olarak göndersin, yoksa frontend patlıyor
                res.type("application/json");
                return gson.toJson(List.of()); // ✅ HATA OLSA BİLE BOŞ DİZİ DÖN
            }
        });



        // PDF Export (tüm koşumlar veya sadece bir runId)
        get("/pdf", (req, res) -> {
            try {
                String runId = req.queryParams("runId");
                String filePath = "test-report.pdf";
                List<Execution> list = service.getExecutions(null, null, null, runId);

                PdfExporter.export(list, filePath);

                res.header("Content-Disposition", "attachment; filename=test-report.pdf");
                res.type("application/pdf");
                return new java.io.FileInputStream(filePath);
            } catch (Exception e) {
                e.printStackTrace();
                halt(500, "PDF oluşturulamadı: " + e.getMessage());
                return null;
            }
        });
    }
}
