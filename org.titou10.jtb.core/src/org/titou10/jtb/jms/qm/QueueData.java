/*
 * Copyright (C) 2015-2017 Denis Forveille titou10.titou10@gmail.com
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
package org.titou10.jtb.jms.qm;

/**
 * Characteristics of a Queue
 * 
 * @author Denis Forveille
 *
 */
public class QueueData implements Comparable<QueueData> {

   private String  name;
   private boolean browsable;

   // ------------
   // Constructors
   // ------------
   public QueueData(String name) {
      this.name = name;
      this.browsable = true;
   }

   public QueueData(String name, boolean browsable) {
      this.name = name;
      this.browsable = browsable;
   }

   // ----------
   // Comparable
   // ----------
   @Override
   public int compareTo(QueueData o) {
      return this.getName().compareTo(o.getName());
   }

   // ------------------------
   // Standard Getters/Setters
   // ------------------------

   public String getName() {
      return name;
   }

   public void setName(String name) {
      this.name = name;
   }

   public boolean isBrowsable() {
      return browsable;
   }

   public void setBrowsable(boolean browsable) {
      this.browsable = browsable;
   }

}
