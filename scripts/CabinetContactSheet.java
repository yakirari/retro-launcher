import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Arrays;
import javax.imageio.ImageIO;

/** Review source identity and silhouette against contrasting backgrounds. */
public class CabinetContactSheet {
    public static void main(String[] args) throws Exception {
        File[] files = new File(args[0]).listFiles((dir, name) -> name.endsWith(".png"));
        if (files == null) throw new IllegalArgumentException("No images");
        Arrays.sort(files);
        int columns = 6, cellW = 180, cellH = 235;
        BufferedImage sheet = new BufferedImage(columns*cellW, ((files.length+columns-1)/columns)*cellH, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = sheet.createGraphics();
        g.setColor(new Color(38, 48, 66)); g.fillRect(0, 0, sheet.getWidth(), sheet.getHeight());
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        for (int i=0; i<files.length; i++) {
            BufferedImage image = ImageIO.read(files[i]);
            int x=(i%columns)*cellW, y=(i/columns)*cellH;
            double scale=Math.min((cellW-12.0)/image.getWidth(), (cellH-28.0)/image.getHeight());
            int w=(int)(image.getWidth()*scale), h=(int)(image.getHeight()*scale);
            g.drawImage(image, x+(cellW-w)/2, y+(cellH-28-h)/2, w, h, null);
            g.setColor(Color.WHITE); g.drawString(files[i].getName(), x+8, y+cellH-8);
        }
        g.dispose(); ImageIO.write(sheet, "png", new File(args[1]));
    }
}