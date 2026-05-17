import java.io.*;
import java.net.*;
import java.util.*;

public class LANDiscovery {
	public static final int PORT = 9999;
	public static boolean isRedlineBlocked = false;
	public static boolean isWhiteListMode = false;
	public static int lastDetectedDeviceCount = 0;

	// 記憶體中的快取安全防線
	public static final Map<String, IPDefenseRule> blackListMap = new HashMap<>();
	public static final Map<String, IPDefenseRule> whiteListMap = new HashMap<>();

	public enum DiscoveryMode { ALL_DETECT, PHYSICAL_ONLY, VIRTUAL_ONLY }

	public static class IPDefenseRule {
		public String ip;
		public String since;
		public String end;
		public IPDefenseRule(String ip, String since, String end) {
			this.ip = ip; this.since = since; this.end = end;
		}
	}

	/**
	 * 🛠️ 自動初始化與修復 JSON 檔案
	 */
	public static void checkAndRepairJsonFiles() {
		try {
			File bFile = new File("banned_ip.json");
			if (!bFile.exists() || bFile.length() < 5) {
				saveDatabase("banned_ip.json", new HashMap<>());
			}
			File wFile = new File("whitelist.json");
			if (!wFile.exists() || wFile.length() < 5) {
				saveDatabase("whitelist.json", new HashMap<>());
			}
		} catch (Exception e) {
			System.out.println("❌ 初始化 JSON 失敗: " + e.getMessage());
		}
	}

	/**
	 * 🔄 精準載入資料庫（支援標準 JSON 陣列解析）
	 */
	public static void loadDatabase() {
		blackListMap.clear();
		whiteListMap.clear();
		parseJsonToMap("banned_ip.json", blackListMap);
		parseJsonToMap("whitelist.json", whiteListMap);
		System.out.println("📂 [防禦資料庫] 載入完成。黑名單: " + blackListMap.size() + " 筆, 白名單: " + whiteListMap.size() + " 筆");
	}

	private static void parseJsonToMap(String fileName, Map<String, IPDefenseRule> targetMap) {
		File file = new File(fileName);
		if (!file.exists()) return;

		StringBuilder sb = new StringBuilder();
		try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"))) {
			String line;
			while ((line = br.readLine()) != null) sb.append(line.trim());
		} catch (Exception e) {
			return;
		}

