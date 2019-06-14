// 13:26 - 14:08 - 42 min
//8:34 - 09:07 first version compilable and beating 4BG - 33 min
// 9:07 - 09:15 reading lead calculation - 8 min
// 9:20 - 10:34 speed prediction and target mgmt - 74 min
// 14:37 - 15:03 move improvement
// 15:05-15:27 move improvements
// 08:04 - 08:39 debug/test, improve
// 11:19 - 11:40 team mode additions
// single: 240 mins
// team: 21 mins + 20 mins + 42 mins
// total: 323 min (~5.4 hours)
public class __Arne_ extends JJRobot {

static Location[] teamPos = new Location[8];

class Location {
  int x,y;
  double vx, vy;
  double time;

  public Location(int x, int y, double time) {
    this.x = x;
  this.y = y;
  this.vx = 0;
  this.vy = 0;
  this.time = time;
  }
  
  public Location(int x, int y, double vx, double vy, double time) {
    this.x = x;
  this.y = y;
  this.vx = vx;
  this.vy = vy;
  this.time = time;
  }

  public Location getLocation(int dist, int angle) {
    return new Location(x+dist*cos(angle)/100000, y+dist*sin(angle)/100000, time);
  }
  
  public double dist(Location l) {
    return d_sqrt((l.x -x)*(l.x -x) + (l.y-y)*(l.y-y));
  }
  
  public double angle(Location l) {
    if(x == l.x) {
    if (y < l.y) return 0;
    else return 180;
  }
  //if((x-l.x) >= 0) return atan(100000*(y-l.y)/(x-l.x));
  //return atan(100000*(y-l.y)/(x-l.x)) + 180;
  return Math.atan2(l.y-y, l.x-x)*180/Math.PI;
  }
  
  public boolean near(Location l) {
    double dist = 1000;
    try {
      dist = l.dist(this);
    }
    catch(Exception e) {
      e.printStackTrace();
    }
    return dist < 10;
  }
  
  public String toString() {
    return "loc ("+x+"/"+y+"), speed ("+vx+"/"+vy+") at time " + time;
  }
}

class Bot {
  int bufferSize = 1000;
  Location[] locations = new Location[bufferSize];
  int locationCount = 0;
  
  public Bot(Location l) {
    locations[0] = l;
  locationCount=1;
  }
  
  void addPos(Location l) {
    locations[locationCount] = l;
  locationCount++;
  }
  
  Location predict(double time) { // return location at time t and the velocity
    // zero order approx
  double evx = 0;
  double evy = 0;
  for(int i = locationCount-1; i > locationCount -10 && i > 0; i--) {
    evx = (lastKnownPos().x-locations[(i-1)%bufferSize].x)/(lastKnownPos().time - locations[(i-1)%bufferSize].time);
    evy = (lastKnownPos().y-locations[(i-1)%bufferSize].y)/(lastKnownPos().time - locations[(i-1)%bufferSize].time);
    //System.err.println("v based on " + ((locationCount-1)%bufferSize) + "(" + lastKnownPos() + ") vs. " + ((i-1)%bufferSize) + "(" + locations[(i-1)%bufferSize] + ") = " +evx + "/" + evy);
  }
  int px = (int)(locations[(locationCount-1)%bufferSize].x+evx*(time-lastKnownPos().time));
  int py = (int)(locations[(locationCount-1)%bufferSize].y+evy*(time-lastKnownPos().time));
  return new Location(px,py,evx,evy,time);
  }
  
