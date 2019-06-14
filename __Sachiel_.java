import java.util.ArrayList;

/* // by Tobias Eckhardt, Senior NodeJS Developer Candidate Jan 2018
Next steps:

- keep track of losing health and estimate next shot. Then always change direction
- use greater periods of time for estimating enemy movement (too small periods could exaggerate precision errors)
*/
public class __Sachiel_ extends JJRobot {
    private static final int missileSpeed = 300;
    private static final int maxEnemyPositions = 1000;
    private static final int maxNavigationSpeed = 100;
    public static final int maxShootingDistance = 700;
    public static final int explosionRadius = 40;
    public static final int maxScanAngleWidth = 20;
    public static final double aimingLeadEstimationPeriod = 0.5;

    private ArrayList<JJVector> enemyPositions = new ArrayList<JJVector>();
    private ArrayList<Double> enemyPositionsTimes = new ArrayList<Double>();

    private int ownMovementDirection = 0;

    private JJVector[] edges = new JJVector[4];
    private int currentEdge = 0;


    void main() {
        this.currentEdge = rand(4);

        this.edges[0] = new JJVector(explosionRadius, explosionRadius);
        this.edges[1] = new JJVector(explosionRadius, 960);
        this.edges[2] = new JJVector(960, 960);
        this.edges[3] = new JJVector(960, explosionRadius);

        while (true) {
            this.doScan();
            this.doFire();
            this.doMove();
        }
    }

    private void doMove() {
        // move in a wide circle around the battlespace
        JJVector ownPosition = this.getOwnPosition();

        JJVector destinationEdge = this.edges[this.currentEdge];
        if (ownPosition.dist(destinationEdge) <= 100) {
            // next edge
            this.currentEdge = (this.currentEdge + 1) % 4;
            destinationEdge = this.edges[this.currentEdge];
        }
        // drive in the direction of that edge
        JJVector directionVector = destinationEdge.minus(ownPosition);
        int direction = (int) directionVector.angle();
        this.ownMovementDirection = direction;
        this.drive(direction, maxNavigationSpeed);
    }

    private JJVector getOwnPosition() {
        return new JJVector(this.d_loc_x(), this.d_loc_y(), 0);
    }

    private void doFire() {
        JJVector ownPosition = this.getOwnPosition();
        JJVector estimatedEnemyPosition = this.estimateEnemyPosition();
        if (null != estimatedEnemyPosition) {
            double distanceToEstimatedEnemyPosition = estimatedEnemyPosition.dist(ownPosition);
            if (distanceToEstimatedEnemyPosition >= explosionRadius && distanceToEstimatedEnemyPosition <= maxShootingDistance + explosionRadius) {
                JJVector enemyVector = estimatedEnemyPosition.minus(ownPosition);
                this.cannon((int) enemyVector.angle(), (int) enemyVector.mag());
                return;
            }
        }
        // just shoot somewhere :D
        this.cannon(this.ownMovementDirection, maxShootingDistance);
    }

    private JJVector estimateEnemyPosition() {
        if (this.enemyPositions.size() < 2) {
            return null;
        }

        int lastElementIdx = this.enemyPositions.size() - 1;
        JJVector lastEnemyPosition = this.enemyPositions.get(lastElementIdx);

        // XXX Cheap cop-out. My bot can't lead the shot :(
        if (lastEnemyPosition != null) {
            return lastEnemyPosition;
        }

        int compareIdx = 0;
        double currentTime = time();
        for (int i = lastElementIdx - 1; i >= 0; --i) {
            if (currentTime - this.enemyPositionsTimes.get(i) >= aimingLeadEstimationPeriod) {
                compareIdx = i;
                break;
            }
        }
        JJVector enemyPositionToCompare = this.enemyPositions.get(compareIdx);
        JJVector enemyMotion = lastEnemyPosition.minus(enemyPositionToCompare);
        double movementTime = this.enemyPositionsTimes.get(lastElementIdx) - this.enemyPositionsTimes.get(compareIdx);

        // normalized enemy direction
        JJVector v = new JJVector(enemyMotion.x(), enemyMotion.y(), movementTime).velocity();

        // From docs: estimate flight time
        JJVector ownPosition = this.getOwnPosition();
        JJVector d = lastEnemyPosition.minus(ownPosition);
        // double roughEstimatedFlightTime = d.mag() / missileSpeed;
        // t = ( sqrt(3002 (Dx2 + Dy2) - (DxVy - DyVx)2) + (DxVx + DyVy) ) / (3002 - (Vx2 + Vy2) )

        double flightTime = (d_sqrt(d_sqr(missileSpeed) * (d_sqr(d.x()) + d_sqr(d.y())) - d_sqr((d.x() * v.y() - d.y() * v.x())))
                + (d.x() * v.x() + d.y() * v.y())) / (d_sqr(missileSpeed) - d_sqr(v.x()) + d_sqr(v.y()));
        JJVector estimatedMovement = v.mult(flightTime);
        JJVector estimatedEnemyPosition = lastEnemyPosition.plus(estimatedMovement);
        return estimatedEnemyPosition;
    }

    private double d_sqr(double v) {
        return v * v;
    }

    private void doScan() {
        int scanAngle = 0;
        while (scanAngle < 360) {
            int distance = this.scan(scanAngle, maxScanAngleWidth);
            if (0 != distance) {
                // refine angle. we could use binary search, but for new, just try each of 20 degrees in order.
                for (int angleOffset = 0; angleOffset < maxScanAngleWidth; ++angleOffset) {
                    int preciseAngle = scanAngle - maxScanAngleWidth / 2 + angleOffset;
                    if (0 != (distance = scan(preciseAngle, 1))) {
                        scanAngle = preciseAngle;
                        break;
                    }
                }

                JJVector enemyVector = JJVector.Polar(distance, scanAngle);
                JJVector newEnemyPosition = enemyVector.plus(this.getOwnPosition());
                this.enemyPositions.add(newEnemyPosition);
                this.enemyPositionsTimes.add(this.time());

                if (this.enemyPositions.size() > maxEnemyPositions) {
                    this.enemyPositions.remove(0);
                    this.enemyPositionsTimes.remove(0);
                }

                return;
            }
            scanAngle += maxScanAngleWidth;
        }
    }
}
