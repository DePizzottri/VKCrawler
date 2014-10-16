## Crawler agents ##

Crawler agent is an application (or just script) which can:
    * Connect to the predefined web address and get a banch of tasks. Task is simple URL to get content from.
    * Query each URL
    * Send result back

Smart crawler can put some parameters to the query and analize results of query and do some transformations/packaging before send it back.

Task example:
{
    [
        {
            "URL" : "https://api.vk.com/method/users.get?user_id=1",
            "ResultType" : "json"
        },
        {
            "URL" : "https://api.vk.com/method/users.get?user_id=1&count=%1&offset=%2"
            "ResultType" : "json"
            "parameters" : [10, 10]
        }
    ]
}