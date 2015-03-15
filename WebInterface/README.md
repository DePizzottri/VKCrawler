Web interface for crawler agents
================================

Get task example
/getTask?version=1&types=raw,friends_list

Task JSON object in the response body

Put task example
/postTask

Task results JSON object in the body in the form
```
FriendsListTaskResult
{
    task: task_statistics,
    friends: [friends_raw]
}
```