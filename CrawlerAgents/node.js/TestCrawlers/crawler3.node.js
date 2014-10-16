/*
    node.js VK crawler3
    Based on simple event queue
    Never ends
    got 50k- ids
*/

http = require('https')
var util = require('util');
var EventEmitter = require('events').EventEmitter;

function EventQueue(size)
{
    this.buf = [];
    this.size = size;
}

util.inherits(EventQueue, EventEmitter);

EventQueue.prototype.push = function(obj)
{
    this.buf.push(obj);
    if(this.buf.length == this.size)
    {
        this.emit('data', this.buf);
        this.buf = []
    }

    var self = this
    setTimeout(function() {
        self.flush();
    }, 5000);    
}

EventQueue.prototype.flush = function()
{
    if(this.buf.length != 0)
    {
        this.emit('data', this.buf);
        this.buf = []
    }
}

const factor = 50;
const ans_len = 80;
const friends_limit = "";//"&count=100"

var cache = {}

var equeue = new EventQueue(factor);

function parse_friends(json)
{
    if(json.response != null)
    {
        for (var fid in json.response)
        {
            if(cache[json.response[fid]] != true)
            {
                equeue.push(json.response[fid])
            }
            else
            {
                continue
            }
        }       
    }
}

function get_friends_list(id)
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
                parse_friends(JSON.parse(ans))
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


function parse_cities(json)
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
            get_friends_list(user.uid)
        }
    }
}

function get_cities(ids)
{
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
                parse_cities(JSON.parse(ans))
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

equeue.on('data', function(ids){
    get_cities(ids)    
});

//start from the first man
//2763114 BV
equeue.push("557323")
