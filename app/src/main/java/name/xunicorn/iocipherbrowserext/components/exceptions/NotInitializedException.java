package name.xunicorn.iocipherbrowserext.components.exceptions;


public class NotInitializedException extends Throwable {

    public NotInitializedException(Class<?> className) {
        super("Class " + className.getSimpleName() + " not initialized");
    }

    public NotInitializedException(String detailMessage) {
        super(detailMessage);
    }
}
