// by Dennis Bücker, Senior NodeJS developer candidate
public class __PlayerOne_ extends JJRobot {
	
	/**
	 * Static array for sharing the locations
	 * of teammates.
	 * 
	 * TODO Check why usage of JJVector[] throws (thread safety ...)
	 */
	private static int friendsX[] = new int[8];
	private static int friendsY[] = new int[8];
	
	/**
	 * Game constants.
	 */
	private final int PLAYGROUND_SIZE          = 1000;
	private final int PLAYGROUND_SAFETY_MARGIN = 150;
	
	private final int MISSILE_RANGE            = 700;
	private final int MISSILE_SAFETY_RANGE     = 50;
	
	private final double V_MAX          	   = 30.0;
	
	private final double TIME_INTERVAL         = 0.5;
	
	/**
	 * Game state
	 */
	private int driveAngle 		= 0;
	private int driveSpeed 		= 100;
	
	private int scanAngle 		= 0;
	private int scanResolution 	= 1;
	
	private boolean shouldChangeCourse = false;
	private boolean shouldFire         = false;
	
	private JJVector savedTargetLocation   = new JJVector();
	private JJVector currentTargetLocation = null;
	
	private double savedTime   = 0.0;
	private double currentTime = 0.0;
	
	private int savedDamage = 0;
	
	@Override
	void main() {
		/* Start in random direction. */
		goRandom();
		
		/* Main loop. */
		while (true) {			
			this.currentTime = time();
			
			updateSharedLocation();
			doIntervalJobs();
			
			scanForTarget();
			shoot();
			
			go();
			
			checkSelfDestruction();
		}		
	}

	void checkSelfDestruction() {
		if (damage() >= 90) {
			/* Disconnect this bot from the team */
			friendsX[id()] = -1;
			friendsY[id()] = -1;
		}
		
		/* TODO destroy myself. */
	}
	
	private boolean checkWallCollision() {
		int x = loc_x();
		int y = loc_y();
		
		boolean collisionDetected =    ((PLAYGROUND_SIZE - x) <= PLAYGROUND_SAFETY_MARGIN && (driveAngle < 90 || driveAngle > 270)) // right border 
									|| ((PLAYGROUND_SIZE - y) <= PLAYGROUND_SAFETY_MARGIN && driveAngle < 180)				      // bottom border
									|| (x <= PLAYGROUND_SAFETY_MARGIN && driveAngle > 90 && driveAngle < 270)					      // left border
									|| (y <= PLAYGROUND_SAFETY_MARGIN && driveAngle > 180);												  // top border
									
		return collisionDetected;
	}

	private void doIntervalJobs() {
		if (this.currentTime - this.savedTime >= TIME_INTERVAL) {
			saveCurrentTime();
			saveTargetLocation();

			/* One second has elapsed, so trigger the shooting routine */
			this.shouldFire = true;
			
			/* Trigger new course calculation if damage is above threshold for the given time */
			this.shouldChangeCourse = ((damage() - this.savedDamage) >= 15) ? true : false;
			saveCurrentDamage();
		}
	}
	
	/**
	 * Controls the bot movement.
	 * 
	 * TODO Introduce drive modes (eg. escape mode) to act on different enemy behaviours.
	 */
	void go() {
		if (this.shouldChangeCourse || checkWallCollision())  {
			int rotation = (rand(1) % 2 == 0) ?  45 : -45;
			
			this.driveAngle = (this.driveAngle + rotation) % 360;
			this.driveSpeed = 49;
			
			drive(this.driveAngle, this.driveSpeed);
			
			this.shouldChangeCourse = false;
		} else {
			this.driveSpeed = 100;
			drive(this.driveAngle, this.driveSpeed);
		}
	}
	
	void goRandom() {
		this.driveAngle = rand(360);
		drive(this.driveAngle, this.driveSpeed);
	}
	
	private boolean isFriend(double targetX, double targetY) {
		int friendsCount = getFriendsCount();

		if(friendsCount > 1) {
			
			for(int i = 0; i < friendsCount; i++) {
				if(i != id()) {
					
					if (friendsX[i] >= 0 && friendsY[i] >= 0) {
						int dx = (int)(targetX - friendsX[i]);
						int dy = (int)(targetY - friendsY[i]);
						
						int r = sqrt(dx*dx + dy*dy);
						
						if (r < V_MAX) {
							return true;
						}
					}
				}
			}
			
		}
		
		return false;
	}
	
	private void scanForTarget() {
		int distance = scan(this.scanAngle, this.scanResolution);
		
		if (distance == 0) {
			/* Nothing found -> change angle for consecutive scans. */
			this.scanAngle = this.scanAngle + this.scanResolution;
			
			this.currentTargetLocation = null;
		} else {
			/* Target found. */		
			double targetX = loc_x() + distance * cos(this.scanAngle)/100000.0;
			double targetY = loc_y() + distance * sin(this.scanAngle)/100000.0;
			
			if (isFriend(targetX, targetY)) {
				this.currentTargetLocation = null;
				this.scanAngle = this.scanAngle + 10; // Move scanner forward.
			} else {
				this.currentTargetLocation = new JJVector(targetX, targetY, this.currentTime);
			}
			
		}
	}
	
	private void saveTargetLocation() {
		if(this.currentTargetLocation != null) {
			this.savedTargetLocation.set(this.currentTargetLocation);
		}
	}
	
	private void saveCurrentTime() {
		this.savedTime = this.currentTime;
	}
	
	private int saveCurrentDamage() {
		return this.savedDamage = damage();
	}

	private void shoot() {
		if (this.shouldFire == false) return;
		
		JJVector myPosition = new JJVector(loc_x(), loc_y(), this.currentTime);
		
		double targetDistance = 0.0;		
		if (this.currentTargetLocation != null) {
			/* Calculate the distance to the target if the scanner found one */
			targetDistance = this.currentTargetLocation.dist(myPosition);
		}
		
		if (targetDistance < MISSILE_RANGE && targetDistance > MISSILE_SAFETY_RANGE) {
			/* Estimate the future position  of the enemy. */
			JJVector targetVelocity = this.currentTargetLocation.minus(this.savedTargetLocation).velocity();
		    JJVector futureTargetLocation = this.currentTargetLocation.plus(targetVelocity.mult(targetDistance / 300.0));
		    
		    /* Convert estimation to cannon() parameters. */
		    double shootingDistance = futureTargetLocation.dist(myPosition);
		    double shootingAngle = (futureTargetLocation.minus(myPosition)).angle();
		    
			cannon((int) shootingAngle, (int) shootingDistance);
		}
	}

	private void updateSharedLocation() {
		if(getFriendsCount() > 1) {
			friendsX[id()] = loc_x();
			friendsY[id()] = loc_y();
		}
	}

}
