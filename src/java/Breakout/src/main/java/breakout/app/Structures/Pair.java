package breakout.app.Structures;

/* Par (tupla) de datos mutable estandar*/
public class Pair <A,B> {
    private A first;
    private B second;

    public Pair(A element1, B element2){
        this.first = element1;
        this.second = element2;
    }

    public A getFirst(){
        return this.first;
    }

    public B getSecond(){
        return this.second;
    }

    public void setFirst(A value){
        this.first = value;
    }

    public void setSecond(B value){
        this.second = value;
    }
}
