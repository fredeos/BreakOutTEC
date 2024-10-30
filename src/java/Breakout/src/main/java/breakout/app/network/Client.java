package breakout.app.network;

import java.io.*;
import java.net.*;

public class Client {

    public String identifier;
    public String username;
    
    protected Socket socket;
    protected BufferedReader in;
    protected PrintWriter out;
    protected boolean standby = true;

    protected String message;
    protected String received;

    public String read() throws IOException{
        this.received = this.in.readLine();
        return this.received;
    }

    public void send() throws IOException{
        this.out.println(message);
        this.out.flush();
    }

    public synchronized void changeOutput(String content){
        this.message = content;
    }

    public synchronized void continue_(){
        this.standby = false;
    }

    public synchronized void terminate() throws IOException{
        if (!this.socket.isClosed()){
            this.socket.close();
            this.in.close();
            this.out.close();
        }
    }

    public String getID(){
        return this.identifier;
    }
}
