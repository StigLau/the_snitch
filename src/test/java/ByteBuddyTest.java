import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.implementation.FieldAccessor;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.SuperCall;
import net.bytebuddy.implementation.bind.annotation.This;
import net.bytebuddy.matcher.ElementMatchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class ByteBuddyTest {
    private static final Logger logger = LoggerFactory.getLogger(ByteBuddyTest.class);
    private Logger mockedLogger;

    @BeforeEach
    public void setup() {
        mockedLogger = mock(Logger.class);
    }

    public interface ByteBuddyProxy {
        public Resource getTarget();
        public void setTarget(Resource target);
    }

    public class LoggerInterceptor {
        public void logger(@Origin Method method, @SuperCall Runnable zuper, @This ByteBuddyProxy self) {
            logger.debug("Method {}", method);
            logger.debug("Called on {} ", self.getTarget());
            mockedLogger.info("Called on {} ", self.getTarget());

            /* Proceed */
            zuper.run();
        }
    }

    public static class ResourceComparator {
        public static boolean equalBeans(Object that, @This ByteBuddyProxy self) {
            if (that == self) {
                return true;
            }
            if (!(that instanceof ByteBuddyProxy)) {
                return false;
            }
            Resource someBeanThis = (Resource)self;
            Resource someBeanThat = (Resource)that;
            logger.debug("someBeanThis: {}", someBeanThis.getId());
            logger.debug("someBeanThat: {}", someBeanThat.getId());

            return someBeanThis.getId().equals(someBeanThat.getId());
        }
    }

    public static class Resource {
        private String id;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }
    }

    @Test
    public void useTarget() throws IllegalAccessException, InstantiationException {
        Class<?> dynamicType = new ByteBuddy()
                .subclass(Resource.class)
                .defineField("target", Resource.class, Visibility.PRIVATE)
                .method(ElementMatchers.any())
                .intercept(MethodDelegation.to(new LoggerInterceptor())
                        .andThen(MethodDelegation.toField("target")))
                .implement(ByteBuddyProxy.class)
                .intercept(FieldAccessor.ofField("target"))
                .method(ElementMatchers.named("equals"))
                .intercept(MethodDelegation.to(ResourceComparator.class))
                .make()
                .load(getClass().getClassLoader())
                .getLoaded();

        Resource someBean = new Resource();
        someBean.setId("id-000");
        ByteBuddyProxy someBeanProxied = (ByteBuddyProxy)dynamicType.newInstance();
        someBeanProxied.setTarget(someBean);

        Resource sameBean = new Resource();
        sameBean.setId("id-000");
        ByteBuddyProxy sameBeanProxied = (ByteBuddyProxy)dynamicType.newInstance();
        sameBeanProxied.setTarget(sameBean);

        Resource someOtherBean = new Resource();
        someOtherBean.setId("id-001");
        ByteBuddyProxy someOtherBeanProxied = (ByteBuddyProxy)dynamicType.newInstance();
        someOtherBeanProxied.setTarget(someOtherBean);

        assertEquals(someBean, someBeanProxied.getTarget(), "Target");
        assertNotEquals(someBeanProxied, sameBean, "someBeanProxied is equal to sameBean");
        assertNotEquals(sameBean, someBeanProxied, "sameBean is equal to someBeanProxied");
        assertEquals(someBeanProxied, sameBeanProxied, "sameBeanProxied is not equal to someBeanProxied");
        assertNotEquals(someBeanProxied, someOtherBeanProxied, "someBeanProxied is equal to Some other bean");
        assertNotEquals(null, someBeanProxied, "equals(null) returned true");

        /* Reset counters */
        mockedLogger = mock(Logger.class);
        String id = ((Resource)someBeanProxied).getId();
        @SuppressWarnings("unused")
        String id2 = ((Resource)someBeanProxied).getId();
        @SuppressWarnings("unused")
        String id3 = ((Resource)someOtherBeanProxied).getId();
        assertEquals(someBean.getId(), id, "Id");
        verify(mockedLogger, times(3)).info(any(String.class), any(Resource.class));
    }
}