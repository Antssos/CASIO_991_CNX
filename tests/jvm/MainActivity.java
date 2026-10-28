package org.fx991cnx.emulator;
// JVM-only host for exercising the real menu controller, with no Android runtime.
final class MainActivity {
 final UiPreferences ui=new UiPreferences();int redraws,feedbacks;
 void appearanceChanged(){redraws++;}void keyFeedback(){feedbacks++;}
}
final class UiPreferences {
 int theme=-1,vibration=-1,sound=-1;
 void theme(int value){theme=value;}
 void vibrationLevel(int value){vibration=value;}
 void soundLevel(int value){sound=value;}
}
