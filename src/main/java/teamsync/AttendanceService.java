package teamsync;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Retrieves and interprets public Google Sheets attendance data. */
public final class AttendanceService {
    private static final Pattern SHEET_ID = Pattern.compile("/spreadsheets/d/([^/]+)");
    private static final Pattern GID = Pattern.compile("(?:[?#&])gid=(\\d+)");
    private final HttpClient client = HttpClient.newHttpClient();

    public List<AttendanceRecord> loadAttendance(String sheetUrl, LocalDate date) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(csvUri(sheetUrl)).GET().build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalArgumentException("The Google Sheet could not be read. Ensure it is shared as Anyone with the link.");
        }
        return attendanceForDate(parseCsv(response.body()), date);
    }

    URI csvUri(String sheetUrl) {
        Matcher idMatcher = SHEET_ID.matcher(sheetUrl);
        if (!idMatcher.find()) throw new IllegalArgumentException("Enter a valid Google Sheets URL.");
        Matcher gidMatcher = GID.matcher(sheetUrl);
        String gid = gidMatcher.find() ? gidMatcher.group(1) : "0";
        String url = "https://docs.google.com/spreadsheets/d/" + idMatcher.group(1)
                + "/gviz/tq?tqx=out:csv&gid=" + URLEncoder.encode(gid, StandardCharsets.UTF_8);
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
            if (!name.isEmpty() && (value.equals("0") || value.equals("0.5") || value.equals("1"))) {
                attendance.add(new AttendanceRecord(name, Double.parseDouble(value)));
            }
        }
        if (attendance.isEmpty()) throw new IllegalArgumentException("No valid attendance values were found for " + requestedDate + ".");
        return attendance;
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
