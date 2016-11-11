import java.io.*;
import java.lang.reflect.*;
import static java.lang.String.join;
import static java.util.Arrays.sort;

public interface Test {
    static void run(Class c) {
        Method[] methods = c.getDeclaredMethods() ;
        sort(methods, (a, b) -> a.getName().compareTo(b.getName()));

        int count = 0;
        for(Method m : methods) {
            if(m.getName().startsWith("_"))
                ++count;
        }

        System.out.println("1.." + count);

        final PrintStream out = System.out;
        System.setOut(new PrintStream(new OutputStream() {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            public void write(int b) throws IOException {
                if(b != '\n') bos.write(b);
                else {
                    byte[] bytes = bos.toByteArray();
                    bos.reset();
                    out.write(' ');
                    out.write(bytes);
                    out.println();
                }
            }
            public void flush() throws IOException {
                byte[] bytes = bos.toByteArray();
                bos.reset();
                out.write(bytes);
                out.flush();
            }
        }));

        int id = 0;
        for(Method m : methods) {
            if(!m.getName().startsWith("_")) continue;
            String ok = "ok " + ++id + " -" + m.getName().replace('_', ' ');

            try {
                m.invoke(null);
                out.println(ok);
            }
            catch(IllegalAccessException e) {
                throw new RuntimeException(e);
            }
            catch(InvocationTargetException e) {
                Throwable cause = e.getCause();
                out.printf(" %s: %s\n",
                    cause.getClass().getSimpleName(),
                    join("\n  ", cause.getMessage().split("\n")));
                out.println("not " + ok);
            }
        }
    }
}
