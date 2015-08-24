function dropAll() {
    db.queue.drop();
    db.friends.drop();
};

function init() {
    //db.tasks_frequency.insert({"type":"friends_list", "freq":NumberLong(3*60*60*1000)}); //initially every 3 hours
    //2763114
    db.queue.insert({id:NumberLong(2763114)})
    db.queue.createIndex({"id":1})
    db.queue.createIndex({"lastUseDate":1})
}

function past(m) {
    var dt = new Date(); 
    return new Date(dt.setMinutes(dt.getMinutes() - m));
}
