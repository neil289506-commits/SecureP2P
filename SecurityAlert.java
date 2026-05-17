import javax.swing.JOptionPane;
public class SecurityAlert {
    public static void show(String fileName) {
        Resources.playSound("windows10.wav");
        JOptionPane.showMessageDialog(null, "⚠️ 攔截威脅檔案: " + fileName, "安全系統警告", JOptionPane.ERROR_MESSAGE);
    }
}