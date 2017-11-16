public class __Sniper_ extends JJRobot {

int corner;
int c1x, c1y;
int c2x, c2y;
int c3x, c3y;
int c4x, c4y;
int s1, s2, s3, s4;
int sc;
int d;

void main() {
  int closest;
  int range;
  int dir;

  c1x = 10;  c1y = 10;  s1 = 0;
  c2x = 10;  c2y = 990; s2 = 270;
  c3x = 990; c3y = 990; s3 = 180;
  c4x = 990; c4y = 10;  s4 = 90;
  closest = 9999;
  new_corner();
  d = damage();
  dir = sc;

  while (true) {

    while (dir < sc + 90) {
      range = scan(dir,1);
      if (range <= 700 && range > 0) { 
        while (range > 0) {
          closest = range;
          cannon(dir,range);
          range = scan(dir,1);
          if (d + 15 > damage())
            range = 0;
        }
        dir -= 10;
      }

      dir += 1;
      if (d != damage()) {
        new_corner();
        d = damage();
        dir = sc;
      }
    }

    if (closest == 9999) {
      new_corner();
      d = damage();
      dir = sc;
    } else
      dir = sc;
    closest = 9999;
  } 

}

void new_corner() {
  int x = 0, y = 0;
  int angle;
  int new_;

  new_ = rand(4);
  if (new_ == corner)
    corner = (new_ + 1) % 4;
  else
    corner = new_;
  if (corner == 0) {
    x = c1x; 
    y = c1y; 
    sc = s1;
  } 
  if (corner == 1) {
    x = c2x; 
    y = c2y; 
    sc = s2;
  }
  if (corner == 2) {
    x = c3x; 
    y = c3y; 
    sc = s3;
  }
  if (corner == 3) { 
    x = c4x; 
    y = c4y; 
    sc = s4;
  }

  angle = plot_course(x,y);

  drive(angle,100);

  while (distance(loc_x(),loc_y(),x,y) > 100 && speed() > 0)
    ;

  drive(angle,10);
  while (distance(loc_x(),loc_y(),x,y) > 20 && speed() > 0)
    ;

  drive(angle,0); 

}

int distance(int x1, int y1, int x2, int y2)
{
  int x, y;

  x = x1 - x2;
  y = y1 - y2;
  d = sqrt((x*x) + (y*y));
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
