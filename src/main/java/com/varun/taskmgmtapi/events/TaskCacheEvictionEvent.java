package com.varun.taskmgmtapi.events;

//event is simply a message saying : "task with this ID has been changed. its redis cache needs to be removed
//The event will be published during the database transaction, but handled only after a successful commit.
//now Create the event listener
public record TaskCacheEvictionEvent(Long taskId) {

}
