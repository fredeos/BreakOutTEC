package breakout.app;
import breakout.app.modules.Server;
import java.io.*;

/**
 * Hello world!
 *
 */
public class Program {
    public static void main( String[] args ) {
        System.out.println( "Hello World!" );

        try {
            Server servidor = new Server(8080, "127.0.0.1");
        } catch (IOException e) {
            System.err.println(e);
        }
    }
}
