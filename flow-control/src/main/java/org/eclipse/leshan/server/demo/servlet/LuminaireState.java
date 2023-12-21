
package org.eclipse.leshan.server.demo.servlet;

public class LuminaireState {
    public String type;
    public int peakpower;
    public int dimlevel;
    public int latitude;
    public int longitude;
    public boolean power;

    public LuminaireState()
    {
	type="LED";
	peakpower=15;
	latitude=0;
	longitude=0;
	power=false;
	dimlevel=100;
    }
}
