/*
 *  Extension to leshan-server-demo for application code.
 *
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
import org.eclipse.leshan.core.response.CreateResponse;
import org.eclipse.leshan.core.request.WriteRequest;
import org.eclipse.leshan.core.request.ReadRequest;
import org.eclipse.leshan.core.request.CreateRequest;
import org.eclipse.leshan.core.request.ObserveRequest;
import org.eclipse.leshan.core.request.WriteRequest.Mode;

import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.LwM2mSingleResource;
import org.eclipse.leshan.core.observation.SingleObservation;

/*
 *  Flow-control
 *
 *  Preconfigured state:
 *  * RoomControl name
 *  * EnergyControl name
 *
 *  Upon registration of an Energy Marketplace client
 *    * Create a Match object for RoomControl+EnergyControl
 *    * Observe the match_status of the created object.
 *  Upon a notication of match_status
 *    * randomly set the related Bid.bid_state to exception.
 *
 *  NOTE: it is not easy to match the Match.RoomControl_name
 *        with a Bid.RoomControl_name, as those values are
 *        on the client with no search option.
 *        Alternative: add a Match.bid_instance_id, such
 *        that Bid can be found easily with /obj_id/inst_id/resc_id
 *
 *  For now: only support a single bid/offer or Room/Energy combination.
 */


public class IoT {
    
    //
    // 2IMN15: keep state.
    //
    private static LeshanServer lwServer;

    // which RoomControl and EnergyControl.
    private static String roomControlName;
    private static String energyControlName;

    private static int matchInstanceId;
    private static int bidInstanceId;
    private static Registration energyMarketPlace;
    
    public static void Initialize(LeshanServer server)
    {
	lwServer = server;
	bidInstanceId = -1;
	matchInstanceId = -1;
	roomControlName = "RoomControl1";
	energyControlName = "EnergyControl1";
	energyMarketPlace = null;
    }

    public static void handleRegistration(Registration registration)
    {
        // Check which objects are available.
        Map<Integer,org.eclipse.leshan.core.LwM2m.Version> supportedObject =
	    registration.getSupportedObject();

        if (supportedObject.get(Constants.MARKETPLACE_STATE_ID) != null) {
	    // State of a MarketPlace, create a Match object.
	    try {
		System.out.println("Market place found. Create Match object");
		CreateRequest createReq =
		    new CreateRequest(Constants.MATCH_ID,
				      LwM2mSingleResource.newStringResource(Constants.RES_ROOMCONTROL_NAME, roomControlName),
				      LwM2mSingleResource.newStringResource(Constants.RES_ENERGYCONTROL_NAME, energyControlName),
				      LwM2mSingleResource.newStringResource(Constants.RES_MATCH_STATUS, "Not active"));
		CreateResponse createResp = lwServer.send(registration,
							  createReq,
							  2000);
		if (createResp.isSuccess()) {
		    String loc = createResp.getLocation();
		    System.out.println("Created a match object with location " + loc);
		    // Convert location to instance ID of match.
		    LwM2mPath path = new LwM2mPath(loc);
		    matchInstanceId = path.getObjectInstanceId();
		    // Observe the 
		    try {
			System.out.println(">>ObserveRequest created << ");
			ObserveResponse matchResponse = lwServer.send(registration, new ObserveRequest(Constants.MATCH_ID, matchInstanceId,Constants.RES_MATCH_STATUS), 3000);
			System.out.println(">>ObserveRequest sent << ");
			if (matchResponse == null) {
			    System.out.println(">> NULL <<");
			}
		    }
		    catch (Exception e) {
			System.out.println("Something wrong with observing created match.");
		    }
		} else {
		    System.out.println("Issue with create " + createResp.toString());
		}
	    }
	    catch (Exception e) {
		System.out.println("Issues with creating match instance.");
	    }
        }
    }
    
    public static void handleDeregistration(Registration registration)
    {
        String rid = registration.getEndpoint();
        Boolean changed = false;
    }
    
    public static void handleObserveResponse(SingleObservation observation, Registration registration, ObserveResponse response)
    {
        if (registration != null && observation != null && response != null) {
	    // Check whether registration is known in list of known objects.
	    String rid = registration.getEndpoint();
	    LwM2mPath obsPath = observation.getPath();
	    Boolean changed = false;
	    System.out.println(">>ObserveResponse " + obsPath.toString());
	    if ((obsPath.getObjectId() == Constants.MATCH_ID) &&
		(obsPath.getObjectInstanceId() == matchInstanceId) &&
		(obsPath.getResourceId() == Constants.RES_MATCH_STATUS)) {
		String stateValue = ((LwM2mResource)response.getContent()).getValue().toString();
		System.out.println("Match status = " + stateValue);
		try {
		    // Randomly set bid_state to exception.
		}
		catch (Exception e) {
		    System.out.println("Exception in reading match response:" + e.getMessage());
		}
	       
	    }
        }
    }


    // Support functions for reading and writing resources of
    // certain types.

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
