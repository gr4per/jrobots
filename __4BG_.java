

public class __4BG_ extends JJRobot {
	
	public static int deg=35;
	public static int spd=15;
	public static int distance=0;
	
	public void main() {

		while(true) {

			try {
				
				randomRun();
				//wait one second
				java.util.concurrent.TimeUnit.SECONDS.sleep(1);
				
				//Identify target
				identifyTarget();
				
				//fire at that spot  
				shotMissile();
			} catch (Exception e) {}
		}
	}
	
	private void randomRun() {
		
		avoidWalls();
		if (deg > 0 || deg <= 90)
				deg = rand(90)+269;
		else if	(deg > 90 || deg <= 180)
				deg = rand(90);
		else if	(deg > 180 || deg <= 270)
				deg = rand(90)+90;
		else if (deg > 270 || deg <= 359)
				deg = rand(90)+180;
		
		drive(deg,spd);
	}
	
	private void avoidWalls() {
		if ((loc_x()>950 || loc_x()<50) || (loc_y()>950 || loc_y()<50)) {spd = 15;}
		else {spd = 30;}
	}
	
	private void identifyTarget() {
		distance = scan(deg,1);
	}
	
	private void shotMissile() {
		int success = cannon(deg,distance);
		while (success == 0) {
			success = cannon(deg,distance);
		}
	}
}