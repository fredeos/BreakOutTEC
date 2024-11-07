package breakout.app.network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import org.json.JSONObject;

import breakout.app.network.observer.Subscriber;  

public class ClientSpectator extends Client implements Subscriber {

    private ClientPlayer target;
    private String update_message = "none";
    
    public ClientSpectator(Socket client, ClientPlayer target, String id, String name){
        this.socket = client;
        this.target = target;
        this.identifier = id;
        this.username = name;
        this.type = "spectator";
        this.standby = true;
        if (this.target != null){
            this.target.subscribe(this);
        }
        try {
            this.in = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
            this.out = new PrintWriter(this.socket.getOutputStream());
        } catch (IOException e) {
            System.err.println("ERROR: No se pudieron iniciar los componentes necesarios para el cliente\nDETALLE:\n"+e);
        }
    }

    @Override
    public synchronized void continue_(){
        try {
            this.IO_lock.acquire();
            this.standby = false;
        } catch (InterruptedException e1) {
            System.err.println(e1);
        } finally {
            this.IO_lock.release();
        }
    }

    public synchronized void acquireUpdate(){
        JSONObject response = new JSONObject(); 
        response.put("code", 100);
        response.put("request","spectating-player");
        response.put("watching",this.getTargetID());
        switch (this.update_message) {
            case "none":
                response.put("response","session-acquired");
                response.put("description","joined-new-session");
                response.put("attach", this.target.acquireSessionData());
                break;
            default:
                response.put("response","session-data-received");
                response.put("description","watching-session");
                response.put("attach", new JSONObject(this.update_message));
                break;
        }
        this.changeMessage(response.toString());
    }

    public synchronized String getTargetID(){
        String target_id = "none";
        try {
            this.IO_lock.acquire();
            if (this.target != null){
                target_id = this.target.identifier;
            }
        } catch (InterruptedException e) {
            System.err.println(e);
        } finally {
            this.IO_lock.release();
        }
        return target_id;
    }

    public synchronized void changeTarget(ClientPlayer newTarget){
        try {
            this.IO_lock.acquire();
            if (this.target != null){
                this.target.unsubscribe(this);
            }
            if (newTarget != null){
                newTarget.subscribe(this);
            }
            this.target = newTarget;
            this.update_message = "none";
        } catch (InterruptedException e1) {
            System.err.println(e1);
        } finally {
            this.IO_lock.release();
        }
    }

    @Override
    public synchronized void update(Object status){
        try {
            this.IO_lock.acquire();
            this.update_message = (String)status;
        } catch (InterruptedException e) {
            System.err.println(e);
        } finally {
            this.IO_lock.release();
        }
    }
    
}
