package propertyusage.handlers;

public class PropertyInstance {
	String name;
	String pkgName;
	String fileName;
	String expression;
	String expressionType;
	String parentStatement;
	String location;
	Boolean isBoolean;


	public PropertyInstance(String name, String pkgName, String fileName, String expression, String expressionType,
			String parentStatement, String location, Boolean isBoolean) {
		super();
		this.name = name;
		this.pkgName = pkgName;
		this.fileName = fileName;
		this.expression = expression;
		this.expressionType = expressionType;
		this.parentStatement = parentStatement;
		this.location = location;
		this.isBoolean = isBoolean;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getPkgName() {
		return pkgName;
	}

	public void setPkgName(String pkgName) {
		this.pkgName = pkgName;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public String getExpression() {
		return expression;
	}

	public void setExpression(String expression) {
		this.expression = expression;
	}

	public String getExpressionType() {
		return expressionType;
	}

	public void setExpressionType(String expressionType) {
		this.expressionType = expressionType;
	}
	

	public String getParentStatement() {
		return parentStatement;
	}

	public void setParentStatement(String parentStatement) {
		this.parentStatement = parentStatement;
	}

	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		this.location = location;
	}


	public Boolean getIsBoolean() {
		return isBoolean;
	}

	public void setIsBoolean(Boolean isBoolean) {
		this.isBoolean = isBoolean;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((expression == null) ? 0 : expression.hashCode());
		result = prime * result + ((expressionType == null) ? 0 : expressionType.hashCode());
		result = prime * result + ((fileName == null) ? 0 : fileName.hashCode());
		result = prime * result + ((isBoolean == null) ? 0 : isBoolean.hashCode());
		result = prime * result + ((location == null) ? 0 : location.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((parentStatement == null) ? 0 : parentStatement.hashCode());
		result = prime * result + ((pkgName == null) ? 0 : pkgName.hashCode());
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
		PropertyInstance other = (PropertyInstance) obj;
		if (expression == null) {
			if (other.expression != null)
				return false;
		} else if (!expression.equals(other.expression))
			return false;
		if (expressionType == null) {
			if (other.expressionType != null)
				return false;
		} else if (!expressionType.equals(other.expressionType))
			return false;
		if (fileName == null) {
			if (other.fileName != null)
				return false;
		} else if (!fileName.equals(other.fileName))
			return false;
		if (isBoolean == null) {
			if (other.isBoolean != null)
				return false;
		} else if (!isBoolean.equals(other.isBoolean))
			return false;
		if (location == null) {
			if (other.location != null)
				return false;
		} else if (!location.equals(other.location))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (parentStatement == null) {
			if (other.parentStatement != null)
				return false;
		} else if (!parentStatement.equals(other.parentStatement))
			return false;
		if (pkgName == null) {
			if (other.pkgName != null)
				return false;
		} else if (!pkgName.equals(other.pkgName))
			return false;
		return true;
	}
}