  Location lastKnownPos() {
    return locations[(locationCount-1)%bufferSize];
  }
}

Bot[] targets = new Bot[32];
int targetCount = 0;
int victimIndex = -1;
int friendsCount = 0;
Location myPos;
double lastShot = -1;
double cx = -1;
double cy = -1;
static Location lockedTarget = null; // careful about race conditions, always make local ref for non atomic ops

boolean isTeamMate(Location estimate, double time) {
  
  for(int i = 0; i < friendsCount; i++) {
    if(teamPos[i] != null && time - teamPos[i].time > 0.15) {
    //System.err.println("removing stale team member " + i);
    teamPos[i] = null;
  }
  if(teamPos[i] != null && teamPos[i].near(estimate)) {
    return true;
  }
  }
  return false;
}

void updateBot(Location estimate, double time) {
  //System.err.println("checking scanned location " + estimate);
  boolean identified = false;
  for(int i = 0; i < targetCount; i++) {
    Bot target = targets[i];
  //System.err.println("target " + i + " at " + target.lastKnownPos() + " predicted at " + target.predict(time));
  if(target.predict(time).near(estimate)) {
    //System.err.println("is identified");
    target.addPos(estimate);
    identified = true;
    break;
  }
  }  
  if(!identified) {
    targets[targetCount] = new Bot(estimate);
  targetCount++;
  }
}

void updateTargets(double time) {
  for(int i = 0; i < 360; i++) {
    int dist = scan(i, 1);
  if(dist > 0) {
    Location estimate = myPos.getLocation(dist, i);
    if(!isTeamMate(estimate, time)) {
      updateBot(estimate, time);
    }
  }
  }
  //remove all stale targets
  int remainingTargets = 0;
  for(int i = 0; i < targetCount; i++) {
    if(time - targets[i].lastKnownPos().time > 0.15) {
    //System.err.println("removing stale target " + i + ", new count = " + (targetCount -1));
    for(int j = i+1; j < targetCount; j++) {
      targets[j-1] = targets[j];
    }
    targetCount--;
  }
  }
}

int pickVictim(double time) {
  Location lt = lockedTarget;
  if(lt != null) {
    //System.err.println("revalidating locked target @ " + lt + ", now = " + time);
    if ((time - lt.time) < 0.15) { // target has been seen by any team mate recently
      for(int i = 0; i < targetCount; i++) {
        if(targets[i].lastKnownPos().near(lt)) {
          //System.err.println("reconciled target with " + targets[i]);
          lockedTarget = targets[i].lastKnownPos();
          return i;
        }
        else {
          //System.err.println("target[" + i + "] @ " + targets[i].lastKnownPos() +" too far from " + lt);
          ;
        }
      }
    }
    //System.err.println("removing target lock!");
    lockedTarget = null;
  }

  int bestTargetIndex = -1;
  double distToCenter = 1000;
  int reachableBy = 0;
  double squareSweetSpotDistanceSum = 8*1000*1000;
  
  for(int i = 0; i < targetCount; i++) {
    double d2c = d_sqrt((targets[i].lastKnownPos().x-500)*(targets[i].lastKnownPos().x-500)+(targets[i].lastKnownPos().y-500)*(targets[i].lastKnownPos().y-500));
    int rb = 0;
    double ds = 0;
    for(int j = 0; j < friendsCount; j++) {
      if(teamPos[j] == null) continue; // skip dead mates
      double d2f =  d_sqrt((targets[i].lastKnownPos().x-teamPos[j].x)*(targets[i].lastKnownPos().x-teamPos[j].x)+(targets[i].lastKnownPos().y-teamPos[j].y)*(targets[i].lastKnownPos().y-teamPos[j].y));
      if(d2f < 720 && d2f > 30) rb++;
      ds += (d2f-350)*(d2f-350);
    }
    if(rb > reachableBy) {
      bestTargetIndex = i;
      distToCenter = d2c;
      squareSweetSpotDistanceSum = ds;
      //System.err.println("target["+i+"] reachable by " + rb + "bots, new target lock candidate");
    }
    else if(rb == reachableBy) {
      if(ds < squareSweetSpotDistanceSum) {
        bestTargetIndex = i;
        distToCenter = d2c;
        squareSweetSpotDistanceSum = ds;
        //System.err.println("target["+i+"] closest to sweet spot, new target lock candidate");
      }
    }
  }
  if(time > 0.1 && bestTargetIndex > -1) lockedTarget = targets[bestTargetIndex].lastKnownPos();
  return bestTargetIndex;
}

void shoot(Bot target, double time) {
  double dist = myPos.dist(target.lastKnownPos());
  double flightTime = dist/300;
  Location impact = target.predict(time+flightTime);
  //System.err.println(time + ": target at " + target.lastKnownPos() + ", estimate " + impact + " @ " + (time+flightTime));
  /*
  double a = impact.vx*impact.vx + impact.vy*impact.vy - 300*300;
  int rx = target.lastKnownPos().x-myPos.x;
  int ry = target.lastKnownPos().y-myPos.y;
  double b = rx*impact.vx+ry*impact.vy;
  double c = rx*rx+ry*ry;
  double d = b*b-4*a*c;
  double t0 = (-b-d_sqrt(d))/(2*a);
  double t1 = (-b+d_sqrt(d))/(2*a);
  double t = (t0< 0)?t1 : (t1 <0)? t0: (t0 < t1) ? t0 : t1;
  //System.err.println("target dist = " + dist + ", target speed = " + impact.vx + ", " + impact.vy + ", t = " + t);
  if(t > 0) {
  impact.x = (int)(target.lastKnownPos().x+impact.vx*t);  
  impact.y = (int)(target.lastKnownPos().y+impact.vy*t);  
  impact.time = time+t;
  
  }
  else {
    System.err.println("no positive solution for t");
  }
  */
  int angle = (int)myPos.angle(impact);
  int range = (int)myPos.dist(impact);
  //System.err.println("myPos " + myPos + ", impact at " + impact + " shooting at " + angle + " deg, dist = " + range);
  if(cannon(angle,range) == 0) {
    //System.err.println("misfire!");
  }
  lastShot = time();

}


void setHome() {
  double[] homeDist = new double[8];
  double shortest = 1000;
  double shortestInd = -1;
  int shortestX = 500;
  int shortestY = 500;
  for(int i = 0; i < 8; i++) {
    int hpx = 500+(int)(350*d_cos(deg2rad(45*i)));
  int hpy = 500+(int)(350*d_sin(deg2rad(45*i)));
  homeDist[i] = d_sqrt((myPos.x-hpx)*(myPos.x-hpx)+(myPos.y-hpy)*(myPos.y-hpy));
  if(homeDist[i] < shortest) {
    shortest = homeDist[i];
    shortestInd = i;
  }
  }
  if(shortestInd > -1) {
    cx = shortestX;
  cy = shortestY;
  }
}

int CIRCLE_RAD = 20;
void chooseDestination(double time) {
  
  if(cx == -1) setHome();
  double deg = myPos.angle(new Location(500,500, time));
  //System.err.println("angle to center = " + deg + " from loc " + myPos);
  if(friendsCount == 1) {
    cx = myPos.x+CIRCLE_RAD*cos((int)(deg))/100000;
    cy = myPos.y+CIRCLE_RAD*sin((int)(deg))/100000;
  }
  if(cx < 100) cx = 100;
  if(cx > 900) cx = 900;
  if(cy < 100) cy = 100;
  if(cy > 900) cy = 900;
  //System.err.println("center at " + cx + "," + cy);
}

void move(double time) {
  if(cx == -1) {
    chooseDestination(time);
    //System.err.println("dest = " + cx + ", " + cy);
  }
  double rx = cx-myPos.x;
  double ry = cy-myPos.y;
  double r = d_sqrt(rx*rx+ry*ry);
  //System.err.println("dist = " + r);
  double angle = myPos.angle(new Location((int)cx,(int)cy,time)) +90;
  //System.err.println("angle = " + angle + " from " + myPos + " to " + cx + "," + cy);
  if(r < CIRCLE_RAD) angle+=10;
  else if(r > CIRCLE_RAD) angle-=10;
  
  drive((int)angle, 49);
}

  void main() {
    friendsCount = getFriendsCount();
    while(true) {
      double time = time();
      if(myPos == null || time - myPos.time > 0.1) {
        myPos = new Location(loc_x(), loc_y(), time);
        teamPos[id()] = myPos;
        updateTargets(time);
        victimIndex = pickVictim(time);  
        move(time);
      }
    
      if(targetCount > 0 && (time=time())-lastShot >= 1 && victimIndex > -1 ) {
        shoot(targets[victimIndex], time);
      }
    }
  }

}
