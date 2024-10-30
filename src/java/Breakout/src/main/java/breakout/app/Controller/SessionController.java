package breakout.app.Controller;

import org.json.JSONArray;
import org.json.JSONObject;

import breakout.app.GameObjects.*;
import breakout.app.GameObjects.factory.PowerUpFactory;
import breakout.app.Model.GameSession;
import breakout.app.Structures.LinkedList;
import breakout.app.network.ClientPlayer;

public class SessionController {

    private ClientPlayer player;
    private GameSession session;

    public SessionController(ClientPlayer player){
        this.player = player;
    }

    public void initiate(){
        this.session = new GameSession();
        this.session.bricks = new Brick[4][8];
        for (int i = 0; i < 4; i++){
            for (int j = 0; j<8; j++){
                if (i == 0){
                    this.session.bricks[i][j] = new Brick(i+1, "green");
                } else if(i == 1){
                    this.session.bricks[i][j] = new Brick(i+1, "red");
                } else if(i == 2){
                    this.session.bricks[i][j] = new Brick(i+1, "blue");
                } else if(i == 3){
                    this.session.bricks[i][j] = new Brick(i+1, "purple");
                }
            }
        }
        this.session.balls = new LinkedList();
            this.session.balls.insert(new Ball(50.0f, 50.0f));
        this.session.racket = new Racket();
    }

    public synchronized String getSessionInformation(){
        JSONObject game = new JSONObject();
        // Obtener todos los ladrillos
        JSONObject bricks = new JSONObject();
        for (int i = 0; i < 4; i++){
            JSONArray layer = new JSONArray();
            for (int j = 0; j < 8; j++){
                Brick brick = this.session.bricks[i][j];
                layer.put(brick.getContent());
            }
            bricks.put("layer"+i, layer.toString());
        }
        game.put("bricks",bricks.toString());
        // Obtener todas las bolas
        JSONArray balls = new JSONArray();
        for (int i = 0; i < this.session.balls.size; i++){
            Ball ball = (Ball)this.session.balls.get(i);
            balls.put(ball.getContent());
        }
        game.put("balls",balls.toString());
        game.put("racket", this.session.racket.getContent());
        return game.toString();
    }

    public synchronized void setBrickPowerUp(String powerup, int i, int j){
        PowerUpFactory factory = new PowerUpFactory();
    }

    public synchronized void applyPowerUp(String target, PowerUp power){
        // TODO
    }

    public synchronized void registerHitOnBrick(int i, int j){
        // TODO
    }
}