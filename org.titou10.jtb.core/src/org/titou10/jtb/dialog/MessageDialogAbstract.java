/* Copyright (C) 2015 Denis Forveille titou10.titou10@gmail.com
 * 
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with this program. If not, see
 * <http://www.gnu.org/licenses/>. */
package org.titou10.jtb.dialog;

import java.io.IOException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jms.JMSException;

import org.eclipse.jface.bindings.keys.KeyStroke;
import org.eclipse.jface.bindings.keys.ParseException;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.fieldassist.ContentProposalAdapter;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.resource.FontDescriptor;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;
import org.eclipse.wb.swt.SWTResourceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.titou10.jtb.config.JTBPreferenceStore;
import org.titou10.jtb.jms.model.JTBMessageTemplate;
import org.titou10.jtb.jms.model.JTBMessageType;
import org.titou10.jtb.jms.model.JTBProperty;
import org.titou10.jtb.jms.qm.JMSPropertyKind;
import org.titou10.jtb.jms.util.JTBDeliveryMode;
import org.titou10.jtb.ui.JTBStatusReporter;
import org.titou10.jtb.ui.hex.BytesDataProvider;
import org.titou10.jtb.ui.hex.HexViewer;
import org.titou10.jtb.ui.hex.IDataProvider;
import org.titou10.jtb.util.Constants;
import org.titou10.jtb.util.FormatUtils;
import org.titou10.jtb.util.Utils;
import org.titou10.jtb.variable.VariablesManager;
import org.titou10.jtb.variable.dialog.VariableContentAdapter;
import org.titou10.jtb.variable.dialog.VariableContentProposalProvider;
import org.titou10.jtb.visualizer.VisualizersManager;

/**
 * Super class for dialogs that deal with Messages and templates
 * 
 * @author Denis Forveille
 *
 */
public abstract class MessageDialogAbstract extends Dialog {

   private static final Logger    log                    = LoggerFactory.getLogger(MessageDialogAbstract.class);

   private static final String    PROPERTY_NAME_INVALID  = "Property '%s' is not a valid JMS property identifier";
   private static final String    PROPERTY_ALREADY_EXIST = "A property with name '%s' is already defined";
   private static final String    PROPERTY_VALUE_INVALID = "The value is invalid for a variable of kind '%s'";

   private static final int       DEFAULT_PRIORITY       = 4;

   // Business data
   private JTBStatusReporter      jtbStatusReporter;
   private JTBPreferenceStore     ps;
   private VariablesManager       variablesManager;
   private VisualizersManager     visualizersManager;
   private JTBMessageTemplate     template;

   // JTBMessage data
   private JTBMessageType         jtbMessageType;
   private List<JTBProperty>      userProperties;
   private byte[]                 payloadBytes;
   private Map<String, Object>    payloadMap;

   // Message common Widgets
   private Button                 btnNonPersistent;
   private Button                 btnPersistent;
   private Text                   txtDeliveryDelay;
   private Text                   txtTimeToLive;

   private Text                   txtReplyToDestinationName;
   private Text                   txtType;
   private Text                   txtCorrelationID;
   private Text                   txtPayload;
   private HexViewer              hvPayLoadHex;
   private Spinner                spinnerPriority;
   private Label                  lblTimestamp;
   private Label                  lblDeliveryTime;
   private Label                  lblExpiration;

   private Composite              cFormat;
   private Composite              cVisualizer;

   // Properties
   private TableViewer            tvProperties;

   // Payload tab
   private TabItem                tbtmPayload;
   private Combo                  comboMessageType;

   private Button                 btnFormatXML;
   private Button                 btnFormatJSON;
   private Combo                  comboVisualizers;

   private Button                 btnExport;
   private Button                 btnImport;
   private Button                 btnShowAs;

   private StackLayout            payLoadStackLayout;
   private Composite              payloadComposite;
   private Composite              textPayloadComposite;
   private Composite              hexPayloadComposite;
   private Composite              mapPayloadComposite;

   private TableViewer            tvMapProperties;

   private ControlDecoration      deco;
   private ContentProposalAdapter contentAssistAdapter;

   public abstract String getDialogTitle();

   public abstract boolean isReadOnly();

   // -----------
   // Constructor
   // -----------
   public MessageDialogAbstract(Shell parentShell,
                                JTBStatusReporter jtbStatusReporter,
                                JTBPreferenceStore ps,
                                VariablesManager variablesManager,
                                VisualizersManager visualizersManager,
                                JTBMessageTemplate template) {
      super(parentShell);
      setShellStyle(SWT.MAX | SWT.RESIZE | SWT.TITLE | SWT.PRIMARY_MODAL);

      this.jtbStatusReporter = jtbStatusReporter;
      this.ps = ps;
      this.variablesManager = variablesManager;
      this.visualizersManager = visualizersManager;
      this.template = template;
   }

   // -----------
   // Dialog stuff
   // -----------

