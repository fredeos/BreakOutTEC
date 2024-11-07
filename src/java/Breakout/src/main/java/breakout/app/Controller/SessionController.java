package breakout.app.Controller;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.concurrent.Semaphore;

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

    public Semaphore session_lock = new Semaphore(1);

    public SessionController(ClientPlayer player){
        this.player = player;
    }

    public void initiate() throws InterruptedException {
        this.session_lock.acquire();

        this.session = new GameSession();
        this.session.lifes = 3;
        this.session.score = 0;
        this.session.bricks = new Brick[4][8];
        for (int i = 0; i < 4; i++){
            for (int j = 0; j<8; j++){
                if (i == 0){
                    this.session.bricks[i][j] = new Brick(1, "red", i, j);
                    //this.session.bricks[i][j].setPowerUp(new AddBall(1));
                } else if(i == 1){
                    this.session.bricks[i][j] = new Brick(1, "orange", i, j);
                    //this.session.bricks[i][j].setPowerUp(new SizeIncrease(2));
                } else if(i == 2){
                    this.session.bricks[i][j] = new Brick(1, "yellow", i, j);
                    //this.session.bricks[i][j].setPowerUp(new SpeedIncrease(2,"ball"));
                } else if(i == 3){
                    this.session.bricks[i][j] = new Brick(1, "green", i, j);
                    //this.session.bricks[i][j].setPowerUp(new AddLife(1));
                }
            }
        }
        Ball ball = new Ball(50.0, 50.0);
        this.session.balls = new LinkedList();
        this.session.balls.insert(ball);
        this.session.racket = new Racket(50.0);
        this.session.racket.setSize(3);

        this.session_lock.release();
    }

    public synchronized JSONObject getSessionInformation(){
        JSONObject game = new JSONObject();
        try {
            this.session_lock.acquire();
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
        } catch (InterruptedException e2) {
            System.err.println(e2);
        } finally {
            this.session_lock.release();
        }
        return game;
    }

    public synchronized Brick getBrick(int i, int j){
        Brick brick = null;
        try {
            this.session_lock.acquire();
            brick = this.session.bricks[i][j];
        } catch (InterruptedException e1) {
            // TODO: handle exception
        } finally {
            this.session_lock.release();
        }
        return brick;
    }

    public synchronized void setBrickPowerUp(String powerup_name, int i, int j, Object value){
        try {
            this.session_lock.acquire();
            PowerUpFactory factory = new PowerUpFactory();
            PowerUp powerup = factory.createPowerUp(powerup_name, value);
            this.session.bricks[i][j].setPowerUp(powerup);
            this.server_changes = this.session.bricks[i][j].getContent();
            System.out.println(this.server_changes.toString());
        } catch (InterruptedException e1) {
            System.err.println(e1);
        } finally {
            this.session_lock.release();
        }
    }

    public JSONObject getServerChanges(){
        JSONObject json = new JSONObject(this.server_changes.toString());
        return json;
    }

    public JSONObject getClientChanges(){
        return this.client_changes;
    }

    public synchronized void clearChanges(){
        this.server_changes.clear();
        this.client_changes.clear();
    }

    public synchronized void registerHitOnBrick(int i, int j){
        try {
            this.session_lock.acquire();
            Brick brick = this.session.bricks[i][j];
            brick.strike();
        } catch (InterruptedException e1) {
            // TODO: handle exception
        } finally {
            this.session_lock.release();
        }
    }

    public synchronized void increaseLife(int life_value){
        try {
            this.session_lock.acquire();
            this.session.lifes = life_value;
        } catch (InterruptedException e1) {
            // TODO: handle exception
        } finally {
            this.session_lock.release();
        }
    }

    public synchronized void increaseScore(int score_value){
        try {
            this.session_lock.acquire();
            this.session.score = score_value;
        } catch (InterruptedException e1) {
            // TODO: handle exception
        } finally {
            this.session_lock.release();
        }
    }

    public synchronized void increaseBallSpeed(int speed_value){
        try {
            this.session_lock.acquire();
            for (int i = 0; i < this.session.balls.size; i++){
            Ball ball = (Ball)this.session.balls.get(i);
            ball.setSpeed(speed_value);
        }
        } catch (InterruptedException e1) {
            // TODO: handle exception
        } finally {
            this.session_lock.release();
        }
    }

    public synchronized void addNewBall(int id, double x, double y){
        try {
            this.session_lock.acquire();
            Ball newBall = new Ball(x, y, id);
            this.session.balls.insert(newBall);
        } catch (InterruptedException e1) {
            // TODO: handle exception
        } finally {
            this.session_lock.release();
        }
    }

    public synchronized void increaseRacketSize(int size_value){
        try {
            this.session_lock.acquire();
            this.session.racket.setSize(size_value);
        } catch (InterruptedException e1) {
            // TODO: handle exception
        } finally {
            this.session_lock.release();
        }
    }

    public synchronized void increaseRacketSpeed(int speed_value){
        try {
            this.session_lock.acquire();
            this.session.racket.setSpeed(speed_value);
        } catch (InterruptedException e1) {
            // TODO: handle exception
        } finally {
            this.session_lock.release();
        }
    }

    public synchronized void moveBall(int id, double x, double y){
        try {
            this.session_lock.acquire();
            for (int i = 0; i < this.session.balls.size; i++){
                Ball ball = (Ball)this.session.balls.get(i);
                JSONObject data = new JSONObject(ball.getContent());
                if (ball.id == id){
                    ball.move(x,y);
                    break;
                }
            }
        } catch (InterruptedException e1) {
            // TODO: handle exception
        } finally {
            this.session_lock.release();
        }
    }

    public synchronized void deleteBall(int id){
        try {
            this.session_lock.acquire();
            int target = -1;
            for (int i = 0; i < this.session.balls.size; i++){
                Ball ball = (Ball)this.session.balls.get(i);
                JSONObject data = ball.getContent();
                if (data.getInt("id") == id){
                    target = i;
                    break;
                }
            }
            if (target != -1){
                this.session.balls.remove(target);
            }
        } catch (InterruptedException e1) {
            // TODO: handle exception
        } finally {
            this.session_lock.release();
        }
    }

    public synchronized void moveRacket(double x){
        try {
            this.session_lock.acquire();
            this.session.racket.move(x);
        } catch (InterruptedException e1) {
            // TODO: handle exception
        } finally {
            this.session_lock.release();
        }
    }
}