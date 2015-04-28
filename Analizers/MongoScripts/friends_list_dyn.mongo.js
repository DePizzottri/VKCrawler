//friends dynamic
//naive avg users per second
//
db.friends_dynamic.aggregate([
    {$group:{
        _id:{
            month:{$month:"$processDate"},
            dayOfMonth:{$dayOfMonth:"$processDate"},
            hour:{$hour:"$processDate"},
            minute:{$minute:"$processDate"},
            second:{$second:"$processDate"}
        },
        count:{$sum:1}
    }},
    {$group:{
        _id:null,
        avgPerSecond:{$avg:"$count"}, 
    }}
])


//friends dynamic time series
db.friends_dynamic.aggregate([
    {$group:{
        _id:{
            month:{$month:"$processDate"},
            dayOfMonth:{$dayOfMonth:"$processDate"},
            hour:{$hour:"$processDate"},
            minute:{$minute:"$processDate"},
            second:{$second:"$processDate"}
        },
        count:{$sum:1}
    }}
],
{allowDiskUse : true}
)

//most popular name
db.friends_list.aggregate([
    {$group:{_id:"$firstName", count:{$sum:1}}},
    {$sort:{count:-1}},
    {$limit:10}
])

//more then once added
//aka ping-pong
//i.e. it compares all added friends as Set and plain array 
//v2 removed one unwind and one group
db.friends_dynamic.aggregate([
    {$project:{_id:1, uid:1, friendsAdded:1}},
    {$unwind:"$friendsAdded"},
    {$group:{_id:"$uid", addedSet:{$addToSet:"$friendsAdded"}, addedAll:{$push:"$friendsAdded"}}},
    {$project:{_id:1, allsz:{$size:"$addedAll"}, setsz:{$size:"$addedSet"}}},
    {$project:{_id:1, setsz:1, allsz:1, cmp:{$cmp:["$setsz", "$allsz"]}}},
    {$match:{cmp:{$ne:0}}},
    {$sort:{setsz:-1}}
],
{allowDiskUse : true})

//most friended with names
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
