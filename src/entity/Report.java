package entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Entity: a generated farm report (UC-09). Holds the compiled content so the
 * same report can be previewed and exported as PDF or CSV.
 */
public class Report {

    /** One data series of a chart. */
    public record Series(String name, List<Point> points) { }

    /** One value on a chart; x is a category label (a date or a field name). */
    public record Point(String x, double y) { }

    /** Chart for the preview and the PDF. bar = bar chart, otherwise a line chart. */
    public record Chart(String title, String yLabel, boolean bar, List<Series> series) { }

    private final int reportId;
    private final String reportType;
    private final LocalDateTime dateGenerated;
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final String fields;
    private final List<String[]> summary;   // {label, value}
    private final String[] columns;
    private final List<String[]> rows;
    private final Chart chart;              // may be null
    private String format = "Preview";      // becomes "PDF", "CSV" or "PDF, CSV" once exported

    public Report(int reportId, String reportType, LocalDateTime dateGenerated, LocalDate startDate,
                  LocalDate endDate, String fields, List<String[]> summary, String[] columns,
                  List<String[]> rows, Chart chart) {
        this.reportId = reportId;
        this.reportType = reportType;
        this.dateGenerated = dateGenerated;
        this.startDate = startDate;
        this.endDate = endDate;
        this.fields = fields;
        this.summary = List.copyOf(summary);
        this.columns = columns.clone();
        this.rows = List.copyOf(rows);
        this.chart = chart;
    }

    public int getReportId() { return reportId; }
    public String getReportType() { return reportType; }
    public LocalDateTime getDateGenerated() { return dateGenerated; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public String getFields() { return fields; }
    public List<String[]> getSummary() { return summary; }
    public String[] getColumns() { return columns.clone(); }
    public List<String[]> getRows() { return rows; }
    public Chart getChart() { return chart; }
    public String getFormat() { return format; }

    /** Records an export format ("PDF" or "CSV") once per format. */
    public void addFormat(String exported) {
        if (format.equals("Preview")) {
            format = exported;
        } else if (!format.contains(exported)) {
            format = format + ", " + exported;
        }
    }

    public String getTitle() { return reportType + " Report"; }
}
