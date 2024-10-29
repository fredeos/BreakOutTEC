package breakout.app.network;

import java.io.*;
import java.net.Socket;

import breakout.app.Structures.LinkedList;
import breakout.app.network.observer.*; 

public class ClientPlayer extends Client implements Publisher {

    private LinkedList subscribers;

    public ClientPlayer(Socket client){
        this.socket = client;
        this.subscribers = new LinkedList();
        try {
            this.in = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
            this.out = new PrintWriter(this.socket.getOutputStream());
        } catch (IOException e) {
            System.err.println("ERROR: No se pudieron iniciar los componentes necesarios para el cliente\nDETALLE:\n"+e);
        }
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
