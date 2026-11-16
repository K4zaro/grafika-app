package grafika.image;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public final class ImageLoader {

    private ImageLoader() {
    }

    public static BufferedImage load(File file) throws IOException {
        if (!file.isFile()) {
            throw new ImageFormatException("Plik nie istnieje lub nie jest zwykłym plikiem.");
        }
        if (file.length() == 0) {
            throw new ImageFormatException("Plik jest pusty.");
        }

        int b0;
        int b1;
        try (InputStream in = new FileInputStream(file)) {
            b0 = in.read();
            b1 = in.read();
        }

        if (b0 == 'P' && b1 >= '0' && b1 <= '9') {
            return PpmReader.read(file);
        }
        if (b0 == 0xFF && b1 == 0xD8) {
            return readJpeg(file);
        }
        throw new ImageFormatException("Nieobsługiwany format pliku. Program obsługuje PPM (P3, P6) oraz JPEG.");
    }

    private static BufferedImage readJpeg(File file) throws IOException {
        try {
            BufferedImage img = ImageIO.read(file);
            if (img == null) {
                throw new ImageFormatException("Nie udało się zdekodować pliku JPEG.");
            }
            return toRgb(img);
        } catch (ImageFormatException e) {
            throw e;
        } catch (IOException | RuntimeException e) {
            throw new ImageFormatException("Uszkodzony lub nieobsługiwany plik JPEG: " + e.getMessage(), e);
        }
    }
    //Konfertuje dowolny obraz na RGB bez kanału alfa
    public static BufferedImage toRgb(BufferedImage src) {
        if (src.getType() == BufferedImage.TYPE_INT_RGB) {
            return src;
        }
        BufferedImage rgb = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = rgb.createGraphics();
        g.drawImage(src, 0, 0, null);
        g.dispose();
        return rgb;
    }
}