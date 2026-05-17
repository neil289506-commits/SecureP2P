import javax.swing.JOptionPane;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Defender {

	/**
	 * 🎯 核心攔截防線：只判定是否進入黑白名單，絕不對本機 127.0.0.1 進行直接放行
	 */
	public static boolean checkAndEnforce(Socket socket) {
		if (socket == null) return false;
		
		String remoteIP = "UNKNOWN";
		try {
			if (socket.getInetAddress() != null) {
				remoteIP = socket.getInetAddress().getHostAddress();
			}
		} catch (Exception e) {}

		// ⚡ 徹底拔除本機（127.0.0.1 / ::1）豁免邏輯，讓自己傳給自己也能被精準捕捉詢問

		boolean isBanned = LANDiscovery.blackListMap.containsKey(remoteIP) || !LANDiscovery.isIPAllowed(remoteIP);
		boolean isRedlineTriggered = LANDiscovery.isRedlineBlocked;

		if (isBanned || isRedlineTriggered) {
			try {
				final String finalizedIP = remoteIP;
				socket.close(); // 只切斷該不安全 socket，不弄傷主隧道
				
				SoundPlayer.playFromJar("windows10 error");

				SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm");
				String currentTime = sdf.format(new Date());
				String reason = isRedlineTriggered ? "環境熔斷" : "黑名單限制";

				String alertMessage = "⚠️ P2P Defender 已成功攔截一次惡意連線！\n" +
									  "時間： " + currentTime + "\n" +
									  "來源： " + finalizedIP + "\n" +
									  "原因： 符合 " + reason + "\n" +
									  "狀態： 該 IP 連線已被精準切斷，中央監聽隧道完好無損！";

				javax.swing.SwingUtilities.invokeLater(() -> {
					JOptionPane.showMessageDialog(null, alertMessage, "🛡️ P2P Defender 攔截報告", JOptionPane.WARNING_MESSAGE);
				});

				System.out.println("🚨 [Defender] 已阻斷惡意 IP: " + finalizedIP);
			} catch (Exception e) {
				System.out.println("❌ 異常: " + e.getMessage());
			}
			return false; 
		}

		return true; 
	}
}