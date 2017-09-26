package propertyusage.handlers;

import java.util.HashSet;
import java.util.Set;

public class PropertySet {
	
	private Set<PropertyInstance> propertySet;

	private static PropertySet ps = new PropertySet();

	public static PropertySet getInstance() {
		return ps;
	}
	
	private PropertySet() {
		super();
		this.propertySet = new HashSet<PropertyInstance>();
	}

	public Boolean add(PropertyInstance p) {
		return this.propertySet.add(p);
	}
	
	public Set<PropertyInstance> getProperty(String name) {
		Set<PropertyInstance> result = new HashSet<PropertyInstance>();
		for (PropertyInstance p: propertySet) {
			if (p.getFileName().equals(name)) {
				result.add(p);
			}
		}
		return result;
	}
	
	public int size() {
		return propertySet.size();
	}

}
