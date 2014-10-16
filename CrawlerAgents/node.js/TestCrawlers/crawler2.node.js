/*
    node.js VK crawler2
    Little smarter: divide huge friend list request into small pieces
    got 50k+ ids
*/

http = require('https')


//maximum depth of crawling
const max_depth = 10
const factor = 10
const ans_len = 80
const friends_limit = ""//"&count=100"

var cache = {}

function parse_friends(json, depth)
{
    if(json.response != null)
    {
        var fids = []
        var cnt = 0
        for (var fid in json.response)
        {
            if(cache[json.response[fid]] != true)
            {
                fids.push(json.response[fid])
            }
            else
                continue
            cnt++;
            if(cnt == factor)
            {   
                get_cities(fids, depth + 1);
                fids = []
                cnt = 0
            }
        }       
        get_cities(fids, depth + 1)
    }
}

function get_friends_list(id, depth)
{
    var url = "https://api.vk.com/method/friends.get?user_id=" + id + friends_limit;

    var req = http.get(url, function (result) {
        var ans = ""
        result
        .on('data', function(chunk) { ans += chunk} )
        .on('end', function() 
        { 
            try
            {
                parse_friends(JSON.parse(ans), depth)
            }
            catch(e)
            {
                console.error("Error parse friends: " + e.message)
                console.error(ans.substring(0, ans_len))
                console.error(id)
            }

        })
        .on('error', function(err) 
        {
            console.error("Error process friends request: " + err);
            console.error(ans.substring(0, ans_len))
            console.error(id);
        });
    });

    req.on('error', function(err) 
    {
        console.error("Error request friens: " + err);
        console.error(id);
    });

}


function parse_cities(json, depth)
{
    if(json.response != null)
    {
        for(var i in json.response)
        {
            var user = json.response[i]
            if(user.city == 148) //148 - Ulan-Ude
            {
                //var obj = {/*city: user.city,*/ _id: user.uid}
                //put in mongodb
                //console.log('db.users.insert(' + JSON.stringify(obj) +  ')'); 
                console.log('{ "_id":' + user.uid + '}')
                cache[user.uid] = true
            }
            get_friends_list(user.uid, depth)
        }
    }
}

function get_cities(ids, depth)
{
    if(depth == max_depth)
        return

    var paramIds = ""
    for (var id in ids)
        paramIds += ids[id] + ","
    
    paramIds = paramIds.substring(0, paramIds.length - 1)
    var url = "https://api.vk.com/method/users.get?user_ids=" + paramIds + "&fields=city"

    var req = http.get(url, function (result) 
    {
        var ans = ""
        result
        .on('data', function(chunk) { ans += chunk} )
        .on('end', function() 
        { 
            try
            {
                parse_cities(JSON.parse(ans), depth)
            }
            catch(e)
            {
                console.error("Error parse cities: " + e.message)
                console.error(ans.substring(0, ans_len))
                console.error(paramIds)
            }
        })
        .on('error', function(err) 
        {
            console.error("Error process cities request: " + err)
            console.error(ans.substring(0, ans_len))
            console.error(paramIds);
        });
    });

    req.on('error', function(err) 
    {
        console.error("Error request cities: " + err)
        console.error(paramIds);
    });
}

//console.log('use VK')
//2763114 BV
get_cities([2763114], 0)