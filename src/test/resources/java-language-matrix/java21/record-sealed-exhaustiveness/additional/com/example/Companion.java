package com.example;

sealed interface Event permits Created, Deleted {
}

record Created(String id) implements Event {
}

record Deleted(String id) implements Event {
}
