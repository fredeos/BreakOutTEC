package breakout.app.GameObjects.factory;
 
import breakout.app.GameObjects.AddBall;
import breakout.app.GameObjects.PowerUp;
import breakout.app.GameObjects.SizeIncrease;
import breakout.app.GameObjects.SpeedIncrease;

public class PowerUpFactory {
    public PowerUpFactory(){}

    public PowerUp createPowerUp(String name, Object value){
        PowerUp power = null;
        switch (name) {
            case "additional_balls":
                int quantity = (int)value;
                power = new AddBall(quantity);
                break;
            case "ball_speed":
                int Bmult = (int)value;
                power = new SpeedIncrease(Bmult, "ball");
                break;
            case "racket_size":
                int size_mult = (int)value;
                power = new SizeIncrease(size_mult);
                break;
            case "racket_speed":
                int Rmult =  (int)value;
                power = new SpeedIncrease(Rmult, "racket");
                break;
            default:
                break;
        }
        return power;
    }
}
