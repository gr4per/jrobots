public class __NiceGuy_ extends JJRobot {

  static boolean[] logCategories = new boolean[10];
  static {
    logCategories[0] = true; // teamTarget
    logCategories[1] = false; // drive
    logCategories[2] = false; // enemyMissileTrack
    logCategories[3] = false; // targetPrediction
    logCategories[4] = false; // targetTracking
    logCategories[5] = false; // missileTrack
    logCategories[6] = true; // fire
  }
  
  final static int MAXRES = 20;
  final static int MAXV = 30;
  final static double MINTIME = 0.1;
  final static int STRETCH = 1000; // max x and max y
  final static int bufferSize = 100; // about 5/100 seconds per entry
  
  NGPoint[] targetPos = new NGPoint[bufferSize];
  int targetPosInd = 0;
  NGTrackItem[] track = new NGTrackItem[bufferSize]; // circular, same index as targetPos
  
  
  boolean hasTarget=false;
  int lastDamage;
  double lastTime=0;
  double lastMissileHit=0;
  
  NGMissileTarget[] activeMissiles = new NGMissileTarget[3]; // holds impact coords and time
  NGPredictionSettings predictionSettings = new NGPredictionSettings(1.0);
  int lastMissileInd = 0;
  double lastFireTime = -1;
  double lastEnemyShot = 0;
  
  int driveMode = 0; // attack, 1 = defend
  int lastHeading = 0;
  double lastDirectionChange = -1;
  static NGTrackItem[] teamPos = new NGTrackItem[8];
  static NGPoint[] teamTarget = new NGPoint[8];
  static int teamSize = 1;
  static int teamTargetLock = -1;
  boolean run = true;  
  
  void main() {
    lastDamage=damage();
    teamPos[id()] = new NGTrackItem(getPosition(), 0, 0);
    if(id()+1 > teamSize)teamSize=id()+1;
    
    while(run) {
      if(time() == lastTime) continue;
      double newTime = time();
      
      // find / track target
      NGPoint lastTargetPos = targetPos[targetPosInd];
      //System.out.println("lastTargetPos = " + lastTargetPos);
      NGPoint p = findTarget(lastTargetPos);
      if(p!=null) {
        hasTarget=true;
        JJVector diff = null;
        if(lastTargetPos == null || !((diff=lastTargetPos.getVectorTo(p)).mag() < 0.1 && (p.t - lastTargetPos.t) < MINTIME)){
          targetPosInd=(targetPosInd+1)%bufferSize;
          targetPos[targetPosInd] = p;
          teamPos[id()] = track[targetPosInd] = new NGTrackItem(getPosition(), speed()*30.0/100, lastHeading);
        }
      }
      else {
        hasTarget=false;
      }
      
      // estimate future target pos in order to fire missile
      // do we have active missiles
      if(hasTarget && time()-lastFireTime >= 1.0) {
        NGPoint impactCoords = fire();
        if(impactCoords != null) {
          lastFireTime = time();
          if(activeMissiles[lastMissileInd] != null) {
            lastMissileInd = (lastMissileInd+1)%3;
          }
          activeMissiles[lastMissileInd] = new NGMissileTarget(impactCoords, predictionSettings);
        }
      }
      
      // estimate target damage taken
      for (int i = 0; targetPos[targetPosInd] != null && i < 3; i++) {
        int missileInd = lastMissileInd-i;
        if(missileInd < 0) missileInd+=3;
        NGMissileTarget missile = activeMissiles[missileInd];
        if(missile == null ) continue;
        if(missile.impactCoords.t < time()) { // missile just exploded
          if(hasTarget) {
            // compare estimated target location with reality
            JJVector offset = new JJVector(targetPos[targetPosInd].x-missile.impactCoords.x, targetPos[targetPosInd].y-missile.impactCoords.y);
            double error = offset.mag();
            //5 meters 10 damage points
            //20 meters 5 damage points
            //40 meters 3 damage points
            int damageInflicted = 0;
            if(error < 5) {
              damageInflicted = 10;
            } else if (error < 20) {
              damageInflicted = 5;
              // slight correction
            } else if (error < 40) {
              damageInflicted = 3;
              // bigger correction
            } else {
              // we missed !
            }
            //debugOut"missile exploded " + missile.impactCoords + ", target at " + targetPos[targetPosInd] + ", error = " + error + ", damageInflicted = " + damageInflicted, 5);
          }
          activeMissiles[missileInd] = null; // remove as we expect it exploded
        }
      }
      
      // check own damage
      if(damage() != lastDamage) {
        // taken damage
        if(speed() == 0 && (loc_x() == 0 || loc_y() == 0 || loc_x() == STRETCH || loc_y() == STRETCH)) {
          // hit a wall
        }
        else {
          // hit by missile
          lastMissileHit = time() - (time()-lastTime)/2;
          //debugOut("was hit by missile", 2);
          estimateEnemyShotTime();
        }
        if(damage() > 70) {
          driveMode = 1; // defend
        }
        lastDamage = damage();
      }
      
      // guess on enemy shooting
      boolean enemyShootingNow = false;
      if(hasTarget && lastEnemyShot >= 0.0) {
        // we know at least one time when enemy was shooting
        // we can expect he was firing in one second intervals after that
        // do we have missiles for the shoot times ?
        if(enemyMissiles[enemyMissileInd] == null) {
          // no current missile, expect enemy was shooting immediately
          if(time() > 0.999) {
            lastEnemyShot = guessEnemyMissile();
            enemyShootingNow = (lastEnemyShot > -1);
          }
        } else {
          if(time() - enemyMissiles[enemyMissileInd].t > 0.999) {
            // reload time elaped since last shot we tracked
            lastEnemyShot = guessEnemyMissile();
            enemyShootingNow = true;
          }
        }
      }
      
      // drive
      if(speed() == 0 || enemyShootingNow || time() - lastDirectionChange > 1.0) {
        accelerate();
      }
      else {
        if((enemyMissiles[enemyMissileInd] != null && time() - enemyMissiles[enemyMissileInd].t > 0.999) 
          || time() - lastDirectionChange > 0.5) {
            slowDown();
          }
      }
      
      lastTime=newTime;
    }
  }

  final static double preferredAttackDist = 350;
  final static double minAttackDist = 40;
  final static double maxAttackDist = 700;
  final static double WALLFIELD = 150;
  double targetSpeed = 15;
  
  void accelerate() {
    NGPoint myPos = getPosition();
    NGPoint enemyPos = targetPos[targetPosInd];
    JJVector closeIn = myPos.getVectorTo(enemyPos);
    double dist = closeIn.mag();
    closeIn = closeIn.unit();
    JJVector clockwise = new JJVector(-closeIn.y(), closeIn.x());
    clockwise = clockwise.unit();
    
    JJVector wall1 = new JJVector(-1, 0, myPos.x);
    if(myPos.x >= STRETCH/2) wall1 = new JJVector(1, 0, STRETCH-myPos.x);
    JJVector wall2 = new JJVector(0, -1, myPos.y);
    if(myPos.y >= STRETCH/2) wall2 = new JJVector(0, 1, STRETCH-myPos.y);
    
    JJVector closestWall = wall1;
    if(wall2.mag() < wall1.mag()) closestWall = wall2;
    
    double lo = -1;
    double hi = +1;
    if(dist < preferredAttackDist) {
      // we are close
      if(dist <= minAttackDist) hi = 0;
      else 
      hi = (dist-minAttackDist)*1.0/(preferredAttackDist-minAttackDist);
    }
    else {
      // we are far
      if(dist >= maxAttackDist) {
        lo = 0;
        targetSpeed = 30;
      }
      else {
        lo = (dist-maxAttackDist)*-1.0/(maxAttackDist-preferredAttackDist);
        targetSpeed = 15;
      }
    }
    double tf = rand(100)/100.0*(hi-lo)+lo;
    JJVector towards = new JJVector(closeIn.x()*tf, closeIn.y()*tf);
    
    // if towards points to a wall, reduce its magnitude
    double cf = 1.0;
    if(towards.dot(closestWall) > 0 ) {
      // closing in on wall
      double wallDist = towards.dot(closestWall)*closestWall.t();
      if (wallDist < WALLFIELD) {
        cf = wallDist/(WALLFIELD);
        towards = towards.mult(cf);
      }
    }
    double cwm = 1-towards.mag();
    tf = rand(100)/100.0*2-1;
    JJVector sideways = new JJVector(clockwise.x()*tf*cwm, clockwise.y()*tf*cwm);
    
    cf = 1.0;
    if(sideways.dot(closestWall) > 0 ) {
      // closing in on wall
      double wallDist = sideways.dot(closestWall)*closestWall.t();
      if (wallDist < WALLFIELD) {
        // flip
        sideways = sideways.mult(-1);
      }
    }
    
    if(sideways.dot(closestWall) > 0 ) {
      // closing in on another wall
      double wallDist = sideways.dot(closestWall)*closestWall.t();
      if (wallDist < WALLFIELD) {
        cf = wallDist/(WALLFIELD);
        sideways = sideways.mult(cf);
      }
    }
    
    JJVector drive = towards.plus(sideways);
    drive(i_rnd(drive.angle()), 50);
    lastHeading=i_rnd(drive.angle());
    lastDirectionChange = time();
    //debugOut"driving " + drive.angle() + " at time " + time() + ", speed = " + speed(), 1);
  }
  
  void slowDown() {
    drive(lastHeading, 50);
  }
  
  NGPoint[] enemyMissiles = new NGPoint[3];
  int enemyMissileInd = 0;
  
  
  void estimateEnemyShotTime() {
    if(!hasTarget) return;

    // we have been hit, try to figure when enemy shot this missile
    NGPoint enemyPos = targetPos[targetPosInd];
    NGPoint myPos = getPosition();
    NGPoint[] pos = new NGPoint[]{enemyPos, myPos, null};
    double shootTime = estimatePosAtShot(pos);
    lastEnemyShot = estimatePosAtShot(pos);
    //debugOut"Expect enemy shot at " + lastEnemyShot + ", from location " + pos[0] + ", aiming at " + pos[2] + " while I was at " + pos[1], 3);
  }
  
  double estimatePosAtShot(NGPoint[] pos) {
    NGPoint myPos = pos[1];
    NGPoint enemyPos = pos[0];
    JJVector toMe = new JJVector(myPos.x-enemyPos.x, myPos.y-enemyPos.y);
    double dist = toMe.mag();
    double flightTime = dist/300.0;
    double shootTime = time()-flightTime;
    NGTrackItem myPosAtShotTime = null;
    // 1st approximation we have, but where was enemy at t - flightTime
    for(int i = 0; i < bufferSize; i++) {
      int ri = targetPosInd-i;
      if(ri<0)ri+=bufferSize;
      NGPoint cep = targetPos[ri];
      if(cep != null && cep.t < shootTime) {
        enemyPos=cep;
        myPosAtShotTime = track[ri];
        //debugOut"Enemy was at " + cep + " when shooting, I was at " + myPosAtShotTime, 2);
        break;
      }
    }
    if(myPosAtShotTime == null){
      //debugOut"no record of own position at " + shootTime, 2);
      return shootTime;
    }
    if(myPosAtShotTime == null){
      //debugOut"no record of enemy position at " + shootTime, 2);
      return shootTime;
    }
    
    NGPoint myFuturePos = new NGPoint(myPosAtShotTime.coords.x*1.0 + d_cos(deg2rad(i_rnd(myPosAtShotTime.heading))) * myPosAtShotTime.speed * flightTime,
                                  myPosAtShotTime.coords.y*1.0 + d_sin(deg2rad(i_rnd(myPosAtShotTime.heading))) * myPosAtShotTime.speed * flightTime,
                                  shootTime+flightTime);
    pos[2] = myFuturePos;
    flightTime = enemyPos.getVectorTo(myFuturePos).mag()/300.0;
    pos[0] = enemyPos;
    pos[1] = myPosAtShotTime.coords;   
    return time()-flightTime;
  }
  
  /** 
   * We expect enemy is shooting at us near now, so lets track his missile
   * returns shootTime or -1 if out of range
   */
  double guessEnemyMissile(){
    
    if(!hasTarget) return -1;
    NGPoint enemyPos = targetPos[targetPosInd];
    NGPoint myPos = getPosition();
    JJVector toMe = new JJVector(myPos.x-enemyPos.x, myPos.y-enemyPos.y);
    double dist = toMe.mag();
    if(dist > 740) return -1; // not in shooting range
    double flightTime = dist/300.0;
    double mySpeed = speed()*30.0/100.0;
    NGPoint myFuturePos = new NGPoint(myPos.x + d_cos(deg2rad(lastHeading)) * mySpeed * flightTime,
                                  myPos.y + d_sin(deg2rad(lastHeading)) * mySpeed * flightTime,
                                  time()+flightTime);
                            
                               
    enemyMissileInd = (enemyMissileInd+1)%3;
    enemyMissiles[enemyMissileInd] = myFuturePos; 
    //debugOut"expect enemy shooting now, from " + enemyPos + ", aiming at " + myFuturePos, 2);
    return myPos.t;
  }
  
  /** 
   * determines where to aim at and returns missile impact coords
   */
  NGPoint fire() {
    NGPoint myPos = getPosition();
    JJVector toTarget = myPos.getVectorTo(targetPos[targetPosInd]);
    
    // extrapolate movement of target 
    double flightTime = toTarget.mag()/300.0;
    NGPoint fp = predictTargetPos(myPos.t + flightTime);
    if(fp == null) return null;
    // let's do a second iteration to get more precise on long shots
    toTarget=myPos.getVectorTo(fp);
    flightTime = toTarget.mag()/300.0;
    fp = predictTargetPos(myPos.t + flightTime);
    
    //debugOut"predicting target to be at " + fp, 3);
    
    if(cannon(i_rnd(toTarget.angle()), i_rnd(toTarget.mag())) > 0) {
      return fp;
    }
    else {
      //debugOut"cannon not fired to angle " + toTarget.angle() + ", dist " + toTarget.mag() + "!", 6);
      return null;
    }
  }
  
  static final int EXTR_CAP = 1; // 1 second to watch enemy move
  static final int SMOOTH_SAMPLES = 3;
  NGPoint predictTargetPos(double impactTime) {
    //System.out.println("myPos = " + getPosition() + ", predicting target pos for impact time " + impactTime + "\ntarget currently at " + targetPos[targetPosInd]);
    // average target movement vector over last samples
    
    // low pass the samples
    double timeDiff = 0;
    int maxSmoothInd = 0;
    JJVector[] smoothed = new JJVector[bufferSize];
    double avx = 0.0, avy = 0.0;

    for(int i = 0; i < bufferSize-SMOOTH_SAMPLES && timeDiff < EXTR_CAP; i++) {
      double sx, sy, st;
      int ri = targetPosInd-i;
      if(ri < 0) ri+=bufferSize; // circular
      //System.out.println(targetPos[ri]);

      int roi = targetPosInd-i-SMOOTH_SAMPLES;
      if(roi < 0) roi+=bufferSize; // circular
      if(targetPos[ri] == null) return null; // no tracking info yet      
      if(targetPos[roi] == null) return null; // no tracking info yet
      sx = targetPos[ri].x-targetPos[roi].x;
      sy = targetPos[ri].y-targetPos[roi].y;
      st = targetPos[ri].t-targetPos[roi].t;
      smoothed[i] = new JJVector(sx,sy,st);
      double vx = smoothed[i].x()/smoothed[i].t();
      double vy = smoothed[i].y()/smoothed[i].t();
      if (vx > MAXV) vx = MAXV;
      if (vx < -MAXV) vx = -MAXV;
      if (vy > MAXV) vy = MAXV;
      if (vy < -MAXV) vy = -MAXV;
      //System.out.println("vx = " + vx + ", vy = " + vy);
      avx += vx;
      avy += vy;
      timeDiff = targetPos[targetPosInd].t-targetPos[ri].t;
      maxSmoothInd = i;
    }
    
    //debugOut"smoothed " + (maxSmoothInd+1) + " samples, timeDiff = " + timeDiff, 4);
    
    avx /= maxSmoothInd;
    avy /= maxSmoothInd;
    //debugOut"averaged target movement to " + avx + ", " + avy, 4);
    // now we know the current movement vector
    return new NGPoint(targetPos[targetPosInd].x+avx*(impactTime-targetPos[targetPosInd].t),
                     targetPos[targetPosInd].y+avy*(impactTime-targetPos[targetPosInd].t),
                     impactTime);
    
  }

 
  /** 
   * Will scan to direction of estimate with high res,
   * reduce res if not found and in worst case scan left and right 
   * until target found
   */
  NGPoint findTarget(NGPoint estimate) {
    return findTarget(estimate, true);
  }
  NGPoint findTarget(NGPoint estimate, boolean checkTeam) {
    NGPoint myPos = getPosition();
    if(estimate == null) {
      // find direction with most open space
      estimate = new NGPoint(STRETCH/2, STRETCH/2);
      if (myPos.x == estimate.x && myPos.y == estimate.y) estimate.x-=10;
    }
    JJVector etv = myPos.getVectorTo(estimate);
    //JJVector  tv = null;
    int dist = 0;
    int res = 1;
    boolean resync = (targetPos[targetPosInd] == null);
    int ang = i_rnd(etv.angle());
    while (dist == 0) {
      dist = scan(ang, res);
      //System.out.println("scanning " + ang + ", " + res);
      if(dist == 0) { 
        if(res < MAXRES) { // not found, decrease resolution
          res = (res << 1); // mult by 2
          if(res > 4) {
            resync=true; // we are not sure we still track the previous target, better check
            //if(teamSize > 1)debugOut("res = " + res + ", requesting resync", 0);
          }
          if(res > MAXRES) res=MAXRES;
          //System.out.println("decresing res");
        }
        else { // we need to scan the adjacent sectors 
          //System.out.println("scanning adjacents");
          int sign = 1;
          int ran = ang;
          if(teamSize <2) {
            for (int i = 0; i < 17 && dist == 0; i++) {
              sign*=-1;
              int sa = (ang+sign*(1+(i>>1))*MAXRES);
              while(sa < 0) sa+=360;
              //System.out.println("scanning " + sa + " with " + res);
              dist = scan(sa, res);
              ran = sa;
            }
          }
          else {
            //res = 1;
            //while( (dist = scan((ang++)%360,1)) == 0 ) {
            //  //debugOut"circle scanned: " + (ang%360) + " at res 1", 0);
            //}
            for (int i = 0; i < 18 && dist == 0; i++) {
              int sa = (ang+i*MAXRES);
              if(sa >= 360) sa-=360;
              if(sa < 0) sa+=360;
              //debugOut("large scanning: " + sa + " at res " + res, 0);
              dist = scan(sa, res);
              ran = sa;
            }
            
            ran = ang;
          }
          ang=ran;
          if (dist == 0) {
            if(damage() > 99) {
              // we are dead !
              run = false;
              throw new RuntimeException("bot is dead!");
            }
            else {
              //debugOut"target not found with large scan", 0);
              teamTarget[id()]=null;
            }
            return checkTeam ? findBestTeamTarget(estimate) :null;
          }
        }        
      }
    }
    // now we have a target (hopefully), but possibly with bad resolution
    //ang = i_rnd(rad2deg(tv.angle()));
    //System.out.println("target at " + ang + " res = " + res);
    while(res > 2) {
      res = res/2;
      if (res%2 == 1) res+=1;
      //System.out.println("close in center " + ang + " with " + res);
      dist = scan(ang, res);
      int ta = ang;
      if(dist == 0) {
        ta = ang-(2*res/3+1);
        //System.out.println("close in lhs " + ta + " with " + res);
        dist = scan(ta, res);
      }
      if(dist == 0) {
        ta = ang+(2*(res/3)+1);
        //System.out.println("close in rhs " + ta + " with " + res);
        dist = scan(ta, res);
      }
      ang = ta;
      if(dist == 0) {
        //throw new RuntimeException("lost target angle while reducing res");
        //System.out.println("lost target, decreasing res");
        res+=1;
      }
    }
    myPos = getPosition(); // get more accurate own pos to avoid time difference
    NGPoint tp = new NGPoint(myPos.x + d_cos(deg2rad(ang))*dist, myPos.y + d_sin(deg2rad(ang))*dist, time());
    //System.out.println("target found at " + ang + ", dist = " + dist + "\nmyPos = " + myPos + ", targetPos = " + tp);
    // now return target
    if(teamSize > 1 && resync) {
      while(tp != null && isFriendly(tp)) {
        //debugOut"scanned friendly target, trying 20 deg clockwise, recursing", 0);
        tp = findTarget(
          new NGPoint(myPos.x + d_cos(deg2rad(ang+20))*dist,
                    myPos.y + d_sin(deg2rad(ang+20))*dist,
                    time()));
      }
      if(tp != null) {
        //debugOut"target at " + tp + " (res=" + res + ") is not known friendly, setting as teamTarget", 0);
        teamTarget[id()] = tp;
      }
      else {
        teamTarget[id()] = null;
      }
      tp = checkTeam ? findBestTeamTarget(tp) : tp;
    }
    return tp;
  }
  
  int aliveMates = teamSize;
  
  NGPoint findBestTeamTarget(NGPoint estimate) {
    //debugOut"choosing new teamTarget, estimate own target = "+ estimate,0);
    if(teamTargetLock > -1) {
      NGPoint ct = findTarget(teamTarget[teamTargetLock], false);
      if(ct != null) {
        //debugOut"confirmed locked team target " + teamTargetLock + ", taking it", 0);
        return ct;
      }
      teamTargetLock = -1;
      //debugOut"released teamTargetLock cause target not confirmed", 0);
      return null;      
    }
    //debugOut"no team target lock, finding by least dist", 0);
    double[] distSum = new double[8];
    double lowestDistSum = 99999999;
    int lowestDistSumInd =-1;
    NGPoint chosenTarget = null;
    int count = 0;
    for(int i = 0; i < teamSize; i++) {
      if(time() > 1 && teamPos[i] != null) {
        double lastUpdate = teamPos[i].coords.t;
        if(time()-lastUpdate > 0.5) {
          //debugOut"team mate " + i + " hasn't updated position since " + lastUpdate + ", supposedly dead", 0);
          aliveMates--;
          distSum[i] = 99999999;
          continue;
        }
      }
      NGPoint currentTarget = teamTarget[i];
      if(currentTarget == null) {
        distSum[i] = 99999999;
        continue;
      }
      distSum[i] = 0;
      for(int j = 0; j < teamSize; j++) {
        NGPoint compareTarget = teamTarget[j];
        if(i==j || compareTarget==null)continue;
        distSum[i] += currentTarget.getVectorTo(compareTarget).mag();
        count ++;
      }
      //debugOut("distSum " + i + " is " + distSum[i] + " for target " + currentTarget, 0);
      if(distSum[i] < lowestDistSum) {
        lowestDistSum = distSum[i];
        lowestDistSumInd = i;
        chosenTarget = currentTarget;
        //debugOut("selecting this as target, previous target = " + targetPos[targetPosInd],0);
      }
      
    }
    // now pick lowest dist sum as target
    if(estimate != null && chosenTarget != null && chosenTarget.getVectorTo(estimate).mag() > 40) {
      // target switched, delete tracking
      //for(int i = 0; i < bufferSize; i++) {
      //  targetPos[i] = null;
      //  track[i] = null;
      //}
      //debugOut"synchronizing on target " + lowestDistSumInd, 0);
      if (count > aliveMates/2) teamTargetLock = lowestDistSumInd;
    }
    teamTarget[id()] = chosenTarget;
    /*chosenTarget = findTarget(chosenTarget);
    if(chosenTarget == null) {
      // not found, maybe died ?
      teamTarget[lowestDistSumInd] = null;
      //debugOut"teamTarget not confirmed by scanning");
    }
    */
    return chosenTarget;
  }
  
  boolean isFriendly(NGPoint estimate) {
    for(int i = 0; i < teamSize; i++) {
      if(teamPos[i] ==  null) {
        //debugOut"teamMate " + i + " hasn't published position yet", 0);
        continue;
      }
      JJVector toTarget = estimate.getVectorTo(teamPos[i].coords);
      if(toTarget.mag() < 40) {
        //debugOut"scanned target " + estimate + " seems to be team mate " + i + " at " + teamPos[i], 0);
        return true;      
      }
      else {
        //debugOut("target " + estimate + " not matching team " + i + " at " + teamPos[i], 0);
      }
    }
    return false;    
  }
  
  NGPoint getPosition() {
    return new NGPoint(d_loc_x(), d_loc_y(), time());
  }
  
  void debugOut(String message, int category) {
    if(logCategories[category])System.out.println("" + (((int)(time()*1000))/1000.0) + " (" + id() + ") - " + message);
  }
}

