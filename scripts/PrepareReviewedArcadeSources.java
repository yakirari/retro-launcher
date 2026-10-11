import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/** Explicit reviewed continuous upright silhouettes. Not a general-purpose black color key.
 * Raw downloads remain untouched. Compile with NormalizeArcadeCabinets.java.
 * Source-specific visible outer rails define each row; restore original dark interior pixels.
 * Excludes cockpits, open frames, partly cropped photos and ambiguous near-black boundaries.
 */
public class PrepareReviewedArcadeSources {
    static final String[] REVIEWED = {
        "1944", "720", "altbeast", "atetris", "bionicc", "bnj",
        "bubbles", "bublbobl", "bzone", "captcomm", "ccastles", "circusc", "commando",
        "congo", "dkongjr", "docastle", "elevator", "gijoeu", "gorf", "kangaroo",
        "mrdo", "nbajam", "nbajamte", "nibbler", "pengo", "pooyan",
        "qix", "robotron", "shollow", "superpac", "timeplt", "toobin",
        "trackfld", "vf", "wow", "xevious", "zaxxon"
    };
    public static void main(String[] args) throws Exception {
        File input = new File(args[0]), output = new File(args[1]);
        if (!output.isDirectory() && !output.mkdirs()) throw new IllegalStateException("Cannot create output");
        for (String rom : REVIEWED) {
            BufferedImage source = ImageIO.read(new File(input, rom + ".png"));
            if (source == null) throw new IllegalStateException("Missing source " + rom);
            BufferedImage cutout = NormalizeArcadeCabinets.normalize(source, true, 2, true);
            if (!ImageIO.write(cutout, "png", new File(output, rom + ".png")))
                throw new IllegalStateException("Cannot encode " + rom);
        }
        System.out.println("Prepared " + REVIEWED.length + " explicitly reviewed upright candidates; inspect contrast sheet before promotion.");
    }
}