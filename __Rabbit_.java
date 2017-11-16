public class __Rabbit_ extends JJRobot {

void main() {
  while(true) {
    go(rand(1000),rand(1000));
  }
}

void go(int dest_x, int dest_y) {
  int course;
  course = plot_course(dest_x,dest_y);
  drive(course,50);
  while(distance(loc_x(),loc_y(),dest_x,dest_y) > 50);
  drive(course,0);
  while (speed() > 0);
}

int distance(int x1, int y1, int x2, int y2) {
  int x, y;
  x = x1 - x2;
  y = y1 - y2;
  int d = sqrt((x*x) + (y*y));
  return(d);
}

int plot_course(int xx, int yy) {
  int d;
  int x,y;
  int scale;
  int curx, cury;
  scale = 100000;
  curx = loc_x();
  cury = loc_y();
  x = curx - xx;
  y = cury - yy;
  if (x == 0) {
    if (yy > cury)
      d = 90;
    else
      d = 270;
  } else {
    if (yy < cury) {
      if (xx > curx)
	    d = 360 + atan((scale * y) / x);
      else
	    d = 180 + atan((scale * y) / x);
    } else {
      if (xx > curx)
	    d = atan((scale * y) / x);
      else
	    d = 180 + atan((scale * y) / x);
    }
  }
  return (d);
}

}
