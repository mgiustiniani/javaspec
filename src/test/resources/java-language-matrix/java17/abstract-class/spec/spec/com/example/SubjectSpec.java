package spec.com.example;

public final class SubjectSpec {
    public void it_does_not_invent_abstract_domain_behavior() {
        addedBehavior().shouldReturn("value");
    }
}
