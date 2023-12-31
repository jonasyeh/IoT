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

public class BudgetAllocation extends BaseInstanceEnabler {
    static final int OBJECT_ID_BUDGET_ALLOCATION = 33003;
    static final int OBJECT_ID = 33003;
    // Static values for resource items
    private static final int RES_BUDGET_AMOUNT = 30009;
    private static final int RES_PRICE = 30010;
    private static final List<Integer> supportedResources =
     Arrays.asList(
             RES_BUDGET_AMOUNT
           , RES_PRICE
           );
    // Variables storing current values.

    private long vBudgetAmount = 0;
   private JLabel glBudgetAmount;
   private JLabel gvBudgetAmount;
   private JTextField tfBudgetAmount;

    private long vPrice = 0;
   private JLabel glPrice;
   private JLabel gvPrice;
   private JTextField tfPrice;
   private JFrame guiFrame;

  public BudgetAllocation() {
      //  Automatically generated GUI code.
    guiFrame = new JFrame();
    guiFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    guiFrame.setTitle("Budget Allocation");

  // Budget amount
    glBudgetAmount = new JLabel();
    glBudgetAmount.setText("Budget amount");
    gvBudgetAmount = new JLabel();
    gvBudgetAmount.setText("");
    tfBudgetAmount = new JTextField();
    tfBudgetAmount.addActionListener(new java.awt.event.ActionListener() {
       public void actionPerformed(java.awt.event.ActionEvent evt) {
           String sValue = (String)tfBudgetAmount.getText();
           setBudgetAmount(Long.valueOf(sValue));
       }
     });

  // Price
    glPrice = new JLabel();
    glPrice.setText("Price");
    gvPrice = new JLabel();
    gvPrice.setText("");
    tfPrice = new JTextField();
    tfPrice.addActionListener(new java.awt.event.ActionListener() {
       public void actionPerformed(java.awt.event.ActionEvent evt) {
           String sValue = (String)tfPrice.getText();
           setPrice(Long.valueOf(sValue));
       }
     });

  // Create layout of labels, inputs and values.
   GridLayout layout = new GridLayout(0,3,10,10);
   guiFrame.getContentPane().setLayout(layout);
   Container guiPane = guiFrame.getContentPane();
   guiPane.add(glBudgetAmount);
   guiPane.add(tfBudgetAmount);
    guiPane.add(gvBudgetAmount);
   guiPane.add(glPrice);
   guiPane.add(tfPrice);
    guiPane.add(gvPrice);
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
    case RES_BUDGET_AMOUNT:
         return ReadResponse.success(resourceId, vBudgetAmount);
    case RES_PRICE:
         return ReadResponse.success(resourceId, vPrice);
    default:
      return super.read(identity, resourceId);
    }
  }

  @Override
  public WriteResponse write(ServerIdentity identity, boolean replace, int resourceId, LwM2mResource value) {
    switch (resourceId) {
    case RES_BUDGET_AMOUNT:
        // vBudgetAmount = (Long) value.getValue();
        // fireResourceChange(resourceId);
        setBudgetAmount((Long) value.getValue());
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

  private synchronized void setBudgetAmount(long value) {
    if (vBudgetAmount != value) {
       vBudgetAmount = value;
 gvBudgetAmount.setText(Long.toString(vBudgetAmount));
       fireResourceChange(RES_BUDGET_AMOUNT);
    }
  }

  private synchronized void setPrice(long value) {
    if (vPrice != value) {
       vPrice = value;
 gvPrice.setText(Long.toString(vPrice));
       fireResourceChange(RES_PRICE);
    }
  }

}
