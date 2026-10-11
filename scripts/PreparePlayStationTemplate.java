import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/** Keep the original jewel-case shell; let the complete cover supply its own branding. */
public class PreparePlayStationTemplate {
    public static void main(String[] args) throws Exception {
        BufferedImage source = ImageIO.read(new File(args[0]));
        if (source.getWidth() != 960 || source.getHeight() != 768) {
            throw new IllegalArgumentException("Expected the 960x768 PS1 case source");
        }
        BufferedImage result = new BufferedImage(960, 768, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = result.createGraphics();
        g.drawImage(source, 0, 0, null);
        g.setComposite(AlphaComposite.Clear);
        g.fillRect(215, 93, 607, 587);
        g.dispose();
        ImageIO.write(result, "png", new File(args[1]));
    }
}