import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class VTScan {
    private static final String API_KEY = "YOUR_VIRUSTOTAL_API_KEY_HERE"; 

    public static int checkFileReport(String sha256) {
        if (API_KEY.contains("YOUR_")) return 0; 
        try {
            URL url = new URL("https://www.virustotal.com/api/v3/files/" + sha256);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("x-apikey", API_KEY);
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(3000);

            if (conn.getResponseCode() == 200) {
                try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        if (line.contains("\"malicious\"")) {
                            String[] parts = line.split(":");
                            if (parts.length > 1) {
                                return Integer.parseInt(parts[1].replaceAll("[^0-9]", "").trim());
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {}
        return 0; 
    }
}