   @Override
   protected Control createDialogArea(Composite parent) {
      final Composite container = (Composite) super.createDialogArea(parent);
      container.setLayout(new FillLayout(SWT.HORIZONTAL));

      TabFolder tabFolder = new TabFolder(container, SWT.NONE);

      // ------------------
      // General Tab
      // ------------------
      TabItem tbtmGeneral = new TabItem(tabFolder, SWT.NONE);
      tbtmGeneral.setText("General");

      Composite composite = new Composite(tabFolder, SWT.NONE);
      composite.setLayout(new GridLayout(1, false));
      tbtmGeneral.setControl(composite);

      // Message Group

      Group groupMessage = new Group(composite, SWT.SHADOW_ETCHED_IN);
      groupMessage.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
      groupMessage.setText("Message Properties");
      groupMessage.setLayout(new GridLayout(2, false));

      Label lblNewLabel6 = new Label(groupMessage, SWT.NONE);
      lblNewLabel6.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
      lblNewLabel6.setText("JMS CorrelationID :");

      txtCorrelationID = new Text(groupMessage, SWT.BORDER);
      txtCorrelationID.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

      Label lblNewLabel5 = new Label(groupMessage, SWT.NONE);
      lblNewLabel5.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
      lblNewLabel5.setText("JMS Type :");

      txtType = new Text(groupMessage, SWT.BORDER);
      txtType.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false, 1, 1));

      Label lblNewLabel4 = new Label(groupMessage, SWT.NONE);
      lblNewLabel4.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
      lblNewLabel4.setText("JMS Reply To :");
      // lblNewLabel4.setEnabled(false);

      txtReplyToDestinationName = new Text(groupMessage, SWT.BORDER);
      txtReplyToDestinationName.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

      // Producer Group

      Group groupProducer = new Group(composite, SWT.SHADOW_ETCHED_IN);
      groupProducer.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
      groupProducer.setText("Message Producer Properties");
      groupProducer.setLayout(new GridLayout(2, false));

      Label lblDeliveryMode = new Label(groupProducer, SWT.NONE);
      lblDeliveryMode.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
      lblDeliveryMode.setText("Delivery Mode :");

      Composite deliveryModeGroup = new Composite(groupProducer, SWT.NULL);
      RowLayout rl = new RowLayout(SWT.HORIZONTAL);
      rl.marginLeft = -1;
      deliveryModeGroup.setLayout(rl);

      btnPersistent = new Button(deliveryModeGroup, SWT.RADIO);
      btnPersistent.setText("Persistent");

      btnNonPersistent = new Button(deliveryModeGroup, SWT.RADIO);
      btnNonPersistent.setText("Non Persistent");

      Label lblNewLabel7 = new Label(groupProducer, SWT.NONE);
      lblNewLabel7.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
      lblNewLabel7.setText("Priority :");

      spinnerPriority = new Spinner(groupProducer, SWT.BORDER);
      spinnerPriority.setMinimum(0);
      spinnerPriority.setMaximum(9);
      spinnerPriority.setTextLimit(5);
      spinnerPriority.setSelection(DEFAULT_PRIORITY);

      Label lblNewLabel81 = new Label(groupProducer, SWT.NONE);
      lblNewLabel81.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
      lblNewLabel81.setText("Delivery Delay (ms) :");

      GridData gd = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
      gd.widthHint = 70;

      txtDeliveryDelay = new Text(groupProducer, SWT.BORDER);
      txtDeliveryDelay.setLayoutData(gd);
      txtDeliveryDelay.setTextLimit(10);
      final Text txtDeliveryDelayFinal = txtDeliveryDelay;
      txtDeliveryDelay.addVerifyListener(new VerifyListener() {
         @Override
         public void verifyText(VerifyEvent e) {
            final String oldS = txtDeliveryDelayFinal.getText();
            final String newS = oldS.substring(0, e.start) + e.text + oldS.substring(e.end);
            if (!newS.isEmpty()) {
               try {
                  new Long(newS);
               } catch (final NumberFormatException nfe) {
                  e.doit = false;
               }
            }
         }
      });

      Label lblNewLabel8 = new Label(groupProducer, SWT.NONE);
      lblNewLabel8.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
      lblNewLabel8.setText("Time to Live (ms) :");

      txtTimeToLive = new Text(groupProducer, SWT.BORDER);
      txtTimeToLive.setLayoutData(gd);
      txtTimeToLive.setTextLimit(10);
      final Text txtTimeToLiveFinal = txtTimeToLive;
      txtTimeToLive.addVerifyListener(new VerifyListener() {
         @Override
         public void verifyText(VerifyEvent e) {
            final String oldS = txtTimeToLiveFinal.getText();
            final String newS = oldS.substring(0, e.start) + e.text + oldS.substring(e.end);
            if (!newS.isEmpty()) {
               try {
                  new Long(newS);
               } catch (final NumberFormatException nfe) {
                  e.doit = false;
               }
            }
         }
      });

      // Message Read Only Group

      Group groupMessageRO = new Group(composite, SWT.SHADOW_ETCHED_IN);
      groupMessageRO.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
      groupMessageRO.setText("Read Only Message Properties");
      groupMessageRO.setLayout(new GridLayout(2, false));

      Label lblNewLabel14 = new Label(groupMessageRO, SWT.NONE);
      lblNewLabel14.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
      lblNewLabel14.setText("JMS Timestamp :");

      lblTimestamp = new Label(groupMessageRO, SWT.NONE);
      lblTimestamp.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

      Label lblNewLabel142 = new Label(groupMessageRO, SWT.NONE);
      lblNewLabel142.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
      lblNewLabel142.setText("JMS Delivery Time :");

      lblDeliveryTime = new Label(groupMessageRO, SWT.NONE);
      lblDeliveryTime.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

      Label lblNewLabel143 = new Label(groupMessageRO, SWT.NONE);
      lblNewLabel143.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
      lblNewLabel143.setText("JMS Expiration :");

      lblExpiration = new Label(groupMessageRO, SWT.NONE);
      lblExpiration.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

      // ----------------
      // Properties Tab
      // ----------------

      TabItem tbtmUserProperty = new TabItem(tabFolder, SWT.NONE);
      tbtmUserProperty.setText("Properties");

      final Composite composite1 = new Composite(tabFolder, SWT.NONE);
      tbtmUserProperty.setControl(composite1);
      composite1.setLayout(new GridLayout(1, false));

      createProperties(composite1);

      // ----------------
      // Payload Tab
      // ----------------

      tbtmPayload = new TabItem(tabFolder, SWT.NONE);
      tbtmPayload.setText("Payload");

      Composite composite2 = new Composite(tabFolder, SWT.NONE);
      tbtmPayload.setControl(composite2);
      composite2.setLayout(new GridLayout(4, false));

      // Message Type
      Composite cMessageType = new Composite(composite2, SWT.NONE);
      cMessageType.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1));
      cMessageType.setLayout(new GridLayout(2, false));
      GridData gd0 = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
      gd0.horizontalIndent = 5;
      cMessageType.setLayoutData(gd0);

      Label lblNewLabel3 = new Label(cMessageType, SWT.NONE);
      lblNewLabel3.setText("Type:");

      comboMessageType = new Combo(cMessageType, SWT.DROP_DOWN | SWT.READ_ONLY);
      comboMessageType.setToolTipText("JMS Message Type");
      comboMessageType.setItems(JTBMessageType.getTypes());

      // Vizualiser combo

      cVisualizer = new Composite(composite2, SWT.NONE);
      cVisualizer.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1));
      cVisualizer.setLayout(new GridLayout(3, false));

      Label lblNewLabel9 = new Label(cVisualizer, SWT.NONE);
      lblNewLabel9.setText("Open as:");

      comboVisualizers = new Combo(cVisualizer, SWT.READ_ONLY);
      comboMessageType.setToolTipText("Choose visualizer");

      btnShowAs = new Button(cVisualizer, SWT.CENTER);
      btnShowAs.setImage(SWTResourceManager.getImage(this.getClass(), "icons/messages/zoom.png"));
      comboMessageType.setToolTipText("Run visuzlizer");

      // Formatting Buttons

      cFormat = new Composite(composite2, SWT.NONE);
      cFormat.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1));
      cFormat.setLayout(new GridLayout(2, true));

      btnFormatXML = new Button(cFormat, SWT.CENTER | SWT.NO_FOCUS);
      btnFormatXML.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1));
      btnFormatXML.setText("{XML}");
      FontDescriptor boldDescriptor = FontDescriptor.createFrom(btnFormatXML.getFont()).setStyle(SWT.BOLD);
      Font boldFont = boldDescriptor.createFont(btnFormatXML.getDisplay());
      btnFormatXML.setFont(boldFont);
      btnFormatXML.setToolTipText("Format as XML");
      btnFormatXML.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
         txtPayload.setText(FormatUtils.xmlPrettyFormat(ps, txtPayload.getText(), true));
      }));

      btnFormatJSON = new Button(cFormat, SWT.CENTER | SWT.NO_FOCUS);
      btnFormatJSON.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1));
      btnFormatJSON.setText("{JSON}");
      btnFormatJSON.setFont(boldFont);
      btnFormatJSON.setToolTipText("Format as Json");
      btnFormatJSON.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
         txtPayload.setText(FormatUtils.jsonPrettyFormat(txtPayload.getText()));
      }));

      // Export/Import buttons

      Composite cImportExport = new Composite(composite2, SWT.NONE);
      cImportExport.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
      cImportExport.setLayout(new GridLayout(2, true));

      btnImport = new Button(cImportExport, SWT.CENTER);
      btnImport.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1));
      btnImport.setText("Import...");
      btnImport.setToolTipText("Import Payload");
      btnImport.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
         try {
            byte[] b = Utils.readFileBytes(getShell());
            if (b == null) {
               return;
            }
            switch (jtbMessageType) {
               case TEXT:
                  String payloadText = new String(b);
                  txtPayload.setText(payloadText);
                  tbtmPayload.setText(String.format(Constants.PAYLOAD_TEXT_TITLE, payloadText.length()));
                  break;
               case BYTES:
                  payloadBytes = b;
                  IDataProvider idp = new BytesDataProvider(b);
                  hvPayLoadHex.setDataProvider(idp);
                  tbtmPayload.setText(String.format(Constants.PAYLOAD_BYTES_TITLE, idp.getDataSize()));
                  break;

               default:
                  // No import for other types of messages
                  break;
            }
         } catch (IOException e1) {
            log.error("IOException while importing payload", e1);
         }
      }));

      btnExport = new Button(cImportExport, SWT.CENTER);
      btnExport.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1));
      btnExport.setText("Export...");
      btnImport.setToolTipText("Export Payload");
      btnExport.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
         try {
            Utils.exportPayloadToOS(getShell(), template, txtPayload.getText(), payloadBytes, payloadMap);
         } catch (IOException | JMSException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
         }
      }));

      // -------------------
      // Payload Body
      // -------------------

      payLoadStackLayout = new StackLayout();

      payloadComposite = new Composite(composite2, SWT.NONE);
      payloadComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 5, 1));
      payloadComposite.setLayout(payLoadStackLayout);

      textPayloadComposite = new Composite(payloadComposite, SWT.NONE);
      textPayloadComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 4, 1));
      textPayloadComposite.setLayout(new GridLayout(1, false));
      createTextPayload(textPayloadComposite);

      hexPayloadComposite = new Composite(payloadComposite, SWT.NONE);
      hexPayloadComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 4, 1));
      hexPayloadComposite.setLayout(new GridLayout(1, false));
      createHexPayload(hexPayloadComposite);

      mapPayloadComposite = new Composite(payloadComposite, SWT.BORDER_SOLID);
      mapPayloadComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 4, 1));
      mapPayloadComposite.setLayout(new GridLayout(1, false));
      createMapPayload(mapPayloadComposite);

      payLoadStackLayout.topControl = textPayloadComposite;

      // --------
      // Behavior
      // --------
      comboMessageType.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
         String sel = comboMessageType.getItem(comboMessageType.getSelectionIndex());
         jtbMessageType = JTBMessageType.fromDescription(sel);
         tbtmPayload.setText("Payload");
         buildVisualizersCombo();
         showHideControls();
      }));

      btnShowAs.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
         String selectedVisualizerName = comboVisualizers.getItem(comboVisualizers.getSelectionIndex());
         try {
            visualizersManager.launchVisualizer(getShell(),
                                                selectedVisualizerName,
                                                jtbMessageType,
                                                txtPayload.getText(),
                                                payloadBytes,
                                                payloadMap);
         } catch (Exception ex) {
            jtbStatusReporter.showError("A problem occurred when running the visualizer", ex, selectedVisualizerName);
            return;
         }
      }));

      // ---------------
      // Dialog Shortcuts
      // ----------------

      getShell().getDisplay().addFilter(SWT.KeyUp, new Listener() {
         @Override
         public void handleEvent(Event e) {
            // Fast fail
            if (e.keyCode != 's') {
               return;
            }
            if (e.widget instanceof Control && isChild(container, (Control) e.widget)) {
               if ((e.stateMask & SWT.MOD1) != 0) {
                  log.debug("CTRL-S pressed");
                  buttonPressed(MessageEditDialog.BUTTON_SAVE_TEMPLATE);
                  buttonPressed(IDialogConstants.OK_ID);
                  return;
               }
            }
         }
      });

      // --------------
      // Initialize data
      // --------------

      populateFields();
      buildVisualizersCombo();
      showHideControls();

      txtCorrelationID.setFocus();

      return container;
   }

   private void buildVisualizersCombo() {
      String[] visualizers = visualizersManager.getVizualisersNamesForMessageType(jtbMessageType);
      if (visualizers == null) {
         cVisualizer.setVisible(false);
      } else {
         comboVisualizers.setItems(visualizers);
         int indexVisualizer = visualizersManager.findIndexVisualizerForType(visualizers, jtbMessageType, payloadBytes);
         comboVisualizers.select(indexVisualizer);
         comboVisualizers.requestLayout();
      }
   }

   @Override
   protected void configureShell(Shell newShell) {
      super.configureShell(newShell);
      newShell.setText(getDialogTitle());
   }

   @Override
   protected Point getInitialSize() {
      return new Point(800, 600);
   }

   @Override
   protected void okPressed() {
      updateTemplate();
      super.okPressed();
   }

   // -------
   // Facility
   // -------

   protected void disableContentAssist() {
      deco.hide();
      contentAssistAdapter.setEnabled(false);
   }

   // -------
   // Helpers
   // -------

   private void populateFields() {

      if (template.getJmsCorrelationID() != null) {
         txtCorrelationID.setText(template.getJmsCorrelationID());
      }

      if ((template.getDeliveryDelay() != null) && (template.getDeliveryDelay() != 0)) {
         txtDeliveryDelay.setText(template.getDeliveryDelay().toString());
      }

      if ((template.getTimeToLive() != null) && (template.getTimeToLive() != 0)) {
         txtTimeToLive.setText(template.getTimeToLive().toString());
      }

      if (template.getReplyToDestinationName() != null) {
         txtReplyToDestinationName.setText(template.getReplyToDestinationName());
      }

      if ((template.getJmsTimestamp() != null) && (template.getJmsTimestamp() != 0)) {
         lblTimestamp.setText(Utils.formatTimestamp(template.getJmsTimestamp(), true));
      }

      if ((template.getJmsExpiration() != null) && (template.getJmsExpiration() != 0)) {
         lblExpiration.setText(Utils.formatTimestamp(template.getJmsExpiration(), true));
      }

      try {
         if ((template.getJmsDeliveryTime() != null) && (template.getJmsDeliveryTime() != 0)) {
            lblDeliveryTime.setText(Utils.formatTimestamp(template.getJmsDeliveryTime(), true));
         }
      } catch (Throwable t) {
         // JMS 2.0+ only..
      }

      if (template.getJmsType() != null) {
         txtType.setText(template.getJmsType());
      }

      if (template.getPriority() != null) {
         spinnerPriority.setSelection(template.getPriority());
      } else {
         spinnerPriority.setSelection(DEFAULT_PRIORITY);
      }

      btnPersistent.setSelection(true);
      btnNonPersistent.setSelection(false);
      if (template.getDeliveryMode() == JTBDeliveryMode.NON_PERSISTENT) {
         btnPersistent.setSelection(false);
         btnNonPersistent.setSelection(true);
      }

      jtbMessageType = template.getJtbMessageType();

      // Set Message Type
      if (jtbMessageType == null) {
         jtbMessageType = JTBMessageType.TEXT;
         template.setJtbMessageType(jtbMessageType);
         comboMessageType.select(0);
      }
      comboMessageType.select(template.getJtbMessageType().ordinal());

      // Set Payload
      payloadBytes = null;
      payloadMap = new HashMap<>();

      txtPayload.setText("");
      hvPayLoadHex.setDataProvider(null);

      switch (template.getJtbMessageType()) {
         case TEXT:
            String payloadText = template.getPayloadText();
            if (payloadText != null) {
               txtPayload.setText(payloadText);
               tbtmPayload.setText(String.format(Constants.PAYLOAD_TEXT_TITLE, payloadText.length()));
            }
            break;

         case BYTES:
            payloadBytes = template.getPayloadBytes();
            if (payloadBytes != null) {
               IDataProvider idp = new BytesDataProvider(payloadBytes);
               hvPayLoadHex.setDataProvider(idp);
               tbtmPayload.setText(String.format(Constants.PAYLOAD_BYTES_TITLE, payloadBytes.length));
            }
            break;

         case MESSAGE:
            break;

         case MAP:
            payloadMap = template.getPayloadMap();
            tbtmPayload.setText("Payload");
            // payloadMap.putAll(template.getPayloadMap());
            break;

         case OBJECT:
            tbtmPayload.setText("Payload");
            break;

         case STREAM:
            tbtmPayload.setText("Payload");
            break;

         default:
            break;
      }

      tvMapProperties.setInput(payloadMap);
      Utils.resizeTableViewer(tvMapProperties);

      userProperties = new ArrayList<>();
      if (template.getJtbProperties() != null) {
         userProperties.addAll(template.getJtbProperties());
      }

      tvProperties.setInput(userProperties);
      Utils.resizeTableViewer(tvProperties);
   }

   protected void updateTemplate() {

      template.setJtbMessageType(jtbMessageType);

      String txt = txtCorrelationID.getText().trim();
      template.setJmsCorrelationID(txt.isEmpty() ? null : txt);

      txt = txtType.getText().trim();
      template.setJmsType(txt.isEmpty() ? null : txt);

      txt = txtReplyToDestinationName.getText().trim();
      template.setReplyToDestinationName(txt.isEmpty() ? null : txt);

      if (btnPersistent.getSelection()) {
         template.setDeliveryMode(JTBDeliveryMode.PERSISTENT);
      } else {
         template.setDeliveryMode(JTBDeliveryMode.NON_PERSISTENT);
      }

      template.setPriority(spinnerPriority.getSelection());

      txt = txtDeliveryDelay.getText().trim();
      template.setDeliveryDelay(txt.isEmpty() ? null : Long.valueOf(txt));

      txt = txtTimeToLive.getText().trim();
      template.setTimeToLive(txt.isEmpty() ? null : Long.valueOf(txt));

      template.setJtbProperties(userProperties);

      switch (jtbMessageType) {
         case TEXT:
            if (txtPayload.getText().isEmpty()) {
               template.setPayloadText(null);
            } else {
               template.setPayloadText(txtPayload.getText());
            }
            template.setPayloadBytes(null);
            template.setPayloadMap(null);
            template.setPayloadObject(null);
            break;

         case BYTES:
            template.setPayloadText(null);
            template.setPayloadBytes(payloadBytes);
            template.setPayloadMap(null);
            template.setPayloadObject(null);
            break;

         case MESSAGE:
            template.setPayloadText(null);
            template.setPayloadBytes(null);
            template.setPayloadMap(null);
            template.setPayloadObject(null);
            break;

         case MAP:
            template.setPayloadText(null);
            template.setPayloadBytes(null);
            template.setPayloadMap(payloadMap);
            template.setPayloadObject(null);
            break;

         case OBJECT:
            template.setPayloadText(null);
            template.setPayloadBytes(null);
            template.setPayloadMap(null);
            // template.setPayloadObject(null);
            break;

         case STREAM:
            template.setPayloadText(null);
            template.setPayloadBytes(null);
            template.setPayloadMap(null);
            template.setPayloadObject(null);
            break;
      }
   }

   private void showHideControls() {

      if (isReadOnly()) {
         comboMessageType.setEnabled(false);
      }

      switch (jtbMessageType) {
         case TEXT:
            comboMessageType.select(0);
            jtbMessageType = JTBMessageType.TEXT;

            deco.show();

            cVisualizer.setVisible(true);
            cFormat.setVisible(true);
            btnImport.setVisible(!isReadOnly());
            btnExport.setVisible(true);
            payloadComposite.setVisible(true);

            payLoadStackLayout.topControl = textPayloadComposite;
            payloadComposite.layout();

            break;

         case BYTES:
            comboMessageType.select(1);
            jtbMessageType = JTBMessageType.BYTES;

            deco.hide();

            cVisualizer.setVisible(true);
            cFormat.setVisible(false);
            btnImport.setVisible(!isReadOnly());
            btnExport.setVisible(true);
            payloadComposite.setVisible(true);

            payLoadStackLayout.topControl = hexPayloadComposite;
            payloadComposite.layout();

            break;

         case MESSAGE:
            comboMessageType.select(2);
            jtbMessageType = JTBMessageType.MESSAGE;

            deco.hide();

            cVisualizer.setVisible(false);
            cFormat.setVisible(false);
            btnImport.setVisible(false);
            btnExport.setVisible(false);
            payloadComposite.setVisible(false);

            break;

         case MAP:
            jtbMessageType = JTBMessageType.MAP;

            deco.hide();

            cVisualizer.setVisible(true);
            cFormat.setVisible(false);
            btnImport.setVisible(false);
            btnExport.setVisible(true);
            payloadComposite.setVisible(true);

            payLoadStackLayout.topControl = mapPayloadComposite;
            payloadComposite.layout();

            break;

         case OBJECT:
            jtbMessageType = JTBMessageType.OBJECT;

            deco.hide();

            cVisualizer.setVisible(false);
            cFormat.setVisible(false);
            btnImport.setVisible(false);
            btnExport.setVisible(false);
            payloadComposite.setVisible(false);

            break;

         case STREAM:
            jtbMessageType = JTBMessageType.STREAM;

            deco.hide();

            cVisualizer.setVisible(false);
            cFormat.setVisible(false);
            btnImport.setVisible(false);
            btnExport.setVisible(false);
            payloadComposite.setVisible(false);

            break;

         default:
            break;
      }

   }

   private boolean isChild(Control parent, Control child) {
      if (child.equals(parent)) {
         return true;
      }

      Composite p = child.getParent();
      if (p == null) {
         return false;
      }

      return isChild(parent, p);
   }

   // TextMessage
   private void createTextPayload(Composite parentComposite) {

      // DF SWT.WRAP slows down A LOT UI for long text Messages (> 1K)
      // txtPayload = new Text(parentComposite, SWT.BORDER | SWT.WRAP | SWT.H_SCROLL | SWT.V_SCROLL | SWT.MULTI);
      txtPayload = new Text(parentComposite, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL | SWT.MULTI);
      GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1);
      gd.horizontalIndent = 4;
      txtPayload.setLayoutData(gd);
      // Add key binding for CTRL-a -> select all
      txtPayload.addListener(SWT.KeyUp, new Listener() {
         public void handleEvent(Event event) {
            if (event.stateMask == SWT.MOD1 && event.keyCode == 'a') {
               ((Text) event.widget).selectAll();
            }
         }
      });

      // Decorator with clue to content assist
      deco = new ControlDecoration(parentComposite, SWT.TOP | SWT.LEFT);
      Image image = FieldDecorationRegistry.getDefault().getFieldDecoration(FieldDecorationRegistry.DEC_CONTENT_PROPOSAL)
               .getImage();
      deco.setDescriptionText("Use Ctrl+Space to insert a variable");
      deco.setImage(image);

      // Content Assist
      KeyStroke actKey = null;
      try {
         actKey = KeyStroke.getInstance("Ctrl+Space");
      } catch (ParseException e) {
         log.warn("ParseException: ", e);
      }
      contentAssistAdapter = new ContentProposalAdapter(txtPayload,
                                                        new VariableContentAdapter(),
                                                        new VariableContentProposalProvider(variablesManager),
                                                        actKey,
                                                        null);
      // new char[] { '$' }); DF: With this, we can not insert variable for datafiles..
      contentAssistAdapter.setProposalAcceptanceStyle(ContentProposalAdapter.PROPOSAL_INSERT);
      contentAssistAdapter.setPropagateKeys(false);
      contentAssistAdapter.setPopupSize(new Point(250, 200));

   }

   // TextMessage
   private void createHexPayload(Composite parentComposite) {

      GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1);
      gd.horizontalIndent = 4;

      hvPayLoadHex = new HexViewer(parentComposite, SWT.READ_ONLY, null, 16);
      hvPayLoadHex.setLayoutData(gd);
   }

   // MapMessage
   @SuppressWarnings("unchecked")
   private void createMapPayload(Composite parentComposite) {

      Composite composite3 = new Composite(parentComposite, SWT.NONE);
      composite3.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
      GridLayout glComposite3 = new GridLayout(3, false);
      glComposite3.marginWidth = 0;
      composite3.setLayout(glComposite3);

      Label lblNewLabel = new Label(composite3, SWT.NONE);
      lblNewLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
      lblNewLabel.setAlignment(SWT.CENTER);
      lblNewLabel.setText("Name");

      Label lblNewLabel2 = new Label(composite3, SWT.NONE);
      lblNewLabel2.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
      lblNewLabel2.setAlignment(SWT.CENTER);
      lblNewLabel2.setText("Value");

      Label lblNewLabel1 = new Label(composite3, SWT.NONE);
      lblNewLabel1.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false, 1, 1));

      final Text newMapPropertyName = new Text(composite3, SWT.BORDER);
      newMapPropertyName.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

      final Text newMapPropertyValue = new Text(composite3, SWT.BORDER);
      newMapPropertyValue.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

      Button btnAddProperty = new Button(composite3, SWT.NONE);
      btnAddProperty.setText("Add");

      final Composite composite4 = new Composite(parentComposite, SWT.NONE);
      composite4.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
      TableColumnLayout tclComposite4 = new TableColumnLayout();
      composite4.setLayout(tclComposite4);

      final TableViewer tableViewer = new TableViewer(composite4, SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI);
      final Table mapPropertyTable = tableViewer.getTable();
      mapPropertyTable.setHeaderVisible(true);
      mapPropertyTable.setLinesVisible(true);

      TableViewerColumn propertyNameColumn = new TableViewerColumn(tableViewer, SWT.NONE);
      TableColumn propertyNameHeader = propertyNameColumn.getColumn();
      tclComposite4.setColumnData(propertyNameHeader, new ColumnWeightData(2, 150, true));
      propertyNameHeader.setAlignment(SWT.CENTER);
      propertyNameHeader.setText("Name");
      propertyNameColumn.setLabelProvider(new ColumnLabelProvider() {
         @Override
         public String getText(Object element) {
            Map.Entry<String, Object> e = (Map.Entry<String, Object>) element;
            return e.getKey();
         }
      });

      TableViewerColumn propertyValueColumn = new TableViewerColumn(tableViewer, SWT.NONE);
      propertyValueColumn.setEditingSupport(new MapPayloadEditingSupport(tableViewer));
      TableColumn propertyValueHeader = propertyValueColumn.getColumn();
      tclComposite4.setColumnData(propertyValueHeader, new ColumnWeightData(3, 150, true));
      propertyValueHeader.setText("Value");
      propertyValueColumn.setLabelProvider(new ColumnLabelProvider() {
         @Override
         public String getText(Object element) {
            Map.Entry<String, Object> e = (Map.Entry<String, Object>) element;
            return e.getValue().toString();
         }
      });

      tableViewer.setContentProvider(new IStructuredContentProvider() {

         @Override
         public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
         }

         @Override
         public void dispose() {
         }

         @Override
         public Object[] getElements(Object inputElement) {
            Map<String, Object> m = (Map<String, Object>) inputElement;
            return m.entrySet().toArray();
         }
      });

      mapPropertyTable.addKeyListener(KeyListener.keyReleasedAdapter(e -> {

         // Remove a property from the list
         if (e.keyCode == SWT.DEL) {
            IStructuredSelection selection = (IStructuredSelection) tableViewer.getSelection();
            if (selection.isEmpty()) {
               return;
            }
            for (Object sel : selection.toList()) {
               Map.Entry<String, Object> en = (Map.Entry<String, Object>) sel;
               log.debug("Remove {} from the list", en);
               payloadMap.remove(en.getKey());
               tableViewer.remove(en);
            }

            composite4.layout();
            Utils.resizeTableViewer(tableViewer);

            return;
         }

         // Select all
         if ((e.stateMask == SWT.MOD1) && (e.keyCode == 'a')) {
            ((Table) e.widget).selectAll();
            return;
         }

         // Copy Map to Clipboard (CTRL+C)
         if (((e.stateMask & SWT.MOD1) != 0) && (e.keyCode == 'c')) {
            IStructuredSelection selection = (IStructuredSelection) tableViewer.getSelection();
            if (selection.isEmpty()) {
               return;
            }
            StringBuilder sb = new StringBuilder(256);
            for (Object sel : selection.toList()) {
               Map.Entry<String, Object> en = (Map.Entry<String, Object>) sel;
               sb.append(en.getKey());
               sb.append("=");
               sb.append(en.getValue());
               sb.append("\r");
            }
            Clipboard cb = new Clipboard(Display.getDefault());
            TextTransfer textTransfer = TextTransfer.getInstance();
            cb.setContents(new Object[] { sb.toString() }, new Transfer[] { textTransfer });
            return;
         }
      }));

      // Add a new Property
      btnAddProperty.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
         final String name = newMapPropertyName.getText().trim();
         if (name.length() == 0) {
            return;
         }
         if (newMapPropertyValue.getText().trim().length() == 0) {
            return;
         }

         // Validate that a property with the same name does not exit
         if (payloadMap.get(name) != null) {
            MessageDialog.openError(getShell(), "Validation error", String.format(PROPERTY_ALREADY_EXIST, name));
            return;
         }

         // Validate that the property name is a valid JMS property name
         if (Utils.isValidJMSPropertyName(name)) {
            payloadMap.put(name, newMapPropertyValue.getText().trim());
            Map.Entry<String, Object> en = new AbstractMap.SimpleEntry<String, Object>(name, newMapPropertyValue.getText().trim());
            tableViewer.add(en);
            composite4.layout();
            Utils.resizeTableViewer(tableViewer);
         } else {
            MessageDialog.openError(getShell(), "Validation error", String.format(PROPERTY_NAME_INVALID, name));
            return;
         }
      }));

      tvMapProperties = tableViewer;
      Utils.resizeTableViewer(tableViewer);
   }

   private void createProperties(final Composite parentComposite) {

      // Header lines
      Composite compositeHeader = new Composite(parentComposite, SWT.NONE);
      compositeHeader.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
      GridLayout glCompositeHeader = new GridLayout(4, false);
      glCompositeHeader.marginWidth = 0;
      compositeHeader.setLayout(glCompositeHeader);

      Label lblNewKind = new Label(compositeHeader, SWT.NONE);
      lblNewKind.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false, 1, 1));
      lblNewKind.setAlignment(SWT.CENTER);
      lblNewKind.setText("Kind");

      Label lblNewName = new Label(compositeHeader, SWT.NONE);
      lblNewName.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
      lblNewName.setAlignment(SWT.CENTER);
      lblNewName.setText("Name");

      Label lblNewValue = new Label(compositeHeader, SWT.NONE);
      lblNewValue.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
      lblNewValue.setAlignment(SWT.CENTER);
      lblNewValue.setText("Value");

      Label lblNewLabel1 = new Label(compositeHeader, SWT.NONE);
      lblNewLabel1.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false, 1, 1));

      // Data
      final Combo newKindCombo = new Combo(compositeHeader, SWT.READ_ONLY);
      newKindCombo.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1));
      newKindCombo.setItems(JMSPropertyKind.getDisplayNames());
      newKindCombo.select(0); // 0 = String

      final Text newPropertyName = new Text(compositeHeader, SWT.BORDER);
      newPropertyName.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

      final Text newPropertyValue = new Text(compositeHeader, SWT.BORDER);
      newPropertyValue.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

      Button btnAddProperty = new Button(compositeHeader, SWT.NONE);
      btnAddProperty.setText("Add");

      // Properties table
      Composite compositeProperties = new Composite(parentComposite, SWT.NONE);
      compositeProperties.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
      TableColumnLayout tclComposite4 = new TableColumnLayout();
      compositeProperties.setLayout(tclComposite4);

      final TableViewer tableViewer = new TableViewer(compositeProperties, SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI);
      final Table propertyTable = tableViewer.getTable();
      propertyTable.setHeaderVisible(true);
      propertyTable.setLinesVisible(true);

      TableViewerColumn propertyKindColumn = new TableViewerColumn(tableViewer, SWT.NONE);
      TableColumn propertyKindHeader = propertyKindColumn.getColumn();
      tclComposite4.setColumnData(propertyKindHeader, new ColumnWeightData(1, 50, true));
      propertyKindHeader.setAlignment(SWT.CENTER);
      propertyKindHeader.setText("Kind");
      propertyKindColumn.setLabelProvider(new ColumnLabelProvider() {
         @Override
         public String getText(Object element) {
            JTBProperty p = (JTBProperty) element;
            return p.getKind().getDisplayName();
         }
      });

      TableViewerColumn propertyNameColumn = new TableViewerColumn(tableViewer, SWT.NONE);
      TableColumn propertyNameHeader = propertyNameColumn.getColumn();
      tclComposite4.setColumnData(propertyNameHeader, new ColumnWeightData(2, 100, true));
      propertyNameHeader.setAlignment(SWT.LEFT);
      propertyNameHeader.setText("Name");
      propertyNameColumn.setLabelProvider(new ColumnLabelProvider() {
         @Override
         public String getText(Object element) {
            JTBProperty u = (JTBProperty) element;
            return u.getName();
         }
      });

      TableViewerColumn propertyValueColumn = new TableViewerColumn(tableViewer, SWT.NONE);
      propertyValueColumn.setEditingSupport(new HeaderPropertyEditingSupport(tableViewer));
      TableColumn propertyValueHeader = propertyValueColumn.getColumn();
      tclComposite4.setColumnData(propertyValueHeader, new ColumnWeightData(3, 150, true));
      propertyValueHeader.setText("Value");
      propertyValueColumn.setLabelProvider(new ColumnLabelProvider() {
         @Override
         public String getText(Object element) {
            JTBProperty u = (JTBProperty) element;
            return u.getValue().toString();
         }
      });

      tableViewer.setContentProvider(ArrayContentProvider.getInstance());

      propertyTable.addKeyListener(KeyListener.keyReleasedAdapter(e -> {

         // Remove a property from the list
         if (e.keyCode == SWT.DEL) {
            IStructuredSelection selection = (IStructuredSelection) tableViewer.getSelection();
            if (selection.isEmpty()) {
               return;
            }
            for (Object sel : selection.toList()) {
               JTBProperty h = (JTBProperty) sel;
               log.debug("Remove {} from the list", h);
               userProperties.remove(h);
               tableViewer.remove(h);
            }

            parentComposite.layout();
            Utils.resizeTableViewer(tableViewer);

            return;
         }

         // Select all
         if ((e.stateMask == SWT.MOD1) && (e.keyCode == 'a')) {
            ((Table) e.widget).selectAll();
            return;
         }

         // Copy Properties to Clipboard (CTRL+C)
         if (((e.stateMask & SWT.MOD1) != 0) && (e.keyCode == 'c')) {
            IStructuredSelection selection = (IStructuredSelection) tableViewer.getSelection();
            if (selection.isEmpty()) {
               return;
            }
            StringBuilder sb = new StringBuilder(256);
            for (Object sel : selection.toList()) {
               JTBProperty en = (JTBProperty) sel;
               sb.append(en.getName());
               sb.append("=");
               sb.append(en.getValue());
               sb.append("\r");
            }
            Clipboard cb = new Clipboard(Display.getDefault());
            TextTransfer textTransfer = TextTransfer.getInstance();
            cb.setContents(new Object[] { sb.toString() }, new Transfer[] { textTransfer });
            return;
         }

      }));

      // Add a new Property
      btnAddProperty.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
         String name = newPropertyName.getText().trim();
         if (name.length() == 0) {
            return;
         }
         if (newPropertyValue.getText().trim().length() == 0) {
            return;
         }

         // Validate that a property with the same name does not exit
         for (JTBProperty unv : userProperties) {
            if (unv.getName().equals(name)) {
               MessageDialog.openError(getShell(), "Validation error", String.format(PROPERTY_ALREADY_EXIST, name));
               return;
            }
         }

         // Validate that the property name is a valid JMS property name
         if (Utils.isValidJMSPropertyName(name)) {
            JMSPropertyKind jmsPropertyKind = JMSPropertyKind
                     .fromDisplayName(newKindCombo.getItem(newKindCombo.getSelectionIndex()));

            // Validate that the value is conform to the kind
            if (JMSPropertyKind.validateValue(jmsPropertyKind, newPropertyValue.getText().trim())) {
               JTBProperty p = new JTBProperty(name, newPropertyValue.getText().trim(), jmsPropertyKind);
               userProperties.add(p);
               tableViewer.add(p);
               parentComposite.layout();
               Utils.resizeTableViewer(tableViewer);
            } else {
               MessageDialog.openError(getShell(),
                                       "Validation error",
                                       String.format(PROPERTY_VALUE_INVALID, jmsPropertyKind.getDisplayName()));
            }
         } else {
            MessageDialog.openError(getShell(), "Validation error", String.format(PROPERTY_NAME_INVALID, name));
            return;
         }
      }));

      tvProperties = tableViewer;
      Utils.resizeTableViewer(tableViewer);
   }

   // --------------
   // Helper Classes
   // --------------

   private static final class MapPayloadEditingSupport extends EditingSupport {

      private final TableViewer viewer;
      private final CellEditor  editor;

      public MapPayloadEditingSupport(TableViewer viewer) {
         super(viewer);
         this.viewer = viewer;
         this.editor = new TextCellEditor(viewer.getTable());
      }

      @Override
      protected CellEditor getCellEditor(Object element) {
         return editor;
      }

      @Override
      protected boolean canEdit(Object element) {
         return true;
      }

      @Override
      protected Object getValue(Object element) {
         @SuppressWarnings("unchecked")
         Map.Entry<String, Object> e = (Map.Entry<String, Object>) element;
         Object s = e.getValue();
         if (s == null) {
            return "";
         } else {
            return s.toString();
         }
      }

      @Override
      protected void setValue(Object element, Object userInputValue) {
         @SuppressWarnings("unchecked")
         Map.Entry<String, Object> e = (Map.Entry<String, Object>) element;
         e.setValue(String.valueOf(userInputValue));
         viewer.update(element, null);
      }
   }

   private static final class HeaderPropertyEditingSupport extends EditingSupport {

      private final TableViewer viewer;
      private final CellEditor  editor;

      public HeaderPropertyEditingSupport(TableViewer viewer) {
         super(viewer);
         this.viewer = viewer;
         this.editor = new TextCellEditor(viewer.getTable());
      }

      @Override
      protected CellEditor getCellEditor(Object element) {
         return editor;
      }

      @Override
      protected boolean canEdit(Object element) {
         return true;
      }

      @Override
      protected Object getValue(Object element) {
         JTBProperty e = (JTBProperty) element;
         Object s = e.getValue();
         if (s == null) {
            return "";
         } else {
            return s.toString();
         }
      }

      @Override
      protected void setValue(Object element, Object userInputValue) {
         JTBProperty e = (JTBProperty) element;
         e.setValue(String.valueOf(userInputValue));
         viewer.update(element, null);
      }
   }

   // ----------------
   // Standard Getters
   // ----------------
   public JTBMessageTemplate getTemplate() {
      return template;
   }

}
