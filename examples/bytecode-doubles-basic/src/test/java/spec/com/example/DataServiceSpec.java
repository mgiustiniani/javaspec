package spec.com.example;

import com.example.DataService;
import com.example.DataStore;
import org.javaspec.api.ObjectBehavior;
import org.javaspec.doubles.Doubles;
import org.javaspec.doubles.InterfaceDouble;

public class DataServiceSpec extends ObjectBehavior<DataService> {
    public DataServiceSpec() {
        super(DataService.class);
    }

    public void it_saves_data_using_the_store() {
        InterfaceDouble<DataStore> storeDouble = Doubles.concreteDouble(DataStore.class);
        storeDouble.control().returns("save", true);
        beConstructedWith(storeDouble.instance());
        match(subject().save("item")).shouldReturn(true);
    }

    public void it_returns_not_found_when_store_has_no_entry() {
        InterfaceDouble<DataStore> storeDouble = Doubles.concreteDouble(DataStore.class);
        storeDouble.control().returns("find", null);
        beConstructedWith(storeDouble.instance());
        match(subject().lookup("missing")).shouldReturn("not found");
    }
}
