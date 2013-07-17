import oscP5.*;
import netP5.*;
import ddf.minim.*;
import ddf.minim.signals.*;

OscP5 oscP5;
NetAddress remoteAddress;

Minim minim;
AudioOutput out;
SineWave sine;

boolean isAdded = false;

void setup() {
  size(400,400);
  frameRate(25);
  oscP5 = new OscP5(this, 7770);
  
  minim = new Minim(this);
  out = minim.getLineOut(Minim.STEREO, 512);
  sine = new SineWave(200, 0.5, out.sampleRate());
}


void draw() {
  background(0);  
}

void mousePressed() {
  if (remoteAddress != null) {
    println(remoteAddress.toString());
    OscMessage myMessage = new OscMessage("/touch");
    oscP5.send(myMessage, remoteAddress);
  } else {
    println("remoteAddress is null");
  }
}

void oscEvent(OscMessage theOscMessage) {
  print("### received an osc message.");
  print(" addrpattern: "+theOscMessage.addrPattern());
  println(" typetag: "+theOscMessage.typetag());
  String tags = theOscMessage.typetag();
  Object[] objs = theOscMessage.arguments();
  println(" length: "+ objs.length);
  
  String addrpattern = theOscMessage.addrPattern();
  String typetag = theOscMessage.typetag();
  if (addrpattern.equals("/ip")) {
    if (objs.length >= 2 && typetag.equals("ss")) {
      OscArgument arg;
      arg = theOscMessage.get(0);
      String ipAddress = arg.stringValue();
      arg = theOscMessage.get(1);
      String sPort = arg.stringValue();
      int port = Integer.parseInt(sPort);
      remoteAddress = new NetAddress(ipAddress, port);
      println("Remote address: " + ipAddress + ":" + port);
    }
  } else if (addrpattern.equals("/touch")) {
    println("Remote touched");
    //  out.play();
    if (!isAdded) {
      out.addSignal(sine);
      isAdded = true;
    }
  }  else {
    if (isAdded) {
      out.removeSignal(sine);
      isAdded = false;
    }
  }
  
  for (int i = 0; i < objs.length; i++) {
    OscArgument arg = theOscMessage.get(i);
    if (tags.charAt(i) == 'i')
      println("" + i + ":" + arg.intValue());
    else if (tags.charAt(i) == 'f')
      println("" + i + ":" + arg.floatValue());
    else if (tags.charAt(i) == 's')
      println("" + i + ":" + arg.stringValue());
    else if (tags.charAt(i) == 'b') {
      byte[] blob = arg.blobValue();
    }
  }
}

void stop()
{
// always closes audio I/O classes
out.close();
// always stop your Minim object
minim.stop();

super.stop();
}
