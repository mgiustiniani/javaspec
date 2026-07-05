package spec.com.example;

import com.example.FinalGreeter;
import com.example.GreetingService;
import com.example.StaticFormatter;
import io.github.jvmspec.api.ObjectBehavior;
import io.github.jvmspec.doubles.BytecodeAgentDoubles;
import io.github.jvmspec.doubles.Doubles;
import io.github.jvmspec.doubles.InterfaceDouble;
import io.github.jvmspec.doubles.StaticDouble;

public class GreetingServiceSpec extends ObjectBehavior<GreetingService> {
    public GreetingServiceSpec() {
        super(GreetingService.class);
    }

    public void it_doubles_a_final_class_collaborator() {
        InterfaceDouble<FinalGreeter> greeter = Doubles.concreteDouble(FinalGreeter.class);
        greeter.when("greet", "Ada").thenReturn("agent Ada");

        beConstructedWith(greeter.instance());

        match(subject().greet("Ada")).shouldReturn("agent Ada");
        greeter.verifyCalled("greet", "Ada");
    }

    public void it_intercepts_static_methods_temporarily() {
        beConstructedWith(new FinalGreeter());

        StaticDouble<StaticFormatter> statics = BytecodeAgentDoubles.staticDouble(StaticFormatter.class);
        try {
            statics.when("format", "hello").thenReturn("stubbed hello");

            match(subject().format("hello")).shouldReturn("stubbed hello");
            statics.control().verifyCalled("format", "hello");
        } finally {
            statics.close();
        }

        match(StaticFormatter.format("hello")).shouldReturn("real:hello");
    }
}
