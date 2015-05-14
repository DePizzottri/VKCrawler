Database description
====================

user_info
------------
Static user information
```
{
    uid: id of user (longs)
    friends: list of ids of friends (array of longs)
    firstName: string
    lastName: string
    birthday: object | null
    city: int 
    interests: string | null
    sex: 0/1
    processDate: ISODate
}
```

graph
---------------
Current state of social graph
```
{
    uid: id of user (longs)
    friends: list of ids of friends (array of longs)
}
```

tasks
-----
Table with tasks for agents
```
{
    type: type of the task (string),
    data: task specific data (object),
    createDate: date of creation (ISODate),
    lastUseDate: date of the last quering (ISODate)
}
```

Data on message broker
-----------
Raw data from crawler agents
```
{
    uid: int
    friends: current friends list (array of obj)
        {
            uid
            city
        }
    firstName: string
    lastName: string
    birthday: object | null
    city: int 
    processDate: ISODate
}
```
