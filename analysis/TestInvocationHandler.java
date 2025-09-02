import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;

class SinkClass {
    public void execute(String cmd) {
        try {
            Runtime.getRuntime().exec(cmd);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
interface ProxyInterface {
    void someMethod(String command);
}

class Handler implements InvocationHandler, Serializable {
    private static final long serialVersionUID = 1L;
    private final SinkClass sink;

    public Handler(SinkClass sink) {
        this.sink = sink;
    }

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (method.getName().equals("someMethod")) {
            System.out.println("Invoke method called, passing tainted arg to sink...");
            sink.execute((String) args[0]);
        }
        return null;
    }
}

public class TestInvocationHandler {
    public static void main(String[] args) throws IOException {
        SinkClass sink = new SinkClass();

        Handler handler = new Handler(sink);
        ProxyInterface proxyInstance = (ProxyInterface) Proxy.newProxyInstance(
                TestInvocationHandler.class.getClassLoader(),
                new Class[]{ProxyInterface.class},
                handler
        );

        java.util.HashMap<Object, Object> map = new java.util.HashMap<>();
        map.put(proxyInstance, "value");

        try {
            Class<?> clazz = Class.forName("sun.reflect.annotation.AnnotationInvocationHandler");
            java.lang.reflect.Constructor<?> constructor = clazz.getDeclaredConstructor(Class.class, Map.class);
            constructor.setAccessible(true);
            InvocationHandler H_AnnotationInvocationHandler = (InvocationHandler) constructor.newInstance(java.lang.annotation.Retention.class, map);
            try (FileOutputStream fos = new FileOutputStream("proxy_test.ser");
                 ObjectOutputStream oos = new ObjectOutputStream(fos)) {
                oos.writeObject(H_AnnotationInvocationHandler);
            }

            System.out.println("Serialized data saved to proxy_test.ser");

        } catch (Exception e) {
            System.out.println("This test case requires a JDK version that has sun.reflect.annotation.AnnotationInvocationHandler.");
            e.printStackTrace();
        }
    }
}