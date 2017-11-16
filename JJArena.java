/*
  JJArena.java version 4.00 alfa (2003/10/19)
    Copyright (C) 2001-2003 Leonardo Boselli (boselli@uno.it)

  Portions of this program were written by:
    Moray Goodwin (moray@jyra.com)
    Christo Fogelberg (doubtme@hotmail.com)
    Alan Lund (alan.lund@acm.org)
    Tim Strazny (timstrazny@gmx.de)
    Walter Nistico' (walnist@infinito.it)
    Samuel (a1rex@hotmail.com)
---

  This program is free software; you can redistribute it and/or
  modify it under the terms of the GNU General Public License
  as published by the Free Software Foundation; either version 2
  of the License, or (at your option) any later version.

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  See the GNU General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with this program; if not, write to the
    Free Software Foundation, Inc.,
    59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.

  Please send remarks, questions and bug reports to
    boselli@uno.it
  or write to:
    Leonardo Boselli
    Via Paoletti, 33
    18100 Imperia
    ITALY
*/

// Changes by S.G.:
// Main changes from version 3.1 to 4.00
// Mouse interupts do not take CPU power from simulator
// To stop match click stop button first


import java.applet.*;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.*;

public final class JJArena extends Applet
  implements Runnable, AppletStub
	//, AppletContext //sg! alows compilation for 1.4
{
//fields/static

static private boolean isApplication;
static private String codeBase = "file://";

//fields/volatile/private
//
volatile private boolean forcedStop=true;    //sg! forced restart of the app
volatile private boolean running=false;      //only one app should run at the same time
volatile private boolean showRanking=false;  //sg! we can restart when showing results
//fields/private

private int winner=-1;
private JJRobots jjRobots;
private String[] names;
private String[] jn;
private int[][] wins;
private int[][] matches;
private float[][] perc;
private JJGraph jjGraph = null;
private String[][] graph;
private Thread th=null;

private Frame results = null;
private TextArea results_ta = null;
private boolean random = false;
private int sorted = -1;
private double speed = 1;
private int countMode;
private int mode;

private long readTime;

private Label speedLabel = new Label("1",Label.CENTER);
private Label emptyLabel = new Label("",Label.CENTER);

private Checkbox loopCheck;
private Button   pauseButton;
private boolean  pause=false;
private Checkbox randomSelCheck;
private java.awt.List chosenRobot;

private boolean smoothDisplay = true;
private boolean noDisplay = false;
private boolean goOnNoDisplay = true;

//speed variables
private int maxSpeed = 16;
private double minSpeed = 0.015625;

// virtual clock generator
private static JJClock clock;
private static int currentPriority = 0;
static int getClockPriority(){
 if (clock.isAlive())
  return clock.getPriority();
 else
  return currentPriority;
}
// switch to choose to run synchronous clock
private final boolean SYNCH = true;

//methods/AppletContext

public AudioClip getAudioClip(URL url) {
  if(isApplication) {
    return null;
  } else {
    return super.getAudioClip(url);
  }
}

public Image getImage(URL url) {
  if(isApplication) {
    return null;
  } else {
    return super.getImage(url);
  }
}

public Applet getApplet(String name) {
  return null;
}

public Enumeration getApplets() {
  return null;
}

public void showDocument(URL url) {
}

public void showDocument(URL url, String target) {
}

public void showStatus(String status) {
  if(isApplication) {
  } else {
    super.showStatus(status);
  }
}

//methods/AppletStub

public boolean isActive() {
  if(isApplication) {
    return true;
  } else {
    return super.isActive();
  }
}

public URL getDocumentBase() {
  if(isApplication) {
    return getCodeBase();
  } else {
    return super.getDocumentBase();
  }
}

public URL getCodeBase() {
  if(isApplication) {
    try {
      return new URL("file:");
    } catch(Exception e) {
      return null;
    }
  } else {
    return super.getCodeBase();
  }
}

public String getParameter(String name) {
  if(isApplication) {
    return null;
  } else {
    return super.getParameter(name);
  }
}

public AppletContext getAppletContext() {
  if(isApplication) {
    return null;
  } else {
    return super.getAppletContext();
  }
}

public void appletResize(int width, int height) {
}

//methods/public

static public void main(String[] args) {
  isApplication = true;
  JJArena arena = new JJArena();
  arena.setStub((AppletStub)arena);
  Frame f = new Frame(arena.getAppletInfo());
  f.setLayout(new BorderLayout());
  f.add("Center",arena);
  //f.reshape(0,0,600,400);
    f.reshape(0,0,700,500);             // al
  f.show();
  arena.init();
  arena.start();
}

public String getAppletInfo() {
  return "JJRobots (c) 2000-2003 L.Boselli - boselli@uno.it";
}

public String getStartInfo() {//sg!
  return "JJRobots (c) starting...";
}

public String getStopInfo() {//sg!
  return "JJRobots (c) to restart a match press stop button first...";
}


public void init() {

   clock = new JJClock();
   clock.setResolution(0.0002);//clock granularity 0.2ms
   if (!SYNCH)
     clock.start();

  setBackground(Color.lightGray);
  setLayout(new BorderLayout());

  readResults();

  chosenRobot = new java.awt.List(jn.length,true);
  for(int ct = 0; ct < jn.length; ct++) {
    String name = jn[ct].substring(2);
    name = name.substring(0,name.indexOf('_'));
    chosenRobot.addItem(name);
  }

  Panel panel = new Panel();
  panel.setLayout(new BorderLayout());
  if(isApplication) {
    Panel p = new Panel();
    p.setLayout(new GridLayout(1,2));
    p.add(new Button("Save"));
    p.add(new Button("Close"));
    panel.add("North",p);
  }
  panel.add("Center",chosenRobot);

  Panel panel2 = new Panel();
  panel2.setLayout(new BorderLayout());

  Panel panel3, panel4, panel5;

  panel3 = new Panel(new GridLayout(10,1));
  panel3.setBackground(Color.white);
  panel3.add(new Label("Show"));
  panel3.add(new Checkbox("Scan"));
  panel3.add(new Checkbox("Trace"));
  panel3.add(new Checkbox("Track"));
  panel3.add(new Checkbox("Results"));
  panel3.add(new Checkbox("Graphs"));
  panel3.add(new Label(""));
  panel3.add(new Label("Time speed"));
  panel4 = new Panel(new GridLayout(1,5));
  panel5 = new Panel(new GridLayout(1,5));
  panel5.add(new Button("/32"));
  panel5.add(new Button("/16"));
  //panel5.add(new Button("/8"));
  panel4.add(new Button("/4"));
  panel4.add(new Button("/2"));
  panel4.add(speedLabel);
  panel4.add(new Button("x2"));
  panel4.add(new Button("x4"));
  //panel5.add(new Button("x8"));
  panel5.add(emptyLabel);
  panel5.add(new Button("x16"));
  panel5.add(new Button("x32"));
  panel3.add(panel4);
  panel3.add(panel5);
  panel2.add("West",panel3);

  panel3 = new Panel(new GridLayout(9,1));
  panel3.add(new Label("Display"));
  CheckboxGroup cbg1 = new CheckboxGroup();
  panel3.add(new Checkbox("Smooth",cbg1,true));
  panel3.add(new Checkbox("Fast",cbg1,false));
  panel3.add(new Checkbox("None",cbg1,false));
  panel3.add(new Label(""));
  panel3.add(new Label(""));
  panel3.add(new Label("Select"));
  CheckboxGroup cbg = new CheckboxGroup();
  panel3.add(randomSelCheck = new Checkbox("Rand",cbg,true));
  panel3.add(new Checkbox("Wght",cbg,false));
  panel2.add("Center",panel3);

  panel3 = new Panel(new GridLayout(9,1));
  panel3.add(new Label("Match"));
  panel3.add(new Button("Single"));
  panel3.add(new Button("Double"));
  panel3.add(new Button("Team"));
  panel3.add(new Button("Rand"));
  panel3.add(new Label(""));
  panel3.add(new Button("Stop"));
//  panel3.add(new Label(""));
  panel3.add(pauseButton = new Button("Pause"));
  panel3.add(loopCheck = new Checkbox("Loop"));
  panel2.add("East",panel3);

  panel.add("South",panel2);

  add("East",panel);

	//sg! -------------------- extremely important ----------------------------------
  try{                                // very important!! allow cleanup of old threats
																			// old threats MUST finish before proceeding
		 Thread.sleep(2000);              // the bigger delay the better
	} catch(InterruptedException e) {}

	//sg! -------------------- extremely important ----------------------------------

  jjRobots = new JJRobots();
  add("Center",jjRobots);

  validate();
}

private void readResults() {
  try {
    readTime = System.currentTimeMillis();
    URLConnection c = new URL(getCodeBase(),"robots.txt").openConnection();
    c.setUseCaches(false);
    DataInputStream dis = new DataInputStream(c.getInputStream());
    Vector lines = new Vector();
    String line;
    if((line = dis.readLine()) != null) {
      while((line = dis.readLine()) != null) lines.addElement(line);
    }
    int count = lines.size();
    jn = new String[count];
    wins = new int[count][];
    matches = new int[count][];
    perc = new float[count][];
    graph = new String[count][];
    for(int ct = 0; ct < count; ct++) {
      StringTokenizer st = new StringTokenizer((String)lines.elementAt(ct));
      jn[ct] = st.nextToken();
      wins[ct] = new int[3];
      matches[ct] = new int[3];
      perc[ct] = new float[3];
      for(int ct1 = 0; ct1 < 3; ct1++) {
        wins[ct][ct1] = Integer.parseInt(st.nextToken());
        matches[ct][ct1] = Integer.parseInt(st.nextToken());
        perc[ct][ct1] = matches[ct][ct1] != 0? wins[ct][ct1]*100f/matches[ct][ct1]: 0;
      }
      if(graph[ct] == null) {
        graph[ct] = new String[3];
        for (int i = 0; i < 3; i++) {
          try { graph[ct][i] = st.nextToken(); } catch(Exception e) { graph[ct][i] = "x"; }
          graph[ct][i] = graph[ct][i].substring(1);
        }
      }
    }
    dis.close();
  } catch(Exception e) {
    e.printStackTrace();
  }
}

private String getResults() {
  if (sorted != countMode) sort(countMode);
  String newLine = System.getProperty("line.separator");
  String dos = "000000"+newLine;
  for(int ct = 0; ct < jn.length; ct++) {
    dos += jn[ct];
    for(int ct1 = 0; ct1 < 3; ct1++)
      dos += " "+wins[ct][ct1]+" "+matches[ct][ct1];
    for (int i = 0; i < 3; i++)
      dos += " x" + graph[ct][i];
    dos += newLine;
  }
  return dos;
}

private void saveResults() {
  try {
    DataOutputStream dos =
      new DataOutputStream(new FileOutputStream("robots.txt"));
    dos.writeBytes(getResults());
    dos.close();
  } catch(Exception e) {
    e.printStackTrace();
  }
}

private void showResults(boolean state) {
  if (state) {
    if (results == null) {
      results = new Frame("JRobots results (copy and paste into \"robots.txt\")");
      results.add(results_ta = new TextArea(chosenRobot.countItems()+2, 40));
      results_ta.setEditable(false);
      results_ta.setBackground(Color.white);
      results.pack();
    }
    updateResults();
  }
  if (results != null) {
    if (state) results.show();
    else results.hide();
  }
}

private void updateResults() {
  results_ta.setText(getResults());
}

private void showGraphs(boolean state) {
  if (state && (jjGraph == null)) {
    String[] items = new String[chosenRobot.countItems()];
    for (int i = 0; i < items.length; i++)
      items[i] = chosenRobot.getItem(i);
    jjGraph = new JJGraph(jn, graph, items);
  }
  if (jjGraph != null) {
    if (state)
      jjGraph.show();
    else
      jjGraph.hide();
  }
}

public void start() {

	showRanking = false;                // ranking not showed yet
   Thread.yield();                    //

  if(th != null) {
    stop();
    try{ Thread.sleep(1000); } catch(InterruptedException e) {}
  }

  th = new Thread(this, "JRobots");

	if(isApplication)
     JJRobots.isApplication(true);
  th.start();
}

private void initArena() {

  if(random) randomize();

  names = new String[countMode == JJRobots.TEAM? 4: 2];
  Random rand = new Random();

  if(randomSelCheck.getState()) {

//BEGIN Random Selection

    int ct = 0;
    String name;
    String[] items = chosenRobot.getSelectedItems();
    for(; ct < items.length && ct < names.length; ct++) {
      names[ct] = "__"+items[ct]+'_';
    }

    for(; ct < names.length; ct++) {
      boolean notFind;
      do {
        notFind = false;
        name = jn[(int)(rand.nextFloat()*(jn.length))];
        for(int ct1 = 0; ct1 < ct; ct1++) {
          if(names[ct1].equals(name)) {
            notFind = true;
            break;
          }
        }
      } while(notFind);
      names[ct] = name;
    }

//END Random Selection

  } else {

//BEGIN Weighted Selection

    // Select one robot, with robots with higher winning percentages
    // being chosen more often.

    // The parameter c affects the distribution of the choices:
    //     larger  values ==> more even distribution
    //     smaller values ==> better robots chosen more often
    // Changing c dynamically based on the number of matches done already
    // prevents the first few matches from being unfairly allocated

    // Calculate the total number of matches so far...
    int      matchCount = 0;

    for (int i = 0; i < jn.length; i++)
      matchCount += matches[i][countMode];

    // Account for the number of teams per match
    matchCount /= (countMode == JJRobots.TEAM) ? 4 : 2;

    double[] w1         = new double[jn.length];
    double   floor      = 0.05;
    double   threshhold = jn.length * 5;
    double   c          =   (threshhold * threshhold)
                                        /
                          (matchCount * matchCount + 1);

    for (int i = 0; i < jn.length; i++)
      w1[i] = floor + c + perc[i][countMode] / 100.0;

    // A clever way to honor the user's selections from the list of
    // robots (no matter how many there are) is just to make them
    // substantially more likely to be chosen.
    magnifyUserSelections(w1, 1e24);

    int firstRobot = weightedSelection(w1, rand.nextFloat());

    names[0] = jn[firstRobot];

    // Now select the rest, using weights related to the difference between
    // each robot's winning percentage and that of the first robot chosen.

    // The parameter p affects the distribution of the choices:
    //     larger  values ==> more even distribution
    //     smaller values ==> closer robots chosen more often

    double[] w2 = new double[jn.length];
    double   p  = 1.0;

    for (int i = 0; i < jn.length; i++) {
      double diff = (perc[firstRobot][countMode] - perc[i][countMode]) / 100.0;

      w2[i] = 1.01 - Math.pow(Math.abs(diff), p);  // al
    }

    // Again, honor the users choices
    magnifyUserSelections(w2, 1e24);

    // Prevent the first robot from being chosen again
    w2[firstRobot] = 0.0;

    // Select the remaining needed robots
    for (int i = 1; i < names.length; i++) {
      int nextRobot = weightedSelection(w2, rand.nextFloat());

      names[i]      = jn[nextRobot];

      // Prevent this robot from being chosen again
      w2[nextRobot] = 0.0;
    }

//END Weighted Selection

  }
  jjRobots.init(mode,names);
}

//BEGIN Weighted Selection Support

private void magnifyUserSelections(double[] weights, double factor)
{
  String[] items = chosenRobot.getSelectedItems();

  for (int i = 0; i < items.length; i++) {
    String name = "__" + items[i] + '_';

    for (int j = 0; j < weights.length; j++) {

      if (jn[j].equals(name))
        weights[j] *= factor;
    }
  }
}

private static int weightedSelection(double[] weights, float fraction)
{
  // First find the total of all of the weights
  double totalWeight = 0.0;

  for (int i = 0; i < weights.length; i++)
    totalWeight += weights[i];

  double goalWeight = totalWeight * fraction;
  int    index      = 0;

  // Need to be careful here that a weight of 0 is never chosen, thus the
  // use of ">="...
  while (index < weights.length && goalWeight >= weights[index]) {
    goalWeight -= weights[index];
    index      += 1;
  }

  // ... but also need to be careful about fraction = 1, which would run
  // off the end of the array (given the use of ">=" above).
  while (index >= weights.length || (index >= 0 && weights[index] == 0.0))
    index--;

  return index;
}

//END Weighted Selection Support

//}

public void stop() {
	forcedStop  = true;                              // sg! This flag indicatess that app was restarted
	showRanking = false;

  jjRobots.stop();                                 // stop running robots threads

  System.runFinalization();
  System.gc();                                     // clean up

  if(th != null) {                                 // stop
    if (th.isAlive()) th.stop();
    th = null;
  }
}

private void sort(int mode) {
  boolean changed;
  do {
    changed = false;
    for(int ct = 0; ct < jn.length-1; ct++) {
      if(perc[ct][mode] < perc[ct+1][mode]) {
        String temps = jn[ct];
        jn[ct] = jn[ct+1];
        jn[ct+1] = temps;
        float[] tempf = perc[ct];
        perc[ct] = perc[ct+1];
        perc[ct+1] = tempf;
        int[] tempi = wins[ct];
        wins[ct] = wins[ct+1];
        wins[ct+1] = tempi;
        tempi = matches[ct];
        matches[ct] = matches[ct+1];
        matches[ct+1] = tempi;
        String[] temp_s = graph[ct];
        graph[ct] = graph[ct+1];
        graph[ct+1] = temp_s;
        changed = true;
      }
    }
  } while(changed);
  sorted = mode;
}

public synchronized void run() {
   if (running) {
    do {
      initArena();
      System.gc();

      jjRobots.start();

      runGame();

      if ((results != null) && results.isVisible()) updateResults();
      if ((jjGraph != null) && jjGraph.isVisible()) jjGraph.updateGraph();

      try{ Thread.sleep(2000); } catch(InterruptedException e) {}  // time for cleanup needed

    } while( loopCheck.getState() && running);
  }

  showStatus(getAppletInfo());             // sg!
  showRanking(countMode);
}

private void runGameVirtual(int updatePeriod) throws InterruptedException{
  long startVT = clock.currentTimeMillis();
  long currentVT, oldVT = startVT;
  updatePeriod *= 5; //must be (0.001/clock.getResolution())
                     // here precomputed for efficiency;
  do {
    clock.tick();
    currentVT = clock.currentTimeMillis();
    winner = jjRobots.step((currentVT-oldVT)*clock.getResolution()*speed);
    Thread.yield();
    oldVT = currentVT;
  } while(
    goOnNoDisplay &&
    ((currentVT-startVT) <= updatePeriod) &&
    (winner < 0) &&
    (th != null) &&
		(forcedStop != true)
  );
}

private void runGameSmooth(int updatePeriod) throws InterruptedException{
  long startVT = clock.currentTimeMillis();
  long currentVT, oldVT = startVT;
  long startRT = System.currentTimeMillis();
  long currentRT;
  do {
    clock.tick();
    currentVT = clock.currentTimeMillis();
		winner = jjRobots.step((currentVT-oldVT)*clock.getResolution()*speed);
    Thread.yield();
    oldVT = currentVT;
    currentRT = System.currentTimeMillis();

  } while(
    goOnNoDisplay &&
    ((currentRT-startRT) <= updatePeriod) &&
    (winner < 0) &&
    (th != null) &&
		(forcedStop != true)
  );

}


public void runGame() {
  try {
    winner      = -1;                   // running, no winner, no draw yet
		forcedStop  = false;                // do loop not broken by stop yet
																				// can be modified asynchroniclly in action loop
  	showRanking = false;                // user can restart the game when it is not running or during
																				// showRanking loop
    showStatus(getAppletInfo());

    do{// Display:
      goOnNoDisplay = true;
			if(!pause){
       if (noDisplay){// None
         runGameVirtual((int)(1000/speed)); //1 frame per Virtual second
       }
       else if (smoothDisplay) {// Smooth
         runGameSmooth(17); //60 fps (real!)
       }
       else {// Fast
         runGameVirtual(33);//30 fpVs
       }
			}
      jjRobots.drawArena(forcedStop);

// To give non-JRobot Threads some cpu time
      Thread.yield();
    } while(running && (winner < 0) && (th != null) && (forcedStop==false));

		// forced stop by stop() or start() function
		if(forcedStop) {
		 winner = 0;    // forcing draw
		}

    jjRobots.drawArena(forcedStop);

    if(winner-- > 0) {
      for(int ct = 0; ct < names.length; ct++) {
        for(int ct1 = 0; ct1 < jn.length; ct1++) {
          if(jn[ct1].equals(names[ct])) {
            if(ct == winner) wins[ct1][countMode]++;
            perc[ct1][countMode] =
              wins[ct1][countMode]*100f/(++matches[ct1][countMode]);
            if ((perc[ct1][countMode] < 11) && ((perc[ct1][countMode] > 0)))
              graph[ct1][countMode] += 0;
            graph[ct1][countMode] += String.valueOf((int)(perc[ct1][countMode]-1));
            break;
          }
        }
      }
    }



    jjRobots.stop();
    System.runFinalization();
    System.gc();


    if(sorted == countMode) sorted = -1;
  } catch(Exception e) {
    e.printStackTrace();
  }
}

private void showRanking(int countMode) {
	forcedStop  = false;
	showRanking = true;
  try {
    Dimension size = size();//getSize();
    int width = size.width;
    int height = size.height;
    Image i = createImage(width,height);
    do {
      sort(countMode);
      String title;
      switch(countMode) {
        default:
        case JJRobots.SINGLE: {
          title = "Single";
          break;
        }
        case JJRobots.DOUBLE: {
          title = "Double";
          break;
        }
        case JJRobots.TEAM: {
          title = "Team";
          break;
        }
      }
      title += " Match Results";
      int startRow = height/2;
      int theRow;
      do {
        theRow = startRow;
			  if(!pause){
         Graphics g = i.getGraphics();
         g.setColor(Color.lightGray);
         g.fillRect(0,0,width,height);
         if ((jjRobots != null) && (jjRobots.getImage() != null)) g.drawImage(jjRobots.getImage(),0,0,null);
         g.setColor(Color.red.darker());
//        g.drawString(title,75,theRow-2);
//        g.setColor(Color.red);
         g.drawString(title,74,theRow-1);
         for(int ct = 0; ct < jn.length; ct++) {
           String name = jn[ct].substring(2);
           name = "["+ct+"] "+name.substring(0,name.indexOf('_'));
           g.setColor(Color.darkGray);
           String ratio = ""+wins[ct][countMode]+"/"+matches[ct][countMode];
           String thePerc = ""+((int)perc[ct][countMode])+"%";
           theRow += 18;
//          g.drawString(ratio,75,theRow);
//          g.drawString(thePerc,185,theRow);
//          g.drawString(name,225,theRow);
//          g.setColor(Color.gray);
           g.drawString(ratio,74,theRow-1);
           g.drawString(thePerc,184,theRow-1);
           g.drawString(name,224,theRow-1);
         }
         startRow--;
         jjRobots.getGraphics().drawImage(i,0,0,null);
				}
        Thread.sleep(50);
      } while(theRow >= height/2 && th != null);
      startRow = height;
      if(countMode++ == JJRobots.TEAM) countMode = 0;
    } while((th != null) && (forcedStop==false));
    i.flush();
  } catch(InterruptedException ie) {/* ignore */}
		showRanking = false;
}

public void update(Graphics g) {
  paint(g);
}
//----------------------------------------------------------------------------------------------------------------
public void increaseSpeed(int i){
  if(speed < maxSpeed && speed*i <= maxSpeed){
    speed = speed*i;
    if(speed < 1) {
      speedLabel.setText(("1/" + Math.pow(speed, -1)).replaceAll("\\.0*$", ""));
    }else if(speed >= 1){
      speedLabel.setText(Double.toString(speed).replaceAll("\\.0*$", ""));
    }
  }else{
    speed = maxSpeed;
    speedLabel.setText(Integer.toString(maxSpeed));
  }
}

public void decreaseSpeed(int i){
  if(speed > minSpeed && speed/i >= minSpeed)  {
    speed = speed / i;
    if (speed < 1) {
      speedLabel.setText(("1/" + Math.pow(speed, -1)).replaceAll("\\.0*$", ""));
    } else if (speed >= 1) {
      speedLabel.setText(Double.toString(speed).replaceAll("\\.0*$", ""));
    }
  }else{
    speed = minSpeed;
    speedLabel.setText(("1/" + Math.pow(minSpeed, -1)).replaceAll("\\.0*$", ""));
  }
}

//----------------------------------------------------------------------------------------------------------------
public /*synchronized*/ boolean action(Event evt, Object what) {
  Object obj = evt.target;
  if(obj instanceof Button) {
    String label = ((Button)obj).getLabel();
    if(label.startsWith("x")){
      increaseSpeed(Integer.parseInt(label.substring(1)));
    }else if(label.startsWith("/")){
      decreaseSpeed(Integer.parseInt(label.substring(1)));
    }
/*  //logic for checking distinct buttons
    if(label.equals("x2")) {
      increaseSpeed(2);
    }else if(label.equals("x4")) {
      increaseSpeed(4);
    }else if(label.equals("x16")) {
      increaseSpeed(16);
    }else if(label.equals("x32")) {
      increaseSpeed(32);
    }
    //standart speed logic
      if(text.equals("1/16")) {
        speed = 0.125;
        speedLabel.setText("1/8");
      } else if(text.equals("1/8")) {
        speed = 0.25;
        speedLabel.setText("1/4");
      } else if(text.equals("1/4")) {
        speed = 0.5;
        speedLabel.setText("1/2");
      } else if(text.equals("1/2")) {
        speed = 1;
        speedLabel.setText("1");
      } else {
        speed = Integer.parseInt(text);
        if(speed != 8) speedLabel.setText(Integer.toString((int)(speed *= 2)));
      }
      //logic for checking distinct buttons
     else if(label.equals("/2")) {
      decreaseSpeed(2);
    }else if(label.equals("/4")) {
      decreaseSpeed(4);
    }else if(label.equals("/16")) {
      decreaseSpeed(16);}
    else if(label.equals("/32")) {
      decreaseSpeed(32);

      //standart speed logic
      if(text.equals("1/8")) {
        speed = 0.0625;
        speedLabel.setText("1/16");
      }
      else if(text.equals("1/4")) {
        speed = 0.125;
        speedLabel.setText("1/8");
      }
      else if(text.equals("1/2")) {
        speed = 0.25;
        speedLabel.setText("1/4");
      } else if(!text.equals("1/8")) {
        speed = Integer.parseInt(text);
        if(speed == 1) {
          speedLabel.setText("1/2");
        } else {
          speedLabel.setText(Integer.toString((int)(speed /= 2)));
        }*/

      else if(label.equals("Save")) {
      saveResults();
    } else if(label.equals("Close")) {
      System.exit(1);
    } else if (label.equals("Stop")) {
      running = false;
      showStatus(getAppletInfo());   //sg!
			pauseButton.setLabel("Pause");
      loopCheck.setState(false);
			pause = false;
    } else if(  label.equals("Pause") || label.equals("Resume") ) {
			if(label.equals("Pause")){
		   pauseButton.setLabel("Resume");
			 pause = true;
			}
			else{
		   pauseButton.setLabel("Pause");
			 pause = false;
			}
		}
		else
		 {
			if( (running==false) || ((forcedStop==false)&&(showRanking==true)) ){  // restart allowed

        random = false;

				if(label.equals("Single")) {
        mode = JJRobots.N_SINGLE;
        countMode = JJRobots.SINGLE;
      } else if(label.equals("Double")) {
        mode = JJRobots.N_DOUBLE;
        countMode = JJRobots.DOUBLE;
      } else if(label.equals("Team")) {
        mode = JJRobots.N_TEAM;
        countMode = JJRobots.TEAM;
      } else if (label.equals("Rand")) {
        random = true;
      }

			 forcedStop  = true;          //sg!
			 showRanking = false;         //sg!
       running = true;

       showStatus(getStartInfo());  //sg!
		   pauseButton.setLabel("Pause");
  		 pause = false;

			 start();
			}
			else{
       showStatus(getStopInfo());  //sg!
			}
      return true;
    }
  } else if(obj instanceof Checkbox) {
    Checkbox cb = (Checkbox)obj;
    String label = cb.getLabel();
    boolean state = cb.getState();
    if(label.equals("Scan")) {
      jjRobots.setShowScans(state);
    } else if(label.equals("Trace")) {
      jjRobots.setShowTraces(state);
    } else if(label.equals("Track")) {
      jjRobots.setShowTracks(state);
    } else if(label.equals("Results")) {
      showResults(state);
    } else if(label.equals("Smooth")) {
      smoothDisplay = true;
      noDisplay = false;
      goOnNoDisplay = false;
      jjRobots.draw = true;
    } else if(label.equals("Fast")) {
      smoothDisplay = false;
      noDisplay = false;
      goOnNoDisplay = false;
      jjRobots.draw = true;
    } else if(label.equals("None")) {
      noDisplay = true;
      goOnNoDisplay = false;
      jjRobots.draw = false;
    } else if(label.equals("Graphs")) {
      showGraphs(state);
    }
  }
  return false;
}

private void randomize() {
  switch((int)(Math.random()*3)) {
    case 0:
      mode = JJRobots.N_SINGLE;
      countMode = JJRobots.SINGLE;
      break;
    case 1:
      mode = JJRobots.N_TEAM;
      countMode = JJRobots.TEAM;
      break;
    case 2:
      mode = JJRobots.N_DOUBLE;
      countMode = JJRobots.DOUBLE;
      break;
  }
}
}
