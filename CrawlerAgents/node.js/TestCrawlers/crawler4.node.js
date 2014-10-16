/*
    node.js VK crawler4
    Based on simple event queue
    Never ends
    Cache directly to mongodb
    Better work with cache
    got 240k for 30min
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
//    setTimeout(function() {
//        self.emit('perfomance',)
//    }, 1000*60);    

}

var parent = {}

EventQueue.prototype.flush = function()
{
    if(this.buf.length != 0)
    {
        this.emit('data', this.buf);
        this.buf = []
    }
    else
    {
        //console.log("No data to flash");
    }
}

const factor = 150;
const ans_len = 80;
const friends_limit = "";//"&count=50"

var count = 0;
var pcount = 0;

var equeue = new EventQueue(factor);

function exists(collection, query, cb)
{
    collection.count(query, function(err, count) 
    {
        if(err == null)
            cb(err, false);
        else
            cb(err, count != 0);   
    });
}

function insertCache(collection, obj, cb)
{
    collection.insert(obj, {}, cb);
}

function parse_friends(json, id, cache)
{
    //console.log("FriendsList: " + json.response.length)
    if(json.response != null)
    {
        json.response.forEach(function(uid)
        {           

            /*
            //var uid = json.response[fid];
            //exists(cache, {"uid":uid}, function(err, res)
            //{
                if(err != null)
                {
                    console.err("Error check cache: " + err);
                    console.err(uid);
                }
            

                if(res == false)
                {*/
                    parent[uid] = id
                    equeue.push(uid);
                //}
            //});
        });
    }
}

function get_friends_list(id, cache)
{
    //console.log("Friends: " + id)

    var url = "https://api.vk.com/method/friends.get?user_id=" + id + friends_limit;

    var req = http.get(url, function (result) {
        var ans = ""
        result
        .on('data', function(chunk) { ans += chunk} )
        .on('end', function() 
        { 
            try
            {
                parse_friends(JSON.parse(ans),id, cache)
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
        console.error("Error request friends: " + err);
        console.error(id);
    });

}


function parse_cities(json, cache)
{
    if(json.response != null)
    {
        //console.log("Parse:" + JSON.stringify(json));
        //for(var i in json.response)
        json.response.forEach(function(user)
        {
            //var user = json.response[i];
            if(user.city == 148) //148 - Ulan-Ude
            {
                //console.log("Parsed: " + user.uid);

                /*
                //exists(cache, {"uid":user.uid}, function(err, res)
                //{
                    if(err != null)
                    {
                        console.log("Error check existing: " + err)
                        return;
                    }
                
                    if(res == true)
                        return;                        
                */
                    insertCache(cache, {uid:user.uid}, function(err, res)
                    {
                        if(err != null)
                        {
                            console.error("Error insert: " + err);
                            console.error(user.uid);
                        }
                        else
                        {
                            var d = new Date()
                            //console.log(d.toTimeString() + user.uid + " " + parent[user.uid])
                            count++;
                        }

                        //console.log("Inserted: " + user.uid);
                        get_friends_list(user.uid, cache);
                    });
                //});
            }
        });
    }
}

function get_cities(ids, cache)
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
                parse_cities(JSON.parse(ans), cache)
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
        console.error("Error request cities: " + err);
        console.error(paramIds);
    });
}

var MongoClient = require('mongodb').MongoClient;

var url = 'mongodb://localhost:27017/VK';

MongoClient.connect(url, function(err, db) 
{
    db.on('error', function(err){
        console.err("DB error: " + err);
    });
    var users = db.collection("users")

    equeue.on('data', function(ids)
    {
        //console.log("Got next ids " + ids.length)
        get_cities(ids, users)    
    });
//2763114 BV
    //equeue.push("2763114")
//start from the first man
    equeue.push("557323")
    equeue.flush()
});