package grafika.image;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

//(P3 - tekstowy, P6 - binarny). Dane czytane są blokowo.
public final class PpmReader {
    private static final int BLOCK_SIZE = 64 * 1024;
    private static final int CHUNK_PIXELS = 16 * 1024;
    private static final long MAX_PIXELS = 50_000_000L;

    private PpmReader() {
    }

    public static BufferedImage read(File file) throws IOException {
        try (InputStream raw = new FileInputStream(file)) {
            BlockInput in = new BlockInput(raw);

            int p = in.read();
            int type = in.read();
            if (p != 'P' || type < '0' || type > '9') {
                throw new ImageFormatException("To nie jest plik PPM (brak sygnatury „P3” / „P6”).");
            }
            if (type != '3' && type != '6') {
                throw new ImageFormatException("Nieobsługiwany wariant formatu PNM: P" + (char) type
                        + ". Obsługiwane są tylko P3 i P6.");
            }

            int width = headerInt(in, "szerokość");
            int height = headerInt(in, "wysokość");
            int maxVal = headerInt(in, "maksymalna wartość koloru");

            if (width <= 0 || height <= 0) {
                throw new ImageFormatException("Nieprawidłowe wymiary obrazu: " + width + " × " + height + ".");
            }
            if ((long) width * height > MAX_PIXELS) {
                throw new ImageFormatException("Obraz jest zbyt duży (" + width + " × " + height + ").");
            }
            if (maxVal < 1 || maxVal > 65535) {
                throw new ImageFormatException("Nieprawidłowa maksymalna wartość koloru: " + maxVal
                        + " (dozwolony zakres 1–65535).");
            }

            int[] pixels = new int[width * height];
            if (type == '3') {
                readAscii(in, pixels, maxVal);
            } else {
                readBinary(in, pixels, maxVal);
            }

            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            image.setRGB(0, 0, width, height, pixels, 0, width);
            return image;
        }
    }

    private static int headerInt(BlockInput in, String what) throws IOException {
        int v = in.readInt();
        if (v < 0) {
            throw new ImageFormatException("Nagłówek jest niekompletny – brak wartości: " + what + ".");
        }
        return v;
    }

    private static void readAscii(BlockInput in, int[] pixels, int maxVal) throws IOException {
        for (int i = 0; i < pixels.length; i++) {
            int r = in.readInt();
            int g = in.readInt();
            int b = in.readInt();
            if (r < 0 || g < 0 || b < 0) {
                throw truncated(pixels.length);
            }
            if (r > maxVal || g > maxVal || b > maxVal) {
                throw tooBig(maxVal, i);
            }
            pixels[i] = pack(scale(r, maxVal), scale(g, maxVal), scale(b, maxVal));
        }
    }

    private static void readBinary(BlockInput in, int[] pixels, int maxVal) throws IOException {
        int bytesPerSample = maxVal < 256 ? 1 : 2;
        byte[] chunk = new byte[CHUNK_PIXELS * 3 * bytesPerSample];
        int done = 0;
        while (done < pixels.length) {
            int n = Math.min(CHUNK_PIXELS, pixels.length - done);
            if (!in.readFully(chunk, n * 3 * bytesPerSample)) {
                throw truncated(pixels.length);
            }
            int k = 0;
            for (int j = 0; j < n; j++) {
                int r;
                int g;
                int b;
                if (bytesPerSample == 1) {
                    r = chunk[k++] & 0xFF;
                    g = chunk[k++] & 0xFF;
                    b = chunk[k++] & 0xFF;
                } else {
                    r = ((chunk[k] & 0xFF) << 8) | (chunk[k + 1] & 0xFF);
                    g = ((chunk[k + 2] & 0xFF) << 8) | (chunk[k + 3] & 0xFF);
                    b = ((chunk[k + 4] & 0xFF) << 8) | (chunk[k + 5] & 0xFF);
                    k += 6;
                }
                if (r > maxVal || g > maxVal || b > maxVal) {
                    throw tooBig(maxVal, done + j);
                }
                pixels[done + j] = pack(scale(r, maxVal), scale(g, maxVal), scale(b, maxVal));
            }
            done += n;
        }
    }

    private static ImageFormatException truncated(int expectedPixels) {
        return new ImageFormatException("Plik jest uszkodzony lub ucięty – dane pikseli kończą się przedwcześnie"
                + " (oczekiwano " + expectedPixels + " pikseli).");
    }

    private static ImageFormatException tooBig(int maxVal, int pixelIndex) {
        return new ImageFormatException("Wartość koloru przekracza zadane maksimum " + maxVal
                + " (piksel nr " + (pixelIndex + 1) + ").");
    }

    private static int scale(int v, int max) {
        return max == 255 ? v : (v * 255 + max / 2) / max;
    }

    private static int pack(int r, int g, int b) {
        return (r << 16) | (g << 8) | b;
    }

    //Bufor blokowy
    private static final class BlockInput {
        private final InputStream in;
        private final byte[] buf = new byte[BLOCK_SIZE];
        private int pos;
        private int len;

        BlockInput(InputStream in) {
            this.in = in;
        }

        private boolean fill() throws IOException {
            len = in.read(buf, 0, buf.length);
            pos = 0;
            if (len <= 0) {
                len = 0;
                return false;
            }
            return true;
        }

        int read() throws IOException {
            if (pos == len && !fill()) {
                return -1;
            }
            return buf[pos++] & 0xFF;
        }

        boolean readFully(byte[] dst, int count) throws IOException {
            int off = 0;
            while (off < count) {
                if (pos == len && !fill()) {
                    return false;
                }
                int c = Math.min(len - pos, count - off);
                System.arraycopy(buf, pos, dst, off, c);
                pos += c;
                off += c;
            }
            return true;
        }

        // Zwraca kolejną nieujemną liczbę całkowitą lub -1 na końcu pliku. Pomija białe znaki i komentarze.
        int readInt() throws IOException {
            int c = read();
            while (c != -1) {
                if (c == '#') {
                    do {
                        c = read();
                    } while (c != -1 && c != '\n' && c != '\r');
                } else if (isSpace(c)) {
                    c = read();
                } else {
                    break;
                }
            }
            if (c == -1) {
                return -1;
            }
            if (c < '0' || c > '9') {
                throw invalidChar(c);
            }
            long v = 0;
            while (c >= '0' && c <= '9') {
                v = v * 10 + (c - '0');
                if (v > Integer.MAX_VALUE) {
                    throw new ImageFormatException("Liczba w pliku jest zbyt duża.");
                }
                c = read();
            }
            if (c == '#') {
                pos--;
            } else if (c != -1 && !isSpace(c)) {
                throw invalidChar(c);
            }
            return (int) v;
        }

        private static boolean isSpace(int c) {
            return c == ' ' || c == '\t' || c == '\n' || c == '\r' || c == '\f' || c == 0x0B;
        }

        private static ImageFormatException invalidChar(int c) {
            return new ImageFormatException(String.format(
                    "Nieprawidłowy znak (bajt 0x%02X) tam, gdzie oczekiwano liczby.", c));
        }
    }
}