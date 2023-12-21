/*
 *  Extension to leshan-server-demo for application code.
 */

package org.course;


import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.io.PrintWriter;

import org.eclipse.leshan.server.californium.LeshanServer;
import org.eclipse.leshan.server.registration.Registration;
import org.eclipse.leshan.core.response.WriteResponse;
import org.eclipse.leshan.core.response.ReadResponse;
import org.eclipse.leshan.core.response.ObserveResponse;
import org.eclipse.leshan.core.request.WriteRequest;
import org.eclipse.leshan.core.request.ReadRequest;
import org.eclipse.leshan.core.request.ObserveRequest;
import org.eclipse.leshan.core.request.WriteRequest.Mode;

import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.observation.SingleObservation;


public class IoT {
    
    //
    // 2IMN15: keep state.
    //
    private static LeshanServer lwServer;

    // state of the luminaires.
    private static Map<String,LuminaireState> luminaireStates;
    // state of the presence detectors
    private static Map<String,PresenceDetectorState> presenceStates;
    // allowed power budget for the room.
    private static int powerBudget;
    // install power of luminaires.
    private static int maxPowerUsage;
    private static int currentDimLevel;
    
    public static void Initialize(LeshanServer server)
    {
	lwServer = server;
	luminaireStates = new HashMap<String,LuminaireState>();
	presenceStates = new HashMap<String,PresenceDetectorState>();
	powerBudget = 0;
	maxPowerUsage = 0;
	currentDimLevel = 0;
    }

    private static void showSystemState()
    {
	// Method to show the system state.
	// It is called when something changed.
	System.out.println("Power budget = " + powerBudget +
			   ", max usage = " + maxPowerUsage +
			   ", current dim level = " + currentDimLevel);
    }
    
    private static void setDimLevels(boolean force)
    {
	// Compute dim settings for all luminaires
	int newDimLevel = 0;
	if (maxPowerUsage == 0) {
	    // No luminaires.
	    newDimLevel = 100;
	} else {
	    newDimLevel = powerBudget*100 / maxPowerUsage;
	    if (newDimLevel > 100) newDimLevel = 100;
	}
	// If needed, update luminaires
	if (newDimLevel != currentDimLevel || force) {
	    // Adjust all luminaires.
	    for (Map.Entry<String,LuminaireState> entry : luminaireStates.entrySet()) {
		// entry.getKey()  is LwM2mEntrypoint.
		// entry.getValue() is LuminaireState.
		Registration regis = lwServer.getRegistrationService().getByEndpoint(entry.getKey());
		writeInteger(regis, Constants.LUMINAIRE_ID, 0, Constants.RES_DIM_LEVEL, newDimLevel);
	    }
	}
	currentDimLevel = newDimLevel;
    }
    
    private static void setLuminaires(boolean on)
    {
	// Adjust all luminaires.
	for (Map.Entry<String,LuminaireState> entry : luminaireStates.entrySet()) {
	    // entry.getKey()  is LwM2mEntrypoint.
	    // entry.getValue() is LuminaireState.
	    Registration regis = lwServer.getRegistrationService().getByEndpoint(entry.getKey());
	    writeBoolean(regis, Constants.LUMINAIRE_ID, 0, Constants.RES_POWER, on);
	}
    }
    
