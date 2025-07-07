package tr.com.logidex.cnetdedicated.fxcontrols;
import javafx.geometry.Bounds;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.stage.Screen;
public class TouchInputUtil {
    public static Screen getScreenForNode(Node node) {
        // Node'un ekran koordinatlarını al
        Bounds screenBounds = node.localToScreen(node.getBoundsInLocal());
        if (screenBounds == null) {
            return Screen.getPrimary();
        }
        // Node'un merkez noktası
        double centerX = screenBounds.getMinX() + screenBounds.getWidth() / 2;
        double centerY = screenBounds.getMinY() + screenBounds.getHeight() / 2;
        // Hangi ekranda olduğunu bul
        for (Screen screen : Screen.getScreens()) {
            Rectangle2D bounds = screen.getBounds();
            if (bounds.contains(centerX, centerY)) {
                System.out.println(screen);
                return screen;
            }
        }
        return Screen.getPrimary();
    }
}
