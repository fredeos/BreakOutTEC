package breakout.app.network;

import java.io.*;
import java.net.Socket;

import org.json.JSONArray;
import org.json.JSONObject; 

import breakout.app.Controller.SessionController; 
import breakout.app.View.ClientWindow;

import breakout.app.Structures.LinkedList; 
import breakout.app.network.observer.*; 

public class ClientPlayer extends Client implements Publisher {

    private LinkedList subscribers;
    private SessionController session;
    private ClientWindow window;

    public ClientPlayer(Socket client, String id, String name){
        this.socket = client;
        this.subscribers = new LinkedList();
        this.identifier = id;
        this.username = name;
        this.type = "player";
        this.session = null;
        this.window = null;
        try {
            this.in = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
            this.out = new PrintWriter(this.socket.getOutputStream());
        } catch (IOException e) {
            System.err.println("ERROR: No se pudieron iniciar los componentes necesarios para el cliente\nDETALLE:\n"+e);
        }
    }

    public synchronized void setClientWindow(ClientWindow window){
        this.window = window;
        this.window.setSession(this.session);
    }

    @Override
    public synchronized void continue_(){
        try {
            this.IO_lock.acquire();
            this.standby = false;
            this.session = new SessionController(this);
            this.session.initiate();
        } catch (InterruptedException e1) {
            System.err.println(e1);
        } finally {
            this.IO_lock.release();
        }
    }

    public synchronized void process(){
        JSONObject json = new JSONObject(this.received);
        JSONObject response = new JSONObject(); 
        switch (json.getString("request")) {
            case "init":
                response.put("code", 100);
                response.put("request", "initiate-game");
                response.put("response", "session-created");
                response.put("description", "new-game");
                response.put("attach", this.session.getSessionInformation()); 
                break;
            case "update-game":
                String[] parameters = json.getString("action").split(":");
                String action = parameters[0];

                response.put("code", 100);
                response.put("request", "update-game");
                response.put("response", "session-updated");
                response.put("description", "data-updated");
                response.put("action", action);
                if (action.equals("move-ball")){
                    JSONObject ball = json.getJSONObject("attach");
                    JSONArray position = ball.getJSONArray("position");
                    this.session.moveBall(ball.getInt("id"), position.getDouble(0), position.getDouble(1));
                } else if (action.equals("move-racket")){
                    JSONObject racket = json.getJSONObject("attach");
                    double position = racket.getDouble("position");
                    this.session.moveRacket(position);
                } else if (action.equals("strike-brick")){
                    JSONObject brick = json.getJSONObject("attach");
                    JSONArray position = brick.getJSONArray("position");
                    this.session.registerHitOnBrick(position.getInt(0), position.getInt(1));
                } else if (action.equals("rm-ball")){ 
                    JSONObject ball = json.getJSONObject("attach");
                    this.session.deleteBall(ball.getInt("id"));
                } else { // Caso de apply-powerup
                    String powerup_type = parameters[1];
                    if (powerup_type.equals("add-life")){
                        int total_life = json.getInt("attach");
                        this.session.increaseLife(total_life);
                    } else if (powerup_type.equals("add-ball")){
                        JSONObject new_ball = json.getJSONObject("attach");
                        JSONArray position = new_ball.getJSONArray("position");
                        this.session.addNewBall(new_ball.getInt("id"), position.getDouble(0),position.getDouble(1));
                    } else if (powerup_type.equals("increase-ball-speed")){
                        int speed = json.getInt("attach");
                        this.session.increaseBallSpeed(speed);
                    } else if (powerup_type.equals("increase-racket-speed")){
                        int speed = json.getInt("attach");
                        this.session.increaseRacketSpeed(speed);
                    } else if (powerup_type.equals("increase-racket-size")){
                        int size = json.getInt("attach");
                        this.session.increaseRacketSize(size);
                    } 
                }
                // Obtener los cambios del hechos en la sesion
                if (this.session.getServerChanges().isEmpty()){
                    response.put("server-updated",false);
                } else {
                    response.put("server-updated",true);
                    response.put("server-changes",this.session.getServerChanges());
                }
                if (this.session.getClientChanges().isEmpty()){
                    response.put("client-updated",false);
                } else {
                    response.put("client-updated",true);
                    response.put("client-changes",this.session.getClientChanges());
                }
                this.session.clearChanges();
                break;
            default:
                break;
        }
        this.changeMessage(response.toString());
        this.NotifyAll();
    }

    @Override 
    public synchronized void subscribe(Subscriber subscriber){
        this.subscribers.insert(subscriber);
    }

    @Override
    public synchronized void unsubscribe(Subscriber subscriber){
        this.subscribers.removeContent(subscriber);
    }

    @Override
    public synchronized void NotifyAll(){
        for (int i = 0; i < this.subscribers.size; i++){
            Subscriber subscriber = (Subscriber) this.subscribers.get(i);
            this.Notify(subscriber);
        }
    }

    @Override
    public synchronized void Notify(Subscriber subscriber){
        subscriber.update(this.received);
    }
    
}
