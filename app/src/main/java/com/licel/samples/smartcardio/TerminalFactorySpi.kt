package com.licel.samples.smartcardio

/**
 * The TerminalFactorySpi class defines the service provider interface.
 * Applications do not access this class directly, instead see
 * [TerminalFactory].
 *
 * <P>Service providers that want to write a new implementation should define
 * a concrete subclass of TerminalFactorySpi with a constructor that takes
 * an `Object` as parameter. That class needs to be registered
 * in a [java.security.Provider]. The engine
 * [type][java.security.Provider.Service.getType] is
 * `TerminalFactory`.
 * Service providers also need to implement subclasses of the abstract classes
 * [CardTerminals], [CardTerminal], [Card],
 * and [CardChannel].
 *
</P> *
 * For example:
 * <pre>*file MyProvider.java:*
 *
 * package com.somedomain.card;
 *
 * import java.security.Provider;
 *
 * public class MyProvider extends Provider {
 * public MyProvider() {
 * super("MyProvider", 1.0d, "Smart Card Example");
 * put("TerminalFactory.MyType", "com.somedomain.card.MySpi");
 * }
 * }
 *
 * *file MySpi.java*
 *
 * package com.somedomain.card;
 *
 * import javax.smartcardio.*;
 *
 * public class MySpi extends TerminalFactoySpi {
 * public MySpi(Object parameter) {
 * // initialize as appropriate
 * }
 * protected CardTerminals engineTerminals() {
 * // add implementation code here
 * }
 * }
</pre> *
 *
 * @see TerminalFactory
 *
 * @see java.security.Provider
 *
 *
 * @since   1.6
 * @author  Andreas Sterbenz
 * @author  JSR 268 Expert Group
 */
abstract class TerminalFactorySpi
/**
 * Constructs a new TerminalFactorySpi object.
 *
 *
 * This class is part of the service provider interface and not accessed
 * directly by applications. Applications
 * should use TerminalFactory objects, which can be obtained by calling
 * one of the
 * [TerminalFactory.getInstance()][TerminalFactory.getInstance]
 * methods.
 *
 *
 * Concrete subclasses should define a constructor that takes an
 * `Object` as parameter. It will be invoked when an
 * application calls one of the [ TerminalFactory.getInstance()][TerminalFactory.getInstance] methods and receives the `params`
 * object specified by the application.
 */
protected constructor() {
    /**
     * Returns the CardTerminals created by this factory.
     *
     * @return the CardTerminals created by this factory.
     */
    protected abstract fun engineTerminals(): CardTerminals?
}