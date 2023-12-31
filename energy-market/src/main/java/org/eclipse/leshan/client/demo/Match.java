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

public class Match extends BaseInstanceEnabler {
    static final int OBJECT_ID_MATCH = 33010;
    static final int OBJECT_ID = 33010;
    // Static values for resource items
    private static final int RES_ROOMCONTROL_NAME = 30016;
    private static final int RES_ENERGYCONTROL_NAME = 30030;
    private static final int RES_MATCH_STATUS = 30053;
    private static final List<Integer> supportedResources =
     Arrays.asList(
             RES_ROOMCONTROL_NAME
           , RES_ENERGYCONTROL_NAME
           , RES_MATCH_STATUS
           );
    // Variables storing current values.

    private String vRoomcontrolName = "";
   private JLabel glRoomcontrolName;
   private JLabel gvRoomcontrolName;
   private JTextField tfRoomcontrolName;

    private String vEnergycontrolName = "";
   private JLabel glEnergycontrolName;
   private JLabel gvEnergycontrolName;
   private JTextField tfEnergycontrolName;

  // Active, Notactive
    private String vMatchStatus = "";
   private JLabel glMatchStatus;
   private JLabel gvMatchStatus;
   private JComboBox cbMatchStatus;
   private String[] cbvMatchStatus = { "Active", "Not active" };
   private JFrame guiFrame;

  public Match() {
      //  Automatically generated GUI code.
    guiFrame = new JFrame();
    guiFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    guiFrame.setTitle("Match");

  // RoomControl Name
    glRoomcontrolName = new JLabel();
    glRoomcontrolName.setText("RoomControl Name");
    gvRoomcontrolName = new JLabel();
    gvRoomcontrolName.setText("");
    tfRoomcontrolName = new JTextField();
    tfRoomcontrolName.addActionListener(new java.awt.event.ActionListener() {
       public void actionPerformed(java.awt.event.ActionEvent evt) {
           String sValue = (String)tfRoomcontrolName.getText();
           setRoomcontrolName(String.valueOf(sValue));
       }
     });

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

  // Match Status
    glMatchStatus = new JLabel();
    glMatchStatus.setText("Match Status");
    gvMatchStatus = new JLabel();
    gvMatchStatus.setText("");
    cbMatchStatus = new JComboBox(cbvMatchStatus);
    cbMatchStatus.addActionListener(new java.awt.event.ActionListener() {
       public void actionPerformed(java.awt.event.ActionEvent evt) {
           String sValue = (String)cbMatchStatus.getSelectedItem();
           setMatchStatus(String.valueOf(sValue));
       }
     });

  // Create layout of labels, inputs and values.
   GridLayout layout = new GridLayout(0,3,10,10);
   guiFrame.getContentPane().setLayout(layout);
   Container guiPane = guiFrame.getContentPane();
   guiPane.add(glRoomcontrolName);
   guiPane.add(tfRoomcontrolName);
    guiPane.add(gvRoomcontrolName);
   guiPane.add(glEnergycontrolName);
   guiPane.add(tfEnergycontrolName);
    guiPane.add(gvEnergycontrolName);
   guiPane.add(glMatchStatus);
   guiPane.add(cbMatchStatus);
    guiPane.add(gvMatchStatus);
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
    case RES_ROOMCONTROL_NAME:
         return ReadResponse.success(resourceId, vRoomcontrolName);
    case RES_ENERGYCONTROL_NAME:
         return ReadResponse.success(resourceId, vEnergycontrolName);
    case RES_MATCH_STATUS:
         return ReadResponse.success(resourceId, vMatchStatus);
    default:
      return super.read(identity, resourceId);
    }
  }

  @Override
  public WriteResponse write(ServerIdentity identity, boolean replace, int resourceId, LwM2mResource value) {
    switch (resourceId) {
    case RES_ROOMCONTROL_NAME:
        // vRoomcontrolName = (String) value.getValue();
        // fireResourceChange(resourceId);
        setRoomcontrolName((String) value.getValue());
        return WriteResponse.success();
    case RES_ENERGYCONTROL_NAME:
        // vEnergycontrolName = (String) value.getValue();
        // fireResourceChange(resourceId);
        setEnergycontrolName((String) value.getValue());
        return WriteResponse.success();
    case RES_MATCH_STATUS:
        // vMatchStatus = (String) value.getValue();
        // fireResourceChange(resourceId);
        setMatchStatus((String) value.getValue());
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

  private synchronized void setRoomcontrolName(String value) {
    if (vRoomcontrolName != value) {
       vRoomcontrolName = value;
 gvRoomcontrolName.setText(vRoomcontrolName);
       fireResourceChange(RES_ROOMCONTROL_NAME);
    }
  }

  private synchronized void setEnergycontrolName(String value) {
    if (vEnergycontrolName != value) {
       vEnergycontrolName = value;
 gvEnergycontrolName.setText(vEnergycontrolName);
       fireResourceChange(RES_ENERGYCONTROL_NAME);
    }
  }

  private synchronized void setMatchStatus(String value) {
    if (vMatchStatus != value) {
       vMatchStatus = value;
 gvMatchStatus.setText(vMatchStatus);
       fireResourceChange(RES_MATCH_STATUS);
    }
  }

}
