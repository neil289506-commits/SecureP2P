import javax.sound.sampled.*;
import java.io.InputStream;
import java.io.BufferedInputStream;
import java.io.File;

public class SoundPlayer {

	/**
	 * 🔊 核心戰術音效播放引擎（全面對齊純英文新檔名）
	 * @param soundName 檔案名稱（不需輸入 .wav，例如 "woosh" 或 "windowsXPnofi"）
	 */
	public static void playFromJar(String soundName) {
		// 🚀 啟動獨立執行緒播放，確保絕對不卡死 P2P Socket 連線與 Swing UI 執行緒
		new Thread(() -> {
			String fullFileName = soundName + ".wav";
			
			// 🛡️ 策略 A：優先檢查程式執行目錄下的實體 resources 資料夾（最強外部救援機制）
			File[] searchPaths = {
				new File("./" + fullFileName),
				new File("./resources/" + fullFileName)
			};

			for (File audioFile : searchPaths) {
				if (audioFile.exists()) {
					try (AudioInputStream ais = AudioSystem.getAudioInputStream(audioFile)) {
						executePlayWithDecoder(ais);
						return; // 播放成功，立即退出
					} catch (Exception e) {
						System.out.println("⚠️ [SoundPlayer] 外部檔案 " + audioFile.getName() + " 播放失敗，嘗試切換內部資源...");
					}
				}
			}

			// 🛡️ 策略 B：外部找不到實體檔案，直接讀取打包在 JAR 內部的資源
			String[] jarPaths = { "/" + fullFileName, "/resources/" + fullFileName };
			for (String jarPath : jarPaths) {
				try (InputStream is = SoundPlayer.class.getResourceAsStream(jarPath)) {
					if (is != null) {
						try (InputStream bufferedIn = new BufferedInputStream(is);
							 AudioInputStream originalAis = AudioSystem.getAudioInputStream(bufferedIn)) {
							executePlayWithDecoder(originalAis);
							return; 
						}
					}
				} catch (Exception e) {
					System.out.println("⚠️ [SoundPlayer] Jar 內路徑 " + jarPath + " 解碼失敗...");
				}
			}

			System.out.println("❌ [SoundPlayer] 無法播放音效，找不到檔案或格式不支援: " + fullFileName);
		}).start();
	}

	/**
	 * 🛠️ 核心音訊轉碼器：自動將各類 WAV 壓縮格式強制解壓為 Java 認識的標準有符號 PCM 數據，防範無聲 Bug
	 */
	private static void executePlayWithDecoder(AudioInputStream sourceAis) throws Exception {
		AudioFormat sourceFormat = sourceAis.getFormat();
		AudioFormat targetFormat = new AudioFormat(
				AudioFormat.Encoding.PCM_SIGNED,
				sourceFormat.getSampleRate() > 0 ? sourceFormat.getSampleRate() : 44100F,
				16,
				sourceFormat.getChannels() > 0 ? sourceFormat.getChannels() : 2,
				(sourceFormat.getChannels() > 0 ? sourceFormat.getChannels() : 2) * 2,
				sourceFormat.getSampleRate() > 0 ? sourceFormat.getSampleRate() : 44100F,
				false
		);

		try (AudioInputStream pcmAis = AudioSystem.getAudioInputStream(targetFormat, sourceAis)) {
			DataLine.Info info = new DataLine.Info(SourceDataLine.class, targetFormat);
			try (SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info)) {
				line.open(targetFormat);
				line.start();
				byte[] buffer = new byte[4096];
				int bytesRead;
				while ((bytesRead = pcmAis.read(buffer)) != -1) {
					line.write(buffer, 0, bytesRead);
				}
				line.drain();
			}
		}
	}
}