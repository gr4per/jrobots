public class __Rook_ extends JJRobot {

int course;
int boundary;
int d;

void main() {
  int y = 0;

  if (loc_y() < 500) {
    drive(90,70);
    while (loc_y() - 500 < 20 && speed() > 0)
      ;
  } else {
    drive(270,70);
    while (loc_y() - 500 > 20 && speed() > 0)
      ;
  }
  drive(y,0);

  d = damage();
  course = 0;
  boundary = 995;
  drive(course,30);

  while(true) {

    look(0);
    look(90);
    look(180);
    look(270);

    if (course == 0) {
      if (loc_x() > boundary || speed() == 0) 
	change();
    }
    else {
      if (loc_x() < boundary || speed() == 0) 
	change();
    }
  }
    
}

void look(int deg) {
  int range;

  while ((range=scan(deg,2)) > 0 && range <= 700)  {
    drive(course,0);
    cannon(deg,range);
    if (d+20 != damage()) {
      d = damage();
      change();
    }
  }
}


void change() {
  if (course == 0) {
    boundary = 5;
    course = 180;
  } else {
    boundary = 995;
    course = 0;
  }
  drive(course,30);
}

}