    public static void handleRegistration(Registration registration)
    {
        // Check which objects are available.
        Map<Integer,org.eclipse.leshan.core.LwM2m.Version> supportedObject =
	    registration.getSupportedObject();
        // Objects 33000 (Presence Detector) and 33001 (Luminaire).
	boolean changed=false;
        int latitude=0;
        int longitude=0;
        if (supportedObject.get(33000) != null ||
            supportedObject.get(33001) != null) {
	    // Either Presence Detector or Luminaire exist.
	    // Retrieve location information.
	    if (supportedObject.get(6) != null) {
                // Retrieve location.
                String latRes="/6/0/0";
                String longRes = "/6/0/1";
                latitude = readInteger(registration,6,0,0);
                longitude = readInteger(registration, 6,0,1);
	    }
        } else {
	    System.out.println("new registration does not contain Presence Detector or Luminaire.");
        }
        if (supportedObject.get(Constants.PRESENCE_DETECTOR_ID) != null) {
	    System.out.println("Presence Detector");
	    PresenceDetectorState pdState = new PresenceDetectorState();
	    // A presence detector.
	    // Retrieve status fields
	    pdState.latitude = latitude;
	    pdState.longitude = longitude;

	    pdState.power = Boolean.valueOf(readString(registration,Constants.PRESENCE_DETECTOR_ID,0,Constants.RES_POWER));
	    pdState.presence = Boolean.valueOf(readString(registration, Constants.PRESENCE_DETECTOR_ID,0,Constants.RES_PRESENCE));
	    
	    presenceStates.put(registration.getEndpoint(), pdState);
	    // Observe dynamic fields
	    try {
		System.out.println(">>ObserveRequest created << ");
		ObserveResponse pdResponse = lwServer.send(registration, new ObserveRequest(Constants.PRESENCE_DETECTOR_ID,0,Constants.RES_PRESENCE), 3000);
		System.out.println(">>ObserveRequest sent << ");
		if (pdResponse == null) {
		    System.out.println(">> NULL <<");
		}
	    }
	    catch (Exception e) {
                System.out.println("Something wrong with observing presence detector.");
	    }
        }
        if (supportedObject.get(Constants.LUMINAIRE_ID) != null) {
	    System.out.println("Luminaire");
	    LuminaireState lmState = new LuminaireState();
	    lmState.latitude = latitude;
	    lmState.longitude = longitude;
	    lmState.peakpower = 0;
	    lmState.type = "LED";
	    lmState.type = readString(registration, Constants.LUMINAIRE_ID,0,Constants.RES_TYPE);
	    lmState.peakpower = readInteger(registration, Constants.LUMINAIRE_ID,0,Constants.RES_PEAK_POWER);
	    lmState.dimlevel = readInteger(registration, Constants.LUMINAIRE_ID,0,Constants.RES_DIM_LEVEL);
	    luminaireStates.put(registration.getEndpoint(), lmState);
	    maxPowerUsage += lmState.peakpower;
	    // TODO: since maxPowerUsage is increased, dim levels of all
	    //       luminaires might have to be adjusted.
	    changed=true;
	    // Observe relevant luminaire information.
	    try {
		System.out.println(">>ObserveRequest created << ");
		ObserveResponse coResponse = lwServer.send(registration, new ObserveRequest(Constants.LUMINAIRE_ID, 0, Constants.RES_POWER), 1000);
		System.out.println(">>ObserveRequest sent << ");
		
		if (coResponse == null) {
		    System.out.println(">>ObserveRequest null << ");
		}
	    }
	    catch (Exception e) {
                System.out.println("Something wrong with observing luminaire power.");
	    }
        }
        if (supportedObject.get(Constants.DEMAND_RESPONSE_ID) != null) {
	    // Demand Response sets the power budget.
	    registerDemandResponse(registration);
	    changed=true;
        }
	if (changed) {
	    setDimLevels(true);
	}
        showSystemState();
    }
    
    public static void handleDeregistration(Registration registration)
    {
        // Update list of known objects.
        // Update webpage
        // Update 8x8 LED matrix
        String rid = registration.getEndpoint();
        Boolean changed = false;
        if (luminaireStates.containsKey(rid)) {
	    // Substract luminaire peak value.
	    LuminaireState lmState = luminaireStates.get(rid);
	    maxPowerUsage -= lmState.peakpower; 
	    System.out.println("Luminaire removed. Max usage is " + maxPowerUsage + ", power budget is " + powerBudget);
	    luminaireStates.remove(rid);
	    changed = true;
        }
        if (presenceStates.containsKey(rid)) {
	    presenceStates.remove(rid);
	    changed = true;
        }
        if (changed) {
	    setDimLevels(false);
	    // Update webpage.
	    showSystemState();
        }
    }
    
    public static void handleObserveResponse(SingleObservation observation, Registration registration, ObserveResponse response)
    {
        if (registration != null && observation != null && response != null) {
	    // Check whether registration is known in list of known objects.
	    String rid = registration.getEndpoint();
	    String obsPath = observation.getPath().toString();
	    Boolean changed = false;
	    System.out.println(">>ObserveResponse " + obsPath);
	    if (luminaireStates.containsKey(rid)) {
		//
		LuminaireState lmState = luminaireStates.get(rid);
		if (obsPath.equals("/33001/0/30000")) {
		    // Luminaire turned on or off.
		    /*
		      String pStatus = ((LwM2mResource)response.getContent()).getValue().toString();
		      
		      if (pStatus.equals(psState.status)) {
		      } else {
		      psState.status = pStatus;
		      parkingSpots.put(rid, psState);
		      changed = true;
		      }
		    */
		}
	    }
           if (presenceStates.containsKey(rid)) {
	       PresenceDetectorState pdState = presenceStates.get(rid);
                if (obsPath.equals("/33000/0/30001")) {
		    String csValue = ((LwM2mResource)response.getContent()).getValue().toString();
		    try {
			boolean ciValue = Boolean.valueOf(csValue);
			if (ciValue != pdState.presence) {
			    pdState.presence = ciValue;
			    presenceStates.put(rid,pdState);
			    // Use luminaireStates to adjust power.
			    // When there are multiple luminaires, ...
			    setLuminaires(ciValue);
			    changed = true;
			}
		    }
		    catch (Exception e) {
                        System.out.println("Exception in reading presence detector:" + e.getMessage());
                   }
                }
                if (obsPath.equals("/33000/0/30000")) {
		    String powValue = ((LwM2mResource)response.getContent()).getValue().toString();
		    try {
			boolean powBool = Boolean.parseBoolean(powValue);
			if (powBool != pdState.power) {
			    pdState.power = powBool;
			    presenceStates.put(rid,pdState);
			    changed = true;
			}
		    }
		    catch (Exception e) {
                        System.out.println("Exception in reading presence detector:" + e.getMessage());
		    }
                }
           }
	   if (obsPath.equals("/33002/0/30005")) {
	       String powValue = ((LwM2mResource)response.getContent()).getValue().toString();
	       try {
		   int newPower = Integer.parseInt(powValue);
		   if (newPower != powerBudget) {
		       powerBudget = newPower;
		       changed = true;
		       System.out.println("Power budget is " + powerBudget + ",  max usage is " + maxPowerUsage);
		       // Use luminaireStates to adjust all the dim levels.
		   }
	       }
	       catch (Exception e) {
		   System.out.println("Exception in reading demand response:" + e.getMessage());
	       }
	       
	   }
	   // Update status of parking lot (available, reserved, occupied)
	   // Update webpage
	   // Update 8x8 LED matrix
           if (changed) {
	       setDimLevels(false);
	       showSystemState();
           }
        }
    }


