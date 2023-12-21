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

    // state of the batteries and solar panels

    // state of the offers

    
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
        // Objects Constants.BATTERY_ID and Constants.SOLAR_PANEL_ID.
	boolean changed=false;
        if (supportedObject.get(Constants.BATTERY_ID) != null) {
	    System.out.println("Battery");
	    // Update state.
	    // Read resources.

	    // If needed, observe resources.
        }
        if (supportedObject.get(Constants.SOLAR_PANEL_ID) != null) {
	    System.out.println("Solar Panel");
	    // Update state.
	    // Read resources.
	    
	    // If needed, observe resources.
        }
	if (supportedObject.get(Constants.MARKETPLACE_STATE_ID) != null) {
	    System.out.println("Energy Market");
	    // Update state.
	    
	    // Create offer.
	    changed=true;
        }
        showSystemState();
    }
    
    public static void handleDeregistration(Registration registration)
    {
        String rid = registration.getEndpoint();
	// Check which clients / objects have disappeared.
	// Update state.
    }
    
    public static void handleObserveResponse(SingleObservation observation, Registration registration, ObserveResponse response)
    {
        if (registration != null && observation != null && response != null) {
	    // Check whether registration is known in list of known objects.
	    String rid = registration.getEndpoint();
	    String obsPath = observation.getPath().toString();
	    Boolean changed = false;
	    System.out.println(">>ObserveResponse " + obsPath);
	    // Process the updated values.

           if (changed) {
	       showSystemState();
           }
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
