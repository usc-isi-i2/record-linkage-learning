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


package cdc.impl.join.dnm;

public class EuclidDistance implements ClusteringDistance {

	public double distance(Object o1, Object o2) {
		Double[] p1 = (Double[]) o1;
		Double[] p2 = (Double[]) o2;
		double x1 = p1[0].doubleValue();
		double y1 = p1[1].doubleValue();
		double x2 = p2[0].doubleValue();
		double y2 = p2[1].doubleValue();
		return Math.sqrt(Math.pow(x1 - x2, 2) + Math.pow(y1 - y2, 2));
	}
	
	public Object centroid(Object[] instances) {
		double sumX = 0, sumY = 0;
		for (int i = 0; i < instances.length; i++) {
			sumX += ((Double[])instances[i])[0].doubleValue();
			sumY += ((Double[])instances[i])[1].doubleValue();
		}
		return new Double[] {new Double(sumX/(double)instances.length), new Double(sumY/(double)instances.length)};
	}

	public double getMaxDistance() {
		throw new RuntimeException("Not implemented");
	}

}
