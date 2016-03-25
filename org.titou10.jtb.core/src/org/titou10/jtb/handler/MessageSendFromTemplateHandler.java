/*
 * Copyright (C) 2015-2016 Denis Forveille titou10.titou10@gmail.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.titou10.jtb.handler;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.xml.bind.JAXBException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.e4.core.di.annotations.CanExecute;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.e4.ui.model.application.ui.menu.MMenuItem;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.titou10.jtb.config.ConfigManager;
import org.titou10.jtb.dialog.MessageSendFromTemplateDialog;
import org.titou10.jtb.dialog.TemplateChooserDialog;
import org.titou10.jtb.jms.model.JTBConnection;
import org.titou10.jtb.jms.model.JTBDestination;
import org.titou10.jtb.jms.model.JTBMessage;
import org.titou10.jtb.jms.model.JTBMessageTemplate;
import org.titou10.jtb.jms.model.JTBObject;
import org.titou10.jtb.jms.model.JTBQueue;
import org.titou10.jtb.jms.model.JTBTopic;
import org.titou10.jtb.template.TemplatesUtils;
import org.titou10.jtb.ui.JTBStatusReporter;
import org.titou10.jtb.ui.dnd.DNDData;
import org.titou10.jtb.ui.navigator.NodeJTBQueue;
import org.titou10.jtb.ui.navigator.NodeJTBTopic;
import org.titou10.jtb.util.Constants;
import org.titou10.jtb.util.Utils;

/**
 * Manage the "Send Message From Template" command
 * 
 * @author Denis Forveille
 * 
 */
public class MessageSendFromTemplateHandler {

   private static final Logger log            = LoggerFactory.getLogger(MessageSendFromTemplateHandler.class);

   private static final String MSG_COPY_MULTI = "Are you sure to blindly post a copy of those %d messages to '%s' ?";

   @Inject
   private IEventBroker        eventBroker;

   @Inject
   private ConfigManager       cm;

   @Inject
   private JTBStatusReporter   jtbStatusReporter;

   // This can be called in various contexts depending on parameter "context":
   // - right click on a session = QUEUE : -> use selection
   // - right click on message browser = MESSAGE : -> use tabJTBQueue
   // - drag & drop

   @Execute
   public void execute(Shell shell,
                       @Named(Constants.COMMAND_CONTEXT_PARAM) String context,
                       @Named(IServiceConstants.ACTIVE_SELECTION) @Optional JTBObject selection,
                       @Named(Constants.CURRENT_TAB_JTBQUEUE) @Optional JTBQueue tabJTBQueue) {
      log.debug("execute context={} selection={} tabJTBQueue={}", context, selection, tabJTBQueue);

      JTBMessageTemplate template = null;
      IFile selectedTemplateFile = null;

      JTBDestination jtbDestination;

      switch (context) {
         case Constants.COMMAND_CONTEXT_PARAM_DRAG_DROP:
            jtbDestination = DNDData.getTargetJTBDestination();
            log.debug("'Send from template' initiated from Drag & Drop. Destination: {}", jtbDestination);

            // Source of drag = Templates or Messages?
            switch (DNDData.getDrag()) {
               case TEMPLATE:
                  selectedTemplateFile = DNDData.getSourceJTBMessageTemplateIFile();
                  break;
               case JTBMESSAGE:
                  try {
                     template = new JTBMessageTemplate(DNDData.getSourceJTBMessages().get(0));
                  } catch (JMSException e) {
                     log.error("Exception when creating template", e);
                     return;
                  }
                  break;

               case JTBMESSAGE_MULTI:
                  List<JTBMessage> jtbMessages = DNDData.getSourceJTBMessages();
                  String msg = String.format(MSG_COPY_MULTI, jtbMessages.size(), jtbDestination.getName());
                  if (!(MessageDialog.openConfirm(shell, "Confirmation", msg))) {
                     return;
                  }
                  try {
                     // Post Messages
                     for (JTBMessage jtbMessage : jtbMessages) {
                        jtbDestination.getJtbConnection().sendMessage(jtbMessage, jtbDestination);
                     }
                     // Refresh List
                     eventBroker.send(Constants.EVENT_REFRESH_MESSAGES, jtbDestination);
                     return;
                  } catch (JMSException e) {
                     jtbStatusReporter.showError("Problem occurred while sending the messages", e, jtbDestination.getName());
                     return;
                  }

               default:
                  break;
            }
            break;

         case Constants.COMMAND_CONTEXT_PARAM_QUEUE:
            log.debug("'Send from template' initiated from Destination...");

            selectedTemplateFile = chooseTemplate(shell, jtbStatusReporter, cm);
            if (selectedTemplateFile == null) {
               return;
            }

            // Queue or Topic?

            if (selection instanceof NodeJTBQueue) {
               NodeJTBQueue nodeJTBQueue = (NodeJTBQueue) selection;
               jtbDestination = (JTBQueue) nodeJTBQueue.getBusinessObject();
            } else {
               NodeJTBTopic nodeJTBTopic = (NodeJTBTopic) selection;
               jtbDestination = (JTBTopic) nodeJTBTopic.getBusinessObject();
            }
            break;

         case Constants.COMMAND_CONTEXT_PARAM_MESSAGE:
            log.debug("'Send from template' initiated from Message Browser...");

            selectedTemplateFile = chooseTemplate(shell, jtbStatusReporter, cm);
            if (selectedTemplateFile == null) {
               return;
            }

            jtbDestination = tabJTBQueue;
            break;

         default:
            log.error("Invalid value : {}", context);
            return;
      }

      // Read template from IFile
      if (template == null) {
         try {
            template = TemplatesUtils.readTemplate(selectedTemplateFile);
         } catch (JAXBException | CoreException e) {
            jtbStatusReporter.showError("A problem occurred when reading the template", e, "");
            return;
         }
      }

      JTBConnection jtbConnection = jtbDestination.getJtbConnection();

      // Show the "edit template" dialog with a send button..
      MessageSendFromTemplateDialog dialog = new MessageSendFromTemplateDialog(shell, cm, template, jtbDestination);
      if (dialog.open() != Window.OK) {
         return;
      }

      template = dialog.getTemplate();

      log.debug("OK {}", template.getJtbMessageType());

      try {
         Message m = jtbConnection.createJMSMessage(template.getJtbMessageType());
         template.toJMSMessage(m);

         // Send Message
         JTBMessage jtbMessage = new JTBMessage(jtbDestination, m);
         jtbDestination.getJtbConnection().sendMessage(jtbMessage);

         // Refresh List
         eventBroker.send(Constants.EVENT_REFRESH_MESSAGES, jtbDestination);

      } catch (JMSException e) {
         jtbStatusReporter.showError("Problem occurred while sending the message", e, jtbDestination.getName());
         return;
      }

   }

