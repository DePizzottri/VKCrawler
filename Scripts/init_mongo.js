db.queue.insert({id:NumberLong(2763114)});
db.queue.createIndex({"id":1});
db.queue.createIndex({"friends_list.lastUseDate":1});
db.queue.createIndex({"wall_posts.lastUseDate":1});
db.queue.createIndex({"user_info.lastUseDate":1});

db.friends.createIndex({"id":1});

db.wall_posts.createIndex({owner_id:1, id:1}, {unique:true});
