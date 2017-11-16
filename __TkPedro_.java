import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;

public class __TkPedro_ extends JJRobot {

    private double timeInInt = 0;

    //enemy position and velocity [0] = x [1] = y
    private double[] enemyLoc = new double[2];
    private double[] enemyVelocity = new double [2];
    private JJVector targetVector = new JJVector();
    private double rocketFlightTime;

    private int enemyDegree = -1;
    private int enemyDistance = -1;


    private boolean happyLittleDance = false;
    File dancingSteps = new File("dancingSteps.txt");

    void main() {
        enemyLoc = calculateEnemyPos();
        calculatedEnemyDestination();

        while(true) {
            checkForMaster();
            calculatedEnemyDestination();
            shootEnemy();
        }
    }

    //calculating distance enemy traveled from oldPos to newPos in clockTimerInMs duration: v = s / t
    //t corresponds time between position calculations
    // value is equivalent to m/s assumed the field is 1000x1000 meters
    private double [] calculateEnemyPos(){
        double[] position = new double[2];
        if(enemyDegree < 0 || enemyDistance < 250){
            for (int i = 0; i < 360; i++) {
                if (scan(i,1) > 0){
                    position[0] = loc_x()+(scan(i,1)*Math.cos(Math.toRadians(i)));
                    position[1] = loc_y()+(scan(i,1)*Math.sin(Math.toRadians(i)));
                    enemyDegree = i;
                    enemyDistance = scan(i,1);
                    break;
                }
            }
        }else{
            int scanLoopStart = enemyDegree - 10;
            for (int i = scanLoopStart; i < (scanLoopStart +20); i++) {
                if (scan(i,1) > 0){
                    position[0] = loc_x()+(scan(i,1)*Math.cos(Math.toRadians(i)));
                    position[1] = loc_y()+(scan(i,1)*Math.sin(Math.toRadians(i)));
                    enemyDegree = i;
                    enemyDistance = scan(i,1);
                    break;
                }
            }
        }
        return position;
    }

    //calculating enemyVelocity every second out of two positions
    //time the rocket needs to travel to the estimated future location of the enemy
    //setting a vector as target enemy location
    private void calculatedEnemyDestination(){
        if((timeInInt + 1) < (int) time()){
            timeInInt++;
            double[] enemyLocOld = enemyLoc;
            enemyLoc = calculateEnemyPos();

            enemyVelocity[0] = Math.sqrt((Math.pow(enemyLoc[0]- enemyLocOld[0],2)))*3.4;
                if(enemyLoc[0] < enemyLocOld[0]){enemyVelocity[0] = enemyVelocity[0]*-1;}
            enemyVelocity[1] = Math.sqrt((Math.pow((enemyLoc[1]- enemyLocOld[1]),2)))*3.4;
                if(enemyLoc[1] < enemyLocOld[1]){enemyVelocity[1] = enemyVelocity[1]*-1;}

            double dx = enemyLoc[0] - loc_x();
            double dy = enemyLoc[1] - loc_y();
            rocketFlightTime = ( sqrt((int)(Math.pow(300,2)*(Math.pow(dx,2) + Math.pow(dy,2)) - Math.pow((dx *enemyVelocity[1] - dy *enemyVelocity[0]),2) + (dx *enemyVelocity[0] + dy *enemyVelocity[1]) )) / (Math.pow(300,2) - (Math.pow(enemyVelocity[0],2) + Math.pow(enemyVelocity[1],2))));

            //estimated enemy location x = (enemyLoc[0]+enemyVelocity[0*rocketFlightTime)
            //estimated enemy location y = (enemyLoc[1]+enemyVelocity[1*rocketFlightTime)
            targetVector.set((enemyLoc[0]+enemyVelocity[0]*rocketFlightTime)-loc_x(),(enemyLoc[1]+enemyVelocity[1]*rocketFlightTime)-loc_y());
        }
    }

    private void shootEnemy(){
        //shooting towards targetVector angle, at the estimated distance the enemy will be
        if(enemyDistance > 40 && enemyDistance < 730) {
            cannon((int) targetVector.angle(), (int) Math.sqrt((Math.pow(((enemyLoc[0] + enemyVelocity[0] * rocketFlightTime) - loc_x()), 2)) + (Math.pow(((enemyLoc[1] + enemyVelocity[1] * rocketFlightTime) - loc_y()), 2))));
        }
    }

    private void selfDestruction(int i) {
        if(damage()<100){
            cannon(i, 35);
            try {
                Thread.sleep(111);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            selfDestruction(i+45);
        }
    }
//reading file dancingSteps, initialising self destruction if available
    private void checkForMaster() {
        if(dancingSteps.exists()){
            happyLittleDance = true;
            try {
                PrintWriter writer = new PrintWriter(dancingSteps, "UTF-8");
                writer.write("DONE");
                writer.close();
                dancingSteps.delete();
                selfDestruction(0);
            } catch (FileNotFoundException | UnsupportedEncodingException e) {
            }
        }
    }
}