package com.goldking.filetracker

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentLinkedDeque

object EventRepository {
    private val maxItems = 200
    private val eventQueue = ConcurrentLinkedDeque<FileAccessEvent>()
    private val _eventsFlow = MutableStateFlow<List<FileAccessEvent>>(emptyList())
    val eventsFlow: StateFlow<List<FileAccessEvent>> = _eventsFlow.asStateFlow()

    private val _isMonitoring = MutableStateFlow(false)
    val isMonitoring: StateFlow<Boolean> = _isMonitoring.asStateFlow()

    fun setMonitoring(active: Boolean) {
        _isMonitoring.value = active
    }

    fun addEvent(event: FileAccessEvent) {
        eventQueue.addFirst(event)
        while (eventQueue.size > maxItems) {
            eventQueue.pollLast()
        }
        _eventsFlow.value = eventQueue.toList()
    }

    fun clearEvents() {
        eventQueue.clear()
        _eventsFlow.value = emptyList()
    }
}
