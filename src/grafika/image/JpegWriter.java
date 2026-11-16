package grafika.image;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;

public final class JpegWriter {

    private JpegWriter() {
    }

    /** @param qualityPercent jakość 0–100 (100 = najlepsza jakość, najmniejsza kompresja) */
    public static void save(BufferedImage image, File file, int qualityPercent) throws IOException {
        if (qualityPercent < 0 || qualityPercent > 100) {
            throw new IllegalArgumentException("Jakość musi mieścić się w zakresie 0–100.");
        }
        BufferedImage rgb = ImageLoader.toRgb(image);

        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
        if (!writers.hasNext()) {
            throw new IOException("Brak kodera JPEG w tej instalacji Javy.");
        }
        ImageWriter writer = writers.next();
        try (ImageOutputStream out = ImageIO.createImageOutputStream(file)) {
            if (out == null) {
                throw new IOException("Nie można otworzyć pliku do zapisu.");
            }
            ImageWriteParam param = writer.getDefaultWriteParam();
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(qualityPercent / 100f);
            writer.setOutput(out);
            writer.write(null, new IIOImage(rgb, null, null), param);
        } finally {
            writer.dispose();
        }
    }
}