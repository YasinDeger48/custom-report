package dashboard;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import dashboard.model.Execution;

import java.io.FileOutputStream;
import java.util.List;

public class PdfExporter {

    public static void export(List<Execution> executions, String filePath) throws Exception {
        Document document = new Document();
        PdfWriter.getInstance(document, new FileOutputStream(filePath));

        document.open();
        document.addTitle("Test Report PDF");

        Font titleFont = new Font(Font.HELVETICA, 16, Font.BOLD);
        document.add(new Paragraph("Test Execution Report", titleFont));
        document.add(new Paragraph(" "));

        PdfPTable table = new PdfPTable(4);
        table.addCell("Scenario");
        table.addCell("Status");
        table.addCell("Duration (ms)");
        table.addCell("Timestamp");

        for (Execution e : executions) {
            table.addCell(e.getScenario() != null ? e.getScenario() : "-");
            table.addCell(e.getStatus() != null ? e.getStatus() : "-");
            table.addCell(String.valueOf(e.getDuration()));
            table.addCell(e.getTimestamp() != null ? e.getTimestamp().toString() : "-");
        }

        document.add(table);
        document.close();
    }

}
