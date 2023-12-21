package org.course;


public class PresenceDetectorState {
    public boolean power;
    public boolean presence;
    public int latitude;
    public int longitude;
    
    public PresenceDetectorState()
    {
	power=false;
	presence=false;
	latitude=0;
	longitude=0;
    }    
}

