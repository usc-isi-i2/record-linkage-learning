/* ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is the FRIL Framework.
 *
 * The Initial Developers of the Original Code are
 * The Department of Math and Computer Science, Emory University and 
 * The Centers for Disease Control and Prevention.
 * Portions created by the Initial Developer are Copyright (C) 2008
 * the Initial Developer. All Rights Reserved.
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either the GNU General Public License Version 2 or later (the "GPL"), or
 * the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the MPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the MPL, the GPL or the LGPL.
 *
 * ***** END LICENSE BLOCK ***** */ 


/*
 * $Id: IncidentInfo.java,v 1.2 2005/10/10 18:01:42 rbair Exp $
 *
 * Copyright 2004 Sun Microsystems, Inc., 4150 Network Circle,
 * Santa Clara, California 95054, U.S.A. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
*/

/**
 * This is abstract class that incapsulates all the information needed
 * to report a problem to the automated report/processing system.
 *
 * @author Alexander Zuev
 * @version 1.0
 */
package cdc.gui.external;

public class IncidentInfo {
    /**
     * Short string that will be used as a error header
     */
    private String header;
    /**
     * Basic message that describes incident
     */
    private String basicErrorMessage;
    /**
     * Message that will fully describe the incident with all the
     * available details
     */
    private String detailedErrorMessage;
    /**
     * Optional Throwable that will be used
     */
    private Throwable errorException;

    /**
     * Main constructor that adds all the information to IncidentInfo
     * @param header
     * @param basicErrorMessage
     * @param detailedErrorMesage
     * @param errorException
     */
    public IncidentInfo(String header, String basicErrorMessage,
                        String detailedErrorMesage, Throwable errorException) {
        this.header = header;
        if(basicErrorMessage != null) {
            this.basicErrorMessage = basicErrorMessage;
        } else {
            if(errorException != null) {
            	if (errorException.getLocalizedMessage() != null) {
            		//System.out.println("Reporting: " + errorException.getLocalizedMessage().substring(0, 1000));
            		this.basicErrorMessage = "<html>" + breakLines(errorException.getLocalizedMessage()) + "</html>";
            	}
            } else {
                this.basicErrorMessage = "";
            }
        }
        this.detailedErrorMessage = detailedErrorMesage;
        this.errorException = errorException;
    }

    private String breakLines(String localizedMessage) {
    	if (localizedMessage == null) {
    		return null;
    	}
		if (localizedMessage.length() > 100) {
			if (localizedMessage.indexOf(' ', 100) == -1) {
				return format(localizedMessage.substring(0, 100)) + "<br>" + 
							breakLines(localizedMessage.substring(100));
			} else {
				return format(localizedMessage.substring(0, localizedMessage.indexOf(' ', 100))) + "<br>" + 
							breakLines(localizedMessage.substring(localizedMessage.indexOf(' ', 100)).trim());
			}
		} else {
			return localizedMessage;
		}
	}

	private String format(String substring) {
		return substring.replaceAll("<", "&lt").replaceAll(">", "&gt");
	}

	public IncidentInfo(String header, String basicErrorMessage, String detailedErrorMessage) {
        this(header, basicErrorMessage, detailedErrorMessage, null);
    }

    public IncidentInfo(String header, Throwable errorException) {
        this(header, null, null, errorException);
    }

    /**
     * Get the current header string
     *
     * @return header string
     */
    public String getHeader() {
        return header;
    }

    /**
     * Set the current header string
     *
     * @param header
     */
    public void setHeader(String header) {
        this.header = header;
    }

    /**
     * Get the basic error description
     *
     * @return basic error description
     */
    public String getBasicErrorMessage() {
        return basicErrorMessage;
    }

    /**
     * Set the current basic error description
     *
     * @param basicErrorMessage
     */
    public void setBasicErrorMessage(String basicErrorMessage) {
        this.basicErrorMessage = basicErrorMessage;
    }

    /**
     * Get the detailed error description
     *
     * @return detailed description
     */
    public String getDetailedErrorMessage() {
        return detailedErrorMessage;
    }

    /**
     * Set the detailed description for this error
     *
     * @param detailedErrorMessage
     */
    public void setDetailedErrorMessage(String detailedErrorMessage) {
        this.detailedErrorMessage = detailedErrorMessage;
    }

    /**
     * Get an exception that contains some additional information about the
     * error if provided.
     *
     * @return exception or null if no exception provided
     */
    public Throwable getErrorException() {
        return errorException;
    }

    /**
     * Set the exception that may contain additional information about the
     * error.
     *
     * @param errorException
     */
    public void setErrorException(Throwable errorException) {
        this.errorException = errorException;
    }
}
