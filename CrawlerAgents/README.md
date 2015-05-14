## Crawler agents ##

Crawlers can get tasks through the next interfaces:
* Mongo REST API [SleepyMongoose] (https://github.com/10gen-labs/sleepy.mongoose)
* Mongo direct - with appropriate driver
* Original REST API

Crawler agent is an application (or just script) which can:
    * Connect to the predefined web address and get a banch of tasks. Task is simple URL to get content from.
    * Query each URL
    * Send result back

Smart crawler can put some parameters to the query and analize results of query and do some transformations/packaging before send it back.

Task example:

{
    "type": "friends_list"
    "data": [NumberLong(123123)]
    "createDate": ISODate
}

{
    "type": "raw"
    "data": [
            {
                "URL" : "https://api.vk.com/method/users.get?user_id=1",
            },
            {
                "URL" : "https://api.vk.com/method/users.get?user_id=1&count=10&offset=10"
            }
            ]
}
