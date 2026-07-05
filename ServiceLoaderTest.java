import io.github.jvmspec.doubles.ConcreteDoubleProvider;
import java.util.ServiceLoader;

public class ServiceLoaderTest {
    public static void main(String[] args) {
        System.out.println("TCCL: " + Thread.currentThread().getContextClassLoader());
        ServiceLoader<ConcreteDoubleProvider> loader =
                ServiceLoader.load(ConcreteDoubleProvider.class,
                        Thread.currentThread().getContextClassLoader());
        for (ConcreteDoubleProvider provider : loader) {
            System.out.println("Found provider: " + provider.getClass().getName());
        }
        System.out.println("Done iterating");
    }
}
