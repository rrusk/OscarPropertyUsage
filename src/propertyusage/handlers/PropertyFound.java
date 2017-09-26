package propertyusage.handlers;

public class PropertyFound {
	String name;
	Integer numInstances;
	Integer numBooleanInstances;
	
	public PropertyFound(String name, Boolean isBoolean) {
		super();
		this.name = name;
		this.numInstances = 1;
		if (isBoolean) {
			this.numBooleanInstances = 1;
		} else {
			this.numBooleanInstances = 0;
		}
	}
	
	public String getName() {
		return name;
	}

	public Integer getNumInstances() {
		return numInstances;
	}

	public Integer getNumBooleanInstances() {
		return numBooleanInstances;
	}
	
	public void addOne(Boolean isBoolean) {
		this.numInstances++;
		if (isBoolean) {
			this.numBooleanInstances++;
		}
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		PropertyFound other = (PropertyFound) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}
	
	

}
