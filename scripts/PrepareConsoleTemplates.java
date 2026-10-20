import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.RenderingHints;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/** Preserve original shells; the actual complete cover supplies branding and ratings. */
public class PrepareConsoleTemplates {
    public static void main(String[] args) throws Exception {
        File root = new File(args.length == 0 ? "artwork/overlays" : args[0]);
        prepare(root, "gc", new Rectangle(287, 132, 364, 517));
        prepare(root, "ps2", new Polygon(new int[]{225, 728, 731, 205},
            new int[]{28, 28, 729, 726}, 4));
        prepareN64Box(root);
    }

    /** Original, unbranded landscape box frame, not a claim to reconstruct a cartridge label. */
    static void prepareN64Box(File root) throws Exception {
        BufferedImage output = new BufferedImage(960, 768, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = output.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setPaint(new GradientPaint(84, 134, new Color(87, 88, 90), 876, 698, new Color(24, 25, 27)));
        g.fillRoundRect(84, 134, 792, 564, 10, 10);
        g.setColor(new Color(160, 160, 160, 140));
        g.drawLine(91, 137, 868, 137);
        g.setColor(new Color(8, 8, 9));
        g.fillRect(92, 142, 776, 548);
        g.setComposite(AlphaComposite.Clear);
        g.fillRect(96, 146, 768, 540);
        g.dispose();
        ImageIO.write(output, "png", new File(root, "n64_box.png"));
    }
    static void prepare(File root, String name, Shape opening) throws Exception {
        BufferedImage source = ImageIO.read(new File(root, name + ".png"));
        if (source.getWidth() != 960 || source.getHeight() != 768)
            throw new IllegalArgumentException("Unexpected dimensions: " + name);
        BufferedImage output = new BufferedImage(960, 768, BufferedImage.TYPE_INT_ARGB);
        output.setRGB(0, 0, 960, 768, source.getRGB(0, 0, 960, 768, null, 0, 960), 0, 960);
        Graphics2D g = output.createGraphics();
        g.setComposite(AlphaComposite.Clear);
        g.fill(opening);
        g.dispose();
        ImageIO.write(output, "png", new File(root, name + "_full_cover.png"));
    }
}