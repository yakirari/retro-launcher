import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/** Original procedural artwork. No Slot assets or source code are used. JDK 17+. */
public class PrepareHandheldShells {
    public static void main(String[] args) throws Exception {
        File output = new File(args.length == 0 ? "artwork/overlays/handheld" : args[0]);
        output.mkdirs();
        for (String system : new String[]{"gb", "gbc", "gba"}) {
            ImageIO.write(shell(system), "png", new File(output, system + "_shell.png"));
        }
    }

    static BufferedImage shell(String system) {
        boolean advance = system.equals("gba"), color = system.equals("gbc");
        int w = 720, h = advance ? 430 : 780;
        Rectangle label = advance ? new Rectangle(99, 132, 522, 236)
            : new Rectangle(104, color ? 258 : 244, 512, 444);
        BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        Path2D body = new Path2D.Double();
        if (advance) {
            body.moveTo(34, 18); body.lineTo(686, 18); body.quadTo(705, 18, 705, 39);
            body.lineTo(705, 108); body.lineTo(686, 108); body.lineTo(686, 389);
            body.quadTo(686, 412, 662, 412); body.lineTo(58, 412);
            body.quadTo(34, 412, 34, 389); body.lineTo(34, 108); body.lineTo(15, 108);
            body.lineTo(15, 39); body.quadTo(15, 18, 34, 18);
        } else if (color) {
            body.moveTo(48, 24); body.quadTo(360, -8, 672, 24);
            body.quadTo(695, 28, 695, 54); body.lineTo(695, 740);
            body.quadTo(695, 762, 672, 762); body.lineTo(48, 762);
            body.quadTo(25, 762, 25, 740); body.lineTo(25, 54);
            body.quadTo(25, 28, 48, 24);
        } else {
            body.moveTo(92, 18); body.lineTo(673, 18); body.quadTo(695, 18, 695, 40);
            body.lineTo(695, 740); body.quadTo(695, 762, 673, 762);
            body.lineTo(47, 762); body.quadTo(25, 762, 25, 740);
            body.lineTo(25, 105); body.lineTo(70, 105); body.lineTo(70, 40);
            body.quadTo(70, 18, 92, 18);
        }
        body.closePath();
        g.setPaint(new GradientPaint(0, 0, new Color(172,172,172), w, h, new Color(95,95,95)));
        g.fill(body);
        g.setClip(body);
        // Fine mould texture is deterministic and remains confined to the plastic.
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
            int noise = ((x * 73 + y * 151 + x * y * 3) & 15) - 7;
            g.setColor(noise < 0 ? new Color(0,0,0,-noise) : new Color(255,255,255,noise));
            g.fillRect(x, y, 1, 1);
        }
        g.setStroke(new BasicStroke(7)); g.setColor(new Color(56,56,56)); g.draw(body);
        g.setStroke(new BasicStroke(2)); g.setColor(new Color(222,222,222,160));
        g.draw(new RoundRectangle2D.Double(38, advance ? 29 : 31, 644, advance ? 369 : 717, 25, 25));
        // Horizontal grip ribs above the label; shorter side ribs beside the well.
        for (int y = advance ? 37 : 55; y < (advance ? 70 : 145); y += 13) {
            g.setColor(new Color(78,78,78,130)); g.fillRoundRect(advance ? 70 : 105, y, advance ? 580 : 510, 5, 4, 4);
            g.setColor(new Color(218,218,218,120)); g.fillRoundRect(advance ? 70 : 105, y+5, advance ? 580 : 510, 2, 2, 2);
        }
        for (int y = label.y + 18; y < label.y + label.height - 10; y += 17) {
            for (int x : new int[]{49, 639}) {
                g.setColor(new Color(65,65,65,140)); g.fillRoundRect(x, y, 30, 5, 3, 3);
                g.setColor(new Color(218,218,218,110)); g.fillRoundRect(x, y+5, 30, 2, 2, 2);
            }
        }
        String name = advance ? "GAME BOY ADVANCE" : color ? "GAME BOY COLOR" : "GAME BOY";
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD | Font.ITALIC, advance ? 30 : 37));
        int tx = (w - g.getFontMetrics().stringWidth(name))/2, ty = label.y - 24;
        g.setColor(new Color(221,221,221,180)); g.drawString(name, tx+1, ty+2);
        g.setColor(new Color(70,70,70)); g.drawString(name, tx, ty);
        // Recessed label well is a separate transparent opening, never tinted artwork.
        g.setColor(new Color(67,67,67));
        g.fillRoundRect(label.x-7, label.y-7, label.width+14, label.height+14, 18,18);
        g.setColor(new Color(207,207,207)); g.setStroke(new BasicStroke(2));
        g.drawRoundRect(label.x-8, label.y-8, label.width+16, label.height+16, 19,19);
        g.setComposite(AlphaComposite.Clear);
        g.fillRect(label.x, label.y, label.width, label.height);
        g.dispose();
        return image;
    }
}