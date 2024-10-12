package breakout.app.modules.data_structures;

public class Triplet <A, B, C> {
    private final A first;
    private final B second;
    private final C third;

    public Triplet(A element1, B element2, C element3) {
        this.first = element1;
        this.second = element2;
        this.third = element3;
    }
}
