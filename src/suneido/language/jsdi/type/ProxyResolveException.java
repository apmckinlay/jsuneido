package suneido.language.jsdi.type;

final class ProxyResolveException extends Exception {

	//
	// SERIALIZATION
	//

	/**
	 * 
	 */
	private static final long serialVersionUID = 7310351403974056739L;

	//
	// DATA
	//

	private final Proxy proxy;
	private final Class<?> actualType;
	private String memberName;
	private String memberType; // 'parameter' or 'member'
	private String parentName;

	//
	// CONSTRUCTORS
	//

	public ProxyResolveException(Proxy proxy, Class<?> actualType) {
		super("Can't resolve " + proxy.getDisplayName());
		this.proxy = proxy;
		this.actualType = actualType;
		this.memberName = null; // to be filled in higher up the call stack
		this.memberType = null; // likewise
		this.parentName = null; // likewise
	}

	//
	// ACCESSORS
	//

	public final Proxy getProxy() {
		return proxy;
	}

	public final Class<?> getActualType() {
		return actualType;
	}

	public final String getMemberName() {
		return memberName;
	}

	public final String getMessage() {
		if (null == memberName || null == memberType || null == parentName)
			return super.getMessage();
		else {
			StringBuilder result = new StringBuilder(128);
			result.append("Expected underlying type ")
					.append(proxy.getUnderlyingTypeName()).append(" of '")
					.append(parentName).append("' ").append(memberType)
					.append(' ').append(proxy.getDisplayName()).append(' ')
					.append(memberName).append(" to be ")
					.append(Structure.class.getSimpleName()).append(" or ")
					.append(Callback.class.getSimpleName())
					.append(" but it is ").append(actualType.getSimpleName());
			return result.toString();
		}
	}

	//
	// MUTATORS
	//

	final void setMemberName(String memberName) {
		this.memberName = memberName;
	}

	final void setMemberType(String memberType) {
		this.memberType = memberType;
	}

	final void setParentName(String parentName) {
		this.parentName = parentName;
	}
}
