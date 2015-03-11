Database description
====================

first_man
---------
collection consist of only one record with only one field - id of the man from wich crawling is started.
{
    uid:557323
}

friends_list
------------
Current state of social graph.
{
    uid: id of user (int)
    friends:list of ids of friends (array of ints)
}

friends_dynamic
---------------
Changing of social graph throught the time.

{
    uid: id of the user (int),
    friendsAdded: new adeed friends (array of ints),
    friendsDeleted: deleted friends (array of ints),
    timestamp: date of processing (ISODate)
}

friends_raw
-----------
Collection for unprocessed raw data from crwaler agents
{
    friends: current friends list (array of ints)
}

tasks
-----
Table with tasks for agents
{
    type: type of the task (string),
    data: some task specific data (object),
    usecount: count of quering this task by agents, tag field to sort tasks (int),
    createDate: date of creation (ISODate),
    lastUse: date of the last quering (ISODate)
}

task_statistics
---------------
Here lies meta information about results of processing of the stasks

{
    type: type of the task (string),
    createDate: date of creation (ISODate),
    lastUseDate: date of the last quering (ISODate),
    processDate: date of the last processing (ISODate)    
}