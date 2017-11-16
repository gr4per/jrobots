import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;

public class __TkMasterOfPuppets_ extends JJRobot {

    void main() {
        File dancingSteps = new File("dancingSteps.txt");
        try{
         dancingSteps.createNewFile();

         while(true) {
             if(damage()>50){
                 if(dancingSteps.exists()){
                     dancingSteps.delete();
                 }
             }
         }
        }catch (Exception e){
        }finally {
            if(dancingSteps.exists()){
                dancingSteps.delete();
            }
        }
    }

}
