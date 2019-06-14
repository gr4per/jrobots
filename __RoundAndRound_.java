// by Stefan Saftic Senior NodeJS developer candidate Jul 2018
import java.util.ArrayList;
public class __RoundAndRound_ extends JJRobot {

	private int driveAngle = 0;
    private int driveChangeSpeed = 49;
    private int driveMaxSpeed = 100;
	private int scanAngle = 0;
    private int scanRes = 20;
    private int scanResAlpha = 0;
    private int[] scanResOptions = {20,10,5,1};
    private int changeAngleTime = 5;
    private double lastChangeTime = 0;
    private ArrayList<JJVector> enemyVecs = new ArrayList();
    private JJVector meVec = new JJVector();
    private JJVector savedTargVec = new JJVector();
    private int savedAngle = 0;
    private int savedDepth = 700;
    private int missileSpeed = 300;

	private final int fieldSize = 1000;
	private final int dangerZone = 100;
	
	@Override
	void main() {
        driveAngle = rand(360);
		while (true) {
			steerRobot();
			scanProgresiveAndShoot();
		}
		
	}

	void steerRobot() {
		
		int x = loc_x();
		int y = loc_y();
        int driveAngleTmp = 0;
        if(damage()>=70){
            changeAngleTime = 2;
        }
        boolean x_left = (x <= dangerZone && driveAngle > 90 && driveAngle < 270);
        boolean x_right = ((fieldSize-x) <= dangerZone && (driveAngle < 90 || driveAngle > 270));
        boolean y_top = (y <= dangerZone && driveAngle>180);
        boolean y_bottom = ((fieldSize-y) <= dangerZone && driveAngle < 180);
		if ( x_left || x_right || y_top	|| y_bottom ) {
            if(speed()<=driveChangeSpeed){
                if(x_left){
                    if(y_top || y_bottom){
                        if(y_top){
                            driveAngle = 45;
                        }
                        driveAngle = 275;
                    }else{
                        driveAngle = 0;
                    }
                }else{
                    if(x_right){
                        if(y_top || y_bottom){
                            if(y_top){
                                driveAngle = 225;
                            }
                            driveAngle = 135;
                        }else{
                            driveAngle = 180;
                        }
                    }
                    else{
                        if(y_top){
                            driveAngle = 90;
                        }else{
                            driveAngle = 230;
                        }
                    }
                }
                drive(driveAngle, driveMaxSpeed);
                lastChangeTime = time();
            }else{
                drive(driveAngle, driveChangeSpeed);
            }
		}else{
            if((time()-lastChangeTime)<changeAngleTime){
                drive(driveAngle, driveMaxSpeed);
            }else{
                if(speed()<=driveChangeSpeed){
                    lastChangeTime = time();
                    driveAngle = rand(360);
                    drive(driveAngle, driveMaxSpeed);
                }else{
                    drive(driveAngle, driveChangeSpeed);
                }
            }
        }
	}

	void scanProgresiveAndShoot() {
        meVec = new JJVector(d_loc_x(),d_loc_y());
        scanAndShoot();
        int history = enemyVecs.size()-1;
        if(history>=2){
            JJVector localLastEnemyVector = enemyVecs.get(history);
            meVec = new JJVector(d_loc_x(),d_loc_y());
            JJVector localDistanceVector = localLastEnemyVector.minus(meVec);
            history--;
            JJVector localRefEnemyVector = enemyVecs.get(history);
            double deltaTime = localLastEnemyVector.t() - localRefEnemyVector.t();
            while(deltaTime<0.5 && history>=0){
                localRefEnemyVector = enemyVecs.get(history);
                deltaTime = localLastEnemyVector.t() - localRefEnemyVector.t();
                history--;
            }
            if(deltaTime >= 0.5){
                JJVector localVelVector = localLastEnemyVector.minus(localRefEnemyVector);
                localVelVector.set_t(deltaTime);
                JJVector localVelMultVector = localVelVector.velocity();
                double timeToImpact = localDistanceVector.mag()/missileSpeed;
                JJVector localLastEnemyVectorProcessed =  localLastEnemyVector.plus(localVelMultVector.mult(timeToImpact));
                savedTargVec = localLastEnemyVectorProcessed.minus(meVec);
                double cannon_distance = (int)(savedTargVec.mag());
                if(cannon_distance > 20){
                    if(cannon_distance<700){
                        cannon((int)(savedTargVec.angle()), (int)(savedTargVec.mag()));
                    }else{
                        cannon((int)(savedTargVec.angle()), 700);
                    }
                }else{
                    cannon((int)(savedTargVec.angle()), 41);
                }
            }else{
                cannon(savedAngle, savedDepth);
            }
        }else{
            cannon(savedAngle, savedDepth);
        }
    }
    void scanAndShoot() {
		int d;
		
		d = scan(scanAngle, scanRes);
		if (d==0) {
            scanAngle += scanRes;
            if(scanResAlpha>0){
                scanResAlpha--;
                scanRes = scanResOptions[scanResAlpha];
            }
		} else { 
            if (d<700) {
                savedAngle = scanAngle;
                savedDepth = d;
            }else{
                if(d<20) {
                    savedAngle = scanAngle;
                    savedDepth = 41;
                }else{
                    savedAngle = scanAngle;
                    savedDepth = 700;
                }
            }
            JJVector tmpEnemyVec = JJVector.Polar(d, scanAngle);
            tmpEnemyVec = meVec.plus(tmpEnemyVec); 
            tmpEnemyVec.set_t(time());
            if(enemyVecs.size()==0 || tmpEnemyVec.t() - enemyVecs.get(enemyVecs.size()-1).t()>=0.25){
                enemyVecs.add(tmpEnemyVec);
            }
            if(enemyVecs.size()>100){
                enemyVecs.remove(0);
            }
            if(scanResAlpha<scanResOptions.length-1){
                scanResAlpha++;
                scanRes = scanResOptions[scanResAlpha];
            }
            
        }
	}
}