		String content = sb.toString();
		int index = 0;
		while ((index = content.indexOf("{", index)) != -1) {
			int endIdx = content.indexOf("}", index);
			if (endIdx == -1) break;
			String block = content.substring(index + 1, endIdx);
			
			String ip = extractField(block, "IP");
			String since = extractField(block, "Since");
			String end = extractField(block, "End");

			if (ip != null && !ip.isEmpty()) {
				targetMap.put(ip, new IPDefenseRule(ip, since, end));
			}
			index = endIdx + 1;
		}
	}

	private static String extractField(String block, String key) {
		int keyIdx = block.indexOf("\"" + key + "\"");
		if (keyIdx == -1) keyIdx = block.indexOf(key); 
		if (keyIdx == -1) return "";
		
		int colonIdx = block.indexOf(":", keyIdx);
		if (colonIdx == -1) return "";
		
		int startQuote = block.indexOf("\"", colonIdx);
		if (startQuote == -1) return "";
		int endQuote = block.indexOf("\"", startQuote + 1);
		if (endQuote == -1) return "";
		
		return block.substring(startQuote + 1, endQuote).trim();
	}

	/**
	 * ⚡ 核心修復：動態安全寫入（標準 JSON 格式）
	 */
	public static synchronized void saveDatabase(String fileName, Map<String, IPDefenseRule> sourceMap) {
		File file = new File(fileName);
		try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8"))) {
			bw.write("[\n");
			Iterator<IPDefenseRule> it = sourceMap.values().iterator();
			while (it.hasNext()) {
				IPDefenseRule rule = it.next();
				bw.write("  {\n");
				bw.write("    \"IP\": \"" + rule.ip + "\",\n");
				bw.write("    \"Since\": \"" + rule.since + "\",\n");
				bw.write("    \"End\": \"" + rule.end + "\"\n");
				bw.write("  }");
				if (it.hasNext()) bw.write(",");
				bw.write("\n");
			}
			bw.write("]\n");
			bw.flush();
		} catch (Exception e) {
			System.out.println("❌ 寫入 JSON 失敗: " + e.getMessage());
		}
	}

	/**
	 * 🚫/✅ 新增、更新安全規則
	 */
	public static void addIPToDatabase(String ip, String since, String end, boolean isWhiteList) {
		if (ip == null || ip.isEmpty() || "192.168.0.xxx".equals(ip)) return;
		
		IPDefenseRule newRule = new IPDefenseRule(ip, since, end);
		if (isWhiteList) {
			whiteListMap.put(ip, newRule);
			blackListMap.remove(ip); 
			saveDatabase("whitelist.json", whiteListMap);
			saveDatabase("banned_ip.json", blackListMap); 
		} else {
			blackListMap.put(ip, newRule);
			whiteListMap.remove(ip); 
			saveDatabase("banned_ip.json", blackListMap);
			saveDatabase("whitelist.json", whiteListMap); 
		}
		System.out.println("🎯 已同步更新 JSON 資料庫。目標: " + ip + "，屬性: " + (isWhiteList ? "白名單" : "黑名單"));
	}

	/**
	 * 🛡️ 門戶判定
	 */
	public static boolean isIPAllowed(String ip) {
		if (isRedlineBlocked) return false; 
		if ("0:0:0:0:0:0:0:1".equals(ip) || "localhost".equals(ip)) ip = "127.0.0.1";

		if (isWhiteListMode) {
			return whiteListMap.containsKey(ip);
		} else {
			return !blackListMap.containsKey(ip);
		}
	}

	/**
	 * 🔄 深度雷達掃描與分流引擎 (已徹底拔除大於 15 台的熔斷限制)
	 */
	public static List<String> detectAndPingAll(DiscoveryMode mode) throws Exception {
		List<String> activeDevices = new ArrayList<>();
		Set<String> uniqueIPs = Collections.synchronizedSet(new HashSet<>()); 
		
		List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());

		for (NetworkInterface netIf : interfaces) {
			if (!netIf.isUp() || netIf.isLoopback() || netIf.isVirtual()) continue;
			
			String name = netIf.getDisplayName().toLowerCase();
			boolean isPhysicalCard = name.contains("wireless") || name.contains("wi-fi") || 
			                         name.contains("ethernet") || name.contains("realtek") || 
			                         name.contains("intel") || name.contains("killer");

			if (mode == DiscoveryMode.PHYSICAL_ONLY && !isPhysicalCard) continue;
			if (mode == DiscoveryMode.VIRTUAL_ONLY && isPhysicalCard) continue;

			for (InterfaceAddress addr : netIf.getInterfaceAddresses()) {
				InetAddress ip = addr.getAddress();
				if (!(ip instanceof Inet4Address)) continue;

				String hostIP = ip.getHostAddress();
				short maskLen = addr.getNetworkPrefixLength();
				if (maskLen < 24 || maskLen > 30) continue; 

				String subnet = hostIP.substring(0, hostIP.lastIndexOf(".") + 1);
				String cardTypeStr = isPhysicalCard ? "實體區網直連" : "虛擬隧道網路";

				Thread[] threads = new Thread[254];
				for (int i = 1; i <= 254; i++) {
					final String targetIP = subnet + i;
					threads[i - 1] = new Thread(() -> {
						try {
							InetAddress address = InetAddress.getByName(targetIP);
							if (address.isReachable(150)) {
								String compName = "Unknown-PC";
								String ssid = "UNKNOWN";
								
								try (Socket s = new Socket()) {
									s.connect(new InetSocketAddress(targetIP, PORT), 150);
									DataOutputStream dos = new DataOutputStream(s.getOutputStream());
									DataInputStream dis = new DataInputStream(s.getInputStream());
									dos.writeUTF("INFO_REQUEST");
									dos.flush();
									String resp = dis.readUTF();
									if (resp.contains("|")) {
										compName = resp.split("\\|")[0];
										ssid = resp.split("\\|")[1];
									}
								} catch (Exception ex) {}

								uniqueIPs.add(targetIP);

								synchronized (activeDevices) {
									activeDevices.add("🚀 [" + cardTypeStr + "] [" + targetIP + "] ➔ " + compName + " (SSID: " + ssid + ")");
								}
							}
						} catch (Exception e) {}
					});
					threads[i - 1].start();
				}

				for (Thread t : threads) {
					if (t != null) t.join();
				}
			}
		}

		// 🎯 核心修正：取消所有數量限制，環境有幾台就顯示幾台，絕對不觸發無謂的熔斷！
		isRedlineBlocked = false;
		lastDetectedDeviceCount = uniqueIPs.size(); 
		return activeDevices;
	}

	public static String getLocalSSID() {
		try {
			Process process = Runtime.getRuntime().exec("netsh wlan show interfaces");
			BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), "Big5"));
			String line;
			while ((line = reader.readLine()) != null) {
				if (line.contains("SSID") && !line.contains("BSSID")) {
					return line.split(":")[1].trim();
				}
			}
		} catch (Exception e) {}
		return "Wired-Ethernet";
	}
}