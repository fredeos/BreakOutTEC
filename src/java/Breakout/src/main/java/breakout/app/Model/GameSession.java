package breakout.app.Model;

import breakout.app.GameObjects.*;
import breakout.app.network.ClientPlayer;
import breakout.app.Structures.LinkedList;

public class GameSession {
    public Brick[][] bricks;
    public Ball[] balls;
    public Racket racket;

    public ClientPlayer player;
}