   private IFile chooseTemplate(Shell shell, JTBStatusReporter jtbStatusReporter, ConfigManager cm) {

      // First Show a list of templates
      IResource[] files;
      try {
         files = cm.getTemplateFolder().members();
      } catch (CoreException e) {
         jtbStatusReporter.showError("Probleme while reading the template folder", e, "");
         return null;
      }

      Arrays.sort(files, new Comparator<IResource>() {
         @Override
         public int compare(IResource o1, IResource o2) {
            return o1.getName().compareToIgnoreCase(o2.getName());
         }
      });

      TemplateChooserDialog dialog1 = new TemplateChooserDialog(shell, false, false, cm.getTemplateFolder());
      if (dialog1.open() != Window.OK) {
         return null;
      }

      return (IFile) dialog1.getSelectedResource();
   }

   @CanExecute
   public boolean canExecute(@Named(Constants.COMMAND_CONTEXT_PARAM) String context,
                             @Named(IServiceConstants.ACTIVE_SELECTION) @Optional JTBObject selection,
                             @Named(Constants.CURRENT_TAB_JTBQUEUE) @Optional JTBQueue tabJTBQueue,

                             @Optional MMenuItem menuItem) {
      log.debug("canExecute context={} selection={} tabJTBQueue={}", context, selection, tabJTBQueue);

      switch (context) {
         case Constants.COMMAND_CONTEXT_PARAM_DRAG_DROP:
            return Utils.enableMenu(menuItem);

         case Constants.COMMAND_CONTEXT_PARAM_QUEUE:
            // Show menu on Queues and Topics only
            if ((!(selection instanceof NodeJTBQueue)) && (!(selection instanceof NodeJTBTopic))) {
               return Utils.disableMenu(menuItem);
            }

            // At least one template must exits
            try {
               if (cm.getTemplateFolder().members().length == 0) {
                  return Utils.disableMenu(menuItem);
               }
            } catch (CoreException e) {
               jtbStatusReporter.showError("Problem occurred while reading the template folder", e, "");
               return Utils.disableMenu(menuItem);
            }
            return Utils.enableMenu(menuItem);

         case Constants.COMMAND_CONTEXT_PARAM_MESSAGE:
            return Utils.enableMenu(menuItem);

         default:
            log.error("Invalid value : {}", context);
            return Utils.disableMenu(menuItem);
      }

   }
}
