import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Arrays;
import javax.imageio.ImageIO;

/** Deterministic build-time cabinet preparation. Originals are never modified. */
public class NormalizeArcadeCabinets {
    private static boolean background(int pixel, boolean blackBackdrop, int blackThreshold) {
        if (blackBackdrop) return (pixel >>> 24) <= 8 ||
            ((pixel >>> 16 & 255) <= blackThreshold && (pixel >>> 8 & 255) <= blackThreshold && (pixel & 255) <= blackThreshold);
        return (pixel >>> 24) <= 8 ||
            ((pixel >>> 16 & 255) >= 245 && (pixel >>> 8 & 255) >= 245 && (pixel & 255) >= 245);
    }

    private static void add(int index, int[] pixels, boolean[] mask, int[] queue, int[] tail, boolean blackBackdrop, int blackThreshold) {
        if (!mask[index] && background(pixels[index], blackBackdrop, blackThreshold)) {
            mask[index] = true;
            queue[tail[0]++] = index;
        }
    }

    static BufferedImage normalize(BufferedImage source) {
        return normalize(source, false);
    }

    static BufferedImage normalize(BufferedImage source, boolean blackBackdrop) {
        return normalize(source, blackBackdrop, 2, false);
    }

    static BufferedImage normalize(BufferedImage source, boolean blackBackdrop, int blackThreshold, boolean solidRowInterior) {
        return normalize(source, blackBackdrop, blackThreshold, solidRowInterior, false);
    }

