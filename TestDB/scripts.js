//db.tasks.aggregate([{$match:{type:"friends_list"}},{$group:{_id:"$type", tot:{$push:{"data":"$data"}}}}])

//get all uids from tasks
{
var redf = function(k, v) { 
    var ret = []; 
    for(var i in v) { 
        ret = ret.concat(v[i].a) 
    } 
    return {a:ret};
}

var mapf = function() {
    emit(this.type, {a: this.data})
}

db.tasks.mapReduce(mapf, redf, {query:{type:"friends_list"}, out:"tuid"})
}

//get all friends from one city from friends_raw as set
db.friends_raw.aggregate([
    {$unwind:"$friends"},
    {$match:{"friends.city":2}},
    {$group:{_id:null, friends:{$addToSet:"$friends.uid"}}}
    ]).next().friends
    
function dropAll() {
    db.friends_list.drop();
    db.tasks.drop();
    db.task_statistics.drop();
    db.friends_raw.drop();
    db.friends_dynamic.drop();
    db.friends_raw_tmp.drop();
};

function Init()
{
    db.first_man.insert("uid":NumberLong(2763114))
}

db.friends_raw.insert({
    uid: new NumberLong(1),
    friends:[{uid:NumberLong(2), city:2},{uid:NumberLong(3),city:3},{uid:NumberLong(4),city:4}],
    firstName:"fn",
    lastName:"ln",
    birthday: new Date(),
    city: 33,
    processDate: new Date()
});

db.friends_list.insert({
    uid: new NumberLong(1),
    friends:[NumberLong(1), NumberLong(2), NumberLong(3)],
    firstName:"a",
    lastName:"b",
    birthday: new Date(),
    city: 11,
    processDate: new Date()
});