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
    public synchronized void turnON(){
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

                    Client new_client;
                    if (type.equals("player")){
                        new_client = new ClientPlayer(client, startup.getString("id"), startup.getString("name"));
                        this.clientlist.insert(new_client);
                        this.playerlist.insert(new_client);
                        this.pending.insert(new_client);
                        new_client.changeOutput(this.prepareResponse("on_standby"));
                        this.openPlayerChannel((ClientPlayer)new_client);
                    } else {
                        new_client = new ClientSpectator(client, null, startup.getString("id"), startup.getString("name"));
                        this.clientlist.insert(new_client);
                        this.pending.insert(new_client);
                        new_client.changeOutput(this.prepareResponse("on_standby"));
                        this.openSpectatorChannel((ClientSpectator)new_client);
                    }
                    this.approveClient(0); // Esto se borra despues, es solo para probar
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
            loop: while (this.isActive()){ 
                try {
                    client.send();
                    if(!client.standby){
                        String receivedmsg = client.read();
                        JSONObject json = new JSONObject(receivedmsg);
                        switch (json.getString("request")) {
                            case "closing":
                                break loop;
                            default:
                                client.process();
                                break;
                        }
                    }
                } catch (IOException e) {
                    // TODO: handle exception
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
                    try {
                        String receivedmsg = client.read();
                        switch (receivedmsg) {
                            case "next-player":
                                break;
                        
                            default:
                                break;
                        }
                        client.send();
                    } catch (IOException e) {
                        // TODO: handle exception
                    }
                }
            }
        });
        communication_thread.start();
    }

    private String prepareResponse(String request){
        String jsonresponse = "";
        JSONObject json = new JSONObject();
        switch (request) {
            case "on_standby":
                json.put("code", 100);
                json.put("request", "connection");
                json.put("response", "standby");
                json.put("description", "wait");
                break;
            case "approve":
                json.put("code", 100);
                json.put("request", "connection");
                json.put("response", "approved");
                json.put("description", "start");
                break;
            case "reject":
                json.put("code", 100);
                json.put("request", "connection");
                json.put("response", "rejected");
                json.put("description", "end");
                break;
            case "closing":
                json.put("code", 400);
                break;
            default:
                break;
        }
        jsonresponse = json.toString();
        return jsonresponse;
    }

    public void approveClient(int i){
        Client client = (Client) this.pending.get(i);
        client.continue_();
        client.changeOutput(this.prepareResponse("approve"));
        this.pending.removeContent(client);
    }

    public void rejectClient(int i){
        Client client = (Client) this.pending.get(i);
        client.continue_();
        client.changeOutput(this.prepareResponse("reject"));
        this.pending.removeContent(client);
    }

    /* Verifica que el servidor aun este activo*/
    private boolean isActive(){
        return this.active;
    }

    /* Desactiva la comunicacion del servidor con sus clientes*/
    public synchronized void turnOFF(){
        this.active = false;
        // TODO: Falta implementar que se notifique a los clientes que la conexion ha finalizado
    }
}
