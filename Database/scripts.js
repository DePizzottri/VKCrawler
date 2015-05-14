function dropAll() {
    db.user_info.drop();
    db.graph.drop();
    db.tasks.drop();
    db.tasks_frequency.drop();
};

function init() {
    db.tasks_frequency.insert({"type":"friends_list", "freq":NumberLong(3*60*60*1000)}); //initially every 3 hours
    //2763114
    db.tasks.insert({
        "type":"friends_list",
        "data":[NumberLong(1)],
        "createDate": new Date()
    })
}

function past(m) {
    var dt = new Date(); 
    return new Date(dt.setMinutes(dt.getMinutes() - m));
}
