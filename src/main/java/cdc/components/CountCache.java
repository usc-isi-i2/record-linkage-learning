package cdc.components;

import java.util.HashMap;
import java.util.Map;

import cdc.utils.Log;

public class CountCache {
	
	private Map map = new HashMap();
	
	public synchronized void increment(int key) {
		Integer keyI = new Integer(key);
		Entry e = (Entry) map.get(keyI);
		if (e == null) {
			e = new Entry();
			map.put(keyI, e);
		} else {
			Log.log(getClass(), "increment: Key " + key + " found. Old count: " + e.cnt, 2);
		}
		e.cnt++;
	}
	
	public synchronized int get(int key) {
		Integer keyI = new Integer(key);
		Entry e = (Entry) map.get(keyI);
		if (e == null) {
			return 0;
		} else {
			Log.log(getClass(), "get: Key " + key + " found. Old count: " + e.cnt, 2);
			return e.cnt;
		}
	}
	
	public synchronized int decrement(int key) {
		Integer keyI = new Integer(key);
		Entry e = (Entry) map.get(keyI);
		if (e == null) {
			return 0;
		} else {
			Log.log(getClass(), "decrement: Key " + key + " found. Old count: " + e.cnt, 2);
			e.cnt--;
			if (e.cnt == 0) {
				map.remove(keyI);
			}
			return e.cnt;
		}
	}
	
	public void reset() {
		map.clear();
	}
	
	private class Entry {
		int cnt = 0;
	}
	
}
