package breakout.app.network.observer; 

public interface Publisher {
    public void subscribe(Subscriber subscriber);
    public void unsubscribe(Subscriber subscriber);
    public void NotifyAll();
    public void Notify(Subscriber subscriber);
}
