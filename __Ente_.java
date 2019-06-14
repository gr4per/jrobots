// by Thomas Schwarz, Student developer candidate Feb 2018
public class __Ente_ extends JJRobot {
    private int speed = 100;
    private double time = 0;

    void main() {
	searchAndDestroy();
    }
    void searchAndDestroy() {
	int degrees = 90;
	int turnright = 45;
	int resolution = 2;	
	int range = 0;
	int deg_shot = 0;

	while(true){
		//check for collision
		if(!checkWall(degrees)){
			//calculate new direction
			int random = rand(315);
			turnright = (random == 180 || random < 45)? 90:random;
			
			//change direction
			degrees = (degrees + turnright) % 360;
		}
		else{
			//drive
			drive(degrees, speed);

			//time to decelerate
			if(time() - time > 4){
				speed = 100;
			}

			//scan the area
			range = scan(deg_shot, resolution);

			//shoot
			if(range > 20 && range <= 730){
				cannon(deg_shot, (range <= 700)? range:range-30);

				if(range > 400 && time() > 80 && degrees != deg_shot){
					speed = 50;
					degrees = (deg_shot) % 360;
					time = time();
				}
			}
			if(range == 0){
				deg_shot = (deg_shot + 2 ) % 360;
			}
		}
	}
    }
    boolean checkWall(int degrees){
	int locx = loc_x();
	int locy = loc_y();
	int start = 0;
	int end = 1000;
	int safe_dist = 76;	

	//distance to the wall <= safe_dist AND direction is faced to wall?
	if(calcDistance(start, locx) <= safe_dist && degrees > 90 && degrees < 270 ||
		calcDistance(end, locx) <= safe_dist && degrees > 270 || loc_x() >= 900 && degrees < 90 ||
		calcDistance(start, locy) <= safe_dist && degrees > 180 && degrees < 360 ||
		calcDistance(end, locy) <= safe_dist && degrees < 180 && degrees > 0)
	{
		speed = 50;
		time = time();
		return false;
	}
	return true;
    }
    //returns the distance between two locations, e.g. wall and current
    int calcDistance(int loc_a, int loc_b){
	return sqrt((loc_a - loc_b) * (loc_a - loc_b));
    }	
}