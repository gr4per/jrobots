public class __TkDummy_ extends JJRobot {

    int course;
    int boundary;
    int d;

    double startX;

    void main() {

        if(loc_x() < 500)
            drive(0, 70);
        else
            drive(180, 70);

        startX = loc_x();

        while(true) {
            if (loc_x() < 50) {
                drive(0, 70);
            }
            if (loc_x() > 950) {
                drive(180, 70);
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
            boundary = 450;
            course = 180;
        } else {
            boundary = 650;
            course = 0;
        }
        drive(course,0);
    }

}
