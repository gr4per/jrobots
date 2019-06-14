// by Gabriel Ichim, Senior DevOps engineer candidate April 2018
public class __Bau_ extends JJRobot {

int corner;
int s1, s2, s3, s4;
int sc;
int d;
int range;
int res = 1;
int angle;
int power = 50;

void main() {
  res = 1;
  power = 50;

  while (true) {
/*
 * Drive to the enemy, but don't get too close	  
 */
	  int x = loc_x(), y = loc_y();
	  int deg = find_scan_degree(x,y);
	  d = damage();
		
	  while((range = scan(deg,res)) == 0)
		  deg+=1;
	  
	  angle=deg;

	  drive(angle,power);
	  if((range <= 700) && (d + 4 > damage()))
		  cannon(angle,range);
  }
}

int distance(int x1, int y1, int x2, int y2)
{
  int x, y;

  x = x1 - x2;
  y = y1 - y2;
  d = sqrt((x*x) + (y*y));
  return(d);
}

/*
 * It takes 60m to decelerate from power 100 to power 50(point where you can change angle)
 */
boolean safe_drive(int x, int y) {
	x = loc_x();
	y = loc_y();
	if((x<=65) || (x>=935) || (y<=65) ||(y>=935))
		return false;
	return true;
}

/*
 * Determine the quadrant and scan the larger area.
 * It's a higher change to find the enemy in the larger area.
 * 	  
 */
int find_scan_degree(int x, int y) {
  int deg;
  x = loc_x();
  y = loc_y();
  int scale = 100000;

    if (y < 500) {
      if (x < 500) 
        deg = 270 + atan((scale * y) / (1000-x));
      else 
    	deg = 0 + atan((scale * y) / (1000-x));

    }  
    else {
      if (x > 500) 
        deg = 90 + atan((scale * (1000-y)) / x);
      else 
        deg = 180 + atan((scale * (1000-y)) / x);      
    }
    
   return (deg);

	}
}