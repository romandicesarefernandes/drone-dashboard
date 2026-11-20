package control;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Minimal PDF 1.4 writer (no external library): US Letter pages with
 * Helvetica text, lines, rectangles and colours. Enough for report exports.
 * Coordinates are PDF points with the origin at the bottom-left of the page.
 */
final class SimplePdf {
    static final float WIDTH = 612, HEIGHT = 792;

    private final List<StringBuilder> pages = new ArrayList<>();
    private StringBuilder page;

    void newPage() {
        page = new StringBuilder();
        pages.add(page);
    }

    int pageCount() { return pages.size(); }

    void text(float x, float y, float size, boolean bold, String s) {
        page.append("BT /").append(bold ? "F2 " : "F1 ").append(f(size)).append(" Tf ")
            .append(f(x)).append(' ').append(f(y)).append(" Td (").append(escape(s)).append(") Tj ET\n");
    }

    void strokeColor(float r, float g, float b) {
        page.append(f(r)).append(' ').append(f(g)).append(' ').append(f(b)).append(" RG\n");
    }

    void fillColor(float r, float g, float b) {
        page.append(f(r)).append(' ').append(f(g)).append(' ').append(f(b)).append(" rg\n");
    }

    void lineWidth(float w) {
        page.append(f(w)).append(" w\n");
    }

    void line(float x1, float y1, float x2, float y2) {
        page.append(f(x1)).append(' ').append(f(y1)).append(" m ").append(f(x2)).append(' ').append(f(y2)).append(" l S\n");
    }

    void polyline(float[] xs, float[] ys) {
        if (xs.length < 2) return;
        page.append(f(xs[0])).append(' ').append(f(ys[0])).append(" m");
        for (int i = 1; i < xs.length; i++) {
            page.append(' ').append(f(xs[i])).append(' ').append(f(ys[i])).append(" l");
        }
        page.append(" S\n");
    }

    void fillRect(float x, float y, float w, float h) {
        page.append(f(x)).append(' ').append(f(y)).append(' ').append(f(w)).append(' ').append(f(h)).append(" re f\n");
    }

    void strokeRect(float x, float y, float w, float h) {
        page.append(f(x)).append(' ').append(f(y)).append(' ').append(f(w)).append(' ').append(f(h)).append(" re S\n");
    }

    /** Approximate Helvetica text width (average glyph width ~0.53 em). */
    static float textWidth(String s, float size) {
        return s.length() * size * 0.53f;
    }

    /** Cuts the text with "..." so it fits in maxWidth. */
    static String fit(String s, float size, float maxWidth) {
        if (textWidth(s, size) <= maxWidth) return s;
        int n = Math.max(1, (int) (maxWidth / (size * 0.53f)) - 3);
        return s.substring(0, Math.min(s.length(), n)) + "...";
    }

    byte[] build() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        List<Integer> offsets = new ArrayList<>();
        write(out, "%PDF-1.4\n");

        int n = pages.size();
        StringBuilder kids = new StringBuilder();
        for (int i = 0; i < n; i++) kids.append(5 + 2 * i).append(" 0 R ");

        object(out, offsets, "<< /Type /Catalog /Pages 2 0 R >>");
        object(out, offsets, "<< /Type /Pages /Kids [" + kids + "] /Count " + n + " >>");
        object(out, offsets, "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica /Encoding /WinAnsiEncoding >>");
        object(out, offsets, "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold /Encoding /WinAnsiEncoding >>");
        for (int i = 0; i < n; i++) {
            object(out, offsets, "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] "
                    + "/Resources << /Font << /F1 3 0 R /F2 4 0 R >> >> /Contents " + (6 + 2 * i) + " 0 R >>");
            byte[] content = pages.get(i).toString().getBytes(StandardCharsets.ISO_8859_1);
            offsets.add(out.size());
            write(out, (offsets.size()) + " 0 obj\n<< /Length " + content.length + " >>\nstream\n");
            out.writeBytes(content);
            write(out, "\nendstream\nendobj\n");
        }

        int xref = out.size();
        StringBuilder sb = new StringBuilder();
        sb.append("xref\n0 ").append(offsets.size() + 1).append("\n0000000000 65535 f \n");
        for (int off : offsets) sb.append(String.format(Locale.ROOT, "%010d 00000 n \n", off));
        sb.append("trailer\n<< /Size ").append(offsets.size() + 1).append(" /Root 1 0 R >>\nstartxref\n")
          .append(xref).append("\n%%EOF\n");
        write(out, sb.toString());
        return out.toByteArray();
    }

    private static void object(ByteArrayOutputStream out, List<Integer> offsets, String body) {
        offsets.add(out.size());
        write(out, offsets.size() + " 0 obj\n" + body + "\nendobj\n");
    }

    private static void write(ByteArrayOutputStream out, String s) {
        out.writeBytes(s.getBytes(StandardCharsets.ISO_8859_1));
    }

    private static String f(float v) {
        return String.format(Locale.ROOT, "%.2f", v);
    }

    /** Escapes PDF string delimiters and replaces characters outside Latin-1. */
    private static String escape(String s) {
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) {
            if (c == '\\' || c == '(' || c == ')') sb.append('\\').append(c);
            else if (c == '–' || c == '—') sb.append('-');
            else if (c > 255 || c < 32) sb.append('?');
            else sb.append(c);
        }
        return sb.toString();
    }
}
