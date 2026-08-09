package spec.com.example;

public class CalculatorSpec extends CalculatorSpecSupport {
    public void it_adds_two_numbers() {
        add(2, 3).shouldReturn(5);
    }
}
