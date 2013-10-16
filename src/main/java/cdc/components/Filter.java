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


package cdc.components;

import java.lang.reflect.InvocationTargetException;
import java.util.Stack;

import org.codehaus.janino.ScriptEvaluator;

import cdc.datamodel.DataColumnDefinition;
import cdc.datamodel.DataRow;
import cdc.utils.Log;
import cdc.utils.RJException;

public class Filter {
	
	private static abstract class Condition {
		public abstract String getCondition();
		
		public Condition merge(Condition other, String logicOp) throws RJException {
			if (this instanceof EmptyCondition) {
				return other;
			} else if (other instanceof EmptyCondition) {
				return this;
			} else if (this instanceof SimpleCondition) {
				CompoundCondition c = new CompoundCondition();
				c.subconditions = new Condition[] {this, other};
				c.operators = new String[] {logicOp};
				return c;
			} else if (this instanceof CompoundCondition) {
				CompoundCondition cc = (CompoundCondition)this;
				String[] newOpers = new String[cc.operators.length + 1];
				System.arraycopy(cc.operators, 0, newOpers, 0, cc.operators.length);
				newOpers[cc.operators.length] = logicOp;
				cc.operators = newOpers;
				Condition[] newConds = new Condition[cc.subconditions.length + 1];
				System.arraycopy(cc.subconditions, 0, newConds, 0, cc.subconditions.length);
				newConds[cc.subconditions.length] = other;
				cc.subconditions = newConds;
				return cc;
			} else {
				throw new RJException("Poorly formed filter condition.");
			}
		}

		public abstract boolean removeNonExistentColumns(DataColumnDefinition[] outputFormat);
		
	}
	
	private static class SimpleCondition extends Condition { 
		DataColumnDefinition column;
		String condition;
		String value;
		
		public String getCondition() {
			if (condition.equals("==") || condition.equals("=") || condition.equals("is") || condition.equals("equals")) {
				return "row.getData(\"" + column.getColumnName() + "\").getValue().toString().equals(\"" + value + "\")";
			} else if (condition.equals("!=") || condition.equals("not equals")) {
				return "!row.getData(\"" + column.getColumnName() + "\").getValue().toString().equals(\"" + value + "\")";
			} else {
				return "Double.parseDouble(row.getData(\"" + column.getColumnName() + "\").getValue().toString()) " + condition + " " + value;
			}
		}
		
		public String toString() {
			String out = "(";
			if (column.getColumnName().indexOf(' ') != -1) {
				out += "\"" + column.getColumnName() + "\"";
			} else {
				out += column.getColumnName();
			}
			out += " " + condition;
			try {
				Double.parseDouble(value);
				out += " " + value + ")";
			} catch (NumberFormatException ex) {
				out += " \"" + value + "\")";
			}
			return out;
		}

		public boolean removeNonExistentColumns(DataColumnDefinition[] outputFormat) {
			boolean found = false;
			for (int i = 0; i < outputFormat.length; i++) {
				if (outputFormat[i].equals(column)) {
					found = true;
					break;
				}
			}
			return found;
		}
	}
	
	private static class CompoundCondition extends Condition {
		Condition[] subconditions;
		String[] operators;
		
		public String getCondition() {
			StringBuilder b = new StringBuilder("");
			for (int i = 0; i < subconditions.length; i++) {
				if (i != 0) {
					if (operators[i - 1].equals("or")) {
						b.append(" || ");
					} else if (operators[i - 1].equals("and")) {
						b.append(" && ");
					}
				}
				if (subconditions[i] instanceof CompoundCondition && subconditions.length != 1) {
					b.append("(").append(subconditions[i].getCondition()).append(")");
				} else {
					b.append(subconditions[i].getCondition());
				}
			}
			return b.append("").toString();
		}
		
		public String toString() {
			if (subconditions.length == 1) {
				return subconditions[0].toString();
			} else {
				String out = "";
				for (int i = 0; i < subconditions.length; i++) {
					if (i != 0) {
						out += " " + operators[i - 1] + " ";
					}
					if (subconditions[i] instanceof CompoundCondition) {
						out += "(" + subconditions[i].toString() + ")";
					} else {
						out += subconditions[i].toString();
					}
				}
				return out;
			}
		}

		public boolean removeNonExistentColumns(DataColumnDefinition[] outputFormat) {
			int removedCnt = 0;
			for (int i = 0; i < subconditions.length; i++) {
				if (!subconditions[i].removeNonExistentColumns(outputFormat)) {
					subconditions[i] = null;
					removedCnt++;
				}
			}
			if (removedCnt != 0) {
				int taken = 0;
				Condition[] cNew = new Condition[subconditions.length - removedCnt];
				for (int i = 0; i < subconditions.length; i++) {
					if (subconditions[i] != null) {
						cNew[i - taken] = subconditions[i];
					} else {
						taken++;
					}
				}
				subconditions = cNew;
			}
			return subconditions.length != 0;
		}
		
	}
	
	private static class EmptyCondition extends Condition {
		public String getCondition() {
			throw new RuntimeException("Should not happen.");
		}

		public boolean removeNonExistentColumns(DataColumnDefinition[] outputFormat) {
			return true;
		}
		
		public String toString() {
			return "";
		}
	}
	
private static class Tokenizer {
		
		private String input;
		private int position;
		
		public Tokenizer(String string) {
			this.input = string.trim();
			this.position = 0;
		}
		
