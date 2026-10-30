import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import javax.imageio.ImageIO;

/** Offline renderer: originals are read-only. Screen quads are measured in prepared 960x768 pixels. */
public class PrepareArcadePresentations {
    static Path root, review, out;
    static Map<String,Object> obj(Object x) { return PrepareAllArcadeCabinets.obj(x); }
    static List<Map<String,Object>> rows(Object x) { return PrepareAllArcadeCabinets.rows(x); }
    static String str(Object x) { return PrepareAllArcadeCabinets.str(x); }
    static BufferedImage source(Map<String,Object> game, String key) throws Exception {
        if (!(game.get(key) instanceof Map<?,?>)) return null;
        Map<String,Object> record = obj(game.get(key));
        Path file = review.resolve("downloads").resolve(str(record.get("file")));
        if (!PrepareAllArcadeCabinets.sha(file).equals(record.get("sha256"))) throw new AssertionError("Source hash " + file);
        BufferedImage image = ImageIO.read(file.toFile());
        if (image == null) throw new IllegalStateException("Cannot decode " + file);
        return image;
    }
    static void fit(Graphics2D g, BufferedImage art, int x, int y, int w, int h) {
        if (art == null) return;
        double scale = Math.min(w / (double)art.getWidth(), h / (double)art.getHeight());
        int aw = (int)Math.round(art.getWidth()*scale), ah = (int)Math.round(art.getHeight()*scale);
        g.drawImage(art, x+(w-aw)/2, y+(h-ah)/2, aw, ah, null);
    }
    /** No gameplay cropping/stretching: original aspect ratio inside a dark glass letterbox. */
    static BufferedImage screen(BufferedImage art, double aspect) {
        int w = 640, h = (int)Math.round(w/aspect);
        BufferedImage image = new BufferedImage(w,h,BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics(); GenerateArcadeConversions.configure(g);
        g.setColor(new Color(0x030508)); g.fillRect(0,0,w,h); fit(g,art,0,0,w,h);
        // Subtle CRT glass; preserve readable game pixels, no invented game imagery.
        g.setPaint(new GradientPaint(0,0,new Color(180,210,255,10),w,h,new Color(0,0,0,12)));
        g.fillRect(0,0,w,h); g.dispose(); return image;
    }
    static BufferedImage lit(BufferedImage original, Map<String,Object> game, BufferedImage gameplay) {
        BufferedImage image = new BufferedImage(960,768,BufferedImage.TYPE_INT_ARGB);
        image.setRGB(0,0,960,768,original.getRGB(0,0,960,768,null,0,960),0,960);
        List<?> points = (List<?>)game.get("quad"); double[] q = new double[8];
        for(int i=0;i<8;i++) q[i]=((Number)points.get(i)).doubleValue();
        double width=(Math.hypot(q[2]-q[0],q[3]-q[1])+Math.hypot(q[4]-q[6],q[5]-q[7]))/2;
        double height=(Math.hypot(q[6]-q[0],q[7]-q[1])+Math.hypot(q[4]-q[2],q[5]-q[3]))/2;
        GenerateArcadeConversions.warp(image,screen(gameplay,width/height),q);
        GenerateArcadeConversions.assertSameAlpha(original,image);
        return image;
    }
    static Color accent(BufferedImage art) {
        if (art == null) return new Color(0x68768d);
        long r=0,g=0,b=0,n=0;
        for(int y=0;y<art.getHeight();y+=3) for(int x=0;x<art.getWidth();x+=3) {
            int c=art.getRGB(x,y),rr=c>>>16&255,gg=c>>>8&255,bb=c&255;
            if (Math.max(rr,Math.max(gg,bb))<65) continue;
            r+=rr;g+=gg;b+=bb;n++;
        }
        return n==0?new Color(0x68768d):new Color((int)(r/n),(int)(g/n),(int)(b/n));
    }
    /** A newly drawn frontal operator cabinet, NOT a warped photo or a factory-original reconstruction. */
    static BufferedImage front(Map<String,Object> game, BufferedImage gameplay, BufferedImage marquee, BufferedImage panel) {
        BufferedImage image = new BufferedImage(460,1000,BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics(); GenerateArcadeConversions.configure(g);
        Color color=accent(marquee);
        g.setColor(new Color(0x090a0e));g.fillRoundRect(2,2,456,996,8,8);
        g.setPaint(new GradientPaint(4,0,new Color(0x3c3e46),24,0,new Color(0x111218)));
        g.fillRect(4,4,16,990);
        g.setPaint(new GradientPaint(436,0,new Color(0x111218),456,0,new Color(0x3c3e46)));
        g.fillRect(440,4,16,990);
        g.setColor(color.darker());g.fillRect(20,10,3,976);g.fillRect(437,10,3,976);
        // Marquee is complete original printing, never stretched into a taller title card.
        g.setColor(new Color(0x020305));g.fillRect(25,20,410,145);
        if(marquee!=null) fit(g,marquee,28,24,404,136);
        else fit(g,GenerateArcadeConversions.sign(null,str(game.get("title"))),28,24,404,136);
        g.setColor(new Color(0x353840));g.drawRect(24,19,411,146);
        g.setPaint(new GradientPaint(0,172,new Color(0x181b23),0,240,new Color(0x07090e)));
        g.fillRect(24,172,412,77);
        // Recessed bezel around the CRT; horizontal and vertical games both retain full frame.
        g.setColor(new Color(0x11141b));g.fillRoundRect(28,245,404,337,16,16);
        g.setColor(color.darker().darker());g.setStroke(new BasicStroke(3));g.drawRoundRect(31,248,398,331,15,15);
        Shape clip=g.getClip();g.clip(new RoundRectangle2D.Double(49,267,362,292,22,22));
        g.drawImage(screen(gameplay,362.0/292),49,267,362,292,null);g.setClip(clip);
        g.setColor(new Color(0x05060a));g.fillRect(25,594,410,123);
        if(panel!=null) fit(g,panel,27,597,406,114);
        else {
            g.setPaint(new GradientPaint(0,598,color.darker(),0,710,new Color(0x11151d)));
            g.fillRect(27,598,406,114);
        }
        // Illustrative controls; avoid claiming accurate button counts or wiring.
        for(int player=0;panel==null&&player<2;player++) {
            int x=86+player*205;
            GenerateArcadeConversions.joystick(g,x,662,1.5);
            for(int i=0;i<3;i++) GenerateArcadeConversions.button(g,x+45+i*25,651-(i%2)*8,1.1,
                player==0?new Color(0xd74e43):new Color(0x5d97d6));
        }
        g.setColor(color.darker());g.fillRect(24,717,412,9);
        g.setPaint(new GradientPaint(0,728,new Color(0x242731),0,980,new Color(0x090a0f)));
        g.fillRect(24,728,412,250);
        // Detailed original coin-door illustration, no unrelated franchise kickplate/logo.
        g.setColor(new Color(0x07090c));g.fillRoundRect(142,762,176,186,12,12);
        g.setColor(new Color(0x3d414b));g.drawRoundRect(145,765,170,180,10,10);
        for(int x:new int[]{163,239}) {
            g.setColor(new Color(0x111318));g.fillRect(x,786,57,59);
            g.setPaint(new GradientPaint(x,789,new Color(0xb5362c),x,823,new Color(0x3d1010)));
            g.fillRect(x+16,793,25,30);
            g.setColor(new Color(0x7a655d));g.fillRect(x+21,796,14,3);
            g.setColor(new Color(0x353943));g.drawRect(x+5,862,46,29);
        }
        g.setColor(new Color(0x5d616b));g.fillOval(227,858,7,7);
        g.setColor(new Color(0x030405));g.fillRect(18,979,424,13);
        g.setColor(new Color(0x4d4e54));g.drawLine(23,977,436,977);
        g.dispose();return image;
    }
    static void sheets(List<Map<String,Object>> games) throws Exception {
        int per=24;
        for(int start=0;start<games.size();start+=per) {
            int count=Math.min(per,games.size()-start);
            BufferedImage sheet=new BufferedImage(1200,((count+7)/8)*350,BufferedImage.TYPE_INT_RGB);
            Graphics2D g=sheet.createGraphics();GenerateArcadeConversions.configure(g);
            g.setColor(new Color(0x20252e));g.fillRect(0,0,sheet.getWidth(),sheet.getHeight());
            g.setFont(new Font(Font.SANS_SERIF,Font.PLAIN,12));
            for(int i=0;i<count;i++) {
                Map<String,Object> game=games.get(start+i);int x=(i%8)*150,y=(i/8)*350;
                BufferedImage cab=ImageIO.read(out.resolve("front").resolve(str(game.get("asset"))).toFile());
                fit(g,cab,x+2,y+3,146,317);g.setColor(Color.WHITE);g.drawString(str(game.get("rom")),x+8,y+337);
            }
            g.dispose();ImageIO.write(sheet,"png",review.resolve("front-"+(start/per+1)+".png").toFile());
        }
    }
    public static void main(String[] args) throws Exception {
        root=Path.of(args[0]);review=root.resolve("artwork/arcade-presentations-review");out=root.resolve("artwork/arcade-presentations");
        boolean verify=args.length>1&&args[1].equals("--verify");
        List<Map<String,Object>> games=rows(obj(PrepareAllArcadeCabinets.Json.read(review.resolve("plan.json"))).get("games"));
        Path importFile=review.resolve("front-import.json");
        Map<String,Object> imported=Files.exists(importFile)?obj(PrepareAllArcadeCabinets.Json.read(importFile)):null;
        Map<String,Map<String,Object>> importedFronts=new HashMap<>();
        if(imported!=null) {
            for(Map<String,Object> entry:rows(imported.get("fronts"))) {
                if(importedFronts.put(str(entry.get("asset")),entry)!=null)throw new AssertionError("Duplicate imported front");
            }
            Set<String> expected=new HashSet<>();for(Map<String,Object> game:games)expected.add(str(game.get("asset")));
            if(!importedFronts.keySet().equals(expected))throw new AssertionError("Incomplete imported front pack");
        }
        Files.createDirectories(out.resolve("front"));Files.createDirectories(out.resolve("lit"));
        List<Map<String,Object>> index=new ArrayList<>();
        for(Map<String,Object> game:games) {
            String asset=str(game.get("asset"));
            Path original=root.resolve("artwork/full-cabinets").resolve(asset);
            if(!PrepareAllArcadeCabinets.sha(original).equals(game.get("originalSha256")))throw new AssertionError("Original changed "+asset);
            BufferedImage gameplay=source(game,"gameplay"),marquee=source(game,"marquee"),panel=source(game,"panel");
            Map<String,Object> row=new LinkedHashMap<>();row.put("asset",asset);row.put("front","front/"+asset);
            row.put("lit",gameplay!=null&&game.get("quad")!=null?"lit/"+asset:null);
            Map<String,Object> replacement=importedFronts.get(asset);
            if(replacement==null) {
                BufferedImage frontal=front(game,gameplay,marquee,panel);
                if(!verify)ImageIO.write(frontal,"png",out.resolve(str(row.get("front"))).toFile());
                assertPixels(frontal,ImageIO.read(out.resolve(str(row.get("front"))).toFile()),asset+" front");
            } else {
                Path file=out.resolve(str(row.get("front")));
                if(!PrepareAllArcadeCabinets.sha(file).equals(replacement.get("sha256")))throw new AssertionError("Imported front changed "+asset);
                BufferedImage image=ImageIO.read(file.toFile());
                if(image==null||image.getWidth()!=((Number)replacement.get("width")).intValue()||
                    image.getHeight()!=((Number)replacement.get("height")).intValue())throw new AssertionError("Imported front dimensions "+asset);
            }
            if(row.get("lit")!=null) {
                BufferedImage lit=lit(ImageIO.read(original.toFile()),game,gameplay);
                Path file=out.resolve(str(row.get("lit")));
                if(!verify)ImageIO.write(lit,"png",file.toFile());
                assertPixels(lit,ImageIO.read(file.toFile()),asset+" lit");
                row.put("litSha256",PrepareAllArcadeCabinets.sha(file));
            }
            row.put("frontSha256",PrepareAllArcadeCabinets.sha(out.resolve(str(row.get("front")))));
            row.put("gameplayRom",game.get("gameplay")==null?null:obj(game.get("gameplay")).get("rom"));
            row.put("representation",replacement==null?"illustrative-front-not-factory-original":"user-supplied-front-unverified-provenance");
            if(replacement!=null) {
                row.put("frontWidth",replacement.get("width"));row.put("frontHeight",replacement.get("height"));
                row.put("frontSource",imported.get("source"));
            }
            index.add(row);
        }
        Map<String,Object> manifest=Map.of("version",1,"mappings",index);
        if(!verify) { PrepareAllArcadeCabinets.Json.write(out.resolve("index.json"),manifest);sheets(games); }
        else {
            Map<String,Object> saved=obj(PrepareAllArcadeCabinets.Json.read(out.resolve("index.json")));
            if(((Number)saved.get("version")).intValue()!=1||!saved.get("mappings").equals(index))throw new AssertionError("Index changed");
        }
        System.out.println("PASS: "+index.size()+" fronts verified ("+importedFronts.size()+" byte-exact imports); "+index.stream().filter(x->x.get("lit")!=null).count()+" gameplay-lit copies; original hashes unchanged.");
    }
    static void assertPixels(BufferedImage expected,BufferedImage actual,String name) {
        if(actual==null||actual.getWidth()!=expected.getWidth()||actual.getHeight()!=expected.getHeight())throw new AssertionError("Dimensions "+name);
        for(int y=0;y<actual.getHeight();y++)for(int x=0;x<actual.getWidth();x++)if(actual.getRGB(x,y)!=expected.getRGB(x,y))throw new AssertionError("Pixel "+name);
        if((actual.getRGB(0,0)>>>24)!=0)throw new AssertionError("Opaque border "+name);
    }
}