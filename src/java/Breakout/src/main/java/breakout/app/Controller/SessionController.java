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
    private JSONObject client_changes = new JSONObject();
    private JSONObject server_changes = new JSONObject();

    public SessionController(ClientPlayer player){
        this.player = player;
    }

    public void initiate(){
        this.session = new GameSession();
        this.session.lifes = 3;
        this.session.score = 0;
        this.session.bricks = new Brick[4][8];
        for (int i = 0; i < 4; i++){
            for (int j = 0; j<8; j++){
                if (i == 0){
                    this.session.bricks[i][j] = new Brick(1, "green", i, j);
                    this.session.bricks[i][j].setPowerUp(new AddBall(1));
                } else if(i == 1){
                    this.session.bricks[i][j] = new Brick(1, "yellow", i, j);
                    this.session.bricks[i][j].setPowerUp(new SizeIncrease(2));
                } else if(i == 2){
                    this.session.bricks[i][j] = new Brick(1, "orange", i, j);
                    this.session.bricks[i][j].setPowerUp(new SpeedIncrease(2,"ball"));
                } else if(i == 3){
                    this.session.bricks[i][j] = new Brick(1, "red", i, j);
                    this.session.bricks[i][j].setPowerUp(new AddLife(1));
                }
            }
        }
        Ball ball = new Ball(50, 50);
        this.session.balls = new LinkedList();
        this.session.balls.insert(ball);
        this.session.racket = new Racket(50, 50);
        this.session.racket.setSize(3);
    }

    public synchronized JSONObject getSessionInformation(){
        JSONObject game = new JSONObject();
        game.put("lifes", this.session.lifes);
        game.put("score",this.session.score);
        // Obtener todos los ladrillos
        JSONArray bricks = new JSONArray();
        for (int i = 0; i < 4; i++){
            JSONArray layer = new JSONArray();
            for (int j = 0; j < 8; j++){
                Brick brick = this.session.bricks[i][j];
                layer.put(brick.getContent());
            }
            bricks.put(layer);
        }
        game.put("bricks",bricks);
        // Obtener todas las bolas
        JSONArray balls = new JSONArray();
        for (int i = 0; i < this.session.balls.size; i++){
            Ball ball = (Ball)this.session.balls.get(i);
            balls.put(ball.getContent());
        }
        game.put("balls",balls);
        game.put("racket", this.session.racket.getContent());
        return game;
    }

    public synchronized void setBrickPowerUp(String powerup, int i, int j){
        PowerUpFactory factory = new PowerUpFactory();
    }

    public JSONObject getServerChanges(){
        return this.server_changes;
    }

    public JSONObject getClientChanges(){
        return this.client_changes;
    }

    public synchronized void clearChanges(){
        this.server_changes.clear();
        this.client_changes.clear();
    }

    public synchronized void registerHitOnBrick(int i, int j){
        Brick brick = this.session.bricks[i][j];
        brick.strike();
    }

    public synchronized void increaseLife(int life_value){
        this.session.lifes = life_value;
    }

    public synchronized void increaseScore(int score_value){
        this.session.score = score_value;
    }

    public synchronized void increaseBallSpeed(int speed_value){
        for (int i = 0; i < this.session.balls.size; i++){
            Ball ball = (Ball)this.session.balls.get(i);
            ball.setSpeed(speed_value);
        }
    }

    public synchronized void addNewBall(int id, int x, int y){
        Ball newBall = new Ball(x, y, id);
        this.session.balls.insert(newBall);
    }

    public synchronized void increaseRacketSize(int size_value){
        this.session.racket.setSize(size_value);
    }

    public synchronized void increaseRacketSpeed(int speed_value){
        this.session.racket.setSpeed(speed_value);
    }

    public synchronized void moveBall(int id, int x, int y){
        for (int i = 0; i < this.session.balls.size; i++){
            Ball ball = (Ball)this.session.balls.get(i);
            JSONObject data = new JSONObject(ball.getContent());
            if (ball.id == id){
                ball.move(x,y);
                break;
            }
        }
    }

    public synchronized void deleteBall(int id){
        int target = -1;
        for (int i = 0; i < this.session.balls.size; i++){
            Ball ball = (Ball)this.session.balls.get(i);
            JSONObject data = new JSONObject(ball.getContent());
            if (data.getInt("id") == id){
                target = i;
                break;
            }
        }
        if (target != -1){
            this.session.balls.remove(target);
        }
    }

    public synchronized void moveRacket(int x, int y){
        this.session.racket.move(x, y);
    }
}