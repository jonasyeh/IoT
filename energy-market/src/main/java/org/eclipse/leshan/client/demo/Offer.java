/****************
 * This code was automatically generated by lwm2m_codegen.
 ****************/
package org.eclipse.leshan.client.demo;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Random;
import java.util.List;
import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.Scanner;

import org.eclipse.leshan.client.servers.ServerIdentity;
import org.eclipse.leshan.client.resource.BaseInstanceEnabler;
import org.eclipse.leshan.core.model.ResourceModel.Type;
import org.eclipse.leshan.core.model.ObjectModel;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.request.argument.Arguments;
import org.eclipse.leshan.core.response.ExecuteResponse;
import org.eclipse.leshan.core.response.ReadResponse;
import org.eclipse.leshan.core.response.WriteResponse;
import org.eclipse.leshan.core.util.NamedThreadFactory;


import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.JFrame;
import javax.swing.JComboBox;
import javax.swing.WindowConstants;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.Container;
import java.awt.GridLayout;
import java.awt.EventQueue;

public class Offer extends BaseInstanceEnabler {
    static final int OBJECT_ID_OFFER = 33008;
    static final int OBJECT_ID = 33008;
    // Static values for resource items
    private static final int RES_ENERGYCONTROL_NAME = 30030;
    private static final int RES_OFFER_STATE = 30031;
    private static final int RES_TOTAL_TRANSFERABLE_ENERGY_AMOUNT = 30032;
    private static final int RES_ENERGY_TO_BE_TRANSFERRED = 30033;
    private static final int RES_PRICE_PER_ENERGY_UNIT = 30034;
    private static final List<Integer> supportedResources =
     Arrays.asList(
             RES_ENERGYCONTROL_NAME
           , RES_OFFER_STATE
           , RES_TOTAL_TRANSFERABLE_ENERGY_AMOUNT
           , RES_ENERGY_TO_BE_TRANSFERRED
           , RES_PRICE_PER_ENERGY_UNIT
           );
    // Variables storing current values.

    private String vEnergycontrolName = "";
   private JLabel glEnergycontrolName;
   private JLabel gvEnergycontrolName;
   private JTextField tfEnergycontrolName;

  // Init, Success,Failure
    private String vOfferState = "";
   private JLabel glOfferState;
   private JLabel gvOfferState;
   private JComboBox cbOfferState;
   private String[] cbvOfferState = { "Init", "Success", "Failure" };

    private long vTotalTransferableEnergyAmount = 0;
   private JLabel glTotalTransferableEnergyAmount;
   private JLabel gvTotalTransferableEnergyAmount;
   private JTextField tfTotalTransferableEnergyAmount;

    private long vEnergyToBeTransferred = 0;
   private JLabel glEnergyToBeTransferred;
   private JLabel gvEnergyToBeTransferred;
   private JTextField tfEnergyToBeTransferred;

    private long vPricePerEnergyUnit = 0;
   private JLabel glPricePerEnergyUnit;
   private JLabel gvPricePerEnergyUnit;
   private JTextField tfPricePerEnergyUnit;
   private JFrame guiFrame;

