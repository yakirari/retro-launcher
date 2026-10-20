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
        // The unbranded CD shell contains the original cover-retaining tabs and side clips.
        // Restore those over the cleared printed face rather than flattening the jewel case.
        File shellFile = args.length > 2 ? new File(args[2]) : new File(new File(args[0]).getParentFile(), "cd.png");
        BufferedImage shell = ImageIO.read(shellFile);
        if (shell == null || shell.getWidth() != 960 || shell.getHeight() != 768) {
            throw new IllegalArgumentException("Expected the original 960x768 CD jewel-case shell");
        }
        g.setComposite(AlphaComposite.SrcOver);
        g.drawImage(shell, 0, 0, null);
        g.dispose();
        ImageIO.write(result, "png", new File(args[1]));
    }
}