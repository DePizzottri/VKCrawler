//most friended
db.friends_list.aggregate([
    {$unwind:"$friends"},
    {$group:{_id:"$uid", sz:{$sum:1}}},
    {$sort:-1}
])

//most friended
db.friends_list.aggregate([
    {$unwind:"$friends"},
    {$group:{_id:{uid:"$uid", fn:"$firstName", ln:"$lastName"}, sz:{$sum:1}}},
    {$sort:{sz:-1}}
],
{allowDiskUse:true}
)

//get last processed dynamics
db.friends_dynamic.aggregate([
    {$sort:{processDate:1}},
    {$group:{_id:"$uid", processDate:{$last:"$processDate"}}},
])

//count of added friends (as set) sorted by count desc 
db.friends_dynamic.aggregate([
    {$project:{_id:1, uid:1, friendsAdded:1}},
    {$unwind:"$friendsAdded"},
    {$group:{_id:"$uid", addedSet:{$addToSet:"$friendsAdded"}}},
    {$unwind:"$addedSet"},
    {$group:{_id:"$_id", count:{$sum:1}}},
    {$sort:{count:-1}}
])


//count of added friends sorted by count desc
db.friends_dynamic.aggregate([
    {$project:{_id:1, uid:1, friendsAdded:1}},
    {$unwind:"$friendsAdded"},
    {$group:{_id:"$uid", count:{$sum:1}}},
    {$sort:{count:-1}}
])

//count of deleted friends sorted by count desc
db.friends_dynamic.aggregate([
    {$project:{_id:1, uid:1, friendsDeleted:1}},
    {$unwind:"$friendsDeleted"},
    {$group:{_id:"$uid", count:{$sum:1}}},
    {$sort:{count:-1}}
])

//total count of added users
db.friends_dynamic.aggregate([
    {$project:{_id:1, uid:1, friendsAdded:1}},
    {$unwind:"$friendsAdded"},
    {$group:{_id:null, count:{$sum:1}}},
])

//total count of deleted users
db.friends_dynamic.aggregate([
    {$project:{_id:1, uid:1, friendsDeleted:1}},
    {$unwind:"$friendsDeleted"},
    {$group:{_id:null, count:{$sum:1}}},
])
