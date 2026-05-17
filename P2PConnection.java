import java.io.*;
import java.net.*;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.util.concurrent.atomic.AtomicBoolean;

public class P2PConnection {
	private static final int PORT = 9999;

	public enum ChannelMode { AUTOMATIC, WLAN_ONLY, VLAN_ONLY }

	public static void startServer() {
		try (ServerSocket serverSocket = new ServerSocket(PORT)) {
			System.out.println("🛰️ [P2P Server] 接收端安全隧道守聽中，連接埠: " + PORT);
			while (true) {
				Socket clientSocket = serverSocket.accept();
				
				// 1. 先進黑著名單過濾
				if (!Defender.checkAndEnforce(clientSocket)) {
					continue; 
				}
				
				// 2. 透過非同步獨立執行緒處理詢問與傳輸
				new Thread(() -> handleIncomingTransfer(clientSocket)).start();
			}
		} catch (Exception e) {
			System.out.println("❌ [Server Error] 主監聽服務異常崩潰: " + e.getMessage());
		}
	}

	private static void handleIncomingTransfer(Socket socket) {
		try {
			DataInputStream dis = new DataInputStream(socket.getInputStream());
			DataOutputStream dos = new DataOutputStream(socket.getOutputStream());

			String requestType = dis.readUTF();
			
			if ("INFO_REQUEST".equals(requestType)) {
				String hostName = InetAddress.getLocalHost().getHostName();
				String currentSSID = LANDiscovery.getLocalSSID();
				dos.writeUTF(hostName + "|" + currentSSID);
				dos.flush();
				socket.close();
				return;
			}

			if ("FILE_SEND".equals(requestType)) {
				String fileName = dis.readUTF();
				long fileSize = dis.readLong();
				String remoteIP = socket.getInetAddress().getHostAddress();

				// 🎯 強制無差別防線：任何 IP 連入（包括本機自身），一律進行同步詢問！
				AtomicBoolean isAccepted = new AtomicBoolean(false);
				final String finalIP = remoteIP;
				
				SwingUtilities.invokeAndWait(() -> {
					SoundPlayer.playFromJar("windowsXPnofi");
					
					int choice = JOptionPane.showConfirmDialog(
						null,
						"📡 偵測到 P2P 安全空投請求！\n\n" +
						"來源 IP: " + finalIP + "\n" +
						"檔案名稱: " + fileName + "\n" +
						"檔案大小: " + (fileSize / 1024) + " KB\n\n" +
						"請問你是否願意接受此檔案傳輸？",
						"🔔 戰術空投接收確認",
						JOptionPane.YES_NO_OPTION,
						JOptionPane.QUESTION_MESSAGE
					);
					if (choice == JOptionPane.YES_OPTION) {
						isAccepted.set(true);
					}
				});

				if (isAccepted.get()) {
					dos.writeUTF("ACCEPT");
					dos.flush();
					
					File downloadFolder = new File("./Downloads");
					if (!downloadFolder.exists()) downloadFolder.mkdirs();
					File savedFile = new File(downloadFolder, fileName);

					try (FileOutputStream fos = new FileOutputStream(savedFile)) {
						byte[] buffer = new byte[4096];
						int bytesRead;
						long totalRead = 0;
						while (totalRead < fileSize && (bytesRead = dis.read(buffer, 0, (int)Math.min(buffer.length, fileSize - totalRead))) != -1) {
							fos.write(buffer, 0, bytesRead);
							totalRead += bytesRead;
						}
						fos.flush();
					}
					
					SoundPlayer.playFromJar("receive_success");
					SwingUtilities.invokeLater(() -> {
						JOptionPane.showMessageDialog(null, "🎉 檔案已成功安全降落！\n路徑: " + savedFile.getAbsolutePath());
					});
				} else {
					dos.writeUTF("REJECT");
					dos.flush();
					SoundPlayer.playFromJar("windows10 error");
					System.out.println("🚫 使用者手動拒絕了來自 [" + finalIP + "] 的空投檔案。");
				}
			}
		} catch (Exception e) {
			System.out.println("❌ 處理連入檔案時發生異常: " + e.getMessage());
		} finally {
			try { socket.close(); } catch (Exception e) {}
		}
	}

	public static boolean sendFile(String targetIP, File file, ChannelMode mode) {
		try (Socket socket = new Socket()) {
			socket.connect(new InetSocketAddress(targetIP, PORT), 3000);
			DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
			DataInputStream dis = new DataInputStream(socket.getInputStream());

			dos.writeUTF("FILE_SEND");
			dos.writeUTF(file.getName());
			dos.writeLong(file.length());
			dos.flush();

			String response = dis.readUTF();
			if ("ACCEPT".equals(response)) {
				try (FileInputStream fis = new FileInputStream(file)) {
					byte[] buffer = new byte[4096];
					int bytesRead;
					while ((bytesRead = fis.read(buffer)) != -1) {
						dos.write(buffer, 0, bytesRead);
					}
					dos.flush();
				}
				return true;
			} else {
				System.out.println("❌ 傳送失敗：接收端拒絕了你的空投請求。");
				return false;
			}
		} catch (Exception e) {
			System.out.println("❌ 傳送檔案發生異常: " + e.getMessage());
			return false;
		}
	}
}