    static BufferedImage normalize(BufferedImage source, boolean blackBackdrop, int blackThreshold, boolean solidRowInterior, boolean preserveAlpha) {
        int w = source.getWidth(), h = source.getHeight();
        int[] pixels = source.getRGB(0, 0, w, h, null, 0, w);
        boolean[] mask = new boolean[pixels.length];
        int[] queue = new int[pixels.length], tail = {0};
        if (!preserveAlpha) {
            for (int x = 0; x < w; x++) { add(x, pixels, mask, queue, tail, blackBackdrop, blackThreshold); add((h-1)*w+x, pixels, mask, queue, tail, blackBackdrop, blackThreshold); }
            for (int y = 0; y < h; y++) { add(y*w, pixels, mask, queue, tail, blackBackdrop, blackThreshold); add(y*w+w-1, pixels, mask, queue, tail, blackBackdrop, blackThreshold); }
        }
        for (int head = 0; head < tail[0]; head++) {
            int i = queue[head], x = i % w;
            if (x > 0) add(i-1, pixels, mask, queue, tail, blackBackdrop, blackThreshold);
            if (x+1 < w) add(i+1, pixels, mask, queue, tail, blackBackdrop, blackThreshold);
            if (i >= w) add(i-w, pixels, mask, queue, tail, blackBackdrop, blackThreshold);
            if (i+w < pixels.length) add(i+w, pixels, mask, queue, tail, blackBackdrop, blackThreshold);
        }
        if (blackBackdrop) {
            // Some scans contain a thin, detached border. Never package that as part of the silhouette.
            boolean[] visited = new boolean[pixels.length];
            int minimumIsland = Math.max(12, pixels.length / 100);
            for (int start = 0; start < pixels.length; start++) {
                if (mask[start] || visited[start]) continue;
                int count = 1;
                queue[0] = start; visited[start] = true;
                for (int head = 0; head < count; head++) {
                    int i = queue[head], x = i % w;
                    int[] neighbors = {x > 0 ? i-1 : -1, x+1 < w ? i+1 : -1,
                        i >= w ? i-w : -1, i+w < pixels.length ? i+w : -1};
                    for (int n : neighbors) if (n >= 0 && !mask[n] && !visited[n]) {
                        visited[n] = true; queue[count++] = n;
                    }
                }
                if (count < minimumIsland) for (int i=0; i<count; i++) mask[queue[i]] = true;
            }
        }
        if (solidRowInterior) {
            // Reviewed SF2 upright: black front meets black backdrop through the bottom.
            // Preserve original pixels between its visible side rails; never apply to sit-down cabs.
            for (int y=0; y<h; y++) {
                int first=w, last=-1;
                for (int x=0; x<w; x++) if (!mask[y*w+x]) { first=Math.min(first,x); last=x; }
                for (int x=first; x<=last; x++) mask[y*w+x]=false;
            }
        }
        int left = w, top = h, right = -1, bottom = -1;
        for (int i = 0; i < pixels.length; i++) {
            if (mask[i]) pixels[i] = 0;
            if ((pixels[i] >>> 24) <= 8) continue;
            int x = i % w, y = i / w;
            left = Math.min(left, x); right = Math.max(right, x);
            top = Math.min(top, y); bottom = Math.max(bottom, y);
        }
        if (right < left || bottom < top) throw new IllegalArgumentException("Cabinet image is blank");
        BufferedImage cleaned = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        cleaned.setRGB(0, 0, w, h, pixels, 0, w);
        int objectW = right-left+1, objectH = bottom-top+1;
        double scale = Math.min(760.0/objectW, 700.0/objectH);
        int drawW = Math.max(1, (int)Math.round(objectW*scale)), drawH = Math.max(1, (int)Math.round(objectH*scale));
        // Progressive downsampling retains detail on oversized sources; never invent texture or branding.
        BufferedImage fitted = cleaned.getSubimage(left, top, objectW, objectH);
        while (fitted.getWidth()/2 >= drawW && fitted.getHeight()/2 >= drawH) {
            fitted = resize(fitted, fitted.getWidth()/2, fitted.getHeight()/2);
        }
        fitted = resize(fitted, drawW, drawH);
        BufferedImage output = new BufferedImage(960, 768, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = output.createGraphics();
        g.drawImage(fitted, (960-drawW)/2, 768-drawH-18, null);
        g.dispose();
        return output;
    }

    private static BufferedImage resize(BufferedImage source, int w, int h) {
        BufferedImage result = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = result.createGraphics();
        g.setComposite(AlphaComposite.Src);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.drawImage(source, 0, 0, w, h, null);
        g.dispose();
        return result;
    }

    public static void main(String[] args) throws Exception {
        File source = new File(args[0]), target = new File(args[1]);
        if (!target.isDirectory() && !target.mkdirs()) throw new IllegalStateException("Cannot create output directory");
        File[] files = source.listFiles((dir, name) -> name.endsWith(".png") && !name.equals("mame.png") && !name.contains("removebg-preview"));
        if (files == null) throw new IllegalArgumentException("No cabinet source directory");
        Arrays.sort(files);
        for (File stale : target.listFiles()) if (stale.getName().endsWith(".png")) stale.delete();
        for (File file : files) {
            BufferedImage image = ImageIO.read(file);
            if (image == null) throw new IllegalArgumentException("Cannot decode " + file);
            if (!ImageIO.write(normalize(image), "png", new File(target, file.getName()))) throw new IllegalStateException("Cannot encode " + file);
        }
        int replacements = 0;
        if (args.length > 2) {
            File[] references = new File(args[2]).listFiles((dir, name) -> name.endsWith(".png"));
            if (references == null) throw new IllegalArgumentException("No cabinet references");
            Arrays.sort(references);
            for (File file : references) {
                BufferedImage image = ImageIO.read(file);
                if (image == null) throw new IllegalArgumentException("Cannot decode reference " + file);
                // Shinobi has a compressed purple-black scan frame; SF2 has an open black base.
                // New reviewed sources already have a deliberate alpha silhouette. Never black-key
                // them a second time: that erases real dark front panels and side walls.
                boolean cutout = image.getColorModel().hasAlpha() && (image.getRGB(0, 0) >>> 24) == 0 &&
                    (image.getRGB(image.getWidth()-1, image.getHeight()-1) >>> 24) == 0;
                BufferedImage prepared = normalize(image, !cutout, file.getName().equals("shinobi.png") ? 16 : 2,
                    !cutout && file.getName().equals("sf2.png"), cutout);
                if ((prepared.getRGB(0, 0) >>> 24) != 0) throw new IllegalStateException("Opaque reference background " + file);
                if (!ImageIO.write(prepared, "png", new File(target, file.getName()))) throw new IllegalStateException("Cannot encode reference " + file);
                replacements++;
            }
        }
        System.out.println("Prepared " + files.length + " original assets; applied " + replacements + " cabinet references with transparent silhouettes.");
    }
}