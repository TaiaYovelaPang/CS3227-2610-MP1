package teamsync;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Retrieves and interprets public Google Sheets attendance data. */
public final class AttendanceService {
    private static final Pattern SHEET_ID = Pattern.compile("/spreadsheets/d/([^/]+)");
    private static final Pattern GID = Pattern.compile("(?:[?#&])gid=(\\d+)");
    // Sheets' raw CSV endpoint commonly redirects to a download host. Follow that redirect so public sheets
    // can be read while retaining mixed numeric/text values such as 1, 0, L, and E.
    private final HttpClient client = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    public List<AttendanceRecord> loadAttendance(String sheetUrl, LocalDate date) throws IOException, InterruptedException {
        return attendanceForDate(fetchRows(sheetUrl), date);
    }

    public MonthlyAttendanceReport loadMonthlyAttendance(String sheetUrl, YearMonth month) throws IOException, InterruptedException {
        return monthlyAttendanceForMonth(fetchRows(sheetUrl), month);
    }

    private List<List<String>> fetchRows(String sheetUrl) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(csvUri(sheetUrl)).GET().build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalArgumentException("The Google Sheet could not be read (HTTP " + response.statusCode()
                    + "). Ensure it is shared as Anyone with the link.");
        }
        return parseCsv(response.body());
    }

    URI csvUri(String sheetUrl) {
        Matcher idMatcher = SHEET_ID.matcher(sheetUrl);
        if (!idMatcher.find()) throw new IllegalArgumentException("Enter a valid Google Sheets URL.");
        Matcher gidMatcher = GID.matcher(sheetUrl);
        String gid = gidMatcher.find() ? gidMatcher.group(1) : "0";
        // Do not use the gviz query endpoint here. It infers a date column containing 1/0 as numeric and
        // can export text entries such as L and E as blank cells. The Sheets CSV export keeps displayed values.
        String url = "https://docs.google.com/spreadsheets/d/" + idMatcher.group(1)
                + "/export?format=csv&gid=" + URLEncoder.encode(gid, StandardCharsets.UTF_8);
        return URI.create(url);
    }

    List<AttendanceRecord> attendanceForDate(List<List<String>> rows, LocalDate date) {
        if (rows.size() < 2 || rows.get(0).isEmpty() || !rows.get(0).get(0).trim().equalsIgnoreCase("Member")) {
            throw new IllegalArgumentException("The sheet needs a first column named Member.");
        }
        String requestedDate = date.toString();
        int dateColumn = rows.get(0).indexOf(requestedDate);
        if (dateColumn < 0) throw new IllegalArgumentException("No attendance column was found for " + requestedDate + ".");
        List<AttendanceRecord> attendance = new ArrayList<>();
        for (int rowIndex = 1; rowIndex < rows.size(); rowIndex++) {
            List<String> row = rows.get(rowIndex);
            if (row.isEmpty()) continue;
            String name = row.get(0).trim();
            String value = dateColumn < row.size() ? row.get(dateColumn).trim() : "";
            if (!name.isEmpty()) {
                if (value.isEmpty()) {
                    throw new IllegalArgumentException("Missing attendance value for " + name + " (sheet row "
                            + (rowIndex + 1) + ") on " + requestedDate + ".");
                }
                try {
                    attendance.add(new AttendanceRecord(name, AttendanceStatus.fromSheetValue(value)));
                } catch (IllegalArgumentException error) {
                    throw new IllegalArgumentException("Unsupported attendance value '" + value + "' for " + name
                            + " (sheet row " + (rowIndex + 1) + ") on " + requestedDate
                            + ". Use 1, 0, L, or E.");
                }
            }
        }
        if (attendance.isEmpty()) throw new IllegalArgumentException("No valid attendance values were found for " + requestedDate + ".");
        return attendance;
    }

    MonthlyAttendanceReport monthlyAttendanceForMonth(List<List<String>> rows, YearMonth month) {
        validateSheet(rows);
        List<SessionColumn> sessionColumns = new ArrayList<>();
        for (int column = 1; column < rows.get(0).size(); column++) {
            try {
                LocalDate date = LocalDate.parse(rows.get(0).get(column).trim());
                if (YearMonth.from(date).equals(month)) sessionColumns.add(new SessionColumn(column, date));
            } catch (java.time.format.DateTimeParseException ignored) {
                // Non-date columns are not attendance sessions.
            }
        }
        if (sessionColumns.isEmpty()) throw new IllegalArgumentException("No attendance columns were found for " + month + ".");

        Map<String, MemberCounter> members = new LinkedHashMap<>();
        List<MonthlyAttendanceReport.SessionAttendance> sessions = new ArrayList<>();
        for (SessionColumn session : sessionColumns) {
            int marked = 0;
            int attended = 0;
            for (int rowIndex = 1; rowIndex < rows.size(); rowIndex++) {
                List<String> row = rows.get(rowIndex);
                if (row.isEmpty()) continue;
                String name = row.get(0).trim();
                String value = session.index() < row.size() ? row.get(session.index()).trim() : "";
                AttendanceStatus status = parseStatus(value);
                if (name.isEmpty() || status == null) continue;
                marked++;
                MemberCounter counter = members.computeIfAbsent(name, ignored -> new MemberCounter());
                counter.marked++;
                if (status.attended()) {
                    attended++;
                    counter.attended++;
                }
            }
            sessions.add(new MonthlyAttendanceReport.SessionAttendance(session.date(), attended, marked));
        }
        List<MonthlyAttendanceReport.MemberAttendance> memberStatistics = members.entrySet().stream()
                .map(entry -> new MonthlyAttendanceReport.MemberAttendance(entry.getKey(), entry.getValue().attended,
                        entry.getValue().marked))
                .sorted(Comparator.comparing(MonthlyAttendanceReport.MemberAttendance::memberName,
                        String.CASE_INSENSITIVE_ORDER))
                .toList();
        return new MonthlyAttendanceReport(month, sessions, memberStatistics);
    }

    private void validateSheet(List<List<String>> rows) {
        if (rows.size() < 2 || rows.get(0).isEmpty() || !rows.get(0).get(0).trim().equalsIgnoreCase("Member")) {
            throw new IllegalArgumentException("The sheet needs a first column named Member.");
        }
    }

    private AttendanceStatus parseStatus(String value) {
        if (value.isBlank()) return null;
        try {
            return AttendanceStatus.fromSheetValue(value);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private record SessionColumn(int index, LocalDate date) { }

    private static final class MemberCounter {
        private int attended;
        private int marked;
    }

    private List<List<String>> parseCsv(String csv) {
        List<List<String>> rows = new ArrayList<>();
        List<String> row = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean quoted = false;
        for (int index = 0; index < csv.length(); index++) {
            char character = csv.charAt(index);
            if (character == '"') {
                if (quoted && index + 1 < csv.length() && csv.charAt(index + 1) == '"') { field.append('"'); index++; }
                else quoted = !quoted;
            } else if (character == ',' && !quoted) { row.add(field.toString().trim()); field.setLength(0); }
            else if ((character == '\n' || character == '\r') && !quoted) {
                if (character == '\r' && index + 1 < csv.length() && csv.charAt(index + 1) == '\n') index++;
                row.add(field.toString().trim()); field.setLength(0);
                if (row.stream().anyMatch(value -> !value.isEmpty())) rows.add(row);
                row = new ArrayList<>();
            } else field.append(character);
        }
        row.add(field.toString().trim());
        if (row.stream().anyMatch(value -> !value.isEmpty())) rows.add(row);
        return rows;
    }
}
