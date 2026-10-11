import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.List;
import javax.imageio.ImageIO;

/**
 * Source-specific full-photo cabinet preparation, JDK 17; never changes raw references.
 * Compile with the existing NormalizeArcadeCabinets.java into a temporary class directory.
 * CLI: PrepareAllArcadeCabinets [workspace-root] [--raw-review]; --self-test runs fixtures.
 * Policies are explicit reviewed source-pixel masks in full-cabinet-review/recipes.json.
 * Runtime contract: cabinet-art.json {mappings:[{rom,asset,kind,source,notes}]}; asset is
 * a PNG basename relative to artwork/full-cabinets. Null means no acceptable located photo.
 */
public class PrepareAllArcadeCabinets {
    static Path root, refs, review, output;
    static List<Map<String,Object>> inventory;
    static Map<String,Map<String,Object>> sources = new TreeMap<>();
    static Map<String,Map<String,Object>> policies = new TreeMap<>();
    static Map<String,Map<String,Object>> results = new TreeMap<>();
    static String str(Object o) { return o == null ? "" : o.toString(); }
    @SuppressWarnings("unchecked") static Map<String,Object> obj(Object o) { return (Map<String,Object>)o; }
    @SuppressWarnings("unchecked") static List<Map<String,Object>> rows(Object o) { return (List<Map<String,Object>>)o; }

    public static void main(String[] args) throws Exception {
        if (args.length > 0 && args[0].equals("--self-test")) { selfTest(); return; }
        root = Path.of(args.length > 0 ? args[0] : ".").toAbsolutePath().normalize();
        refs = root.resolve("artwork/cabinet-references");
        review = refs.resolve("full-cabinet-review");
        output = root.resolve("artwork/full-cabinets");
        Files.createDirectories(review); Files.createDirectories(output);
        inventory = rows(obj(Json.read(refs.resolve("cabinet-manifest.json"))).get("mappings"));
        for (Map<String,Object> row : inventory) {
            String selected = str(row.get("selected"));
            if (selected.isEmpty()) continue;
            Map<String,Object> chosen = null;
            for (Map<String,Object> tried : rows(row.get("tried")))
                if (selected.equals(str(tried.get("rom"))) && tried.get("file") != null) chosen = tried;
            if (chosen == null) throw new IllegalStateException("Missing selected photo " + selected);
            sources.put(selected, chosen);
        }
        if (args.length > 1 && args[1].equals("--raw-review")) { sheets("raw", false); return; }
        Map<String,Object> config = obj(Json.read(review.resolve("recipes.json")));
        if(config.get("additionalSources") instanceof List<?> extras)for(Object extra:extras) {
            Map<String,Object> src=obj(extra);sources.put(str(src.get("rom")),src);
        }
        for (Map<String,Object> group : rows(config.get("groups"))) {
            for (String name : str(group.get("roms")).split("\\s+")) {
                if (policies.put(name, group) != null) throw new IllegalArgumentException("Duplicate policy " + name);
            }
        }
        Map<String,Object> overrides=obj(config.get("overrides"));
        for (String name : overrides.keySet()) {
            Map<String,Object> policy=new LinkedHashMap<>(policies.getOrDefault(name,Map.of()));
            policy.putAll(obj(overrides.get(name))); policies.put(name,policy);
        }
        for (String name : sources.keySet()) {
            Map<String,Object> policy=policies.get(name);
            if (policy==null) throw new IllegalStateException("Photo has no explicit review policy: "+name);
            Map<String,Object> result=new LinkedHashMap<>(); results.put(name,result);
            result.put("sourceRom",name);result.put("sourceFile",sources.get(name).get("file"));
            result.put("url",sources.get(name).get("url"));
            result.put("sourceSha256",sha(refs.resolve(str(sources.get(name).get("file")))));
            result.put("policy",policy);
            if (str(policy.get("mode")).equals("reject")) {
                result.put("asset",null);result.put("status","rejected");
                Files.deleteIfExists(output.resolve(name+".png"));continue;
            }
            BufferedImage image=ImageIO.read(refs.resolve(str(sources.get(name).get("file"))).toFile());
            if(image==null) throw new IllegalStateException("Cannot decode "+name);
            BufferedImage cutout=segment(image,policy);
            BufferedImage fitted=NormalizeArcadeCabinets.normalize(cutout,false,2,false,true);
            verify(fitted,name);
            Path file=output.resolve(name+".png"); ImageIO.write(fitted,"png",file.toFile());
            result.put("asset",name+".png");result.put("status","prepared-contrast-reviewed");
            result.put("outputSha256",sha(file));
            result.put("sourceWidth",image.getWidth());result.put("sourceHeight",image.getHeight());
            result.put("alphaAudit",alphaAudit(fitted));
        }
        writeMappings(); sheets("cutouts",true);
        Json.write(review.resolve("provenance.json"),Map.of("note","Actual source photos; operator conversions/restorations may be shown. Exact-set-reference describes the source lookup, not factory originality or exact controls.","sources",new ArrayList<>(results.values())));
        System.out.println("Reviewed " + sources.size() + " distinct photos (including explicit additional parent references); manifest located " + inventory.stream().filter(r -> r.get("selected") != null).count() + " ROMs");
    }

