import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.List;
import java.util.zip.*;
import javax.imageio.ImageIO;

/** Local review/export helper; no image is copied into Android build inputs. JDK 17+. */
public class PrepareCartridgeArtwork {
    public static void main(String[] args) throws Exception {
        String mode = args[0];
        Path output = Path.of(args[2]);
        Files.createDirectories(output);
        try (ZipFile zip = new ZipFile(args[1])) {
            if (mode.equals("inventory")) inventory(zip, output);
            else if (mode.equals("prepare")) prepare(zip, output, Path.of(args[3]));
            else if (mode.equals("batch-crop")) batchCrop(zip, output, Path.of(args[3]));
            else if (mode.equals("verify-crops")) verifyCrops(zip, output);
            else throw new IllegalArgumentException("Unknown mode");
        }
    }

    static BufferedImage image(ZipFile zip, String name) throws Exception {
        ZipEntry entry = zip.getEntry(name);
        if (entry == null || entry.getSize() < 1 || entry.getSize() > 1024 * 1024)
            throw new IllegalArgumentException("Missing/oversized source: " + name);
        try (InputStream input = zip.getInputStream(entry)) {
            BufferedImage image = ImageIO.read(input);
            if (image == null || image.getWidth() > 4096 || image.getHeight() > 4096)
                throw new IllegalArgumentException("Invalid source: " + name);
            return image;
        }
    }

