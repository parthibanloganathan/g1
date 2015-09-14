Author:  Orestis Polychroniou
Email:   orestis@cs.columbia.edu


Player

The players must extend the "pppp/sim/Player.java" interface. An example
dummy player is provided in "pppp/g0/Player.java". The player code must
be on a separate directory and the basic class must be in "Player.java".
For example, group 4 should have the main class in "pppp/g4/Player.java".

The interface for a player specifies two methods, an "init()" method to
initialize the player object and a "play()" method to return the next
movement of the player. The simulator first calls the default constructor
with no arguments and then calls the "init()" method. As the game is
played, the "play()" method is called on every turn until the game ends.

The "play()" method should return the movements of the players. Any moves
left unfilled (null) will be assumed to be zero moves (the pipers stays
still without playing music). The same would apply even if an exception
is thrown by the player. The player can move at any speed (using dx, dy).
However, if the speed exceeds the upper limit depending on whether the
piper is playing music or not, the simulator will reduce the speed to the
upper limit without notifying the player class. For example, if the player
returns dx=0 and dy=1, then the simulator will set either dy=0.1, if the
piper is playing music, or dy=0.5, if the piper is not playing music.
If a player fails during during initialization, he will not participate
in the game and the pipers will stay still without playing music.


Simulator

The compiled Java bytecode of the simulator is included in the folder.
The simulator will compile player classes (see arguments), so you will
not need to call the Java bytecode compiler ("javac") yourself at all.
To (re)compile the simulator on Unix & Mac OS X:   javac pppp/sim/*.java
To (re)compile the simulator on Windows:           javac pppp\sim\*.java
To run the simulator:  java pppp.sim.Simulator <arguments>

The simulator is written and tested using Java version 8 (jre/jdk 1.8).
To check the Java virtual machine version:     java -version
To check the Java bytecode compiler version:   javac -version
Earlier versions of Java may (will probably) not work. The simulator is
operating system (OS) independent and has been (successfully) tested on:
  Microsoft Windows 10
  Apple Mac OS X Yosemite (10.10)
  GNU/Linux Ubuntu 15.04
  GNU/Linux CentOS 6.7

On Windows, the "java" and "javac" may not be accessible directly from the
command promt. Instead, use full (absolute) file system paths. with Java 8
JDK build 60 installed, the locations of "java" and "javac" are typically:
C:\Program Files\Java\jdk1.8.0_60\bin\javac.exe
C:\Program Files\Java\jdk1.8.0_60\bin\java.exe
A good idea is to put the "bin\" directory location in the system PATH.
(Control Panel -> System -> Advanced -> Environment Variables -> PATH)

The simulator arguments are:
 -g, --groups <north group> <east group>
              <south group> <west group>
 -s, --side   <square side>
 -p, --pipers <number of pipers per group>
 -r, --rats   <number of rats>
     --turns  <turn limit divided by 10>
     --fps    <frames per second for GUI>
     --recompile
     --verbose
     --gui

If the GUI option is enabled, the simulator creates a visualization of
the game as a dynamic HTML page. To view the GUI, open the browser on
localhost using the port displayed in a terminal message. For example,
if the simulator shows port 54321 in the terminal, open the browser and
use the address "http://localhost:54321" (or "http://127.0.0.1:54321").
Javascript must be enabled. The GUI uses HTML 5 canvas and the state
updates use AJAX. Page reloads are supported (used when FPS is set to 0).
The simulator GUI visualization has been tested on recent versions of:
  Google Chrome
  Mozilla Firefox
  Internet Explorer 11
  Microsoft Edge (IE 12)
  Opera
  Safari


Final Notes

All files (including this README file) use the Windows end of line (\r\n)
rather than the Unix end of line (\n) for easier access in Windows. Unix
systems should remain unaffected from this.

This README file applies to the labeled stable version of the simulator
uploaded directly on CourseWorks. Any testing version of the simulator
may contain changes not documented here and will not be properly tested
on all of the platforms described above.