class NGMissileTarget {
  NGPoint impactCoords;
  NGPredictionSettings predictionSettings;
  public NGMissileTarget(NGPoint impactCoords, NGPredictionSettings predictionSettings) {
    this.impactCoords=impactCoords;
    this.predictionSettings=predictionSettings;
  }
}

class NGTrackItem {
  NGPoint coords;
  double speed; // m/s
  double heading; // degrees
  
  public NGTrackItem(NGPoint coords, double speed, double heading) {
    this.coords=coords;
    this.speed=speed;
    this.heading=heading;
  }
  
  public String toString() {
    return "{ " + coords.toString() + ", speed: " + speed + ", heading " + heading +  " }";
  }
}

class NGPredictionSettings {
  double factor;
  public NGPredictionSettings(double factor) {
    this.factor=factor;
  }
}
class NGPoint {
  double x;
  double y;
  double t; // time
  
  public NGPoint(double x, double y) {
    this.x = x;
    this.y = y;    
  }
  
  public NGPoint(double x, double y, double t) {
    this.x = x;
    this.y = y;    
    this.t = t;
  }

  public String toString() {
    return "(x: " + x + ", y: " + y + ", t: " + t + ")";
  }
  public JJVector getVectorTo(NGPoint p) {
    return new JJVector(p.x-x, p.y-y, p.t-t);
  }
}
