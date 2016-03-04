package it.unica.co2.api.process;

import java.io.Serializable;
import java.util.function.Consumer;

public interface SerializableConsumer<T> extends Serializable, Consumer<T> {

}
