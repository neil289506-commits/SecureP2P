import javax.sound.sampled.*;
import java.net.URL;
import java.io.BufferedInputStream;

public class Resources {
    public static URL get(String path) { return Resources.class.getResource(path); }
    public static void playSound(String fileName) {
        try {
            BufferedInputStream bis = new BufferedInputStream(Resources.class.getResourceAsStream("/" + fileName));
            AudioInputStream ais = AudioSystem.getAudioInputStream(bis);
            Clip clip = AudioSystem.getClip();
            clip.open(ais);
            clip.start();
        } catch (Exception e) { System.err.println("音效播放失敗: " + fileName); }
    }
}