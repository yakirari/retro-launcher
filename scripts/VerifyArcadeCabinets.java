import java.awt.Graphics2D;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/** Run after compiling together with NormalizeArcadeCabinets.java. No Android needed. */
public class VerifyArcadeCabinets {
    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void transparentBorder(BufferedImage image, String label) {
        for (int x=0; x<image.getWidth(); x++) {
            require((image.getRGB(x,0) >>> 24) == 0, label + " top border");
            require((image.getRGB(x,image.getHeight()-1) >>> 24) == 0, label + " bottom border");
        }
        for (int y=0; y<image.getHeight(); y++) {
            require((image.getRGB(0,y) >>> 24) == 0, label + " left border");
            require((image.getRGB(image.getWidth()-1,y) >>> 24) == 0, label + " right border");
        }
    }

    public static void main(String[] args) throws Exception {
        BufferedImage synthetic = new BufferedImage(100,100,BufferedImage.TYPE_INT_RGB);
        Graphics2D g = synthetic.createGraphics();
        g.setColor(Color.RED); g.fillRect(30,20,40,60);
        g.setColor(Color.BLACK); g.fillRect(40,30,20,20);
        g.setColor(Color.WHITE); g.fillRect(40,60,20,10);
        // Isolated rectangular scan-border debris must not become part of the cabinet.
        g.setColor(new Color(4,4,4)); g.drawLine(95,5,95,95);
        g.dispose();
        BufferedImage normalized = NormalizeArcadeCabinets.normalize(synthetic, true);
        transparentBorder(normalized, "synthetic");
        require(normalized.getRGB(480,300) == Color.BLACK.getRGB(), "Enclosed black screen preserved");
        require(normalized.getRGB(480,590) == Color.WHITE.getRGB(), "White cabinet details preserved");
        require((normalized.getRGB(730,400) >>> 24) == 0, "Disconnected border removed");
        BufferedImage openBase = new BufferedImage(100,100,BufferedImage.TYPE_INT_RGB);
        g = openBase.createGraphics();
        g.setColor(Color.YELLOW); g.fillRect(30,20,40,5);
        g.fillRect(30,20,3,60); g.fillRect(67,20,3,60); g.dispose();
        BufferedImage solid = NormalizeArcadeCabinets.normalize(openBase,true,2,true);
        require(solid.getRGB(480,500) == Color.BLACK.getRGB(), "Open-base black cabinet body preserved");
        require((solid.getRGB(200,500) >>> 24) == 0, "Outside solid cabinet remains transparent");
        BufferedImage alphaCabinet = new BufferedImage(100,100,BufferedImage.TYPE_INT_ARGB);
        g = alphaCabinet.createGraphics();
        g.setColor(Color.BLACK); g.fillRect(30,20,40,60);
        g.setColor(Color.WHITE); g.fillRect(30,20,40,5); g.dispose();
        BufferedImage retained = NormalizeArcadeCabinets.normalize(alphaCabinet,false,2,false,true);
        require(retained.getRGB(480,500) == Color.BLACK.getRGB(), "Prepared alpha silhouette dark body must never be keyed again");
        require(retained.getRGB(480,60) == Color.WHITE.getRGB(), "Prepared alpha silhouette white edge retained");
        transparentBorder(retained, "prepared alpha silhouette");
        int files = 0;
        for (File file : new File(args[0]).listFiles((dir,name) -> name.endsWith(".png"))) {
            BufferedImage image = ImageIO.read(file);
            require(image.getWidth()==960 && image.getHeight()==768, file + " normalized canvas");
            transparentBorder(image,file.getName());
            int occupied = 0;
            for (int y=0; y<768; y++) for (int x=0; x<960; x++) {
                if ((image.getRGB(x,y) >>> 24) > 8) occupied++;
            }
            require(occupied > 20000 && occupied < 760*700, file + " nonblank nonrectangular silhouette");
            files++;
        }
        String[] owned = {"1942","1943","asteroid","btime","contra","ddragonu","defender",
            "digdug","dkong3","ffight","frogger","galaga","galaxian","ghouls","goldnaxe",
            "invaders","joust","mario","mk2","mspacman","mvsc","outrunb","pacman","paperboy",
            "qbert","rtype2","sf2","shinobi","starwars","tapper","tekken3","tmnt2","tron","xmen"};
        for (String rom : owned) require(new File(args[0],rom+".png").isFile(), "Missing owned cabinet " + rom);
        System.out.println("Passed: black/white detail preservation, debris removal, " + files +
            " transparent silhouettes, and all " + owned.length + " owned-game cabinet assets.");
    }
}