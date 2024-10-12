package com.elveum.container.subject.factories

import com.elveum.container.subject.FlowSubject
import com.elveum.container.subject.FlowSubjects

public interface FlowSubjectFactory {

    /**
     * Create a new instance of [FlowSubject].
     * You can create your own instance of this factory and
     * make it as a default factory via [FlowSubjects.setDefaultConfiguration]
     */
    public fun <T> create(): FlowSubject<T>

}