		public String getNextToken() {
			if (position >= input.length()) {
				return null;
			}
			while (position < input.length() && Character.isWhitespace(input.charAt(position))) {
				position++;
			}
			char symbol = input.charAt(position);
			position++;
			if (isOpenParenthesis(symbol)) {
				return "(";
			} else if (isQuotation(symbol)) {
				int lookPosition = position;
				while (lookPosition < input.length() && !isQuotation(input.charAt(lookPosition))) {
					lookPosition++;
				}
				String token = input.substring(position, lookPosition);
				position = lookPosition + 1;
				return token;
			} else if (isCloseParenthesis(symbol)) {
				return ")";
			} else if (isOperator(symbol)) {
				int lookPosition = position;
				while (lookPosition < input.length() && isOperator(input.charAt(lookPosition))) {
					lookPosition++;
				}
				String operator = input.substring(position - 1, lookPosition);
				position = lookPosition;
				return operator;
			} else {
				//read full token
				int lookPoistion = position;
				while (lookPoistion < input.length() && !isBreak(input.charAt(lookPoistion))) {
					lookPoistion++;
				}
				String token = input.substring(position - 1, lookPoistion);
				position = lookPoistion;
				return token;
			}
		}
		
		private boolean isBreak(char c) {
			return isOpenParenthesis(c) || isCloseParenthesis(c) || isQuotation(c) || isWhitespace(c) || isOperator(c);
		}
		
		private boolean isOperator(char c) {
			return c == '=' || c == '<' || c == '>' || c =='!';
		}

		private boolean isOpenParenthesis(char c) {
			return c == '(';
		}
		
		private boolean isCloseParenthesis(char c) {
			return c == ')';
		}
		
		private boolean isQuotation(char c) {
			 return c == '"';
		}
		
		private boolean isWhitespace(char c) {
			return Character.isWhitespace(c);
		}
	}



	
	private ScriptEvaluator scriptedFilter;
	private Condition parsedCondition;
	
	public Filter(String filter, DataColumnDefinition[] columns) throws RJException {
		parsedCondition = parse(filter, columns);
		compileFilter();
		
	}

	private void compileFilter() throws RJException {
		try {
			if (parsedCondition != null && !(parsedCondition instanceof EmptyCondition)) {
				scriptedFilter = new ScriptEvaluator("return " + parsedCondition.getCondition() + ";", Boolean.TYPE, new String[] {"row"}, new Class[] {DataRow.class});
			} else {
				scriptedFilter = null;
			}
			
		} catch (Exception e) {
			Log.log(getClass(), "Error in the following generated filter rule: " + parsedCondition.getCondition());
			throw new RJException("Error occured when compiling filter rule.", e);
		}
	}
	
	private static Condition parse(String filter, DataColumnDefinition[] columns) throws RJException {
		
		Tokenizer tokenizer = new Tokenizer(filter);
		String token;
		Stack stack = new Stack();
		Condition currentCond = new EmptyCondition();
		String logicalOp = null;
		boolean lastTokenFinishing = false;
		while ((token = tokenizer.getNextToken()) != null) {
			//process token
			if (token.equals("(")) {
				stack.push(currentCond);
				currentCond = new EmptyCondition();
			} else if (token.equals(")")) {
				Condition old = (Condition) stack.pop();
				currentCond = old.merge(currentCond, logicalOp);
				lastTokenFinishing = true;
			} else {
				if (lastTokenFinishing) {
					logicalOp = token;
					if (!logicalOp.equals("and") && !logicalOp.equals("or")) {
						throw new RJException("Poorly formed filter expression around " + token + ". Only 'or' and 'and' logical operations are allowed.");
					}
					lastTokenFinishing = false;
				} else {
					//read simple condition
					String column = token;
					String operator = tokenizer.getNextToken();
					String val = tokenizer.getNextToken();
					if (operator == null || val == null) {
						throw new RJException("Poorly formed filter expression around " + token);
					}
					
					int columnId = -1;
					for (int i = 0; i < columns.length; i++) {
						if (columns[i].getColumnName().equals(column)) {
							columnId = i;
							break;
						}
					}
					if (columnId == -1) {
						throw new RJException("Filter expression uses non-existent attribute: " + column + ".");
					}
					
					SimpleCondition cond = new SimpleCondition();
					cond.column = columns[columnId];
					cond.condition = operator;
					cond.value = val;
					if ((operator.indexOf("<") != -1 || operator.indexOf(">") != -1)) {
						try {
							Double.parseDouble(val);
						} catch (NumberFormatException e) {
							throw new RJException("Operator '" + operator + "' can be applied only to a numeric value.");
						}
					}
					currentCond = currentCond.merge(cond, logicalOp);
					lastTokenFinishing = true;
				}
			}
		}
		
		if (!stack.isEmpty()) {
			throw new RJException("Poorly formed filter condition. Probably missing parenthesis or quotation mark.");
		}
		
		return currentCond;
	}

	public boolean isSatisfied(DataRow row) throws RJException {
		try {
			if (scriptedFilter == null) {
				return true;
			}
			Object o = scriptedFilter.evaluate(new Object[] {row});
			Boolean b = (Boolean)o;
			return b.booleanValue();
		} catch (InvocationTargetException e) {
			throw new RJException("Error occured when evaluating filter rule.", e);
		}
	}
	
	public String toString() {
		return parsedCondition.toString();
	}

	public void removeNonExistentColumns(DataColumnDefinition[] outputFormat) throws RJException {
		//remove
		if (!parsedCondition.removeNonExistentColumns(outputFormat)) {
			parsedCondition = new EmptyCondition();
		}
		parsedCondition = parse(parsedCondition.toString(), outputFormat);
		
		//recompile
		compileFilter();
		
	}

	public boolean isEmpty() {
		return this.parsedCondition instanceof EmptyCondition;
	}
	
}
