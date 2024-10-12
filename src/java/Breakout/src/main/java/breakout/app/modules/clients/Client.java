package breakout.app.modules.clients;

import java.io.*;
import java.net.*;

public class Client {

    protected String identifier;
    protected Socket socket;
    protected BufferedReader in;
    protected PrintWriter out;

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
