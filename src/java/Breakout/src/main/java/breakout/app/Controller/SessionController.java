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
    public String changes = "none";

    public SessionController(ClientPlayer player){
        this.player = player;
    }

    public void initiate(){
        this.session = new GameSession();
        this.session.bricks = new Brick[4][8];
        for (int i = 0; i < 4; i++){
            for (int j = 0; j<8; j++){
                if (i == 0){
                    this.session.bricks[i][j] = new Brick(i+1, "green", i, j);
                } else if(i == 1){
                    this.session.bricks[i][j] = new Brick(i+1, "yellow", i, j);
                } else if(i == 2){
                    this.session.bricks[i][j] = new Brick(i+1, "orange", i, j);
                } else if(i == 3){
                    this.session.bricks[i][j] = new Brick(i+1, "red", i, j);
                }
            }
        }
        this.session.balls = new LinkedList();
        this.session.balls.insert(new Ball(50.0f, 50.0f));
        this.session.racket = new Racket(50.f, 50.f);
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

    private synchronized void applyPowerUp(PowerUp power){
        JSONObject description = new JSONObject(power.getDescription());
        if (description.get("category").equals("ball")){
            switch (description.getString("modifier")) {
                case "speed":
                    for (int i = 0; i < this.session.balls.size; i++){
                        Ball ball = (Ball)this.session.balls.get(i);
                        JSONObject balldesc = new JSONObject(ball.getContent());
                        ball.setSpeed(balldesc.getFloat("speed")*description.getFloat("value"));
                    }
                    break;
                case "quantity":
                    Ball ball = (Ball)this.session.balls.get(0);
                    for (int i = 0; i < description.getInt("value"); i++){
                        this.session.balls.insert(ball.duplicate());
                    }
                    break;
            }
            JSONArray jsonchanges = new JSONArray();
            for (int i = 0; i < this.session.balls.size; i++){
                Ball ball = (Ball)this.session.balls.get(i);
                jsonchanges.put(ball.getContent());
            }
            this.changes = jsonchanges.toString();
        } else if (description.get("category").equals("racket")) {
            Racket racket = this.session.racket;
            JSONObject racketdesc = new JSONObject(racket.getContent());
            switch (description.getString("modifier")) {
                case "speed":
                    racket.setSpeed(racketdesc.getFloat("speed")*description.getFloat("value"));
                    break;
                case "size":
                    racket.setSize(racketdesc.getFloat("size")*description.getFloat("value"));
                    break;
            }
            this.changes = this.session.racket.getContent();
        }
    }

    public synchronized void registerHitOnBrick(int i, int j){
        Brick brick = this.session.bricks[i][j];
        PowerUp power = brick.strike();
        if (power != null){
            this.applyPowerUp(power);
        }
    }

    public synchronized void moveBall(String id, float x, float y){
        for (int i = 0; i < this.session.balls.size; i++){
            Ball ball = (Ball)this.session.balls.get(i);
            JSONObject data = new JSONObject(ball.getContent());
            if (data.getString("id").equals(id)){
                ball.move(x,y);
                break;
            }
        }
    }

    public synchronized void moveRacket(float x, float y){
        this.session.racket.move(x, y);
    }
}