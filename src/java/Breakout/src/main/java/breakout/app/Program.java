package breakout.app;
//      _____________________________
//_____/ Librerias
import org.json.JSONObject;

//      _____________________________
//_____/ Modulos
import breakout.app.modules.Server;
import java.io.*;

/**
 * Hello world!
 *
 */
public class Program {
    public static void main( String[] args ) {
        JSONObject object = new JSONObject("{\"mensaje\":\"Ejecutando: [Servidor]\"}");
        System.out.println(object.getString("mensaje"));
        System.out.println(object.toString());
        try {
            Server servidor = new Server(8080, "127.0.0.1");
        } catch (IOException e) {
            System.err.println(e);
        }
    }
}