  public Offer() {
      //  Automatically generated GUI code.
    guiFrame = new JFrame();
    guiFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    guiFrame.setTitle("Offer");

  // EnergyControl Name
    glEnergycontrolName = new JLabel();
    glEnergycontrolName.setText("EnergyControl Name");
    gvEnergycontrolName = new JLabel();
    gvEnergycontrolName.setText("");
    tfEnergycontrolName = new JTextField();
    tfEnergycontrolName.addActionListener(new java.awt.event.ActionListener() {
       public void actionPerformed(java.awt.event.ActionEvent evt) {
           String sValue = (String)tfEnergycontrolName.getText();
           setEnergycontrolName(String.valueOf(sValue));
       }
     });

  // Offer state
    glOfferState = new JLabel();
    glOfferState.setText("Offer state");
    gvOfferState = new JLabel();
    gvOfferState.setText("");
    cbOfferState = new JComboBox(cbvOfferState);
    cbOfferState.addActionListener(new java.awt.event.ActionListener() {
       public void actionPerformed(java.awt.event.ActionEvent evt) {
           String sValue = (String)cbOfferState.getSelectedItem();
           setOfferState(String.valueOf(sValue));
       }
     });

  // Total transferable energy amount
    glTotalTransferableEnergyAmount = new JLabel();
    glTotalTransferableEnergyAmount.setText("Total transferable energy amount");
    gvTotalTransferableEnergyAmount = new JLabel();
    gvTotalTransferableEnergyAmount.setText("");
    tfTotalTransferableEnergyAmount = new JTextField();
    tfTotalTransferableEnergyAmount.addActionListener(new java.awt.event.ActionListener() {
       public void actionPerformed(java.awt.event.ActionEvent evt) {
           String sValue = (String)tfTotalTransferableEnergyAmount.getText();
           setTotalTransferableEnergyAmount(Long.valueOf(sValue));
       }
     });

  // Energy to be transferred
    glEnergyToBeTransferred = new JLabel();
    glEnergyToBeTransferred.setText("Energy to be transferred");
    gvEnergyToBeTransferred = new JLabel();
    gvEnergyToBeTransferred.setText("");
    tfEnergyToBeTransferred = new JTextField();
    tfEnergyToBeTransferred.addActionListener(new java.awt.event.ActionListener() {
       public void actionPerformed(java.awt.event.ActionEvent evt) {
           String sValue = (String)tfEnergyToBeTransferred.getText();
           setEnergyToBeTransferred(Long.valueOf(sValue));
       }
     });

  // Price per energy unit
    glPricePerEnergyUnit = new JLabel();
    glPricePerEnergyUnit.setText("Price per energy unit");
    gvPricePerEnergyUnit = new JLabel();
    gvPricePerEnergyUnit.setText("");
    tfPricePerEnergyUnit = new JTextField();
    tfPricePerEnergyUnit.addActionListener(new java.awt.event.ActionListener() {
       public void actionPerformed(java.awt.event.ActionEvent evt) {
           String sValue = (String)tfPricePerEnergyUnit.getText();
           setPricePerEnergyUnit(Long.valueOf(sValue));
       }
     });

  // Create layout of labels, inputs and values.
   GridLayout layout = new GridLayout(0,3,10,10);
   guiFrame.getContentPane().setLayout(layout);
   Container guiPane = guiFrame.getContentPane();
   guiPane.add(glEnergycontrolName);
   guiPane.add(tfEnergycontrolName);
    guiPane.add(gvEnergycontrolName);
   guiPane.add(glOfferState);
   guiPane.add(cbOfferState);
    guiPane.add(gvOfferState);
   guiPane.add(glTotalTransferableEnergyAmount);
   guiPane.add(tfTotalTransferableEnergyAmount);
    guiPane.add(gvTotalTransferableEnergyAmount);
   guiPane.add(glEnergyToBeTransferred);
   guiPane.add(tfEnergyToBeTransferred);
    guiPane.add(gvEnergyToBeTransferred);
   guiPane.add(glPricePerEnergyUnit);
   guiPane.add(tfPricePerEnergyUnit);
    guiPane.add(gvPricePerEnergyUnit);
  guiFrame.pack();
  // Code to make the frame visible.
  java.awt.EventQueue.invokeLater(new Runnable() {
      public void run() {
         guiFrame.setVisible(true);
      }
    });
  }

