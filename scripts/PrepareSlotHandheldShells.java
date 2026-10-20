import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.HexFormat;
import javax.imageio.ImageIO;

/** Adapts Slot's GPL-3.0-or-later artwork; retains its shading/embossing without redrawing it. */
public class PrepareSlotHandheldShells {
    record Shell(String system, int width, int height, Rectangle label, String sha256) {}
    static final Shell[] SHELLS = {
        new Shell("gb", 312, 337, new Rectangle(44, 95, 223, 199),
            "0771e3ac10a0d89c74609ce0081d1af0fb76d4b314ccff17d8306661e32487a2"),
        new Shell("gbc", 312, 337, new Rectangle(45, 110, 223, 199),
            "5244d369f029132db74f28fd51f65bbfdebbf62cd869b36e9449177cdf4ad9cf"),
        new Shell("gba", 360, 212, new Rectangle(48, 50, 264, 136),
            "9cb83dc6f5c9529cb7582e869f9afcdb546d5ea4b5bf667cbd271fc05beae8a3")
    };

    public static void main(String[] args) throws Exception {
        File source = new File(args.length > 0 ? args[0] : "artwork/slot/source");
        File target = new File(args.length > 1 ? args[1] : "artwork/overlays/handheld");
        Files.createDirectories(target.toPath());
        for (Shell shell : SHELLS) {
            File input = new File(source, shell.system + "_neutral.png");
            String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(Files.readAllBytes(input.toPath())));
            if (!hash.equals(shell.sha256)) throw new IllegalStateException("Unexpected source: " + input);
            BufferedImage original = ImageIO.read(input);
            if (original.getWidth() != shell.width || original.getHeight() != shell.height)
                throw new IllegalStateException("Unexpected dimensions: " + input);
            BufferedImage overlay = new BufferedImage(shell.width, shell.height, BufferedImage.TYPE_INT_ARGB);
            overlay.setRGB(0, 0, shell.width, shell.height,
                original.getRGB(0, 0, shell.width, shell.height, null, 0, shell.width), 0, shell.width);
            Graphics2D graphics = overlay.createGraphics();
            // Only remove the blank well, allowing Retro Launcher's independent label below it.
            graphics.setComposite(AlphaComposite.Clear);
            graphics.fill(shell.label);
            graphics.dispose();
            for (int y = 0; y < shell.height; y++) for (int x = 0; x < shell.width; x++) {
                if (!shell.label.contains(x, y) && original.getRGB(x, y) != overlay.getRGB(x, y))
                    throw new IllegalStateException("Plastic changed outside label well: " + input);
            }
            if (!ImageIO.write(overlay, "png", new File(target, shell.system + "_shell.png")))
                throw new IllegalStateException("PNG writer unavailable");
            System.out.println("Prepared Slot " + shell.system + " (source hash and unchanged plastic verified)");
        }
    }
}