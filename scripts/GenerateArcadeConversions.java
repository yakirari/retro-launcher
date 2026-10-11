import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import javax.imageio.ImageIO;

/** Photo-based, deliberately imagined operator conversions; not historical cabinet reconstructions. */
public class GenerateArcadeConversions {
    static final int W=960,H=768;
    static Path root,review,out;
    static Map<String,BufferedImage> shells=new HashMap<>();
    static Map<String,Object> obj(Object o) { return PrepareAllArcadeCabinets.obj(o); }
    static String text(Object o) { return PrepareAllArcadeCabinets.str(o); }
    static List<Map<String,Object>> rows(Object o) { return PrepareAllArcadeCabinets.rows(o); }
    static void configure(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING,RenderingHints.VALUE_RENDER_QUALITY);
    }
    static Path2D poly(double... points) {
        Path2D p=new Path2D.Double();p.moveTo(points[0],points[1]);
        for(int i=2;i<points.length;i+=2)p.lineTo(points[i],points[i+1]);p.closePath();return p;
    }
    static void paint(Graphics2D g,Paint color,double... points) { g.setPaint(color);g.fill(poly(points)); }
    static GradientPaint gradient(int x,int y,Color a,int xx,int yy,Color b) {return new GradientPaint(x,y,a,xx,yy,b);}
    static Color rgb(int n) {return new Color(n);}
    static double[] marquee(String shell) {
        return switch(shell) {
            case "frogger" -> new double[]{474,68,635,91,632,154,474,139};
            case "columns" -> new double[]{469,69,655,80,648,163,469,169};
            // Replacement operator insert in the existing sloped roof fascia,
            // never in the bezel or CRT below it.
            case "sonic" -> new double[]{308,61,550,67,577,117,291,117};
            default -> throw new IllegalArgumentException(shell);
        };
    }
    /** Replace only game-bearing donor regions, preserving cabinet walls, coin door and complete base. */
    static void clean(BufferedImage image,String shell,String controls) {
        Graphics2D g=image.createGraphics();configure(g);
        switch(shell) {
            case "frogger" -> {
                // This photo is a 1981 dedicated woodgrain upright. The upper left
                // side decal is repainted; the original lower wood/door/trim stays.
                paint(g,gradient(320,150,rgb(0x22201d),465,400,rgb(0x100f0e)),
                    320,175,460,61,460,152,421,217,412,247,416,286,433,367,467,403,467,422,323,397);
                darkGlass(g,new double[]{425,290,508,295,546,362,436,370});
                paint(g,gradient(440,358,rgb(0x4b4c4b),485,405,rgb(0x242628)),
                    435,368,546,353,632,381,475,403);
                paint(g,gradient(479,402,rgb(0x676969),479,426,rgb(0x34383a)),
                    476,401,632,381,632,407,476,427);
                if(controls.equals("handlebar"))handlebar(g,544,380);
                else joystick(g,544,379,0.8);
                button(g,500,411,0.6,rgb(0xd0bd8d));button(g,518,409,0.6,rgb(0xd0bd8d));
            }
            case "columns" -> {
                // Plain sides already lack franchise art. Remove the donor game's
                // image from the CRT and badge, and replace branded deck artwork.
                darkGlass(g,new double[]{418,264,520,250,579,385,445,433});
                paint(g,rgb(0x473727),456,240,480,236,483,252,459,256);
                paint(g,gradient(485,429,rgb(0x4b4f54),514,481,rgb(0x25282b)),
                    466,439,594,393,632,413,633,443,514,486,476,478);
                if(controls.equals("handlebar")) handlebar(g,553,431);
                else {
                    joystick(g,511,438,0.95);joystick(g,589,411,0.8);
                    int count=controls.equals("six-button")?6:3;
                    for(int i=0;i<count;i++) {
                        button(g,529+(i%3)*9,433-(i%3)*3+(i/3)*9,0.55,rgb(0xd1b575));
                        button(g,603+(i%3)*8,414-(i%3)*3+(i/3)*8,0.5,rgb(0xd1b575));
                    }
                }
            }
            case "sonic" -> {
                // Replace roof fascia with an operator sign insert; remove Sonic
                // CRT art and stickers, retaining white body, door and full base.
                paint(g,gradient(370,165,rgb(0x191b21),590,430,rgb(0x08090d)),
                    315,163,572,162,619,422,339,446);
                darkGlass(g,new double[]{333,218,568,212,588,379,348,399});
                paint(g,gradient(380,476,rgb(0x677278),400,522,rgb(0x293840)),
                    373,487,624,450,644,495,386,524);
                paint(g,rgb(0x102947),368,473,613,444,623,452,373,487);
                if(controls.equals("spinner")) {spinner(g,452,499);button(g,566,483,0.9,rgb(0xd9ad3e));}
                else {
                    joystick(g,418,499,1.15);joystick(g,549,480,1.15);
                    int count=controls.equals("six-button")?6:3;
                    for(int player=0;player<2;player++)for(int i=0;i<count;i++)
                        button(g,448+player*131+(i%3)*13,494-player*19+(i/3)*12,0.8,
                            player==0?rgb(0x6bb9a4):rgb(0x77a8d3));
                }
            }
            default -> throw new IllegalArgumentException(shell);
        }
        g.dispose();
    }
    /** Dark convex CRT glass, never a title or cover. A modest reflection is drawn, not gameplay. */
    static void darkGlass(Graphics2D g,double[] quad) {
        Shape before=g.getClip();g.clip(poly(quad));
        Rectangle bounds=poly(quad).getBounds();
        g.setPaint(gradient(bounds.x,bounds.y,rgb(0x222a30),bounds.x+bounds.width,bounds.y+bounds.height,rgb(0x070b0f)));
        g.fill(bounds);
        g.setColor(new Color(170,184,196,18));g.fillOval(bounds.x-bounds.width/2,bounds.y-bounds.height/4,bounds.width,bounds.height/2);
        g.setColor(new Color(218,228,231,16));g.setStroke(new BasicStroke(2));
        g.drawLine(bounds.x+5,bounds.y+6,bounds.x+bounds.width/2,bounds.y+9);
        g.setClip(before);
    }
    static void button(Graphics2D g,int x,int y,double scale,Color color) {
        int w=(int)Math.round(12*scale),h=Math.max(3,(int)Math.round(7*scale));
        g.setColor(rgb(0x101214));g.fillOval(x-w/2-1,y-h/2,w+2,h+3);
        g.setPaint(gradient(x,y-h/2,color.brighter(),x,y+h,color.darker()));g.fillOval(x-w/2,y-h/2-1,w,h);
        g.setColor(new Color(255,255,255,125));g.drawLine(x-w/4,y-h/2,x+w/5,y-h/2);
    }
    static void joystick(Graphics2D g,int x,int y,double scale) {
        int radius=(int)(6*scale),height=(int)(16*scale);
        g.setColor(new Color(0,0,0,110));g.fillOval(x-8,y-2,21,7);
        g.setColor(rgb(0x151719));g.fillOval(x-7,y-3,14,7);
        g.setStroke(new BasicStroke((float)(3*scale)));g.setColor(rgb(0xb2b7b8));g.drawLine(x,y,x,y-height);
        g.setPaint(gradient(x-radius,y-height-radius,rgb(0xd74438),x+radius,y-height+radius,rgb(0x651514)));
        g.fillOval(x-radius,y-height-radius,radius*2,radius*2);
        g.setColor(new Color(255,230,214,150));g.fillOval(x-radius/2,y-height-radius/2,Math.max(2,radius/2),Math.max(2,radius/2));
    }
    static void spinner(Graphics2D g,int x,int y) {
        g.setColor(rgb(0x131619));g.fillOval(x-16,y-7,32,16);
        g.setPaint(gradient(x-14,y-8,rgb(0xbcc1c3),x+13,y+7,rgb(0x434c51)));g.fillOval(x-14,y-9,28,15);
        g.setColor(rgb(0x222a2e));g.drawOval(x-11,y-7,22,11);
    }
    static void handlebar(Graphics2D g,int x,int y) {
        g.setColor(rgb(0x131619));g.fillOval(x-12,y-2,24,10);
        g.setStroke(new BasicStroke(6,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
        g.setColor(rgb(0x969c9e));g.drawPolyline(new int[]{x-52,x-34,x-18,x+19,x+39,x+53},new int[]{y-8,y-20,y-7,y-15,y-33,y-28},6);
        g.setStroke(new BasicStroke(9,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));g.setColor(rgb(0x25272a));
        g.drawLine(x-57,y-5,x-43,y-12);g.drawLine(x+46,y-31,x+58,y-27);
    }
    /** Aspect-preserving marquee letterbox. No cropping or stretching of source logos. */
    static BufferedImage sign(BufferedImage source,String title) {
        BufferedImage sign=new BufferedImage(1000,300,BufferedImage.TYPE_INT_ARGB);
        Graphics2D g=sign.createGraphics();configure(g);
        g.setPaint(gradient(0,0,rgb(0x111a25),0,300,rgb(0x02050a)));g.fillRect(0,0,1000,300);
        if(source!=null) {
            double scale=Math.min(970.0/source.getWidth(),270.0/source.getHeight());
            int w=(int)Math.round(source.getWidth()*scale),h=(int)Math.round(source.getHeight()*scale);
            g.drawImage(source,(1000-w)/2,(300-h)/2,w,h,null);
        } else {
            List<String> lines=new ArrayList<>();StringBuilder current=new StringBuilder();
            for(String word:title.split("\\s+")) {
                if(current.length()+word.length()>32&&!current.isEmpty()) {lines.add(current.toString());current.setLength(0);}
                if(!current.isEmpty())current.append(' ');current.append(word);
            }
            if(!current.isEmpty())lines.add(current.toString());
            int size=Math.min(50,170/Math.max(1,lines.size()));
            Font font=new Font(Font.SANS_SERIF,Font.BOLD,size);
            g.setFont(font);FontMetrics fm=g.getFontMetrics();
            int width=lines.stream().mapToInt(fm::stringWidth).max().orElse(1);
            if(width>900) {font=font.deriveFont((float)(size*900.0/width));g.setFont(font);fm=g.getFontMetrics();}
            int y=150-(lines.size()-1)*fm.getHeight()/2+fm.getAscent()/3;
            g.setColor(rgb(0xe1d6b3));
            for(String line:lines) {g.drawString(line,(1000-fm.stringWidth(line))/2,y);y+=fm.getHeight();}
            g.setColor(rgb(0x716750));g.fillRect(180,264,640,2);
        }
        g.dispose();return sign;
    }
    /** Inverse projective sampling, four measured corners TL/TR/BR/BL. */
    static void warp(BufferedImage dest,BufferedImage src,double[] q) {
        double x0=q[0],y0=q[1],x1=q[2],y1=q[3],x2=q[4],y2=q[5],x3=q[6],y3=q[7];
        double dx1=x1-x2,dx2=x3-x2,dx3=x0-x1+x2-x3;
        double dy1=y1-y2,dy2=y3-y2,dy3=y0-y1+y2-y3;
        double denominator=dx1*dy2-dx2*dy1;
        double a31=(dx3*dy2-dx2*dy3)/denominator,a32=(dx1*dy3-dx3*dy1)/denominator;
        double a11=x1-x0+a31*x1,a12=x3-x0+a32*x3;
        double a21=y1-y0+a31*y1,a22=y3-y0+a32*y3;
        Rectangle bounds=poly(q).getBounds();
        for(int y=bounds.y;y<=bounds.y+bounds.height;y++)for(int x=bounds.x;x<=bounds.x+bounds.width;x++) {
            double xx=x+.5,yy=y+.5;
            double aa=a11-xx*a31,bb=a12-xx*a32,cc=xx-x0;
            double dd=a21-yy*a31,ee=a22-yy*a32,ff=yy-y0;
            double determinant=aa*ee-bb*dd;
            double u=(cc*ee-bb*ff)/determinant,v=(aa*ff-cc*dd)/determinant;
            if(u<0||u>1||v<0||v>1)continue;
            int color=sample(src,u*(src.getWidth()-1),v*(src.getHeight()-1));
            // Preserve donor alpha exactly; drawing cannot turn background opaque.
            int originalAlpha=dest.getRGB(x,y)>>>24;
            dest.setRGB(x,y,(color&0xffffff)|(originalAlpha<<24));
        }
    }
    static int sample(BufferedImage image,double x,double y) {
        int x0=(int)x,y0=(int)y,x1=Math.min(x0+1,image.getWidth()-1),y1=Math.min(y0+1,image.getHeight()-1);
        double fx=x-x0,fy=y-y0;int result=0xff000000;
        int[] c={image.getRGB(x0,y0),image.getRGB(x1,y0),image.getRGB(x0,y1),image.getRGB(x1,y1)};
        for(int shift:new int[]{16,8,0}) {
            double top=((c[0]>>>shift)&255)*(1-fx)+((c[1]>>>shift)&255)*fx;
            double bottom=((c[2]>>>shift)&255)*(1-fx)+((c[3]>>>shift)&255)*fx;
            result|=((int)Math.round(top*(1-fy)+bottom*fy))<<shift;
        }
        return result;
    }
    static BufferedImage render(Map<String,Object> p) throws Exception {
        String shell=text(p.get("shell"));BufferedImage original=shells.get(shell);
        BufferedImage image=new BufferedImage(W,H,BufferedImage.TYPE_INT_ARGB);
        Graphics2D g=image.createGraphics();g.drawImage(original,0,0,null);g.dispose();
        clean(image,shell,text(p.get("controls")));
        BufferedImage art=null;
        if(p.get("marquee")!=null) {
            art=ImageIO.read(review.resolve(text(obj(p.get("marquee")).get("file"))).toFile());
            if(art==null)throw new IllegalArgumentException("Undecodable marquee "+p.get("rom"));
        }
        warp(image,sign(art,text(p.get("title"))),marquee(shell));
        // Exact original silhouette including partial edge alpha, no new rectangles.
        for(int y=0;y<H;y++)for(int x=0;x<W;x++) {
            int a=original.getRGB(x,y)>>>24;
            image.setRGB(x,y,a==0?0:(image.getRGB(x,y)&0xffffff)|(a<<24));
        }
        PrepareAllArcadeCabinets.verify(image,text(p.get("rom")));
        assertSameAlpha(original,image);
        return image;
    }
    static void assertSameAlpha(BufferedImage a,BufferedImage b) {
        for(int y=0;y<a.getHeight();y++)for(int x=0;x<a.getWidth();x++)
            if((a.getRGB(x,y)>>>24)!=(b.getRGB(x,y)>>>24))throw new AssertionError("Shell alpha changed");
    }
    public static void main(String[] args) throws Exception {
        if(args.length>0&&args[0].equals("--self-test")) {selfTest();return;}
        root=Path.of(args[0]);review=root.resolve("artwork/cabinet-references/full-cabinet-review/conversions");
        out=root.resolve("artwork/full-cabinets");
        Map<String,Object> plan=obj(PrepareAllArcadeCabinets.Json.read(review.resolve("plan.json")));
        for(Map<String,Object> s:rows(plan.get("shells")))shells.put(text(s.get("rom")),ImageIO.read(out.resolve(text(s.get("asset"))).toFile()));
        if(args.length>1&&args[1].equals("--verify")) {verifyArtifacts(plan);return;}
        List<Map<String,Object>> evidence=new ArrayList<>();
        List<Map<String,Object>> games=rows(plan.get("games"));
        for(Map<String,Object> p:games) {
            String asset=text(p.get("asset"));
            if(!asset.matches("[a-z0-9_]+__conversion\\.png"))throw new IllegalArgumentException("Unsafe output name");
            BufferedImage image=render(p);Path file=out.resolve(asset);ImageIO.write(image,"png",file.toFile());
            Map<String,Object> e=new LinkedHashMap<>(p);e.put("outputSha256",PrepareAllArcadeCabinets.sha(file));
            e.put("alphaAudit",PrepareAllArcadeCabinets.alphaAudit(image));
            e.put("shellAlphaUnchanged",true);e.put("screenPolicy",plan.get("screenPolicy"));evidence.add(e);
        }
        Map<String,Object> provenance=new LinkedHashMap<>();provenance.put("representation",plan.get("representation"));
        provenance.put("disclaimer","Imagined operator conversions assembled from real shell photos plus sign/decal/control edits. NOT photographs of these titles' authentic cabinets. No original photo files were altered.");
        provenance.put("shells",plan.get("shells"));provenance.put("games",evidence);
        PrepareAllArcadeCabinets.Json.write(review.resolve("provenance.json"),provenance);
        sheets(games);marqueeSheets(games);
        verifyArtifacts(plan);
        System.out.println("PASS: rendered "+games.size()+" 960x768 RGBA complete-shell alternatives; pixel-exact donor alpha retained.");
    }
    /** Decode saved PNGs independently of render buffers; audit originals as well. */
    static void verifyArtifacts(Map<String,Object> plan) throws Exception {
        Set<String> photoAssets=new HashSet<>();
        Map<String,Object> photos=obj(PrepareAllArcadeCabinets.Json.read(review.getParent().resolve("provenance.json")));
        for(Map<String,Object> p:rows(photos.get("sources")))if(p.get("asset")!=null) {
            String asset=text(p.get("asset"));Path file=out.resolve(asset);
            BufferedImage saved=ImageIO.read(file.toFile());
            PrepareAllArcadeCabinets.verify(saved,asset);
            if(!PrepareAllArcadeCabinets.sha(file).equals(p.get("outputSha256")))throw new AssertionError("Changed original photo "+asset);
            photoAssets.add(asset);
        }
        for(Map<String,Object> p:rows(plan.get("games"))) {
            String asset=text(p.get("asset")),shell=text(p.get("shell"));
            BufferedImage saved=ImageIO.read(out.resolve(asset).toFile());
            PrepareAllArcadeCabinets.verify(saved,asset);assertSameAlpha(shells.get(shell),saved);
            int color=saved.getRGB(shell.equals("sonic")?450:480,shell.equals("sonic")?300:330);
            if((color>>>24)!=255||Math.max((color>>>16)&255,Math.max((color>>>8)&255,color&255))>55)
                throw new AssertionError("CRT center not dark opaque glass "+asset);
        }
        System.out.println("PASS: independently decoded "+photoAssets.size()+" original + "+rows(plan.get("games")).size()
            +" conversion PNGs; transparent borders, full silhouettes, unchanged original hashes, exact donor alpha and dark CRT centers.");
    }
    static void marqueeSheets(List<Map<String,Object>> games) throws Exception {
        List<Map<String,Object>> sources=games.stream().filter(p->p.get("marquee")!=null).toList();
        int cols=6,per=30,cw=240,ch=125;
        for(int start=0;start<sources.size();start+=per) {
            int count=Math.min(per,sources.size()-start);
            BufferedImage sheet=new BufferedImage(cols*cw,((count+cols-1)/cols)*ch,BufferedImage.TYPE_INT_RGB);
            Graphics2D g=sheet.createGraphics();configure(g);g.setColor(rgb(0x283039));g.fillRect(0,0,sheet.getWidth(),sheet.getHeight());
            g.setFont(new Font(Font.MONOSPACED,Font.PLAIN,12));
            for(int i=0;i<count;i++) {
                Map<String,Object> p=sources.get(start+i),m=obj(p.get("marquee"));
                BufferedImage image=ImageIO.read(review.resolve(text(m.get("file"))).toFile());
                int x=i%cols*cw,y=i/cols*ch;
                double scale=Math.min(224.0/image.getWidth(),96.0/image.getHeight());
                int w=(int)(image.getWidth()*scale),h=(int)(image.getHeight()*scale);
                g.drawImage(image,x+(cw-w)/2,y+(100-h)/2,w,h,null);
                g.setColor(Color.WHITE);g.drawString(text(p.get("rom"))+" <- "+text(m.get("rom")),x+5,y+118);
            }
            g.dispose();ImageIO.write(sheet,"png",review.resolve("marquee-review-"+(start/per+1)+".png").toFile());
        }
    }
    static void sheets(List<Map<String,Object>> games) throws Exception {
        int cols=6,per=30,cw=240,ch=245;
        for(int start=0;start<games.size();start+=per) {
            int count=Math.min(per,games.size()-start),height=((count+cols-1)/cols)*ch;
            BufferedImage sheet=new BufferedImage(cols*cw,height,BufferedImage.TYPE_INT_RGB);
            Graphics2D g=sheet.createGraphics();configure(g);g.setFont(new Font(Font.MONOSPACED,Font.PLAIN,12));
            for(int i=0;i<count;i++) {
                int x=i%cols*cw,y=i/cols*ch;Map<String,Object> p=games.get(start+i);
                g.setColor(rgb(0x7fa2b6));g.fillRect(x,y,cw/2,ch);
                g.setColor(rgb(0xe0cfab));g.fillRect(x+cw/2,y,cw/2,ch);
                g.drawImage(ImageIO.read(out.resolve(text(p.get("asset"))).toFile()),x,y,240,192,null);
                g.setColor(Color.BLACK);g.drawString(text(p.get("rom")),x+6,y+208);
                g.drawString(text(p.get("shell"))+" / "+text(p.get("year")),x+6,y+224);
                g.drawString(p.get("marquee")==null?"TYPESET TOP SIGN":"SOURCE TOP MARQUEE",x+6,y+239);
            }
            g.dispose();ImageIO.write(sheet,"png",review.resolve("review-"+(start/per+1)+".png").toFile());
        }
    }
    static void selfTest() {
        BufferedImage img=new BufferedImage(120,100,BufferedImage.TYPE_INT_ARGB);
        Graphics2D g=img.createGraphics();g.setColor(Color.BLACK);g.fillRect(5,5,110,90);g.dispose();
        BufferedImage red=new BufferedImage(20,20,BufferedImage.TYPE_INT_ARGB);
        g=red.createGraphics();g.setColor(Color.RED);g.fillRect(0,0,20,20);g.dispose();
        warp(img,red,new double[]{20,15,100,25,85,80,30,70});
        if(img.getRGB(60,45)!=0xffff0000||img.getRGB(10,10)!=0xff000000||img.getRGB(0,0)!=0)throw new AssertionError("Perspective placement/background");
        BufferedImage fallback=sign(null,"A Long Game Title: Operator Conversion");
        if(!fallback.getColorModel().hasAlpha())throw new AssertionError("Sign alpha");
        BufferedImage large=new BufferedImage(W,H,BufferedImage.TYPE_INT_ARGB);
        g=large.createGraphics();g.setColor(Color.BLACK);g.fillRect(250,50,400,700);g.dispose();
        BufferedImage copy=new BufferedImage(W,H,BufferedImage.TYPE_INT_ARGB);g=copy.createGraphics();g.drawImage(large,0,0,null);g.dispose();
        darkGlass(g=copy.createGraphics(),new double[]{330,220,570,210,588,380,348,399});g.dispose();assertSameAlpha(large,copy);
        // A source sign cannot bleed into a CRT: pixels outside measured sign quad
        // must be byte-identical, and source alpha remains unchanged everywhere.
        int screen=copy.getRGB(450,300);warp(copy,red,marquee("columns"));
        if(screen!=copy.getRGB(450,300))throw new AssertionError("Marquee leaked into CRT");assertSameAlpha(large,copy);
        System.out.println("PASS: projective marquee placement, outside-sign preservation, dark CRT, title isolation and alpha fixtures.");
    }
}