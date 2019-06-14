// by Chrisitan Weiss, Senior NodeJS developer candidate April 2018
public class __Weiss_ extends __TkPedro_ {

    int currentCourse;
    int scanCourse;
    int currentSpeed;

    public __Weiss_() {

    }

    //first thoughts
    //initialize
    //set mode to "sniper"
    //scan for enemy
    //no enemy found
    //after 30 seconds: if nothing happens: set mode to "hunting"
    //enemy found
    //attack

    private void init() {
        this.currentCourse = 10;
        this.currentSpeed = 50;
    }

    public void main() {
        this.init();

        while(true) {
            this.comandant();
        }
    }

    //global strategy goes here
    private void comandant() {
        this.spotter();
        this.navigator();

    }

    //navigation strategy goes here
    private void navigator() {

        System.out.println("course= "+this.currentCourse);

        this.drive(this.currentCourse, this.currentSpeed);

/*
        //test effect of waiting loops
        double var7 = this.time();
        while(this.time() - var7 < 2.0D) {
            ;
        }
*/
        //stop if you reach the border
        if (this.loc_x() < 50 || this.loc_x() > 950 || this.loc_y() < 50 || this.loc_y() > 950) {
            //slow down
            this.currentSpeed = 50;

            //calculate new direction
            this.currentCourse = this.currentCourse + 90;
            if (this.currentCourse > 360) {
                this.currentCourse = this.currentCourse - 360;
            }

            //change direction
            this.drive(this.currentCourse, this.currentSpeed);

        }


    }

    //artillery strategy goes here
    private void canoneer() {

    }

    //spotter strategy goes here
    private void spotter() {
        int distanceToTarget;
        int distanceToTarget2;
        int var1;
        boolean isMoving;

        this.scanCourse = 0;
        System.out.println( this.time() + " initial time");

        //initial scan
        distanceToTarget = this.scan(this.scanCourse, 20);
        System.out.println("Initial. enemyDistance="+ distanceToTarget +" Direction="+ this.scanCourse);

        System.out.println(this.time() + " pan scan time");
        while (distanceToTarget == 0 && this.scanCourse < 360) {
            this.scanCourse = this.scanCourse +20;
            distanceToTarget = this.scan(this.scanCourse, 20);
            System.out.println("enemyDistance="+ distanceToTarget +" Direction="+ this.scanCourse);
            System.out.println(this.time() + " pan scan time");
            if (this.scanCourse > 360) {
                this.scanCourse = this.scanCourse - 360;
            }
        }

        //detected section:
        int currentDirection = this.scanCourse-10; //start direction of section
        int sectionEnd = this.scanCourse+10;
        int newDistance = 0;
        boolean enemyFineScanConfirmed = false;

        System.out.println(this.time() + " start fine scan time");

        while (enemyFineScanConfirmed == false && currentDirection <= sectionEnd) {
            System.out.println("Performing scan at direction="+ currentDirection );
            newDistance = this.scan(currentDirection, 1);
            if (newDistance > 0) {
                System.out.println("Confirmed via fine scan! Distance="+ newDistance +" exactDirection="+ currentDirection);
                System.out.println(this.time() + " fine scan confirmed time");
                enemyFineScanConfirmed = true;
            }
            currentDirection += 1;
        }

        //add this to the event log (TBD: implement event-log)
        int myPositionX1 = this.loc_x();
        int myPositionY1 = this.loc_y();
        int myDirection1 = this.currentCourse;
        int mySpeed1 = this.currentSpeed;

        //time to travel 1m:
        //full speed 1/30s
        //75%  speed 1/22.5s
        //50%  speed 1/15s
        //25%  speed 1/7.5s

        System.out.println(this.time() + " start waiting");

        //experiment: wait a tiny friction of time to let the enemy tank move before performing a second scan
        double var7 = this.time();
        while(this.time() - var7 < 2.0D) {
           ;
        }

        System.out.println(this.time() + " end waiting time");

        //(second ping) probe if is moving
        distanceToTarget2 = this.scan(currentDirection-1, 20);
        if (newDistance == distanceToTarget2) {
            isMoving = false;
            System.out.println("Target is NOT moving!");
        } else {
            System.out.println("Target is moving!");
        }
        System.out.println("Second ping=" + distanceToTarget2);

        //TODO: (shoult go to the cannoneer method)
        this.cannon(this.scanCourse, newDistance); //just to have some "random" boom ;-)
    }

}