  @Override
  public synchronized ReadResponse read(ServerIdentity identity, int resourceId) {
    switch (resourceId) {
    case RES_ENERGYCONTROL_NAME:
         return ReadResponse.success(resourceId, vEnergycontrolName);
    case RES_OFFER_STATE:
         return ReadResponse.success(resourceId, vOfferState);
    case RES_TOTAL_TRANSFERABLE_ENERGY_AMOUNT:
         return ReadResponse.success(resourceId, vTotalTransferableEnergyAmount);
    case RES_ENERGY_TO_BE_TRANSFERRED:
         return ReadResponse.success(resourceId, vEnergyToBeTransferred);
    case RES_PRICE_PER_ENERGY_UNIT:
         return ReadResponse.success(resourceId, vPricePerEnergyUnit);
    default:
      return super.read(identity, resourceId);
    }
  }

  @Override
  public WriteResponse write(ServerIdentity identity, boolean replace, int resourceId, LwM2mResource value) {
    switch (resourceId) {
    case RES_ENERGYCONTROL_NAME:
        // vEnergycontrolName = (String) value.getValue();
        // fireResourceChange(resourceId);
        setEnergycontrolName((String) value.getValue());
        return WriteResponse.success();
    case RES_OFFER_STATE:
        // vOfferState = (String) value.getValue();
        // fireResourceChange(resourceId);
        setOfferState((String) value.getValue());
        return WriteResponse.success();
    case RES_TOTAL_TRANSFERABLE_ENERGY_AMOUNT:
        // vTotalTransferableEnergyAmount = (Long) value.getValue();
        // fireResourceChange(resourceId);
        setTotalTransferableEnergyAmount((Long) value.getValue());
        return WriteResponse.success();
    case RES_ENERGY_TO_BE_TRANSFERRED:
        // vEnergyToBeTransferred = (Long) value.getValue();
        // fireResourceChange(resourceId);
        setEnergyToBeTransferred((Long) value.getValue());
        return WriteResponse.success();
    case RES_PRICE_PER_ENERGY_UNIT:
        // vPricePerEnergyUnit = (Long) value.getValue();
        // fireResourceChange(resourceId);
        setPricePerEnergyUnit((Long) value.getValue());
        return WriteResponse.success();
    default:
      return super.write(identity, replace, resourceId,value);
    }
  }

  @Override
  public synchronized ExecuteResponse execute(ServerIdentity identity, int resourceId, Arguments arguments) {
    switch (resourceId) {
    default:
      return super.execute(identity, resourceId,arguments);
    }
  }

  @Override
  public List<Integer> getAvailableResourceIds(ObjectModel model) {
     return supportedResources;
  }

  private synchronized void setEnergycontrolName(String value) {
    if (vEnergycontrolName != value) {
       vEnergycontrolName = value;
 gvEnergycontrolName.setText(vEnergycontrolName);
       fireResourceChange(RES_ENERGYCONTROL_NAME);
    }
  }

  private synchronized void setOfferState(String value) {
    if (vOfferState != value) {
       vOfferState = value;
 gvOfferState.setText(vOfferState);
       fireResourceChange(RES_OFFER_STATE);
    }
  }

  private synchronized void setTotalTransferableEnergyAmount(long value) {
    if (vTotalTransferableEnergyAmount != value) {
       vTotalTransferableEnergyAmount = value;
 gvTotalTransferableEnergyAmount.setText(Long.toString(vTotalTransferableEnergyAmount));
       fireResourceChange(RES_TOTAL_TRANSFERABLE_ENERGY_AMOUNT);
    }
  }

  private synchronized void setEnergyToBeTransferred(long value) {
    if (vEnergyToBeTransferred != value) {
       vEnergyToBeTransferred = value;
 gvEnergyToBeTransferred.setText(Long.toString(vEnergyToBeTransferred));
       fireResourceChange(RES_ENERGY_TO_BE_TRANSFERRED);
    }
  }

  private synchronized void setPricePerEnergyUnit(long value) {
    if (vPricePerEnergyUnit != value) {
       vPricePerEnergyUnit = value;
 gvPricePerEnergyUnit.setText(Long.toString(vPricePerEnergyUnit));
       fireResourceChange(RES_PRICE_PER_ENERGY_UNIT);
    }
  }

}
