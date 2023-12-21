package org.eclipse.leshan.client.demo;

import org.eclipse.leshan.client.resource.LwM2mObjectEnabler;
import org.eclipse.leshan.client.resource.LwM2mObjectTree;
import org.eclipse.leshan.client.resource.listener.ObjectListener;
import org.eclipse.leshan.client.servers.ServerIdentity;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.request.ReadRequest;
import org.eclipse.leshan.core.request.WriteRequest;
import org.eclipse.leshan.core.response.ReadResponse;
import org.eclipse.leshan.core.response.WriteResponse;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class AuctionRunnable implements Runnable {

    private LocalDateTime startTime;
    private final LwM2mObjectTree objectTree;

    public AuctionRunnable(LwM2mObjectTree objectTree) {
        this.objectTree = objectTree;

        // Register resource listener for marketplace state
        objectTree.getObjectEnabler(MarketplaceState.OBJECT_ID).addListener(new ObjectListener() {
            @Override
            public void objectInstancesAdded(LwM2mObjectEnabler object, int... instanceIds) {

            }

            @Override
            public void objectInstancesRemoved(LwM2mObjectEnabler object, int... instanceIds) {

            }

            @Override
            public void resourceChanged(LwM2mPath... paths) {
                for (LwM2mPath path : paths) {
                    if (!path.getResourceId().equals(Constants.RES_MARKETPLACE_STATE)) {
                        continue;
                    }

                    // Retrieve marketplace instance id from object tree
                    String marketplaceState = readString(objectTree.getObjectEnabler(MarketplaceState.OBJECT_ID), path.getObjectInstanceId(), path.getResourceId());

                    if (marketplaceState.equals("")) {
                        System.out.println("Failed to read marketplace state");
                        continue;
                    }

                    if (marketplaceState.equals("Acquisition")) {
                        // Reset start time
                        startTime = LocalDateTime.now();
                    }
                }
            }
        });
    }

    @Override
    public void run() {
        // Check if the marketplace exists and the start time has been set, thus acquisition state
        LwM2mObjectEnabler marketplaceObject = objectTree.getObjectEnabler(MarketplaceState.OBJECT_ID);
        if (marketplaceObject.getAvailableInstanceIds().size() < 1 || startTime == null) {
            return;
        }

        int marketplaceId = marketplaceObject.getAvailableInstanceIds().get(0);

        // Compute time since acquisition start
        long minutes = Duration.between(startTime, LocalDateTime.now()).toMillis()/1000;
        System.out.println("Checking auction time: " + minutes);

        if (minutes < readInteger(marketplaceObject, marketplaceId, Constants.RES_MARKET_END_TIME)) {
            // End time has not been reached yet
            return;
        }

        // Retrieve all objects from object tree
        LwM2mObjectEnabler offerObject = objectTree.getObjectEnabler(Offer.OBJECT_ID);
        LwM2mObjectEnabler matchObject = objectTree.getObjectEnabler(Match.OBJECT_ID);
        LwM2mObjectEnabler bidObject = objectTree.getObjectEnabler(Bid.OBJECT_ID);
        LwM2mObjectEnabler walletObject = objectTree.getObjectEnabler(Wallet.OBJECT_ID);

        writeString(marketplaceObject, marketplaceId, Constants.RES_MARKETPLACE_STATE, "Matching");

        System.out.println("Matching");

        // Initialize available offer and bid lists
        ArrayList<Integer> localOffers = new ArrayList<>();
        ArrayList<Integer> localBids = new ArrayList<>();

        // Add all bids that match requirements to the list
        for (int instanceId : bidObject.getAvailableInstanceIds()) {
            String roomControlName = readString(bidObject, instanceId, Constants.RES_ROOMCONTROL_NAME);
            int walletId = getWalletInstanceId(roomControlName);
            if (readInteger(bidObject, instanceId, Constants.RES_REQUESTED_PRICE_PER_ENERGY_UNIT) == 0 || readInteger(bidObject, instanceId, Constants.RES_REQUESTED_ENERGY_AMOUNT) == 0
                || walletId == -1 || readInteger(walletObject, walletId, Constants.RES_COIN_BALANCE) == 0
                || readInteger(bidObject, instanceId, Constants.RES_REQUESTED_PRICE_PER_ENERGY_UNIT) * readInteger(bidObject, instanceId, Constants.RES_REQUESTED_ENERGY_AMOUNT) > readInteger(walletObject, walletId, Constants.RES_COIN_BALANCE)) {
                writeString(bidObject, instanceId, Constants.RES_BID_STATE, "Exception");
                continue;
            }

            localBids.add(instanceId);
        }

        // Add all offers that match requirements to the list
        for (int instanceId : offerObject.getAvailableInstanceIds()) {
            if (readInteger(offerObject, instanceId, Constants.RES_PRICE_PER_ENERGY_UNIT) == 0 || readInteger(offerObject, instanceId, Constants.RES_TOTAL_TRANSFERABLE_ENERGY_AMOUNT) == 0) {
                writeString(offerObject, instanceId, Constants.RES_OFFER_STATE, "Failure");
                continue;
            }

            localOffers.add(instanceId);
        }

        System.out.println("Offers: " + localOffers.size() + " Bids: " + localBids.size());

        // Initialize indices arrays
        int local_offer_indices[] = new int[localOffers.size()];
        Arrays.fill(local_offer_indices, -1);

        int local_bid_index_groups[][] = new int[localOffers.size()][localBids.size()];
        Arrays.stream(local_bid_index_groups).forEach(a -> Arrays.fill(a, -1));

        // Match offers and bids by price per energy unit
        for (int offerIndex = 0; offerIndex < localOffers.size(); offerIndex++) {
            int offerId = localOffers.get(offerIndex);
            int energyPerUnit = readInteger(offerObject, offerId, Constants.RES_PRICE_PER_ENERGY_UNIT);
            for (int bidIndex = 0; bidIndex < localBids.size(); bidIndex++) {
                int bidId = localBids.get(bidIndex);
                if (energyPerUnit <= readInteger(bidObject, bidId, Constants.RES_REQUESTED_PRICE_PER_ENERGY_UNIT)) {
                    if (local_offer_indices[offerIndex] == -1) {
                        local_offer_indices[offerIndex] = offerIndex;
                    }
                    local_bid_index_groups[offerIndex][bidIndex] = bidIndex;
                }
            }
        }

        // Initialize map and sort array by total transferable energy
        Map<Integer, Integer> local_accomodated_bids_offers = new HashMap<Integer, Integer>();
        local_offer_indices = Arrays.stream(local_offer_indices).
                boxed().
                sorted((a, b) -> {
                    if (a == -1 && b == -1) {
                        return 0;
                    } else if (a == -1) {
                        return -1;
                    } else if (b == -1) {
                        return 1;
                    }

                    int amountA = readInteger(offerObject, localOffers.get(a), Constants.RES_TOTAL_TRANSFERABLE_ENERGY_AMOUNT);
                    int amountB = readInteger(offerObject, localOffers.get(b), Constants.RES_TOTAL_TRANSFERABLE_ENERGY_AMOUNT);

                    return Integer.compare(amountA, amountB);
                }). // sort descending
                        mapToInt(i -> i).
                toArray();

        writeString(marketplaceObject, marketplaceId, Constants.RES_MARKETPLACE_STATE, "Validation");

        // Match bids to offers and subtract from available energy
        for (int offer_index : local_offer_indices) {
            if (offer_index == -1) {
                break;
            }

            int energy_left = readInteger(offerObject, localOffers.get(offer_index), Constants.RES_TOTAL_TRANSFERABLE_ENERGY_AMOUNT);
            local_bid_index_groups[offer_index] = Arrays.stream(local_bid_index_groups[offer_index]).
                    boxed().
                    sorted((a, b) -> {
                        if (a == -1 && b == -1) {
                            return 0;
                        } else if (a == -1) {
                            return 1;
                        } else if (b == -1) {
                            return -1;
                        }

                        int amountA = readInteger(bidObject, localBids.get(a), Constants.RES_REQUESTED_ENERGY_AMOUNT);
                        int amountB = readInteger(bidObject, localBids.get(b), Constants.RES_REQUESTED_ENERGY_AMOUNT);

                        return Integer.compare(amountB, amountA);
                    }). // sort descending
                            mapToInt(i -> i).
                    toArray();

            for (int bid_index : local_bid_index_groups[offer_index]) {
                if (bid_index == -1) {
                    break;
                }

                if (readInteger(bidObject, localBids.get(bid_index), Constants.RES_REQUESTED_ENERGY_AMOUNT) <= energy_left &&
                    !local_accomodated_bids_offers.keySet().contains(bid_index)) {
                    local_accomodated_bids_offers.put(bid_index, offer_index);
                    energy_left -= readInteger(bidObject, localBids.get(bid_index), Constants.RES_REQUESTED_ENERGY_AMOUNT);
                }
            }

            if (readInteger(offerObject, localOffers.get(offer_index), Constants.RES_TOTAL_TRANSFERABLE_ENERGY_AMOUNT) == energy_left) {
                String energyControlName = readString(offerObject, localOffers.get(offer_index), Constants.RES_ENERGYCONTROL_NAME);
                for (int offerId : localOffers) {
                    if (readString(offerObject, offerId, Constants.RES_ENERGYCONTROL_NAME).equals(energyControlName)) {
                        writeString(offerObject, offerId, Constants.RES_OFFER_STATE, "Failure");
                    }
                }
            } else {
                String energyControlName = readString(offerObject, localOffers.get(offer_index), Constants.RES_ENERGYCONTROL_NAME);
                int newEnergyToBeTransfered = readInteger(offerObject, localOffers.get(offer_index), Constants.RES_TOTAL_TRANSFERABLE_ENERGY_AMOUNT) - energy_left;
                for (int offerId : localOffers) {
                    if (readString(offerObject, offerId, Constants.RES_ENERGYCONTROL_NAME).equals(energyControlName)) {
                        writeInteger(offerObject, offerId, Constants.RES_ENERGY_TO_BE_TRANSFERRED, newEnergyToBeTransfered);
                    }
                }

                writeString(offerObject, localOffers.get(offer_index), Constants.RES_OFFER_STATE, "Success");
            }
        }

        // Update match and bid objects and update wallet
        for (int bid_index : local_accomodated_bids_offers.keySet()) {
            int offer_index = local_accomodated_bids_offers.get(bid_index);
            String roomControlName = readString(bidObject, localBids.get(bid_index), Constants.RES_ROOMCONTROL_NAME);
            String energyControlName = readString(offerObject, localOffers.get(offer_index), Constants.RES_ENERGYCONTROL_NAME);
            for (int bidId : localBids) {
                if (readString(bidObject, bidId, Constants.RES_ROOMCONTROL_NAME).equals(roomControlName)) {
                    writeString(bidObject, bidId, Constants.RES_BID_STATE, "Success");
                }
            }

            int cost = readInteger(bidObject, localBids.get(bid_index), Constants.RES_REQUESTED_ENERGY_AMOUNT) * readInteger(offerObject, localOffers.get(offer_index), 30034);

            writeInteger(walletObject, getWalletInstanceId(roomControlName), Constants.RES_COIN_BALANCE, -cost);
            writeString(matchObject, getMatchInstanceId(roomControlName, energyControlName), Constants.RES_MATCH_STATUS, "Active");
        }

        // Set all unmatched bids to failure
        List<Integer> unsuccessful_bids = IntStream.range(0, localBids.size()-1).boxed().collect(Collectors.toList());
        unsuccessful_bids.removeAll(local_accomodated_bids_offers.keySet());
        for (int bid_index : unsuccessful_bids) {
            String roomControlName = readString(offerObject, localBids.get(bid_index), Constants.RES_ROOMCONTROL_NAME);
            for (int bidId : localBids) {
                if (readString(bidObject, bidId, Constants.RES_ROOMCONTROL_NAME).equals(roomControlName)) {
                    writeString(bidObject, bidId, Constants.RES_BID_STATE, "Failure");
                }
            }
        }

        // Sleep to show results of auction
        /*try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }*/

        // Reset all bids to Init state
        for (int instanceId : bidObject.getAvailableInstanceIds()) {
            writeString(bidObject, instanceId, Constants.RES_BID_STATE, "Init");
        }

        // Reset all offers to Init state
        for (int instanceId : offerObject.getAvailableInstanceIds()) {
            writeString(offerObject, instanceId, Constants.RES_OFFER_STATE,  "Init");
            writeInteger(offerObject, instanceId, Constants.RES_ENERGY_TO_BE_TRANSFERRED, 0);
        }

        // Reset all matches to Not active state
        for (int instanceId : matchObject.getAvailableInstanceIds()) {
            writeString(matchObject, instanceId, Constants.RES_MATCH_STATUS, "Not active");
        }

        writeString(marketplaceObject, marketplaceId, Constants.RES_MARKETPLACE_STATE, "Acquisition");
    }

    private int getWalletInstanceId(String roomControlName) {
        // Get the instance id of the wallet with a given room control name
        LwM2mObjectEnabler walletObject = objectTree.getObjectEnabler(Wallet.OBJECT_ID);
        for (int instanceId : walletObject.getAvailableInstanceIds()) {
            if (readString(walletObject, instanceId, Constants.RES_ROOMCONTROL_NAME).equals(roomControlName)) {
                return instanceId;
            }
        }

        return -1;
    }

    private int getMatchInstanceId(String roomControlName, String energyControlName) {
        // Get the instance id of the match with given room control name and energy control name
        LwM2mObjectEnabler matchObject = objectTree.getObjectEnabler(Match.OBJECT_ID);
        for (int instanceId : matchObject.getAvailableInstanceIds()) {
            if (readString(matchObject, instanceId, Constants.RES_ROOMCONTROL_NAME).equals(roomControlName)
                && readString(matchObject, instanceId, Constants.RES_ENERGYCONTROL_NAME).equals(energyControlName)) {
                return instanceId;
            }
        }

        return -1;
    }

    private static int readInteger(LwM2mObjectEnabler object, int instanceId, int resourceId) {
        try {
            // Request integer from local object instance
            ReadRequest request = new ReadRequest(object.getId(), instanceId, resourceId);
            ReadResponse cResponse = object.read(ServerIdentity.SYSTEM, request);
            if (cResponse.isSuccess()) {
                String sValue = ((LwM2mResource) cResponse.getContent()).getValue().toString();
                try {
                    int iValue = Integer.parseInt(sValue);
                    return iValue;
                } catch (Exception e) {
                }
                float fValue = Float.parseFloat(((LwM2mResource) cResponse.getContent()).getValue().toString());
                return (int) fValue;
            } else {
                return 0;
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
            System.out.println("readInteger: exception");
            return 0;
        }
    }

    private static String readString(LwM2mObjectEnabler object, int instanceId, int resourceId) {
        try {
            // Request string from local object instance
            ReadRequest request = new ReadRequest(object.getId(), instanceId, resourceId);
            ReadResponse cResponse = object.read(ServerIdentity.SYSTEM, request);
            if (cResponse.isSuccess()) {
                String value = ((LwM2mResource) cResponse.getContent()).getValue().toString();
                return value;
            } else {
                return "";
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
            System.out.println("readString: exception");
            return "";
        }
    }

    private static void writeInteger(LwM2mObjectEnabler object, int instanceId, int resourceId, int value) {
        try {
            // Write integer to local object instance's resource
            WriteRequest request = new WriteRequest(object.getId(), instanceId, resourceId, value);
            WriteResponse cResponse = object.write(ServerIdentity.SYSTEM, request);
            if (cResponse.isSuccess()) {
                System.out.println("writeInteger: Success");
            } else {
                System.out.println("writeInteger: Failed, " + cResponse.toString());
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
            System.out.println("writeInteger: exception");
        }
    }

    private static void writeString(LwM2mObjectEnabler object, int instanceId, int resourceId, String value) {
        try {
            // Write string to local object instance's resource
            WriteRequest request = new WriteRequest(object.getId(), instanceId, resourceId, value);
            WriteResponse cResponse = object.write(ServerIdentity.SYSTEM, request);
            if (cResponse.isSuccess()) {
                System.out.println("writeString: Success");
            } else {
                System.out.println("writeString: Failed, " + cResponse.toString());
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
            System.out.println("writeString: exception");
        }
    }
}
