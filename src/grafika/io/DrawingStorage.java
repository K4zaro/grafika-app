package grafika.io;

import grafika.model.GShape;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

public final class DrawingStorage {

    private DrawingStorage() {
    }

    public static void save(File file, List<GShape> shapes) throws IOException {
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(file))) {
            out.writeObject(new ArrayList<>(shapes));
        }
    }

    public static List<GShape> load(File file) throws IOException {
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(file))) {
            Object obj = in.readObject();
            if (!(obj instanceof List)) {
                throw new IOException("Nieprawidłowy format pliku.");
            }
            List<GShape> result = new ArrayList<>();
            for (Object o : (List<?>) obj) {
                if (!(o instanceof GShape)) {
                    throw new IOException("Plik zawiera nieobsługiwane obiekty.");
                }
                result.add((GShape) o);
            }
            return result;
        } catch (ClassNotFoundException | ClassCastException e) {
            throw new IOException("Nieprawidłowy format pliku.", e);
        }
    }
}
