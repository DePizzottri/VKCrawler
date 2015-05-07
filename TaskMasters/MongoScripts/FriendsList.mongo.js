function FriendsListTaskMaster()
{  
    var current_friends = db.friends_list.distinct("uid")
    
    if (current_friends.length == 0)
    {
        print("No users in friends_list");
        return;
    }
    
    var users_in_tasks_c = db.tasks.aggregate([
        {$match:{type:"friends_list"}},
        {$unwind:"$data"},
        {$group:{_id:null, friends:{$addToSet:"$data"}}}
        ])
    var users_in_tasks = []
    
    //check for emptyness
    if(users_in_tasks_c.hasNext())
        users_in_tasks = users_in_tasks_c.next().friends
    
    //find new users
    //current_friends - users_in_tasks
    users_in_tasks = users_in_tasks.map(function(i) {return i.toNumber()})
    var new_users = current_friends.filter(function(i) {return users_in_tasks.indexOf(i.toNumber()) < 0;});
    
    //add new users to tasks
    //group by ten
    print("Friends list task master: find " + new_users.length + " new users");
    
    data = []
    var acc = []
    for (var i = 0; i<new_users.length; ++i)
    {
        if ((i+1) % 10 == 0)
        {
            data.push(acc)
            acc = []
        }
        
        acc.push(new_users[i]);
    }
    
    if(acc.length != 0)
        data.push(acc);
    
    data.forEach(function(uids) {
        db.tasks.insert(
            {
                "type":"friends_list",
                "data":uids,
                "createDate": new Date()
            }
        )
    })
}