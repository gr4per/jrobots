public class __LLima_ extends JJRobot {	

	int scanAngle=0;
	double lastShotTime=0.0;
	JJVector targetCurrPosition, targetPreviousPosition;

	static JJVector[] teamPos = new JJVector[8];
	  
	void main() {
		
		//Insert the robot position on the array (for team purposes)
		teamPos[id()] = getRobotPosition();
			
		//Scan to find the/a target's position
		targetCurrPosition = scanTarget();	

		//If it is not in single mode - fire the target
		if(getFriendsCount()!=1)
			fire(targetCurrPosition);
	
		while(true)
		{
			//Update the robot position on the array (for team purposes)
			teamPos[id()] = new JJVector(d_loc_x(), d_loc_y());
			
			// Validate if the cannon had time to reload -if it shoot before-, otherwise repeat until it did
			if (lastShotTime!=0.0 && lastShotTime + 1.0 > time()) {
				continue;
			}
			
			//Map the target position to the previous one, as a new one will be calculated
			targetPreviousPosition = targetCurrPosition;
			
			//Get the new target position
			targetCurrPosition = scanTarget();
			
			//Validate if it is in single mode
			if(getFriendsCount()==1)
			{
				//Fire the predicted next position
				fire(getTargetNextPosition());			
			}
			else
			{
				//Fire the position found
				fire(targetCurrPosition);	
			}
			
			//Move around
			move();
		}
	
	}
	
	JJVector getRobotPosition() {
		return new JJVector(d_loc_x(), d_loc_y(), time());
	}
	
	JJVector getTargetPosition(int scanAngle, int distance, JJVector robotPosition) {
		// Calculate the target's x coordenate by summing to the robot's x coordenate the cos of the angle multiplied by the distance 
		double xT = robotPosition.x() + distance * d_cos(deg2rad(scanAngle));
		// Calculate the target's y coordenate by summing to the robot's y coordenate the sin of the angle multiplied by the distance 
		double yT = robotPosition.y() + distance * d_sin(deg2rad(scanAngle));
		
		return new JJVector(xT, yT, time());
	}
	
	JJVector scanTarget() {
		int i = 0;
		while (true) {
			
			//Scan the map in the smallest resolution (1) for the defined angle
			i = scan(scanAngle, 1);
			
			//If the difference is more than 700 meters, get closer to the target
			while(i>700)
			{
				//Get closer to the target
				drive(scanAngle, 50);
				//Scan the map in the smallest resolution (1) for the defined angle
				i = scan(scanAngle, 1);
			}
			
			//If a target was identified, get its position
			if (i != 0) {

				//Get the robot's current position
				JJVector robotPosition = getRobotPosition();
				
				//Calculate the target position
				JJVector target = getTargetPosition(scanAngle, i, robotPosition);
				
				if (!isTeamMate(target)) {
				  return target;
				}
	
			}
			//Otherwise - increment the angle
			else
			{
				if(scanAngle<360)
					scanAngle += 1;
				else
					scanAngle=0;
			}

		}
	}
	
	void fire(JJVector target) {
		//Get the robot's current position
		JJVector myPosition = getRobotPosition();
		
		//Calculate the distance to the target 
		int i = i_rnd(myPosition.dist(target));
		
		//Calculate the angle in relation to the target
		int j = i_rnd(target.minus(myPosition).angle());

		//If it is not close to cause damage to robot - fire the target
		if(i>=40)
		{
			//Fire
			cannon(j, i);
			//Map the last shot time
			lastShotTime=time();
		}

	}
	
	JJVector getTargetNextPosition() {
		//Calculate the missile flight time - from the robot's current position to the target position
		double flightTime  = getRobotPosition().dist(this.targetCurrPosition) / 300.0;

		//Calculate the robot's current direction and real speed
		JJVector velocity = this.targetCurrPosition.minus(this.targetPreviousPosition).velocity();

		return this.targetCurrPosition.plus(velocity.mult(flightTime));
	}
	
	void move()
	{
		
		//Get robot position
		JJVector robotPosition = getRobotPosition();
		
		//Randomize one edge
		JJVector destinationPosition = new JJVector(rand(1000),rand(1000));
		
		//If close to the walls, limit the randomization
		if(robotPosition.x()<50)
			destinationPosition = new JJVector(1000,rand(1000));
		else if(robotPosition.x()> 950)
			destinationPosition = new JJVector(0,rand(1000));
		else if(robotPosition.y()> 950)
			destinationPosition = new JJVector(rand(1000),0);
		else if(robotPosition.y()< 50)
			destinationPosition = new JJVector(rand(1000),1000);
		
		JJVector direction = destinationPosition.minus(robotPosition);
		
		//Move in a random way to avoid prediction - not over 50%, otherwise can't change direction
		drive((int)direction.angle(), 50);
	}
	
	boolean isTeamMate(JJVector target) {
		//If in single mode - return false
		if(getFriendsCount()==1)
			return false;
		else
		{
			//Otherwise - try to compare the target with the team positions and declare team mate if closer than 40
			for (int i = 0; i < getFriendsCount(); i++) {				
				if ((i != id()) && (teamPos[i] != null) && 
					(teamPos[i].dist(target) < 40.0)) {
					return true;
				}

			}
		}
		return false;
	}

}