public class __Counter_ extends JJRobot {

void main() {
  int angle, range;
  int res;
  int d;
  long i;
  res = 1;
  d = damage();
  angle = rand(360);
  while(true) {
    while ((range = scan(angle,res)) > 0) {
      if (range > 700) {
        drive(angle,50);
        double tm = time();
        while(time()-tm < 2);
        drive (angle,0);
        if (d != damage()) { 
  	  d = damage();
          r();
	}
	angle -= 3;
      } else {
        cannon(angle,range);
        while (cannon(angle,range) == 0);
        if (d != damage()) { 
  	  d = damage();
          r();
	}
	angle -=15;
      }
    }
    if (d != damage()) { 
      d = damage();
      r();
    }
    angle += res;
    angle %= 360;
  }
}

int last_dir;

void r() {
  int x, y;
  int i = 0;

  x = loc_x();
  y = loc_y();

  double tm = time();
  if (last_dir == 0) {
    if (y > 512) {
      last_dir = 1;
      drive(270,100);
      while (y -100 < loc_y() && time()-tm < 2);
      drive(270,0);
    } else {
      last_dir = 1;
      drive(90,100);
      while (y +100 > loc_y() && time()-tm < 2);
      drive(90,0);
    }
  } else {
    if (x > 512) {
      last_dir = 0;
      drive(180,100);
      while (x -100 < loc_x() && time()-tm < 2);
      drive(180,0);
    } else {
      last_dir = 0;
      drive(0,100);
      while (x +100 > loc_x() && time()-tm < 2);
      drive(0,0);
    }
  }
}

}
