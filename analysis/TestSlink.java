import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.ObjectOutputStream;
import java.io.Serializable;

class FileWriteSlink implements Serializable {
    private static final long serialVersionUID = 1L;

    private String filename;
    private String content;

    public FileWriteSlink(String filename, String content) {
        this.filename = filename;
        this.content = content;
    }

    private void readObject(java.io.ObjectInputStream in) throws Exception {
        in.defaultReadObject();

        System.out.println("Source method 'readObject' triggered!");
        System.out.println("Attempting to write to file: " + this.filename);

        FileWriter writer = new FileWriter(this.filename);
        writer.write(this.content);
        writer.close();

        System.out.println("File write operation completed.");
    }
}

public class TestSlink {
    public static void main(String[] args) throws Exception {
        FileWriteSlink payload = new FileWriteSlink("pwned.txt", "test.");
        try (FileOutputStream fos = new FileOutputStream("slink_test.ser");
             ObjectOutputStream oos = new ObjectOutputStream(fos)) {
            oos.writeObject(payload);
        }

        System.out.println("Serialized data for SLINK test saved to slink_test.ser");
    }
}