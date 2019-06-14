// by Fahad Siddiqui, Senior NodeJS developer candidate April 2018
public class __Terminator_ extends JJRobot {
    int d;
    int xcor;
    int ycor;
    int points[][];

            
    
    void main() {
        points= new int[][]{
        {500,500},
        {750,251},
        {250,252},
        {250,753},
        {750,754}
        };
                
        for(int i=0;i<5;){
            xcor = points[i][0];
            ycor = points[i][1];
            gotoPoint();
            i++;
            if(i==5)
                i=0;
        }

    }
    
    void gotoPoint(){  
        int rdist;
        int angle = plot_course(xcor,ycor);
        drive(angle,100);
        System.out.println(xcor+", "+ycor);

        while(distance(loc_x(),loc_y(),xcor,ycor) > 50){   
            rdist = scan(angle,15);
            while(rdist > 10 && rdist <= 700){
                drive(angle,50);
                cannon(angle+1,rdist);
                angle+=1;
                rdist = scan(angle,10);
            }
            angle+=10;            
        }
        drive(angle,10);        
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
    
    int distance(int x1, int y1, int x2, int y2)
    {
      int x, y,dist;

      x = x1 - x2;
      y = y1 - y2;
      dist = sqrt((x*x) + (y*y));
      return(dist);
    }

}
