function dropAll() {
    db.friends_list.drop();
    db.tasks.drop();
    db.task_statistics.drop();
    db.friends_raw.drop();
    db.friends_dynamic.drop();
    db.friends_raw_tmp.drop();
};

function init() {
    db.first_man.remove({});
    db.first_man.insert({"uid":NumberLong(2763114)});
    db.first_man.insert({start:new Date()});
    db.friends_list.createIndex({uid:1});
}

function past(m) {
    var dt = new Date(); 
    return new Date(dt.setMinutes(dt.getMinutes() - m));
}
