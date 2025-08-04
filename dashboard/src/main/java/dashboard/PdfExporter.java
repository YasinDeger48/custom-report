package dashboard;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.pdf.*;
import dashboard.model.Execution;

import java.awt.*;
import java.io.FileOutputStream;
import java.util.List;

public class PdfExporter {

    public static void export(List<Execution> executions, String filePath, String runId) throws Exception {
        Document document = new Document(PageSize.A4.rotate(), 36, 36, 54, 36);

        PdfWriter.getInstance(document, new FileOutputStream(filePath));
        document.open();

        try {
            Image logo = Image.getInstance(PdfExporter.class.getClassLoader().getResource("templates/logo.png"));
            logo.scaleToFit(120, 60);
            logo.setAlignment(Image.ALIGN_LEFT);
            document.add(logo);
        } catch (Exception e) {
            // Logo bulunamazsa sorun olmaz
        }

        Font titleFont = new Font(Font.HELVETICA, 18, Font.BOLD);
        Paragraph title = new Paragraph("Test Execution Report", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);

        Font runIdFont = new Font(Font.HELVETICA, 12, Font.NORMAL);
        Paragraph runIdParagraph = new Paragraph("Run ID: " + (runId != null ? runId : "-"), runIdFont);
        runIdParagraph.setAlignment(Element.ALIGN_CENTER);
        runIdParagraph.setSpacingAfter(20);
        document.add(runIdParagraph);

        PdfPTable table = new PdfPTable(new float[]{3, 1, 1, 2, 4});
        table.setWidthPercentage(100);

        addHeaderCell(table, "Scenario");
        addHeaderCell(table, "Status");
        addHeaderCell(table, "Duration (ms)");
        addHeaderCell(table, "Timestamp");
        //addHeaderCell(table, "Screenshot");
        addHeaderCell(table, "Error Message");

        for (Execution e : executions) {
            addCell(table, e.getScenario() != null ? e.getScenario() : "-");
            addStatusCell(table, e.getStatus());
            addCell(table, String.valueOf(e.getDuration()));
            addCell(table, e.getTimestamp() != null ? e.getTimestamp().toString() : "-");
          //:(  addCell(table, e.getScreenshot() != null ? "Available" : "-");
            addCell(table, e.getError() != null ? e.getError() : "-");
        }

        document.add(table);
        document.close();
    }

    private static void addHeaderCell(PdfPTable table, String text) {
        Font font = new Font(Font.HELVETICA, 12, Font.BOLD);
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(new Color(200, 200, 200));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setPadding(5);
        table.addCell(cell);
    }

    private static void addCell(PdfPTable table, String text) {
        Font font = new Font(Font.HELVETICA, 10);
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setPadding(5);
        table.addCell(cell);
    }

    private static void addStatusCell(PdfPTable table, String status) {
        Font font = new Font(Font.HELVETICA, 10, Font.BOLD);
        PdfPCell cell = new PdfPCell(new Phrase(status != null ? status : "-", font));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setPadding(5);

        if ("PASSED".equalsIgnoreCase(status)) {
            cell.setBackgroundColor(new Color(25, 135, 84));  // Yeşil
        } else if ("FAILED".equalsIgnoreCase(status)) {
            cell.setBackgroundColor(new Color(220, 53, 69));  // Kırmızı
        } else if ("SKIPPED".equalsIgnoreCase(status)) {
            cell.setBackgroundColor(new Color(255, 193, 7));  // Sarı
        } else {
            cell.setBackgroundColor(new Color(211, 211, 211)); // Gri
        }

        table.addCell(cell);
    }
}
