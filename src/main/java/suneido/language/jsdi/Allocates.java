package suneido.language.jsdi;

/**
 * This annotation flags the code as allocating memory in the DLL (or being
 * attached to allocated memory in the JSDI DLL). All classes tagged with this
 * annotation must have finalizers to ensure that any garbage collection done
 * on the Java side propagates to the native side.
 * @author Victor Schappert
 * @since 20130621
 */
public @interface Allocates {

}