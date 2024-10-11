package com.neshan.neshantask.data.util;

import androidx.lifecycle.Observer;

/**
 * An [Observer] for [Event]s, simplifying the pattern of checking if the [Event]'s content has
 * already been handled.
 *
 * [onEventUnhandledContent] is *only* called if the [Event]'s contents have not been handled.
 */
public class EventObserver<T> implements Observer<Event<T>> {
    private final EventConsumer<T> onEventUnhandledContent;

    public EventObserver(EventConsumer<T> onEventUnhandledContent) {
        this.onEventUnhandledContent = onEventUnhandledContent;
    }

    @Override
    public void onChanged(Event<T> event) {
        if (event != null) {
            T value = event.getContentIfNotHandled();
            if (value != null) {
                onEventUnhandledContent.accept(value);
            }
        }
    }

    // Functional interface for the consumer
    @FunctionalInterface
    public interface EventConsumer<T> {
        void accept(T value);
    }
}