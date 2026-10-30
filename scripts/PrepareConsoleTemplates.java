import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.Color;
import java.awt.Image;
import java.awt.GradientPaint;
import java.awt.RenderingHints;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayDeque;
import javax.imageio.ImageIO;

/** Preserve original shells; the actual complete cover supplies branding and ratings. */
public class PrepareConsoleTemplates {
    public static void main(String[] args) throws Exception {
        File root = new File(args.length == 0 ? "artwork/overlays" : args[0]);
        prepare(root, "gc", new Rectangle(287, 132, 364, 517));
        prepare(root, "ps2", new Polygon(new int[]{225, 728, 731, 205},
            new int[]{28, 28, 729, 726}, 4));
        prepareSnesJapan(root);
        prepareN64Box(root);
    }

    /**
     * Fits the supplied Super Famicom template (black background and black label window) into the
     * shared 960x768 overlay canvas, then opens the label window by flood-filling its dark pixels.
     */
    static void prepareSnesJapan(File root) throws Exception {
        BufferedImage template = ImageIO.read(new File(root, "../snes/super_famicom_template.png"));
        if (template.getWidth() != 1536 || template.getHeight() != 1024)
            throw new IllegalArgumentException("Unexpected Super Famicom template dimensions");
        float scale = 0.55f;
        int scaledWidth = Math.round(template.getWidth() * scale);
        int scaledHeight = Math.round(template.getHeight() * scale);
        int offsetX = (960 - scaledWidth) / 2;
        int offsetY = (768 - scaledHeight) / 2;
        BufferedImage output = new BufferedImage(960, 768, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = output.createGraphics();
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, 960, 768);
        g.drawImage(template.getScaledInstance(scaledWidth, scaledHeight, Image.SCALE_AREA_AVERAGING),
            offsetX, offsetY, null);
        g.dispose();

        // Label window center in template pixels.
        int seedX = offsetX + Math.round(767 * scale);
        int seedY = offsetY + Math.round(322 * scale);
        int dark = 48;
        int solidBlack = 10;
        boolean[] visited = new boolean[960 * 768];
        ArrayDeque<int[]> queue = new ArrayDeque<>();
        queue.add(new int[]{seedX, seedY});
        visited[seedY * 960 + seedX] = true;
        int count = 0;
        while (!queue.isEmpty()) {
            int[] p = queue.poll();
            int rgb = output.getRGB(p[0], p[1]);
            int level = Math.max((rgb >> 16) & 255, Math.max((rgb >> 8) & 255, rgb & 255));
            int alpha = Math.max(0, Math.min(255, (level - solidBlack) * 255 / (dark - solidBlack)));
            output.setRGB(p[0], p[1], alpha << 24 | (rgb & 0xFFFFFF));
            count++;
            for (int[] d : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                int x = p[0] + d[0];
                int y = p[1] + d[1];
                if (x < 0 || y < 0 || x >= 960 || y >= 768 || visited[y * 960 + x]) continue;
                int next = output.getRGB(x, y);
                int nextLevel = Math.max((next >> 16) & 255, Math.max((next >> 8) & 255, next & 255));
                if (nextLevel >= dark) continue;
                visited[y * 960 + x] = true;
                queue.add(new int[]{x, y});
            }
        }
        if (count < 50_000 || count > 200_000)
            throw new IllegalArgumentException("Super Famicom label window not isolated: " + count);
        ImageIO.write(output, "png", new File(root, "snes_japan.png"));
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