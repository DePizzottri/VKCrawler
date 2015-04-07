function makeid()
{
    var text = "";
    var possible = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    for( var i=0; i < 15; i++ )
        text += possible.charAt(Math.floor(Math.random() * possible.length));

    return text;
}

var friends_raw_tmp = makeid();

function FriendsListRefiner()
{ 
    print("Friends list refiner: started on " + friends_raw_tmp)
    var start = new Date()
    var current_friends = db.friends_list.distinct("uid")
    
    if (current_friends.length == 0)
    { //start from first man
        var fmuid = db.first_man.findOne({}).uid
        db.friends_list.insert({"uid":fmuid})
        current_friends = [fmuid]
        print("First man added")
    }
    
    //move working data    
    {
        var bulkInsert = db[friends_raw_tmp].initializeUnorderedBulkOp()
        var bulkRemove = db.friends_raw.initializeUnorderedBulkOp()
        db.friends_raw.find({}).limit(5000).forEach(
            function(doc){
                bulkInsert.insert(doc);
                bulkRemove.find({_id:doc._id}).removeOne();
            }
        )
        bulkInsert.execute()
        bulkRemove.execute()
    }
    
    var raw_friends_c = db[friends_raw_tmp].aggregate([
        {$unwind:"$friends"},
        {$match:{"friends.city":148}},
        {$group:{_id:null, friends:{$addToSet:"$friends.uid"}}}
        ])
    var raw_friends = []
    
    //check for emptyness
    if(raw_friends_c.hasNext())
        raw_friends = raw_friends_c.next().friends
    
    //find new users
    //raw_friends - current_friends
    
    current_friends = current_friends.map(function(i) {return i.toNumber()})
    var new_users = raw_friends.filter(function(i) {return current_friends.indexOf(i.toNumber()) < 0;});
    
    print("Friends list refiner: find " + new_users.length + " new users");

    //add new users to main friends_list collection
    new_users.forEach(function (uid) {
        db.friends_list.insert({
                "uid":uid
            })
    });
    var newAddedTime = new Date() - start
    start = new Date()
    
    //add friends_dynamic docs
    //and fill main collection 
    var bulkInsertDyn = db.friends_dynamic.initializeUnorderedBulkOp()
    var bulkUpdateList = db.friends_list.initializeUnorderedBulkOp()
    
    print("Update/insert cycle");
    db[friends_raw_tmp].find({}).forEach(
        function(obj) {
            var have = db.friends_list.findOne({"uid":obj.uid}).friends != null
            //var isNew = db.friends_list.findOne({"uid":obj.uid}).firstName == ""
            
            var newFriends = obj.friends.map(function(o) {return o.uid})
            if(have)
            {
                var newFriendsInt = newFriends.map(function(o) {return o.toNumber()})
                var oldFriendsInt = db.friends_list.findOne({"uid":obj.uid}).friends.map(function(o) {return o.toNumber()})

                bulkInsertDyn.insert({
                    "uid":obj.uid,
                    "friendsAdded":newFriendsInt.filter(function(o) {return oldFriendsInt.indexOf(o) < 0}).map(function(o) {return new NumberLong(o)}),
                    "friendsDeleted":oldFriendsInt.filter(function(o) {return newFriendsInt.indexOf(o) < 0}).map(function(o) {return new NumberLong(o)}),
                    "birthday":obj.birthday,
                    "processDate":obj.processDate
                })
            }
        
            bulkUpdateList.find({"uid":obj.uid}).update(
                { $set:{
                    "friends":newFriends,
                    "firstName":obj.firstName,
                    "lastName":obj.lastName,
                    "birthday":obj.birthday,
                    "city":obj.city,
                    "interests":obj.interests
                }}
            )
        }
    )
    
    print("Insert into dynamycs execute");
    bulkInsertDyn.execute();
    print("Update list execute")
    bulkUpdateList.execute();
        
    db[friends_raw_tmp].drop()
    var finished = new Date - start
    print(
        "Friends list refiner: new users time: " + Math.round(newAddedTime/1000).toString() + "\n" +
        "Friends list refiner: update time: " + Math.round(finished/1000).toString() + "\n" +
        "Friends list refiner: full time: " + Math.round((newAddedTime + finished)/1000).toString() + "\n"
    )
}