    static int number(Map<String,Object> p,String key,int fallback) { return p.get(key) instanceof Number n?n.intValue():fallback; }
    static String sha(Path file) throws Exception { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(file))); }

    /** Row interiors are enabled only by explicit reviewed upright policy, never for open cabinets. */
    static BufferedImage segment(BufferedImage source,Map<String,Object> policy) {
        int w=source.getWidth(),h=source.getHeight(),n=w*h;
        int[] pixels=source.getRGB(0,0,w,h,null,0,w); boolean[] keep=new boolean[n];
        String mode=str(policy.get("mode"));
        boolean precut=source.getColorModel().hasAlpha() && (pixels[0]>>>24)==0 && (pixels[n-1]>>>24)==0;
        if (precut && !mode.equals("polygon")) {
            for(int i=0;i<n;i++)keep[i]=(pixels[i]>>>24)>8;
        } else if (mode.equals("polygon")) {
            polygon(keep,w,h,policy.get("polygon"),true);
        } else {
            int threshold=number(policy,"threshold",2),margin=number(policy,"margin",1);
            boolean white=str(policy.get("background")).equals("white");
            boolean[] bg=new boolean[n];int[] queue=new int[n];int tail=0;
            for(int y=0;y<h;y++)for(int x=0;x<w;x++) {
                int i=y*w+x,c=pixels[i];int r=c>>>16&255,g=c>>>8&255,b=c&255;
                boolean color=(c>>>24)<=8 || (white?Math.min(r,Math.min(g,b))>=255-threshold:Math.max(r,Math.max(g,b))<=threshold);
                if(x<margin||y<margin||x>=w-margin||y>=h-margin) {bg[i]=true;queue[tail++]=i;}
                else keep[i]=!color;
            }
            // Flood only exterior backdrop. Interior screens, panels, and dark details stay original.
            for(int head=0;head<tail;head++) {
                int i=queue[head],x=i%w;
                for(int j:new int[]{x>0?i-1:-1,x<w-1?i+1:-1,i>=w?i-w:-1,i+w<n?i+w:-1})
                    if(j>=0&&!bg[j]&&!keep[j]) {bg[j]=true;queue[tail++]=j;}
            }
            for(int i=0;i<n;i++)keep[i]=!bg[i];
            // Discard detached scan specks/frames, retaining small meaningful parts on open frames.
            boolean[] seen=new boolean[n];int minimum=Math.max(4,n/number(policy,"islandDivisor",1000));
            for(int start=0;start<n;start++)if(keep[start]&&!seen[start]) {
                int count=1;queue[0]=start;seen[start]=true;
                for(int head=0;head<count;head++) {
                    int i=queue[head],x=i%w;
                    for(int j:new int[]{x>0?i-1:-1,x<w-1?i+1:-1,i>=w?i-w:-1,i+w<n?i+w:-1})
                        if(j>=0&&keep[j]&&!seen[j]){seen[j]=true;queue[count++]=j;}
                }
                if(count<minimum)for(int j=0;j<count;j++)keep[queue[j]]=false;
            }
            if(mode.equals("rows"))for(int y=0;y<h;y++) {
                int left=w,right=-1;for(int x=0;x<w;x++)if(keep[y*w+x]){left=Math.min(left,x);right=x;}
                for(int x=left;x<=right;x++)keep[y*w+x]=true;
            }
        }
        if(policy.get("restore") instanceof List<?> polys)for(Object poly:polys)polygon(keep,w,h,poly,true);
        if(policy.get("holes") instanceof List<?> polys)for(Object poly:polys)polygon(keep,w,h,poly,false);
        for(int i=0;i<n;i++)pixels[i]=keep[i]?(precut&&!mode.equals("polygon")?pixels[i]:pixels[i]|0xff000000):0;
        BufferedImage result=new BufferedImage(w,h,BufferedImage.TYPE_INT_ARGB);result.setRGB(0,0,w,h,pixels,0,w);return result;
    }

    /** Coordinates are normalized source-photo pixels, not a generic cabinet template. */
    static void polygon(boolean[] mask,int w,int h,Object coordinates,boolean value) {
        Polygon p=new Polygon();
        for(Object pair:(List<?>)coordinates) {List<?> xy=(List<?>)pair;p.addPoint((int)Math.round(((Number)xy.get(0)).doubleValue()*w),(int)Math.round(((Number)xy.get(1)).doubleValue()*h));}
        Rectangle bounds=p.getBounds();
        for(int y=Math.max(0,bounds.y);y<Math.min(h,bounds.y+bounds.height);y++)for(int x=Math.max(0,bounds.x);x<Math.min(w,bounds.x+bounds.width);x++)if(p.contains(x+.5,y+.5))mask[y*w+x]=value;
    }

    static void verify(BufferedImage image,String name) {
        if(image.getWidth()!=960||image.getHeight()!=768||!image.getColorModel().hasAlpha())throw new IllegalStateException("Invalid canvas "+name);
        int count=0,left=960,right=-1,top=768,bottom=-1;
        for(int y=0;y<768;y++)for(int x=0;x<960;x++)if((image.getRGB(x,y)>>>24)>8) {
            if(x==0||y==0||x==959||y==767)throw new IllegalStateException("Opaque border "+name);
            count++;left=Math.min(left,x);right=Math.max(right,x);top=Math.min(top,y);bottom=Math.max(bottom,y);
        }
        if(count<15000||count>535000)throw new IllegalStateException("Blank/oversized silhouette "+name+" "+count);
        // A rectangular photograph background should not survive as the complete object bounds.
        double coverage=count/(double)((right-left+1)*(bottom-top+1));
        if(coverage>.995)throw new IllegalStateException("Rectangular opaque silhouette requires review "+name);
    }

    static Map<String,Object> alphaAudit(BufferedImage image) {
        int w=image.getWidth(),h=image.getHeight(),n=w*h,opaque=0,partial=0,dark=0;
        int[] pixels=image.getRGB(0,0,w,h,null,0,w),queue=new int[n];boolean[] seen=new boolean[n];
        List<Integer> islands=new ArrayList<>();
        for(int i=0;i<n;i++) {
            int c=pixels[i],a=c>>>24;if(a==255)opaque++;else if(a>0)partial++;
            if(a>240&&Math.max(c>>>16&255,Math.max(c>>>8&255,c&255))<=24)dark++;
            if(a<=8||seen[i])continue;
            int count=1;queue[0]=i;seen[i]=true;
            for(int head=0;head<count;head++) {
                int index=queue[head],x=index%w;
                for(int j:new int[]{x>0?index-1:-1,x<w-1?index+1:-1,index>=w?index-w:-1,index+w<n?index+w:-1})
                    if(j>=0&&!seen[j]&&(pixels[j]>>>24)>8){seen[j]=true;queue[count++]=j;}
            }
            islands.add(count);
        }
        islands.sort(Comparator.reverseOrder());
        return Map.of("width",w,"height",h,"opaquePixels",opaque,"antialiasedPixels",partial,
            "retainedDarkPixels",dark,"componentsOver8Alpha",islands,"transparentBorder",true);
    }

    static Map<String,Object> mapping(String rom,String selected,String relation,String extra) {
        Map<String,Object> source=results.get(selected);Map<String,Object> row=new LinkedHashMap<>();
        row.put("rom",rom);row.put("asset",source==null?null:source.get("asset"));
        row.put("kind",source==null?"missing":source.get("asset")==null?"rejected":relation);
        row.put("source",source==null?null:source.get("url"));
        String note=source==null?"No located real cabinet photo in source manifest.":
            "Photo source ROM "+selected+"; "+str(obj(source.get("policy")).get("notes"))+" Source photo may show operator conversion/restoration; not a factory-original or exact-control-layout claim.";
        row.put("notes",note+(extra.isEmpty()?"":" "+extra));return row;
    }

    static void writeMappings() throws Exception {
        Map<String,Map<String,Object>> mappings=new TreeMap<>();List<Map<String,Object>> gaps=new ArrayList<>();
        Map<String,Object> config=obj(Json.read(review.resolve("recipes.json")));
        Map<String,Object> replacements=obj(config.getOrDefault("mappingOverrides",Map.of()));
        for(Map<String,Object> row:inventory) {
            String rom=str(row.get("rom")),selected=str(row.get("selected"));
            String relation=str(row.get("relation")),extra="";
            if(replacements.containsKey(rom)) {
                Map<String,Object> replacement=obj(replacements.get(rom));String parent=str(replacement.get("sourceRom"));
                if(!parent.equals(str(row.get("parent"))))throw new IllegalArgumentException("Unconfirmed parent override "+rom);
                selected=parent;relation="confirmed-parent-alternative";extra=str(replacement.get("notes"));
            }
            Map<String,Object> mapped=mapping(rom,selected,relation,extra);
            mappings.put(rom,mapped);if(mapped.get("asset")==null)gaps.add(mapped);
        }
        List<Map<String,Object>> metadata=rows(obj(Json.read(root.resolve("app/src/main/assets/arcade-metadata/metadata.json"))).get("sets"));
        // Canonical aliases are taken from existing catalog metadata, never guessed from similar titles.
        for(Map<String,Object> set:metadata) {
            String rom=str(set.get("rom")),canonical=str(set.get("canonical"));
            Map<String,Object> real=mappings.get(rom);
            boolean inventoryKey=inventory.stream().anyMatch(r->canonical.equals(str(r.get("rom"))));
            if(real!=null&&!canonical.isEmpty()&&!inventoryKey&&
                (!mappings.containsKey(canonical)||(mappings.get(canonical).get("asset")==null&&real.get("asset")!=null))) {
                Map<String,Object> alias=new LinkedHashMap<>(real);alias.put("rom",canonical);
                alias.put("kind",real.get("asset")==null?real.get("kind"):"catalogue-canonical-alias");
                alias.put("notes",real.get("notes")+" Canonical catalogue alias of inventory ROM "+rom+" (existing metadata).");mappings.put(canonical,alias);
            }
        }
        // Include every metadata lookup key associated with an inventory canonical group.
        Set<String> groups=new HashSet<>();for(Map<String,Object> set:metadata)if(mappings.containsKey(str(set.get("rom"))))groups.add(str(set.get("canonical")));
        for(Map<String,Object> set:metadata) {
            String rom=str(set.get("rom")),canonical=str(set.get("canonical"));
            if(mappings.containsKey(rom)||!groups.contains(canonical))continue;
            Map<String,Object> base=mappings.get(canonical);if(base==null)continue;
            Map<String,Object> alias=new LinkedHashMap<>(base);alias.put("rom",rom);
            alias.put("kind",base.get("asset")==null?base.get("kind"):"catalogue-canonical-alias");
            alias.put("notes",base.get("notes")+" Metadata alias "+rom+" of "+canonical+"; cabinet appearance/set controls may differ.");mappings.put(rom,alias);
        }
        Json.write(root.resolve("app/src/main/assets/arcade-metadata/cabinet-art.json"),Map.of("mappings",new ArrayList<>(mappings.values())));
        Json.write(review.resolve("gaps.json"),Map.of("gaps",gaps));Json.write(Path.of("/tmp/full-cabinet-gaps.json"),Map.of("gaps",gaps));
        long covered=inventory.size()-gaps.size(),assets=results.values().stream().filter(r->r.get("asset")!=null).count();
        System.out.println("Prepared "+assets+" unique real-photo assets; "+covered+"/"+inventory.size()+" inventory ROMs covered; "+gaps.size()+" gaps; "+mappings.size()+" total lookup keys.");
    }

    static void selfTest() {
        BufferedImage src=new BufferedImage(100,100,BufferedImage.TYPE_INT_ARGB);
        Graphics2D g=src.createGraphics();g.setColor(Color.BLACK);g.fillRect(0,0,100,100);
        g.setColor(Color.RED);g.fillRect(20,10,4,80);g.fillRect(76,10,4,80);g.fillRect(20,10,60,4);g.dispose();
        BufferedImage upright=segment(src,Map.of("mode","rows"));
        if(upright.getRGB(50,80)!=0xff000000||upright.getRGB(10,50)!=0)throw new AssertionError("Dark upright front/background");
        BufferedImage open=segment(src,Map.of("mode","edge"));
        if(open.getRGB(50,80)!=0||open.getRGB(21,80)!=0xffff0000)throw new AssertionError("Open-frame gap preservation");
        List<List<Double>> outline=List.of(List.of(.2,.1),List.of(.8,.1),List.of(.8,.9),List.of(.2,.9));
        BufferedImage polygon=segment(src,Map.of("mode","polygon","polygon",outline));
        if(polygon.getRGB(50,80)!=0xff000000||polygon.getRGB(10,80)!=0)throw new AssertionError("Photo polygon");
        BufferedImage white=new BufferedImage(100,100,BufferedImage.TYPE_INT_RGB);
        g=white.createGraphics();g.setColor(Color.WHITE);g.fillRect(0,0,100,100);g.setColor(Color.BLACK);g.fillRect(20,20,60,60);g.dispose();
        BufferedImage whiteCut=segment(white,Map.of("mode","edge","background","white","threshold",10));
        if(whiteCut.getRGB(50,50)!=0xff000000||whiteCut.getRGB(10,10)!=0)throw new AssertionError("White backdrop");
        BufferedImage alpha=new BufferedImage(100,100,BufferedImage.TYPE_INT_ARGB);alpha.setRGB(50,50,0x80776655);
        if(segment(alpha,Map.of("mode","edge")).getRGB(50,50)!=0x80776655)throw new AssertionError("Existing alpha retained");
        BufferedImage fitted=NormalizeArcadeCabinets.normalize(upright,false,2,false,true);
        if(fitted.getWidth()!=960||fitted.getHeight()!=768||(fitted.getRGB(0,0)>>>24)!=0||(fitted.getRGB(480,500)>>>24)!=255)throw new AssertionError("Canvas/alpha normalization");
        System.out.println("PASS: dark interiors, exterior removal, cockpit/open gaps, photo polygons, white backgrounds, existing alpha, and normalized canvas.");
    }

    static void sheets(String prefix, boolean prepared) throws Exception {
        List<String> names = new ArrayList<>(sources.keySet());
        int cols=6, per=36, cw=240, ch=250;
        for (int start=0; start<names.size(); start+=per) {
            int count=Math.min(per,names.size()-start);
            BufferedImage sheet=new BufferedImage(cols*cw, ((count+cols-1)/cols)*ch, BufferedImage.TYPE_INT_RGB);
            Graphics2D g=sheet.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            for(int j=0;j<count;j++) {
                String name=names.get(start+j); int x=j%cols*cw,y=j/cols*ch;
                g.setColor(new Color(130,165,188)); g.fillRect(x,y,cw,ch);
                g.setColor(new Color(225,205,169)); g.fillRect(x+cw/2,y,cw/2,ch-24);
                Path file=prepared?output.resolve(name+".png"):refs.resolve(str(sources.get(name).get("file")));
                if (Files.exists(file)) {
                    BufferedImage im=ImageIO.read(file.toFile());
                    double scale=Math.min((cw-12.0)/im.getWidth(),(ch-30.0)/im.getHeight());
                    int w=(int)(im.getWidth()*scale),h=(int)(im.getHeight()*scale);
                    g.drawImage(im,x+(cw-w)/2,y+(ch-30-h)/2,w,h,null);
                }
                g.setColor(Color.BLACK); g.setFont(new Font(Font.SANS_SERIF,Font.BOLD,15));
                g.drawString(name,x+8,y+ch-6);
            }
            g.dispose(); ImageIO.write(sheet,"png",review.resolve(prefix+"-"+(start/per+1)+".png").toFile());
        }
    }

    /** Minimal strict JSON reader so the tool has no Gradle or third-party dependency. */
    static class Json {
        final String text; int i;
        Json(String s) { text=s; }
        static Object read(Path p) throws Exception { Json j=new Json(Files.readString(p)); Object v=j.value();j.ws();if(j.i!=j.text.length())throw new IllegalArgumentException("Trailing JSON");return v; }
        static void write(Path p,Object value) throws Exception {Files.createDirectories(p.getParent());Files.writeString(p,encode(value,0)+"\n");}
        static String encode(Object value,int depth) {
            if(value==null)return "null";if(value instanceof Boolean||value instanceof Number)return value.toString();
            if(value instanceof String s)return "\""+s.replace("\\","\\\\").replace("\"","\\\"").replace("\n","\\n").replace("\r","\\r").replace("\t","\\t")+"\"";
            String indent="  ".repeat(depth),child=indent+"  ";List<String> items=new ArrayList<>();
            if(value instanceof Map<?,?> m){for(var entry:m.entrySet())items.add(child+encode(entry.getKey().toString(),depth+1)+": "+encode(entry.getValue(),depth+1));return "{\n"+String.join(",\n",items)+"\n"+indent+"}";}
            for(Object item:(List<?>)value)items.add(child+encode(item,depth+1));return "[\n"+String.join(",\n",items)+"\n"+indent+"]";
        }
        void ws(){while(i<text.length()&&Character.isWhitespace(text.charAt(i)))i++;}
        Object value(){ws();char c=text.charAt(i);if(c=='"')return string();if(c=='{'){i++;Map<String,Object> m=new LinkedHashMap<>();ws();if(text.charAt(i)=='}'){i++;return m;}do{ws();String k=string();ws();expect(':');m.put(k,value());ws();if(text.charAt(i)=='}'){i++;return m;}expect(',');}while(true);}if(c=='['){i++;List<Object> l=new ArrayList<>();ws();if(text.charAt(i)==']'){i++;return l;}do{l.add(value());ws();if(text.charAt(i)==']'){i++;return l;}expect(',');}while(true);}for(String k:List.of("null","true","false"))if(text.startsWith(k,i)){i+=k.length();return k.equals("null")?null:k.equals("true");}int s=i;while(i<text.length()&&"-+0123456789.eE".indexOf(text.charAt(i))>=0)i++;return Double.valueOf(text.substring(s,i));}
        String string(){expect('"');StringBuilder b=new StringBuilder();while(i<text.length()){char c=text.charAt(i++);if(c=='"')return b.toString();if(c=='\\'){c=text.charAt(i++);if(c=='u'){b.append((char)Integer.parseInt(text.substring(i,i+4),16));i+=4;continue;}c=switch(c){case 'n'->'\n';case 'r'->'\r';case 't'->'\t';case 'b'->'\b';case 'f'->'\f';default->c;};}b.append(c);}throw new IllegalArgumentException("Unterminated JSON string");}
        void expect(char c){if(text.charAt(i++)!=c)throw new IllegalArgumentException("Expected "+c+" at "+i);}
    }
}