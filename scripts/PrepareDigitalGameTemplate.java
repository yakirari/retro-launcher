import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/** Original store-neutral digital library card. Transparent center is the game cover window. */
public class PrepareDigitalGameTemplate {
    public static void main(String[] args) throws Exception {
        BufferedImage image = new BufferedImage(600, 900, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setPaint(new GradientPaint(40, 40, new Color(46, 63, 85), 560, 860, new Color(12, 18, 30)));
        g.fillRoundRect(30, 24, 540, 852, 30, 30);
        g.setColor(new Color(123, 184, 220));
        g.setStroke(new BasicStroke(3));
        g.drawRoundRect(32, 26, 536, 848, 28, 28);
        g.setComposite(AlphaComposite.Clear);
        g.fillRect(54, 81, 492, 738);
        g.dispose();
        ImageIO.write(image, "png", new File(args[0]));
    }
}