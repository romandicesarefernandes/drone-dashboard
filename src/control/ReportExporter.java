package control;

import entity.Report;
import entity.Report.Chart;
import entity.Report.Point;
import entity.Report.Series;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Writes a generated Report as a PDF or CSV file (UC-09 step 5: Export PDF / Export CSV). */
public final class ReportExporter {

    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("MM/dd/yyyy h:mm a", Locale.US);
    /** Series colours (RGB 0..1), same order as the on-screen chart palette. */
    private static final float[][] PALETTE = {
        { 0.18f, 0.49f, 0.20f }, { 0.13f, 0.47f, 0.71f }, { 0.90f, 0.49f, 0.13f }, { 0.56f, 0.27f, 0.68f } };

    private static final float MARGIN = 50;
    private static final float BOTTOM = 60;

    private ReportExporter() { }

    /** Suggested file name, e.g. Farm_Summary_2026-10-01_to_2026-10-31.pdf */
    public static String fileName(Report r, String extension) {
        return r.getReportType().replace(' ', '_') + "_" + r.getStartDate() + "_to_" + r.getEndDate() + "." + extension;
    }

    // ------------------------------------------------------------ CSV

    public static void writeCsv(Report r, String farm, Path file) throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add(csv("AgriDrone Farm Dashboard", r.getTitle()));
        lines.add(csv("Farm", farm));
        lines.add(csv("Period", r.getStartDate().format(ReportController.DATE) + " - "
                + r.getEndDate().format(ReportController.DATE)));
        lines.add(csv("Fields", r.getFields()));
        lines.add(csv("Generated", r.getDateGenerated().format(STAMP)));
        lines.add("");
        lines.add(csv("Summary", ""));
        for (String[] s : r.getSummary()) lines.add(csv(s));
        lines.add("");
        lines.add(csv(r.getColumns()));
        for (String[] row : r.getRows()) lines.add(csv(row));
        Files.write(file, lines, StandardCharsets.UTF_8);
    }

    private static String csv(String... cells) {
        List<String> out = new ArrayList<>();
        for (String c : cells) {
            String v = c == null ? "" : c;
            if (v.contains(",") || v.contains("\"") || v.contains("\n")) v = "\"" + v.replace("\"", "\"\"") + "\"";
            out.add(v);
        }
        return String.join(",", out);
    }

    // ------------------------------------------------------------ PDF

    public static void writePdf(Report r, String farm, Path file) throws IOException {
        Layout l = new Layout();
        l.pdf.newPage();
        l.footer();

        l.pdf.fillColor(0.18f, 0.49f, 0.20f);
        l.pdf.text(MARGIN, l.y, 18, true, "AgriDrone Farm Dashboard");
        l.pdf.fillColor(0, 0, 0);
        l.y -= 24;
        l.pdf.text(MARGIN, l.y, 14, true, r.getTitle());
        l.y -= 20;
        l.info("Farm", farm);
        l.info("Period", r.getStartDate().format(ReportController.DATE) + " - "
                + r.getEndDate().format(ReportController.DATE));
        l.info("Fields", r.getFields());
        l.info("Generated", r.getDateGenerated().format(STAMP) + "   (report #" + r.getReportId() + ")");
        l.y -= 8;

        l.heading("Summary");
        for (String[] s : r.getSummary()) l.info(s[0], s[1]);
        l.y -= 8;

        if (r.getChart() != null) {
            l.ensure(230);
            l.heading(r.getChart().title());
            drawChart(l, r.getChart());
        }

        l.heading("Details");
        drawTable(l, r.getColumns(), r.getRows());

        Files.write(file, l.pdf.build());
    }

    /** Line or bar chart in a 512 x 160 pt box, with axes, gridlines and legend. */
    private static void drawChart(Layout l, Chart chart) {
        SimplePdf pdf = l.pdf;
        float x0 = MARGIN + 30, w = SimplePdf.WIDTH - 2 * MARGIN - 30, h = 150;
        float y0 = l.y - h - 4;

        double max = 0;
        for (Series s : chart.series()) for (Point p : s.points()) max = Math.max(max, p.y());
        double top = niceTop(max);
        Set<String> cats = new LinkedHashSet<>();
        for (Series s : chart.series()) for (Point p : s.points()) cats.add(p.x());
        List<String> categories = new ArrayList<>(cats);

        // gridlines and y labels
        pdf.lineWidth(0.5f);
        for (int i = 0; i <= 4; i++) {
            float y = y0 + h * i / 4f;
            pdf.strokeColor(0.85f, 0.85f, 0.85f);
            pdf.line(x0, y, x0 + w, y);
            pdf.fillColor(0.35f, 0.35f, 0.35f);
            pdf.text(MARGIN - 4, y - 3, 7, false, label(top * i / 4));
        }
        pdf.strokeColor(0.4f, 0.4f, 0.4f);
        pdf.line(x0, y0, x0 + w, y0);
        pdf.line(x0, y0, x0, y0 + h);

        int n = categories.size();
        if (chart.bar()) {
            float slot = w / Math.max(1, n);
            float barW = Math.min(60, slot * 0.6f);
            for (Series s : chart.series()) {
                for (Point p : s.points()) {
                    int i = categories.indexOf(p.x());
                    float bh = (float) (p.y() / top * h);
                    float bx = x0 + slot * i + (slot - barW) / 2;
                    pdf.fillColor(PALETTE[0][0], PALETTE[0][1], PALETTE[0][2]);
                    pdf.fillRect(bx, y0, barW, bh);
                    pdf.fillColor(0.2f, 0.2f, 0.2f);
                    String v = label(p.y());
                    pdf.text(bx + barW / 2 - SimplePdf.textWidth(v, 7) / 2, y0 + bh + 3, 7, false, v);
                    String name = SimplePdf.fit(p.x(), 7, slot - 4);
                    pdf.text(bx + barW / 2 - SimplePdf.textWidth(name, 7) / 2, y0 - 10, 7, false, name);
                }
            }
        } else {
            float step = n > 1 ? w / (n - 1) : 0;
            int every = Math.max(1, (int) Math.ceil(n / 10.0));
            pdf.fillColor(0.35f, 0.35f, 0.35f);
            for (int i = 0; i < n; i += every) {
                pdf.text(x0 + step * i - 8, y0 - 10, 7, false, categories.get(i));
            }
            pdf.lineWidth(1.4f);
            int c = 0;
            for (Series s : chart.series()) {
                float[] col = PALETTE[c++ % PALETTE.length];
                pdf.strokeColor(col[0], col[1], col[2]);
                float[] xs = new float[s.points().size()], ys = new float[s.points().size()];
                for (int i = 0; i < xs.length; i++) {
                    Point p = s.points().get(i);
                    xs[i] = x0 + step * categories.indexOf(p.x());
                    ys[i] = (float) (y0 + p.y() / top * h);
                }
                pdf.polyline(xs, ys);
            }
            // legend
            float lx = x0;
            c = 0;
            for (Series s : chart.series()) {
                float[] col = PALETTE[c++ % PALETTE.length];
                pdf.fillColor(col[0], col[1], col[2]);
                pdf.fillRect(lx, y0 - 26, 10, 6);
                pdf.fillColor(0.2f, 0.2f, 0.2f);
                pdf.text(lx + 14, y0 - 26, 8, false, s.name());
                lx += 24 + SimplePdf.textWidth(s.name(), 8);
            }
        }
        pdf.lineWidth(1);
        pdf.fillColor(0, 0, 0);
        pdf.strokeColor(0, 0, 0);
        l.y = y0 - 44;
    }

    /** Table with a shaded header row; continues on new pages and repeats the header. */
    private static void drawTable(Layout l, String[] cols, List<String[]> rows) {
        float total = SimplePdf.WIDTH - 2 * MARGIN;
        float[] weight = new float[cols.length];
        float sum = 0;
        for (int c = 0; c < cols.length; c++) {
            int longest = cols[c].length();
            for (String[] row : rows) longest = Math.max(longest, row[c].length());
            weight[c] = Math.min(28, Math.max(5, longest));
            sum += weight[c];
        }
        float[] widths = new float[cols.length];
        for (int c = 0; c < cols.length; c++) widths[c] = total * weight[c] / sum;

        float rowH = 15;
        l.ensure(rowH * 3);
        header(l, cols, widths, rowH);
        for (int i = 0; i < rows.size(); i++) {
            if (l.y - rowH < BOTTOM) {
                l.newPage();
                header(l, cols, widths, rowH);
            }
            if (i % 2 == 1) {
                l.pdf.fillColor(0.96f, 0.97f, 0.96f);
                l.pdf.fillRect(MARGIN, l.y - rowH + 4, total, rowH);
            }
            l.pdf.fillColor(0, 0, 0);
            cells(l, rows.get(i), widths, false);
            l.y -= rowH;
        }
        l.pdf.strokeColor(0.75f, 0.75f, 0.75f);
        l.pdf.line(MARGIN, l.y + 4, MARGIN + total, l.y + 4);
        l.pdf.strokeColor(0, 0, 0);
    }

    private static void header(Layout l, String[] cols, float[] widths, float rowH) {
        float total = SimplePdf.WIDTH - 2 * MARGIN;
        l.pdf.fillColor(0.88f, 0.93f, 0.88f);
        l.pdf.fillRect(MARGIN, l.y - rowH + 4, total, rowH);
        l.pdf.fillColor(0, 0, 0);
        cells(l, cols, widths, true);
        l.y -= rowH;
    }

    private static void cells(Layout l, String[] values, float[] widths, boolean bold) {
        float x = MARGIN + 4;
        for (int c = 0; c < values.length; c++) {
            l.pdf.text(x, l.y - 7, 8, bold, SimplePdf.fit(values[c], 8, widths[c] - 6));
            x += widths[c];
        }
    }

    private static double niceTop(double max) {
        if (max <= 0) return 1;
        if (max <= 1) return Math.ceil(max * 10) / 10.0;
        double mag = Math.pow(10, Math.floor(Math.log10(max)));
        double[] steps = { 1, 2, 2.5, 5, 10 };
        for (double s : steps) if (s * mag >= max) return s * mag;
        return 10 * mag;
    }

    private static String label(double v) {
        return v == Math.rint(v) ? String.valueOf((long) v) : String.format(Locale.US, "%.2f", v);
    }

    /** Tracks the write position and starts new pages with a footer. */
    private static final class Layout {
        final SimplePdf pdf = new SimplePdf();
        float y = SimplePdf.HEIGHT - 60;

        void newPage() {
            pdf.newPage();
            footer();
            y = SimplePdf.HEIGHT - 60;
        }

        void footer() {
            pdf.fillColor(0.5f, 0.5f, 0.5f);
            pdf.text(MARGIN, 30, 8, false, "AgriDrone Farm Dashboard - page " + pdf.pageCount());
            pdf.fillColor(0, 0, 0);
        }

        void ensure(float space) {
            if (y - space < BOTTOM) newPage();
        }

        void heading(String text) {
            ensure(40);
            pdf.text(MARGIN, y, 12, true, text);
            pdf.strokeColor(0.18f, 0.49f, 0.20f);
            pdf.line(MARGIN, y - 4, SimplePdf.WIDTH - MARGIN, y - 4);
            pdf.strokeColor(0, 0, 0);
            y -= 20;
        }

        void info(String label, String value) {
            ensure(16);
            pdf.text(MARGIN, y, 10, true, label + ":");
            pdf.text(MARGIN + 150, y, 10, false, SimplePdf.fit(value, 10, SimplePdf.WIDTH - 2 * MARGIN - 150));
            y -= 14;
        }
    }
}
