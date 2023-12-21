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
    // state of the presence detectors

    // allowed power budget for the room from demand response
    private static int powerBudget;
    
    // installed power of luminaires.

    // Energy marketplace interaction
    // Bid, Wallet, BudgetAllocation.
    
    public static void Initialize(LeshanServer server)
    {
	lwServer = server;
	// Initialize state
    }
    
    private static void showSystemState()
    {
	// Method to show the system state.
	// It is called when something changed.
    }
    
    public static void handleRegistration(Registration registration)
    {
        // Check which objects are available.
        Map<Integer,org.eclipse.leshan.core.LwM2m.Version> supportedObject =
	    registration.getSupportedObject();
        // Objects Constants.PRESENCE_DETECTOR_ID and Constants.LUMINAIRE_ID.
	boolean changed=false;
        if (supportedObject.get(Constants.PRESENCE_DETECTOR_ID) != null) {
	    System.out.println("Presence Detector");
	    // Update local state:
	    // Read relevant resources.

	    // Observe dynamic resources.
        }
        if (supportedObject.get(Constants.LUMINAIRE_ID) != null) {
	    System.out.println("Luminaire");
	    // Update local state:
	    // Read relevant resources.

	    //Observer dynamic resources.
        }
        if (supportedObject.get(Constants.DEMAND_RESPONSE_ID) != null) {
	    // Demand Response sets the power budget.
	    registerDemandResponse(registration);
	    changed=true;
        }
	if (supportedObject.get(Constants.MARKETPLACE_STATE_ID) != null) {
	    // Market place is available.
	    // energyMarketPlaceEndpoint = registration.getEndpoint();
	    // Wallet and Bid objects are created based on
	    // BudgetAllocation and DemandResponse values.
	    // state is observed based on created bids.
 	}
        showSystemState();
    }
    
    public static void handleDeregistration(Registration registration)
    {
        String rid = registration.getEndpoint();
        Boolean changed = false;
	// Clean up local state.
    }
    
    public static void handleObserveResponse(SingleObservation observation, Registration registration, ObserveResponse response)
    {
        if (registration != null && observation != null && response != null) {
	    // Check whether registration is known in list of known objects.
	    String rid = registration.getEndpoint();
	    String obsPath = observation.getPath().toString();
	    Boolean changed = false;
	    System.out.println(">>ObserveResponse " + obsPath);
	    // Process updated values.
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
