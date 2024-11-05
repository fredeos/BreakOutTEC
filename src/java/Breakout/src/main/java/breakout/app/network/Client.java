package breakout.app.network;

import java.io.*;
import java.net.*;
import java.util.concurrent.Semaphore;

public class Client {

    public String identifier;
    public String username;
    public String type;
    
    protected Socket socket;
    protected BufferedReader in;
    protected PrintWriter out;
    protected boolean standby = true;

    protected String message;
    protected String received;
    protected Semaphore mutex = new Semaphore(1);

    public String read() throws IOException{
        try {
            mutex.acquire();
            this.received = this.in.readLine();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            mutex.release();
        }
        return this.received;
    }

    public synchronized boolean isOnStandBy(){
        return this.standby;
    }

    public void send() throws IOException{
        try {
            mutex.acquire();
            this.out.println(message);
            this.out.flush();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            mutex.release();
        }
    }

    public String checkOutput(){
        return this.message;
    }

    public synchronized void changeOutput(String content){
        try {
            mutex.acquire();
            this.message = content;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            mutex.release();
        }
    }

    public synchronized void continue_(){
        this.standby = false;
    }

    public synchronized void terminate() throws IOException{
        try {
            mutex.acquire();
            if (!this.socket.isClosed()){
                this.socket.close();
                this.in.close();
                this.out.close();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            mutex.release();
        }
    }

}
