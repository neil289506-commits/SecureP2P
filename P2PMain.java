import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Enumeration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class P2PMain extends JFrame {
	private DefaultListModel<String> deviceModel = new DefaultListModel<>();
	private JList<String> ipList = new JList<>(deviceModel);
	private JTextField ipInput = new JTextField("192.168.0.xxx");
	
	private JComboBox<LANDiscovery.DiscoveryMode> modeCombo = new JComboBox<>(LANDiscovery.DiscoveryMode.values());
	private JLabel lblDefenseStatus = new JLabel("🛡️ 防護狀態：正常監控中 (環境安全)");
	
	// 🎯 多按鈕防禦系統：黑名單、白名單、解除限制按鈕
	private JButton btnBlockIP = new JButton("🚫 Block IP");
	private JButton btnApproveIP = new JButton("✅ Approve IP");
	private JButton btnUnblockIP = new JButton("🔓 Unblock IP");
	
	private JRadioButton radioBlack = new JRadioButton("黑名單模式", true);
	private JRadioButton radioWhite = new JRadioButton("白名單模式");

	private TrayIcon trayIcon;
	private SystemTray tray;

	// 📡 右鍵戰術快顯選單同步支援
	private JPopupMenu rightClickPopupMenu = new JPopupMenu();
	private JMenuItem popBlockItem = new JMenuItem("🚫 立即封鎖此設備 (寫入黑名單)");
	private JMenuItem popApproveItem = new JMenuItem("✅ 立即信任此設備 (寫入白名單)");
	private JMenuItem popUnblockItem = new JMenuItem("🔓 立即解除限制 (清空黑白名單)");

	public P2PMain() {
		super("Secure P2P AirDrop Portable v1.0");
		initGlobalSkinAndFont();
		
		LANDiscovery.checkAndRepairJsonFiles();
		LANDiscovery.loadDatabase();

		checkAndSetAutoStart();
		new Thread(P2PConnection::startServer).start();
		initUI();
		initSystemTray();
	}

	private void initGlobalSkinAndFont() {
		try { 
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); 
			Font msFont = new Font("Microsoft JhengHei", Font.PLAIN, 14);
			Enumeration<Object> keys = UIManager.getDefaults().keys();
			while (keys.hasMoreElements()) {
				Object key = keys.nextElement();
				if (UIManager.get(key) instanceof Font) UIManager.put(key, msFont);
			}
		} catch(Exception e){}
	}

	private void checkAndSetAutoStart() {
		File flagFile = new File("./.installed");
		if (!flagFile.exists()) {
			try {
				String currentPath = new File(P2PMain.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getAbsolutePath();
				if (currentPath.endsWith(".jar")) {
					String java17Path = "C:\\Program Files\\Java\\jdk17.x.x\\bin\\javaw.exe";
					String cmd = "reg add \"HKEY_CURRENT_USER\\Software\\Microsoft\\Windows\\CurrentVersion\\Run\" /v \"SecureP2PAirDrop\" /t REG_SZ /d \"\\\"" + java17Path + "\\\" -jar \\\"" + currentPath + "\\\" --background\" /f";
					Runtime.getRuntime().exec(cmd);
					flagFile.createNewFile();
				}
			} catch (Exception e) {}
		}
	}

	private void initSystemTray() {
		if (!SystemTray.isSupported()) return;
		tray = SystemTray.getSystemTray();
		
		Image image = null;
		try {
			File iconFile = new File("./resources/icon.png");
			if (iconFile.exists()) image = Toolkit.getDefaultToolkit().getImage(iconFile.getAbsolutePath());
		} catch (Exception e) {}
		if (image == null) image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);

		PopupMenu popup = new PopupMenu();
		MenuItem openItem = new MenuItem("開啟傳輸主介面");
		MenuItem exitItem = new MenuItem("徹底退出安全系統");
		openItem.addActionListener(e -> { setVisible(true); setExtendedState(JFrame.NORMAL); });
		exitItem.addActionListener(e -> System.exit(0));
		popup.add(openItem); popup.add(exitItem);

		trayIcon = new TrayIcon(image, "Secure P2P AirDrop", popup);
		trayIcon.setImageAutoSize(true);
		trayIcon.addActionListener(e -> { setVisible(true); setExtendedState(JFrame.NORMAL); });

		this.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				try {
					tray.add(trayIcon);
					setVisible(false);
				} catch (AWTException ex) {
					System.exit(0);
				}
			}
		});
	}

	private void initUI() {
		this.setLayout(new BorderLayout(12, 12));
		((JPanel)this.getContentPane()).setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

		JPanel topPanel = new JPanel(new GridBagLayout());
		topPanel.setBorder(BorderFactory.createTitledBorder("🛡️ 機場戰術級主動防禦系統"));
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = new Insets(6, 6, 6, 6); 
		gbc.fill = GridBagConstraints.HORIZONTAL;

		lblDefenseStatus.setFont(new Font("Microsoft JhengHei", Font.BOLD, 15));
		lblDefenseStatus.setForeground(new Color(0, 153, 76));

		modeCombo.setRenderer(new DefaultListCellRenderer() {
			@Override
			public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
				JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
				if (value == LANDiscovery.DiscoveryMode.ALL_DETECT) label.setText("🔍 雷達模式：自動偵測 (全網卡掃描)");
				if (value == LANDiscovery.DiscoveryMode.PHYSICAL_ONLY) label.setText("🌐 雷達模式：實體區網 (同SSID優先)");
				if (value == LANDiscovery.DiscoveryMode.VIRTUAL_ONLY) label.setText("🔒 雷達模式：虛擬區網 (VPN/Hamachi)");
				return label;
			}
		});

		gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 5; gbc.weightx = 1.0;
		topPanel.add(lblDefenseStatus, gbc);
		gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 5; gbc.weightx = 1.0;
		topPanel.add(modeCombo, gbc);

		ButtonGroup fwGroup = new ButtonGroup();
		fwGroup.add(radioBlack); fwGroup.add(radioWhite);

		gbc.gridwidth = 1; gbc.weightx = 0.15;
		gbc.gridx = 0; gbc.gridy = 2; topPanel.add(radioBlack, gbc);
		gbc.gridx = 1; gbc.gridy = 2; topPanel.add(radioWhite, gbc);
		
		btnBlockIP.setPreferredSize(new Dimension(105, 30));
		btnApproveIP.setPreferredSize(new Dimension(105, 30));
		btnUnblockIP.setPreferredSize(new Dimension(105, 30));
		
		gbc.gridx = 2; gbc.gridy = 2; gbc.weightx = 0.23; topPanel.add(btnBlockIP, gbc);
		gbc.gridx = 3; gbc.gridy = 2; gbc.weightx = 0.23; topPanel.add(btnApproveIP, gbc);
		gbc.gridx = 4; gbc.gridy = 2; gbc.weightx = 0.23; topPanel.add(btnUnblockIP, gbc);

		this.add(topPanel, BorderLayout.NORTH);

		ipList.setFont(new Font("Microsoft JhengHei", Font.PLAIN, 14));
		JScrollPane scrollPane = new JScrollPane(ipList);
		this.add(scrollPane, BorderLayout.CENTER);

		// 右鍵選單組裝
		rightClickPopupMenu.add(popBlockItem);
		rightClickPopupMenu.add(popApproveItem);
		rightClickPopupMenu.add(popUnblockItem);
		popBlockItem.setForeground(Color.RED);
		popApproveItem.setForeground(new Color(0, 153, 76));

		// 🎯 點選事件：點擊自動填入目標 IP
		ipList.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				String selected = ipList.getSelectedValue();
				if (selected != null) {
					String extractedIP = parseIPFromSelectedLine(selected);
					if (extractedIP != null) ipInput.setText(extractedIP); 
				}
			}
			@Override
			public void mousePressed(MouseEvent e) { handlePopupMenuTrigger(e); }
			@Override
			public void mouseReleased(MouseEvent e) { handlePopupMenuTrigger(e); }

			private void handlePopupMenuTrigger(MouseEvent e) {
				if (e.isPopupTrigger()) {
					int index = ipList.locationToIndex(e.getPoint());
					if (index != -1) {
						ipList.setSelectedIndex(index);
						rightClickPopupMenu.show(ipList, e.getX(), e.getY());
					}
				}
			}
		});

		// 🛠️ 按鈕 & 右鍵整合事件：Block IP (黑名單)
		ActionListener blockAction = e -> {
			String ip = parseIPFromSelectedLine(ipList.getSelectedValue());
			if (ip == null) ip = askForValue("請輸入要 Block 的惡意 IP:", "🚫 手動封鎖");
			if (ip == null || ip.trim().isEmpty()) return;

			LANDiscovery.addIPToDatabase(ip.trim(), "2026/01/01", "FOREVER", false);
			SoundPlayer.playFromJar("windowsXPnofi"); // 🎯 成功提示音
			JOptionPane.showMessageDialog(this, "🎯 已成功將 " + ip + " 列入黑名單！");
		};
		btnBlockIP.addActionListener(blockAction);
		popBlockItem.addActionListener(blockAction);

		// 🛠️ 按鈕 & 右鍵整合事件：Approve IP (白名單)
		ActionListener approveAction = e -> {
			String ip = parseIPFromSelectedLine(ipList.getSelectedValue());
			if (ip == null) ip = askForValue("請輸入要 Approve 的信任 IP:", "✅ 白名單放行");
			if (ip == null || ip.trim().isEmpty()) return;

			LANDiscovery.addIPToDatabase(ip.trim(), "2026/01/01", "FOREVER", true);
			SoundPlayer.playFromJar("windowsXPnofi"); // 🎯 成功提示音
			JOptionPane.showMessageDialog(this, "🎉 已成功將 " + ip + " 寫入安全白名單！");
		};
		btnApproveIP.addActionListener(approveAction);
		popApproveItem.addActionListener(approveAction);

		// 🛠️ 按鈕 & 右鍵整合事件：Unblock IP (解除限制)
		ActionListener unblockAction = e -> {
			String ip = parseIPFromSelectedLine(ipList.getSelectedValue());
			if (ip == null) ip = askForValue("請輸入要解除黑白名單限制的 IP:", "🔓 解除限制");
			if (ip == null || ip.trim().isEmpty()) return;

			LANDiscovery.blackListMap.remove(ip.trim());
			LANDiscovery.whiteListMap.remove(ip.trim());
			LANDiscovery.saveDatabase("banned_ip.json", LANDiscovery.blackListMap);
			LANDiscovery.saveDatabase("whitelist.json", LANDiscovery.whiteListMap);
			
			SoundPlayer.playFromJar("windowsXPnofi"); // 🎯 成功提示音
			JOptionPane.showMessageDialog(this, "🔓 已成功移除 " + ip + " 的黑白名單規則。");
		};
		btnUnblockIP.addActionListener(unblockAction);
		popUnblockItem.addActionListener(unblockAction);

		JPanel bottomPanel = new JPanel(new GridBagLayout());
		GridBagConstraints bGbc = new GridBagConstraints();
		bGbc.insets = new Insets(5, 2, 5, 2);
		bGbc.fill = GridBagConstraints.HORIZONTAL;

		JButton btnScan = new JButton("🔄 深度雷達掃描 (具備 10 秒安全冷卻機制)");
		btnScan.setFont(new Font("Microsoft JhengHei", Font.BOLD, 14));
		btnScan.setPreferredSize(new Dimension(100, 42));
		
		bGbc.gridx = 0; bGbc.gridy = 0; bGbc.gridwidth = 3; bGbc.weightx = 1.0;
		bottomPanel.add(btnScan, bGbc);

		JLabel lblTarget = new JLabel("目標 IP: ");
		ipInput.setFont(new Font("Consolas", Font.PLAIN, 15));
		ipInput.setPreferredSize(new Dimension(100, 35)); 
		JButton btnSend = new JButton("選擇並發送檔案 ➔");
		btnSend.setFont(new Font("Microsoft JhengHei", Font.BOLD, 14));
		btnSend.setPreferredSize(new Dimension(150, 35));

		bGbc.gridy = 1; bGbc.gridwidth = 1; 
		bGbc.gridx = 0; bGbc.weightx = 0.0; bGbc.fill = GridBagConstraints.NONE;
		bottomPanel.add(lblTarget, bGbc);
		bGbc.gridx = 1; bGbc.weightx = 0.6; bGbc.fill = GridBagConstraints.HORIZONTAL;
		bottomPanel.add(ipInput, bGbc);
		bGbc.gridx = 2; bGbc.weightx = 0.4; bGbc.fill = GridBagConstraints.HORIZONTAL;
		bottomPanel.add(btnSend, bGbc);

		this.add(bottomPanel, BorderLayout.SOUTH);

		radioBlack.addActionListener(e -> LANDiscovery.isWhiteListMode = false);
		radioWhite.addActionListener(e -> LANDiscovery.isWhiteListMode = true);

		btnScan.addActionListener(e -> {
			deviceModel.clear();
			new Thread(() -> {
				try {
					java.util.List<String> activeDevices = LANDiscovery.detectAndPingAll((LANDiscovery.DiscoveryMode) modeCombo.getSelectedItem());
					SwingUtilities.invokeLater(() -> {
						lblDefenseStatus.setText("🛡️ 防護狀態：環境安全 (當前網域共 " + LANDiscovery.lastDetectedDeviceCount + " 台設備)");
						if(activeDevices.isEmpty()) deviceModel.addElement("未發現任何線上 P2P 設備。");
						for(String info : activeDevices) deviceModel.addElement(info);
					});
				} catch (Exception ex) {}
			}).start();
		});

		btnSend.addActionListener(e -> {
			String targetIP = ipInput.getText().trim();
			if (targetIP.isEmpty() || targetIP.equals("192.168.0.xxx")) {
				JOptionPane.showMessageDialog(this, "❌ 請先選擇或輸入有效的目標 IP！");
				return;
			}

			FileDialog nativeChooser = new FileDialog(this, "選擇要空投發送的檔案", FileDialog.LOAD);
			nativeChooser.setVisible(true);
			
			if (nativeChooser.getDirectory() != null && nativeChooser.getFile() != null) {
				File file = new File(nativeChooser.getDirectory(), nativeChooser.getFile());
				if (!file.exists()) return;

				// 🎯 發送開始瞬間，立刻引爆新音效 woosh.wav！
				SoundPlayer.playFromJar("woosh");

				new Thread(() -> {
					boolean success = P2PConnection.sendFile(targetIP, file, P2PConnection.ChannelMode.AUTOMATIC);
					if (success) {
						// 🎯 傳輸成功，改播放 windowsXPnofi.wav
						SoundPlayer.playFromJar("windowsXPnofi");
						SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "🎉 檔案傳輸成功！安全空投已送達。"));
					} else {
						SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "❌ 傳送失敗，目標可能拒絕或中端連線。"));
					}
				}).start();
			}
		});

		this.setSize(780, 600);
		this.setDefaultCloseOperation(EXIT_ON_CLOSE);
		this.setLocationRelativeTo(null);
		setVisible(true);
	}

	private String parseIPFromSelectedLine(String selectedLine) {
		if (selectedLine == null) return null;
		Pattern pattern = Pattern.compile("\\[([0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3})\\]");
		Matcher matcher = pattern.matcher(selectedLine);
		if (matcher.find()) return matcher.group(1).trim();
		return null;
	}

	private String askForValue(String message, String title) {
		return JOptionPane.showInputDialog(this, message, title, JOptionPane.QUESTION_MESSAGE);
	}

	public static void main(String[] args) {
		SwingUtilities.invokeLater(P2PMain::new);
	}
}