/*
    node.js VK crawler1
    got 10k ids
*/

http = require('https')


//maximum depth of crawling
const max_depth = 5

function parse_friends(json, depth)
{
    if(json.response != null)
    {
        var fids = []
        for (var fid in json.response)
        {
            fids.push(json.response[fid])
        }
        get_cities(fids, depth + 1)
    }
}

function get_friends_list(id, depth)
{

    var url = "https://api.vk.com/method/friends.get?user_id=" + id;

    var req = http.get(url, function (result) {
    var ans = ""
    result
    .on('data', function(chunk) { ans += chunk} )
    .on('end', function() { 
        //console.log(ans)
        try{
        parse_friends(JSON.parse(ans), depth)
        }
        catch(e)
        {
            console.error("error parse: " + e.message)
            console.error(ans.substring(0, 50))
        }

    })
    .on('error', function(err) {
        console.error(err);
    });
    });

    req.on('error', function(err) {
    console.error(err);
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
                var obj = {/*city: user.city,*/ _id: user.uid}
                //put in mongodb
                console.log('db.users.insert(' + JSON.stringify(obj) +  ')'); 
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


  var req = http.get(url, function (result) {
    var ans = ""
    result
    .on('data', function(chunk) { ans += chunk} )
    .on('end', function() { 
        //console.log(ans)
        try{
            parse_cities(JSON.parse(ans), depth)
        }
        catch(e)
        {
            console.error("error parse: " + e.message)
            console.error(ans.substring(0, 50))
        }
    })
    .on('error', function(err) {
        console.error(err);
    });
  });

  req.on('error', function(err) {
    console.error(err);
  });
}

console.log('use VK')

//start from the first man
get_cities([2763114], 0)