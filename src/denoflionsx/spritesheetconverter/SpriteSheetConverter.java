package denoflionsx.spritesheetconverter;

import java.awt.Graphics;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.HeadlessException;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.awt.image.PixelGrabber;
import java.io.File;
import java.net.URL;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.HashMap;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

public class SpriteSheetConverter {

    public static String path;
    public static String saveFolder;
    public static BufferedImage blank;
    private static final String x = "x";
    private static final String y = "y";
    private static final HashMap<Integer, HashMap<String, Integer>> coordinates = new HashMap();

    static {
        int a = 16;
        int currentIndex = -1;
        int currentX = 0;
        int currentY = 0;
        for (int i = 0; i < a; i++) {
            for (int j = 0; j < a; j++) {
                currentIndex++;
                HashMap<String, Integer> coords = new HashMap();
                coords.put(x, currentX);
                coords.put(y, currentY);
                coordinates.put(currentIndex, coords);
                currentX += a;
            }
            currentX = 0;
            currentY += a;
        }
    }

    public static void main(String[] args) {
        path = args[0];
        saveFolder = args[1];
        for (String s : args) {
            System.out.println(s);
        }
        URL r = SpriteSheetConverter.class.getResource("blank.png");
        Toolkit tk = Toolkit.getDefaultToolkit();
        Image i = tk.getImage(r);
        blank = toBufferedImage(i);
        split();
    }

    public static int[] TranslateIndexToCoords(int index) {
        try {
            HashMap<String, Integer> coords = coordinates.get(index);
            return new int[]{coords.get(x), coords.get(y)};
        } catch (Exception ex) {
            System.out.println("Index translation failure. Failed on: " + index + ". Replacing with 0.");
            return TranslateIndexToCoords(0);
        }
    }

    public static byte[] md5(BufferedImage bimg) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            ByteBuffer bb = ByteBuffer.allocate(4 * bimg.getWidth());
            for (int y = bimg.getHeight() - 1; y >= 0; y--) {
                bb.clear();
                for (int x = bimg.getWidth() - 1; x >= 0; x--) {
                    bb.putInt(bimg.getRGB(x, y));
                }
                md.update(bb.array());
            }
            byte[] digBytes = md.digest();
            return digBytes;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    public static String hexify(byte[] digBytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : digBytes) {
            sb.append(String.format("%02X", b & 0xff));
        }
        String signature = sb.toString();
        return signature;
    }

    public static void split() {
        Toolkit tk = Toolkit.getDefaultToolkit();
        Image image = tk.getImage(path);
        BufferedImage buf = toBufferedImage(image);
        if (!new File(saveFolder).exists()) {
            new File(saveFolder).mkdirs();
        }
        for (int a = 0; a < 256; a++) {
            int[] coords = TranslateIndexToCoords(a);
            BufferedImage clone = buf.getSubimage(coords[0], coords[1], 16, 16);
            if (hexify(md5(clone)).equals(hexify(md5(blank)))) {
                continue;
            }
            String coordname = "Sprite-" + coords[0] + "_" + coords[1];
            File f = new File(saveFolder + "/" + coordname + ".png");
            if (!f.exists()) {
                f.mkdirs();
                ImageIOWrapper.write(clone, "png", f);
            }
        }
    }

    public static boolean hasAlpha(Image image) {
        // If buffered image, the color model is readily available
        if (image instanceof BufferedImage) {
            return ((BufferedImage) image).getColorModel().hasAlpha();
        }

        // Use a pixel grabber to retrieve the image's color model;
        // grabbing a single pixel is usually sufficient
        PixelGrabber pg = new PixelGrabber(image, 0, 0, 1, 1, false);
        try {
            pg.grabPixels();
        } catch (InterruptedException e) {
        }

        // Get the image's color model
        return pg.getColorModel().hasAlpha();
    }

    public static BufferedImage toBufferedImage(Image image) {

        if (image instanceof BufferedImage) {
            return (BufferedImage) image;
        }

        // This code ensures that all the pixels in the image are loaded
        image = new ImageIcon(image).getImage();

        // Determine if the image has transparent pixels
        boolean hasAlpha = hasAlpha(image);

        // Create a buffered image with a format that's compatible with the
        // screen
        BufferedImage bimage = null;
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        try {
            // Determine the type of transparency of the new buffered image
            int transparency = Transparency.OPAQUE;
            if (hasAlpha == true) {
                transparency = Transparency.BITMASK;
            }

            // Create the buffered image
            GraphicsDevice gs = ge.getDefaultScreenDevice();
            GraphicsConfiguration gc = gs.getDefaultConfiguration();
            bimage = gc.createCompatibleImage(image.getWidth(null), image.getHeight(null), transparency);
        } catch (HeadlessException e) {
        } // No screen

        if (bimage == null) {
            // Create a buffered image using the default color model
            int type = BufferedImage.TYPE_INT_RGB;
            if (hasAlpha == true) {
                type = BufferedImage.TYPE_INT_ARGB;
            }
            bimage = new BufferedImage(image.getWidth(null), image.getHeight(null), type);
        }

        // Copy image to buffered image
        Graphics g = bimage.createGraphics();

        // Paint the image onto the buffered image
        g.drawImage(image, 0, 0, null);
        g.dispose();

        return bimage;
    }

    public static class ImageIOWrapper {

        // Wrap all this crap in Try/Catch blocks so that I don't have to do it
        // every time I use these methods.
        public static void write(BufferedImage image, String type, File f) {
            try {
                if (f.exists()) {
                    f.delete();
                }
                ImageIO.write(image, type, f);
            } catch (Exception ex) {
                //ex.printStackTrace();
                // LiquidRoundup.Proxy.print("Image write crapped out!");
            }
        }

        public static BufferedImage read(File f) {
            try {
                return ImageIO.read(f);
            } catch (Exception ex) {
                //ex.printStackTrace();
                //LiquidRoundup.Proxy.print("Image read crapped out!");
                return null;
            }
        }
    }
}
