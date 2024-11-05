package breakout.app.network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import breakout.app.network.observer.Subscriber; 

public class ClientSpectator extends Client implements Subscriber {

    private ClientPlayer target;
    
    public ClientSpectator(Socket client, ClientPlayer target, String id, String name){
        this.socket = client;
        this.target = target;
        this.identifier = id;
        this.username = name;
        this.type = "spectator";
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

    public synchronized void changeTarget(ClientPlayer newTarget){
        if (this.target != null){
            this.target.unsubscribe(this);
        }
        if (newTarget != null){
            newTarget.subscribe(this);
        }
        this.target = newTarget;
    }

    @Override
    public synchronized void update(Object status){
        this.message = (String)status;
    }
    
}
