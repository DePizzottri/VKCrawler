function FriendsListRefiner()
{  
    var current_friends = db.friends_list.distinct("uid")
    
    if (current_friends.length == 0)
    { //start from first man
        var fmuid = db.first_man.findOne({}).uid
        db.friends_list.insert({"uid":new NumberLong(fmuid)})
        current_friends = [new NumberLong(fmuid)]
        print("First man added")
    }

    var raw_friends_c = db.friends_raw.aggregate([
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
    
    //print(raw_friends.length)
    //print(current_friends.length)
    current_friends = current_friends.map(function(i) {return i.toNumber()})
    var new_users = raw_friends.filter(function(i) {return current_friends.indexOf(i.toNumber()) < 0;});
    
    //print(raw_friends.length)
    //print(current_friends)
    print("Friends list refiner: find " + new_users.length + " new users");
    //print(new_users);
    //add new users to main friends_list collection
    new_users.forEach(function (uid) {
        //if(db.friends_list.find({"uid":uid}).hasNext())
        //    print("Dublicate");
        
        db.friends_list.insert({
                "uid":uid
            })
    });
    
    //add friends_dynamic docs
}