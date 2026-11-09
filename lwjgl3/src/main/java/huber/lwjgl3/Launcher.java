package huber.lwjgl3;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import huber.HuberGame;

public class Launcher {
    public static void main(String[] args) {
        Lwjgl3ApplicationConfiguration c = new Lwjgl3ApplicationConfiguration();
        c.setTitle("Hubergeddon: Come Grow With Us");
        c.setWindowedMode(1280, 720);
        c.setWindowSizeLimits(960, 540, -1, -1);
        c.useVsync(true);
        c.setForegroundFPS(60);
        c.setBackBufferConfig(8, 8, 8, 8, 16, 0, 4);
        c.setWindowIcon("img/icon_brick.png");
        new Lwjgl3Application(new HuberGame(), c);
    }
}
