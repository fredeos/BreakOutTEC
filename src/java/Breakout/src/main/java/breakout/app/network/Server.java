package breakout.app.network;

import java.io.*;
import java.net.*;
import org.json.*;

import breakout.app.Structures.CircularList;
import breakout.app.Structures.LinkedList;

/* Clase para crear un servidor por medio de sockets TCP
 * Maneja las conexiones con todos los clientes
 */
public class Server {

    // ------------------------------[ Atributos/Propiedades ]------------------------------
    private ServerSocket socket; 
    private Thread handler;
    private boolean active;

    public LinkedList pending;
    private LinkedList clientlist;
    private CircularList playerlist;

    // ------------------------------[ Metodos ]------------------------------
    /* Metodo constructor de la clase servidor
     * @param PORT: 
     * @param IP
     * @returns
    */
    public Server(int PORT, String IP) throws IOException{
        InetAddress address = InetAddress.getByName(IP);
        this.socket = new ServerSocket(PORT, 0, address);
        System.out.println("Servidor activo!");
        System.out.println(PORT);
        System.out.println(IP);
    }

    /* Abre una sala de espera donde recibe
     * 
    */
    private synchronized void turnON(){
        this.active = true;
        this.handler = new Thread(()->{
            System.out.println("Iniciando sala de espera...");
            System.out.println("Esperando que se conecten clientes");
            do {
                try {
                    Socket client = this.socket.accept();
                    System.out.println("Cliente conectado!");
                    
                    BufferedReader input = new BufferedReader(new InputStreamReader(client.getInputStream()));
                    String message = input.readLine();
                    JSONObject startup = new JSONObject(message);
                    String type = startup.getString("type");
                    if (type == "player"){
                        ClientPlayer new_client = new ClientPlayer(client, startup.getString("id"), startup.getString("name"));
                        this.clientlist.insert(new_client);
                        this.playerlist.insert(new_client);
                        this.pending.insert(new_client);
                        this.openPlayerChannel(new_client);
                    } else {
                        ClientSpectator new_client = new ClientSpectator(client, null, startup.getString("id"), startup.getString("name"));
                        this.clientlist.insert(new_client);
                        this.pending.insert(new_client);
                        this.openSpectatorChannel(new_client);
                    }

                } catch (IOException e) {
                    System.err.println(e);
                    break;
                }
            } while (this.isActive());
            System.out.println("Finalizando sala de espera...");
        });
        this.handler.start();
    }

    private void openPlayerChannel(ClientPlayer client){
        Thread communication_thread = new Thread(()->{
            System.out.println("Escuchando al cliente jugador: "+client.username+"."+client.identifier);
            while (this.isActive()){
                if(!client.standby){

                }
            }
        });
        communication_thread.start();
    }

    private void openSpectatorChannel(ClientSpectator client){
        Thread communication_thread = new Thread(()->{
            System.out.println("Escuchando al cliente espectador: "+client.username+"."+client.identifier);
            while (this.isActive()){
                if(!client.standby){
                    
                }
            }
        });
        communication_thread.start();
    }

    private String prepareResponse(String request){
        String jsonresponse = "";
        switch (request) {
            case "on_standby":
                JSONObject json = new JSONObject();
                    json.put("code", 100);
                    json.put("request", "connect");
                    json.put("response", "standby");
                    json.put("description", "wait");
                break;
            case "accept":
                break;
            case "approve":
                break;
            default:
                break;
        }
        return jsonresponse;
    }

    public void approveClient(int i){
        Client client = (Client) this.pending.get(i);
        client.continue_();
        this.pending.removeContent(client);
    }

    public void rejectClient(int i){
        
    }

    /* Verifica que el servidor aun este activo*/
    private boolean isActive(){
        return this.active;
    }

    /* Activa/desactiva la comunicacion del servidor con sus clientes*/
    public synchronized void toggleActive(){
        this.active = !this.active;
        if (this.active == false){
            // TODO: Falta implementar que se notifique a los clientes que la conexion ha finalizado
        }
    }
}
