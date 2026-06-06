package server;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class SmsSender {

    private static final String ACCOUNT_SID = "AC8b675c9e216ab5a791b6151a77748ced";
    private static final String AUTH_TOKEN  = "96ab24052ac26528bfa9fa27e3ff88b4";
    private static final String FROM_NUMBER = "+14788878805";

    private static final String URL = "https://api.twilio.com/2010-04-01/Accounts/"
                                    + ACCOUNT_SID + "/Messages.json";

    public static void sendSms(String toNumber, String body) {
        String normalized = normalizePhone(toNumber);
        if (normalized == null) {
            System.out.println(">>> SMS skipped for '" + toNumber + "': not a valid phone number.");
            return;
        }

        try {
            String credentials = Base64.getEncoder()
                .encodeToString((ACCOUNT_SID + ":" + AUTH_TOKEN).getBytes(StandardCharsets.UTF_8));

            String formData = "To="   + URLEncoder.encode(normalized, StandardCharsets.UTF_8)
                           + "&From=" + URLEncoder.encode(FROM_NUMBER, StandardCharsets.UTF_8)
                           + "&Body=" + URLEncoder.encode(body, StandardCharsets.UTF_8);

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(URL))
                .header("Authorization", "Basic " + credentials)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(formData))
                .build();

            HttpResponse<String> response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 201) {
                System.out.println(">>> SMS successfully sent to " + normalized);
            } else {
                System.err.println(">>> SMS Failed. HTTP " + response.statusCode() + " | " + response.body());
            }

        } catch (Exception e) {
            System.err.println(">>> SMS Failed to Send: " + e.getMessage());
        }
    }

    // Converts Israeli local format (05X...) to E.164 (+9725X...).
    // Leaves numbers already in international format (+X...) untouched.
    // Returns null if the input is clearly not a phone number.
    private static String normalizePhone(String raw) {
        if (raw == null || raw.isBlank() || raw.equalsIgnoreCase("Not Provided")) return null;
        String digits = raw.replaceAll("[\\s\\-()]", "");
        if (digits.startsWith("+"))  return digits;
        if (digits.startsWith("00")) return "+" + digits.substring(2);
        if (digits.startsWith("0"))  return "+972" + digits.substring(1);
        return null;
    }
}