    // Support functions for reading and writing resources of
    // certain types.

    private static void registerDemandResponse(Registration registration)
    {
	System.out.println("Demand Response found");
	
	powerBudget = readInteger(registration, Constants.DEMAND_RESPONSE_ID,0,
				  Constants.RES_TOTAL_BUDGET);
	System.out.println("Power budget is " + powerBudget);
	// Observe the total budget information for updates.
	try {
	    ObserveRequest obRequest =
		new ObserveRequest(Constants.DEMAND_RESPONSE_ID,
				   0,
				   Constants.RES_TOTAL_BUDGET);
	    System.out.println(">>ObserveRequest created << ");
	    ObserveResponse coResponse =
		lwServer.send(registration, obRequest, 1000);
	    System.out.println(">>ObserveRequest sent << ");
	    if (coResponse == null) {
		System.out.println(">>ObserveRequest null << ");
	    }
	}
	catch (Exception e) {
	    System.out.println("Something wrong with observing demand response.");
	}
    }

    
    
    private static int readInteger(Registration registration, int objectId, int instanceId, int resourceId)
    {
        try {
	    ReadRequest request = new ReadRequest(objectId, instanceId, resourceId);
	    ReadResponse cResponse = lwServer.send(registration, request, 5000);
	    if (cResponse.isSuccess()) {
		String sValue = ((LwM2mResource)cResponse.getContent()).getValue().toString();
		try {
		    int iValue = Integer.parseInt(((LwM2mResource)cResponse.getContent()).getValue().toString());
		    return iValue;
		}
		catch (Exception e) {
		}
		float fValue = Float.parseFloat(((LwM2mResource)cResponse.getContent()).getValue().toString());
		return (int)fValue;
	    } else {
		return 0;
	    }
        }
        catch (Exception e) {
	    System.out.println(e.getMessage());
	    System.out.println("readInteger: exception");
	    return 0;
        }
    }
    
    private static String readString(Registration registration, int objectId, int instanceId, int resourceId)
    {
        try {
	    ReadRequest request = new ReadRequest(objectId, instanceId, resourceId);
	    ReadResponse cResponse = lwServer.send(registration, request, 1000);
	    if (cResponse.isSuccess()) {
		String value = ((LwM2mResource)cResponse.getContent()).getValue().toString();
		return value;
	    } else {
		return "";
	    }
        }
        catch (Exception e) {
	    System.out.println(e.getMessage());
	    System.out.println("readString: exception");
	    return "";
        }
    }
    
    private static void writeInteger(Registration registration, int objectId, int instanceId, int resourceId, int value)
    {
	try {
	    WriteRequest request = new WriteRequest(objectId, instanceId, resourceId, value);
	    WriteResponse cResponse = lwServer.send(registration, request, 1000);
	    if (cResponse.isSuccess()) {
		System.out.println("writeInteger: Success");
	    } else {
		System.out.println("writeInteger: Failed, " + cResponse.toString());
	    }
	}
	catch (Exception e) {
	    System.out.println(e.getMessage());
	    System.out.println("writeInteger: exception");
	}
    }
    
    private static void writeString(Registration registration, int objectId, int instanceId, int resourceId, String value)
    {
	try {
	    WriteRequest request = new WriteRequest(objectId, instanceId, resourceId, value);
	    WriteResponse cResponse = lwServer.send(registration, request, 1000);
	    if (cResponse.isSuccess()) {
		System.out.println("writeString: Success");
	    } else {
		System.out.println("writeString: Failed, " + cResponse.toString());
	    }
	}
	catch (Exception e) {
	    System.out.println(e.getMessage());
	    System.out.println("writeString: exception");
	}
    }
    
    private static void writeBoolean(Registration registration, int objectId, int instanceId, int resourceId, boolean value)
    {
	try {
	    WriteRequest request = new WriteRequest(objectId, instanceId, resourceId, value);
	    WriteResponse cResponse = lwServer.send(registration, request, 1000);
	    if (cResponse.isSuccess()) {
		System.out.println("writeBoolean: Success");
	    } else {
		System.out.println("writeBoolean: Failed, " + cResponse.toString());
	    }
	}
	catch (Exception e) {
	    System.out.println(e.getMessage());
	    System.out.println("writeBoolean: exception");
	}
    }
    
}
