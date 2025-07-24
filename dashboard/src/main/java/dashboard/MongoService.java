package dashboard;

import com.mongodb.ConnectionString;
import com.mongodb.client.*;
import com.mongodb.client.model.Filters;
import dashboard.model.Execution;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

public class MongoService {

    private final MongoCollection<Document> collection;

    public MongoService() {
        String uri = "mongodb://admin:admin123@localhost:27017/?authSource=admin";
        MongoClient client = MongoClients.create(uri);
        MongoDatabase db = client.getDatabase("test-report-db");
        this.collection = db.getCollection("test-executions");
    }

    public List<Execution> getExecutions(LocalDateTime from, LocalDateTime to, String filter, String runId) {
        List<Execution> executions = new ArrayList<>();
        List<Bson> filters = new ArrayList<>();

        if (from != null) {
            filters.add(Filters.gte("timestamp", from.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()));
        }
        if (to != null) {
            filters.add(Filters.lte("timestamp", to.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()));
        }
        if (filter != null && !filter.isEmpty()) {
            filters.add(Filters.regex("scenario", filter, "i"));
        }

        // Eğer runId varsa, filtrele. Yoksa sadece runId olanları getir
        if (runId != null && !runId.isEmpty()) {
            filters.add(Filters.eq("runId", runId));
        } else {
            filters.add(Filters.exists("runId", true));
        }

        Bson query = filters.isEmpty() ? new Document() : Filters.and(filters);

        try (MongoCursor<Document> cursor = collection.find(query).iterator()) {
            while (cursor.hasNext()) {
                Document doc = cursor.next();

                String scenario = doc.getString("scenario");
                String status = doc.getString("status");
                long duration = doc.get("duration") != null ? doc.getLong("duration")
                        : doc.get("duration_ms") != null ? doc.getLong("duration_ms")
                        : 0L;
                String screenshot = doc.getString("screenshot");
                String runIdVal = doc.getString("runId");

                LocalDateTime timestamp;
                try {
                    Object ts = doc.get("timestamp");
                    if (ts instanceof Long) {
                        timestamp = Instant.ofEpochMilli((Long) ts).atZone(ZoneId.systemDefault()).toLocalDateTime();
                    } else if (ts instanceof String) {
                        timestamp = LocalDateTime.parse((String) ts);
                    } else {
                        timestamp = LocalDateTime.now();
                    }
                } catch (Exception e) {
                    timestamp = LocalDateTime.now();
                }
                String error = doc.getString("error");
                executions.add(new Execution(scenario, status, duration, screenshot, timestamp, runIdVal, error));

            }
        }

        return executions;
    }

}