    static String hash(ZipFile zip, String name) throws Exception {
        try (InputStream input = zip.getInputStream(zip.getEntry(name))) {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(input.readAllBytes()));
        }
    }

    static void inventory(ZipFile zip, Path output) throws Exception {
        List<String> names = zip.stream().map(ZipEntry::getName).filter(n -> n.endsWith(" Cart.jpg"))
            .sorted().toList();
        List<String> rows = new ArrayList<>();
        rows.add("filename\tsourceWidth\tsourceHeight\tsha256");
        BufferedImage sheet = null;
        Graphics2D g = null;
        for (int i = 0; i < names.size(); i++) {
            if (i % 48 == 0) {
                if (g != null) { g.dispose(); ImageIO.write(sheet, "jpg", output.resolve("review-" + ((i-1)/48+1) + ".jpg").toFile()); }
                sheet = new BufferedImage(1600, 1440, BufferedImage.TYPE_INT_RGB);
                g = sheet.createGraphics();
                g.setColor(Color.WHITE); g.fillRect(0, 0, 1600, 1440);
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            }
            String name = names.get(i);
            BufferedImage im = image(zip, name);
            rows.add(name + "\t" + im.getWidth() + "\t" + im.getHeight() + "\t" + hash(zip, name));
            int x = (i % 8) * 200, y = ((i % 48) / 8) * 240;
            // Entire cartridge, not a guessed crop: unreviewed images never become runtime labels.
            double scale = Math.min(192.0 / im.getWidth(), 190.0 / im.getHeight());
            g.drawImage(im, x+4, y+4, (int)(im.getWidth()*scale), (int)(im.getHeight()*scale), null);
            g.setColor(Color.BLACK); g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
            String caption = (i+1) + ": " + name;
            for (int line = 0; line < 3 && line * 28 < caption.length(); line++)
                g.drawString(caption.substring(line*28, Math.min(caption.length(), (line+1)*28)), x+4, y+198+line*13);
        }
        if (g != null) { g.dispose(); ImageIO.write(sheet, "jpg", output.resolve("review-" + ((names.size()-1)/48+1) + ".jpg").toFile()); }
        Files.write(output.resolve("inventory.tsv"), rows, StandardCharsets.UTF_8);
        System.out.println("Inventoried " + names.size() + " cartridge scans; all require per-label review.");
    }

    static void prepare(ZipFile zip, Path output, Path manifest) throws Exception {
        for (String row : Files.readAllLines(manifest, StandardCharsets.UTF_8)) {
            String[] fields = row.split("\t");
            if (fields.length != 9) throw new IllegalArgumentException("Invalid reviewed row");
            String signature = fields[0], name = fields[1];
            if (!signature.matches("[0-9A-F]{8}") || !hash(zip, name).equals(fields[2]))
                throw new IllegalArgumentException("Identity/hash mismatch: " + name);
            BufferedImage source = image(zip, name);
            if (source.getWidth() != Integer.parseInt(fields[3]) || source.getHeight() != Integer.parseInt(fields[4]))
                throw new IllegalArgumentException("Dimensions changed: " + name);
            BufferedImage label = source.getSubimage(Integer.parseInt(fields[5]), Integer.parseInt(fields[6]),
                Integer.parseInt(fields[7]), Integer.parseInt(fields[8]));
            ImageIO.write(label, "png", output.resolve(signature + ".png").toFile());
        }
    }

    // The archive photographs share front-on cartridge geometry. Search for the actual
    // straight sticker sides inside that geometry; retain curved corners as original pixels.
    static double edge(BufferedImage image, boolean vertical, int coordinate, int start, int end) {
        double[] samples = new double[end-start];
        for (int i = start; i < end; i++) {
            int a = vertical ? image.getRGB(coordinate-2, i) : image.getRGB(i, coordinate-2);
            int b = vertical ? image.getRGB(coordinate+2, i) : image.getRGB(i, coordinate+2);
            double sum = 0;
            for (int shift : new int[]{0,8,16}) sum += Math.abs(((a >> shift)&255)-((b >> shift)&255));
            samples[i-start] = sum / 3;
        }
        Arrays.sort(samples);
        // Ignore isolated text/moire peaks: a real boundary persists down most of the side.
        double sum = 0;
        for (int i = samples.length/5; i < samples.length*4/5; i++) sum += samples[i];
        return sum / Math.max(1, samples.length*4/5-samples.length/5);
    }

    static int boundary(BufferedImage im, boolean vertical, double from, double to, double low, double high) {
        int dimension = vertical ? im.getWidth() : im.getHeight();
        int span = vertical ? im.getHeight() : im.getWidth();
        int best = (int)(dimension*(from+to)/2);
        double score = -1;
        for (int p = Math.max(2,(int)(dimension*from)); p <= Math.min(dimension-3,(int)(dimension*to)); p++) {
            double candidate = edge(im, vertical, p, (int)(span*low), (int)(span*high));
            if (candidate > score) { score = candidate; best = p; }
        }
        return best;
    }

    static void batchCrop(ZipFile zip, Path output, Path overridesFile) throws Exception {
        Map<String,int[]> overrides = new HashMap<>();
        for (String row : Files.readAllLines(overridesFile, StandardCharsets.UTF_8)) {
            if (row.isBlank() || row.startsWith("#")) continue;
            String[] f = row.split("\t");
            if (f.length != 5) throw new IllegalArgumentException("Invalid crop override: " + row);
            overrides.put(f[0], new int[]{Integer.parseInt(f[1]),Integer.parseInt(f[2]),Integer.parseInt(f[3]),Integer.parseInt(f[4])});
        }
        List<String> names = zip.stream().map(ZipEntry::getName).filter(n -> n.endsWith(" Cart.jpg")).sorted().toList();
        List<String> rows = new ArrayList<>();
        rows.add("id\tfilename\tsourceWidth\tsourceHeight\tsourceSha256\tleft\ttop\twidth\theight\tmethod");
        Path labels = output.resolve("labels"), reviews = output.resolve("crop-review");
        Files.createDirectories(labels); Files.createDirectories(reviews);
        BufferedImage sheet = null; Graphics2D graphics = null;
        for (int i = 0; i < names.size(); i++) {
            String name = names.get(i);
            BufferedImage source = image(zip, name);
            int w = source.getWidth(), h = source.getHeight();
            int left = boundary(source,true,.25,.285,.23,.82);
            int right = boundary(source,true,.72,.755,.23,.82);
            int top = boundary(source,false,.065,.115,.34,.64);
            int bottom = boundary(source,false,.86,.98,.33,.65);
            // Two-pixel inclusion margin protects fine borders at the detected boundary.
            int[] crop = overrides.getOrDefault(name, new int[]{Math.max(0,left-1),Math.max(0,top-1),right-left+3,bottom-top+3});
            int x=crop[0], y=crop[1], cw=crop[2], ch=crop[3];
            if (x<0 || y<0 || cw<1 || ch<1 || x+cw>w || y+ch>h) throw new IllegalArgumentException("Crop outside source: "+name);
            String id=String.format(Locale.ROOT,"%03d",i+1);
            BufferedImage label=source.getSubimage(x,y,cw,ch);
            ImageIO.write(label,"png",labels.resolve(id+".png").toFile());
            rows.add(String.join("\t", id,name,""+w,""+h,hash(zip,name),""+x,""+y,""+cw,""+ch,
                overrides.containsKey(name)?"manual-override":"edge-detected"));
            if (i%48==0) {
                if(graphics!=null){graphics.dispose();ImageIO.write(sheet,"jpg",reviews.resolve("labels-"+((i-1)/48+1)+".jpg").toFile());}
                sheet=new BufferedImage(1600,1680,BufferedImage.TYPE_INT_RGB);graphics=sheet.createGraphics();
                graphics.setColor(new Color(225,225,225));graphics.fillRect(0,0,1600,1680);
                graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            }
            int sx=(i%8)*200,sy=((i%48)/8)*280;
            double scale=Math.min(188.0/cw,225.0/ch);
            graphics.drawImage(label,sx+(200-(int)(cw*scale))/2,sy+4,(int)(cw*scale),(int)(ch*scale),null);
            graphics.setColor(Color.BLACK);graphics.setFont(new Font(Font.SANS_SERIF,Font.PLAIN,11));
            String caption=id+": "+name;
            for(int line=0;line<3&&line*28<caption.length();line++)
                graphics.drawString(caption.substring(line*28,Math.min(caption.length(),(line+1)*28)),sx+3,sy+240+line*13);
        }
        if(graphics!=null){graphics.dispose();ImageIO.write(sheet,"jpg",reviews.resolve("labels-"+((names.size()-1)/48+1)+".jpg").toFile());}
        Files.write(output.resolve("all-crops.tsv"),rows,StandardCharsets.UTF_8);
        System.out.println("Prepared original-resolution crops for all "+names.size()+" scans.");
    }

    static void verifyCrops(ZipFile zip, Path output) throws Exception {
        Set<String> remaining = new HashSet<>(zip.stream().map(ZipEntry::getName)
            .filter(n -> n.endsWith(" Cart.jpg")).toList());
        int count = 0;
        List<String> rows = Files.readAllLines(output.resolve("all-crops.tsv"), StandardCharsets.UTF_8);
        for (String row : rows.subList(1, rows.size())) {
            String[] f = row.split("\t");
            if (!remaining.remove(f[1]) || !hash(zip,f[1]).equals(f[4]))
                throw new IllegalArgumentException("Duplicate or changed source: " + f[1]);
            BufferedImage source = image(zip,f[1]);
            BufferedImage label = ImageIO.read(output.resolve("labels/"+f[0]+".png").toFile());
            int x=Integer.parseInt(f[5]), y=Integer.parseInt(f[6]), w=Integer.parseInt(f[7]), h=Integer.parseInt(f[8]);
            if (label.getWidth()!=w || label.getHeight()!=h || source.getWidth()!=Integer.parseInt(f[2]) ||
                    source.getHeight()!=Integer.parseInt(f[3])) throw new IllegalArgumentException("Dimensions: "+f[1]);
            for (int cy=0;cy<h;cy++) for(int cx=0;cx<w;cx++)
                if(source.getRGB(x+cx,y+cy)!=label.getRGB(cx,cy))
                    throw new IllegalArgumentException("Pixel mismatch: "+f[1]);
            count++;
        }
        if (!remaining.isEmpty() || count!=313) throw new IllegalArgumentException("Incomplete archive coverage");
        System.out.println("Verified all "+count+" PNGs: exact original crop pixels, dimensions, source hashes, complete coverage.");
    }
}