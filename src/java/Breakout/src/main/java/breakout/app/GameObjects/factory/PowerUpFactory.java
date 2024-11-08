package breakout.app.GameObjects.factory;
 
import breakout.app.GameObjects.AddBall;
import breakout.app.GameObjects.AddLife;
import breakout.app.GameObjects.PowerUp;
import breakout.app.GameObjects.SizeIncrease;
import breakout.app.GameObjects.SpeedIncrease;

public class PowerUpFactory {
    public PowerUpFactory(){}

    public PowerUp createPowerUp(String name, Object value){
        PowerUp power = null;
        switch (name) {
            case "additional-balls":
                int extra_balls = (int)value;
                power = new AddBall(extra_balls);
                break;
            case "ball-speed":
                int b_speed_mult = (int)value;
                power = new SpeedIncrease(b_speed_mult, "ball");
                break;
            case "racket-speed":
                int r_speed_mult = (int)value;
                power = new SpeedIncrease(r_speed_mult, "racket");
                break;
            case "racket-size":
                int r_size_mult =  (int)value;
                power = new SizeIncrease(r_size_mult);
                break;
            case "additional-life":
                int extra =  (int)value;
                power = new AddLife(extra);
                break;
            default:
                break;
        }
        return power;
    }
}
