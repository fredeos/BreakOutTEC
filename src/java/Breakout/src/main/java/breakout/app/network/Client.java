package breakout.app.network;

import java.io.*;
import java.net.*;
import java.util.concurrent.Semaphore;

public abstract class Client {

    public String identifier;
    public String username;
    public String type;
    
    protected Socket socket;
    protected BufferedReader in;
    protected PrintWriter out;
    protected boolean standby = true;

    protected String message;
    protected String received;
    protected String priority_message = "none";
    protected Semaphore IO_lock = new Semaphore(1);

    public String read() throws IOException{
        try {
            this.IO_lock.acquire();
            this.received = this.in.readLine();
            System.out.println("CLIENTE: "+this.received);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            this.IO_lock.release();
        }
        return this.received;
    }

    public boolean isOnStandBy(){
        boolean flag = false;
        try {
            this.IO_lock.acquire();
            flag = this.standby;
        } catch (InterruptedException e1) {
            System.err.println(e1);
        } finally {
            this.IO_lock.release();
        }
        return flag;
    }

    public void send() throws IOException{
        try {
            IO_lock.acquire();
            if (!this.priority_message.equals("none")){
                System.out.println("SERVIDOR: "+this.priority_message);
                this.out.println(this.priority_message);
                this.priority_message = "none";
            } else {
                System.out.println("SERVIDOR: "+this.message);
                this.out.println(this.message);
            }
            this.out.flush();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            IO_lock.release();
        }
    }

    public String checkOutput(){
        return this.message;
    }

    public synchronized void changeMessage(String content){
        try {
            IO_lock.acquire();
            this.message = content;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            IO_lock.release();
        }
    }

    public synchronized void changePriorityMessage(String content){
        try {
            IO_lock.acquire();
            this.priority_message = content;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            IO_lock.release();
        }
    }

    public abstract void continue_();

    public synchronized void terminate() throws IOException{
        try {
            IO_lock.acquire();
            if (!this.socket.isClosed()){
                this.socket.close();
                this.in.close();
                this.out.close();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            IO_lock.release();
        }
    }

}
